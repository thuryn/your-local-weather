package org.thosp.yourlocalweather.service;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.AppWakeUpManager;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PermissionUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetService;
import org.thosp.yourlocalweather.widget.LessWidgetService;
import org.thosp.yourlocalweather.widget.MoreWidgetService;

import java.util.Calendar;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LocationUpdateService extends Service implements LocationListener {

    private static final String TAG = "LocationUpdateService";

    private static final long LOCATION_TIMEOUT_IN_MS = 30000L;
    private static final long GPS_LOCATION_TIMEOUT_IN_MS = 30000L;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT = 1500;
    private static final float LENGTH_UPDATE_LOCATION_SECOND_LIMIT = 30000;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION = 200;
    private static final long UPDATE_WEATHER_ONLY_TIMEOUT = 900000; //15 min
    private static final long REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT = 180000; //3 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN = 900000000000l; //15 min
    private static final long ACCELEROMETER_UPDATE_TIME_SECOND_SPAN = 300000000000l; //5 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION = 300000000000l; //5 min

    private PowerManager powerManager;
    private LocationManager locationManager;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private String updateSource;

    private long lastLocationUpdateTime;
    private long lastUpdatedPossition = 0;
    private long lastUpdate = 0;
    private float currentLength = 0;
    private float currentLengthLowPassed = 0;
    private float gravity[] = new float[3];
    private MoveVector lastMovement;

    private SensorEventListener sensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            try {
                Sensor mySensor = sensorEvent.sensor;

                if (mySensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                    return;
                }
                processSensorEvent(sensorEvent);
            } catch (Exception e) {
                appendLog(getBaseContext(), TAG, "Exception on onSensorChanged", e);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "receive intent: " + intent);

            CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
            CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());
            long storedWeatherTime = (weatherRecord != null)?weatherRecord.getLastUpdatedTime():0;
            long now = System.currentTimeMillis();
            appendLog(context, TAG, "SCREEN_ON called, lastUpdate=" +
                    currentLocation.getLastLocationUpdate() +
                    ", now=" +
                    now +
                    ", storedWeatherTime=" +
                    storedWeatherTime);
            if ((now <= (storedWeatherTime + UPDATE_WEATHER_ONLY_TIMEOUT)) || (now <= (currentLocation.getLastLocationUpdate() + REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT))) {
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT - (now - storedWeatherTime));
                return;
            }
            requestWeatherCheck("-");
            timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
        }
    };

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "receive intent: " + intent);
            timerScreenOnHandler.removeCallbacksAndMessages(null);
        }
    };

    Handler timerScreenOnHandler = new Handler();
    Runnable timerScreenOnRunnable = new Runnable() {

        @Override
        public void run() {
            if (!powerManager.isScreenOn()) {
                return;
            }
            CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
            CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

            if (weatherRecord == null) {
                requestWeatherCheck("-");
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
                return;
            }

            long storedWeatherTime = weatherRecord.getLastUpdatedTime();
            long now = System.currentTimeMillis();

            appendLog(getBaseContext(), TAG, "screen timer called, lastUpdate=" +
                    currentLocation.getLastLocationUpdate() +
                    ", now=" +
                    now +
                    ", storedWeatherTime=" +
                    storedWeatherTime);

            if ((now <= (storedWeatherTime + UPDATE_WEATHER_ONLY_TIMEOUT)) || (now <= (currentLocation.getLastLocationUpdate() + REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT))) {
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT);
                return;
            }
            requestWeatherCheck("-");
            timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        currentLength = 0;
        currentLengthLowPassed = 0;
        lastUpdate = 0;
        lastUpdatedPossition = 0;
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        
        if (intent == null) {
            return ret;
        }

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():" + intent.getAction());
        if ("android.intent.action.START_SENSOR_BASED_UPDATES".equals(intent.getAction())) {
            if (senSensorManager != null) {
                return ret;
            }
            appendLog(getBaseContext(), TAG, "START_SENSOR_BASED_UPDATES recieved");
            senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            appendLog(getBaseContext(), TAG, "Selected accelerometer sensor:" + senAccelerometer);
            senSensorManager.registerListener(sensorListener, senAccelerometer , SensorManager.SENSOR_DELAY_FASTEST);
            IntentFilter filterScreenOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
            IntentFilter filterScreenOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            getApplication().registerReceiver(screenOnReceiver, filterScreenOn);
            getApplication().registerReceiver(screenOffReceiver, filterScreenOff);
            return START_STICKY;
        }

        if ("android.intent.action.STOP_SENSOR_BASED_UPDATES".equals(intent.getAction())) {
            if (senSensorManager == null) {
                return ret;
            }
            appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
            getApplication().unregisterReceiver(screenOnReceiver);
            getApplication().unregisterReceiver(screenOffReceiver);
            senSensorManager.unregisterListener(sensorListener);
            senSensorManager = null;
            senAccelerometer = null;
            return ret;
        }

        if ("android.intent.action.LOCATION_UPDATE".equals(intent.getAction()) && (intent.getExtras() != null)) {
            Location inputLocation = (Location) intent.getExtras().getParcelable("inputLocation");
            Address addresses = (Address) intent.getExtras().getParcelable("addresses");
            appendLog(getBaseContext(), TAG, "LOCATION_UPDATE recieved:" + inputLocation + ":" + addresses);
            onLocationChanged(inputLocation, addresses);
            return ret;
        }

        if ("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE".equals(intent.getAction()) && (intent.getExtras() != null)) {

            boolean isGPSEnabled = AppPreference.isGpsEnabledByPreferences(this) &&
                    locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                    && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                    && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            updateSource = intent.getStringExtra("updateSource");
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "-");
            AppWakeUpManager.getInstance(getBaseContext()).wakeUp();

            boolean isUpdateOfLocationEnabled = AppPreference.isUpdateLocationEnabled(this, currentLocation);
            appendLog(this, TAG, "START_LOCATION_AND_WEATHER_UPDATE, isUpdateOfLocationEnabled=" +
                                                isUpdateOfLocationEnabled +
                                                ", isGPSEnabled=" +
                                                isGPSEnabled +
                                                ", isNetworkEnabled=" +
                                                isNetworkEnabled);
            String geocoder = AppPreference.getLocationGeocoderSource(this);
            if (isUpdateOfLocationEnabled && (isGPSEnabled || isNetworkEnabled || !"location_geocoder_system".equals(geocoder))) {
                appendLog(getBaseContext(), TAG, "Widget calls to update location, geocoder = " + geocoder);
                if ("location_geocoder_unifiednlp".equals(geocoder) || "location_geocoder_local".equals(geocoder)) {
                    updateNetworkLocation(false);
                } else {
                    detectLocation();
                }
            } else {
                requestWeatherCheck("-");
            }
        }
        
        return ret;
    }

    @Override
    public void onLocationChanged(Location location) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        locationsDbHelper.updateLocationSource(currentLocation.getId(), "G");
        onLocationChanged(location, null);
    }
    
    public void onLocationChanged(Location location, Address address) {
        AppWakeUpManager.getInstance(getBaseContext()).wakeDown();
        lastLocationUpdateTime = System.currentTimeMillis();
        timerHandler.removeCallbacksAndMessages(null);
        removeUpdates(this);

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        /*org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        float storedLocationAccuracy = autoLocation.getAccuracy();
        long storedLocationTime = autoLocation.getLastLocationUpdate();

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MILLISECOND, -300000);

        // remove acccuracy checking to get fast responses
        if ((storedLocationTime > now.getTimeInMillis()) && (location != null) && (location.getAccuracy() > storedLocationAccuracy)) {
            appendLog(getBaseContext(), TAG, "stored location is recent and more accurate, stored location accuracy = " +
                    storedLocationAccuracy + ", location accuracy =" + ((location != null)?location.getAccuracy():"") +
                    ", stored location time = " + storedLocationTime + ", location time" + ((location != null)?location.getTime():""));
            locationDbHelper.updateLocationSource(currentLocation.getId(), locationSource);
            requestWeatherCheck();
            return;
        }*/

        if(location == null) {
            gpsRequestLocation();
            return;
        }

        String updateDetailLevel = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString(
                Constants.KEY_PREF_UPDATE_DETAIL, "preference_display_update_nothing");

        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);

        if ("gps".equals(location.getProvider())) {
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "G");
        } else if (updateDetailLevel.equals("preference_display_update_location_source")) {
            StringBuilder networkSourceBuilder = new StringBuilder();
            networkSourceBuilder.append("N");
            boolean additionalSourceSetted = false;

            if ((location.getExtras() != null) && (location.getExtras().containsKey("source"))) {
                String networkSource = location.getExtras().getString("source");
                if (networkSource != null) {
                    if (networkSource.contains("cells")) {
                        networkSourceBuilder.append("c");
                        additionalSourceSetted = true;
                    }
                    if (networkSource.contains("wifis")) {
                        networkSourceBuilder.append("w");
                        additionalSourceSetted = true;
                    }
                }
            }
            if (!additionalSourceSetted) {
                networkSourceBuilder.append(location.getProvider().substring(0, 1));
            }
            String updateSource = networkSourceBuilder.toString();
            appendLog(getBaseContext(), TAG, "send update source to " + updateSource);
            locationsDbHelper.updateLocationSource(currentLocation.getId(), updateSource);
        } else if ("-".equals(currentLocation.getLocationSource())) {
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "N");
        }
        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        locationsDbHelper.updateAutoLocationGeoLocation(location.getLatitude(), location.getLongitude(), currentLocation.getLocationSource(), location.getAccuracy(), getLocationTimeInMilis(location));
        appendLog(getBaseContext(), TAG, "put new location from location update service, latitude=" + location.getLatitude() + ", longitude=" + location.getLongitude());
        if (address != null) {
            locationsDbHelper.updateAutoLocationAddress(PreferenceUtil.getLanguage(getBaseContext()), address);
        } else {
            String geocoder = AppPreference.getLocationGeocoderSource(this);
            boolean resolveAddressByOS = !("location_geocoder_unifiednlp".equals(geocoder) || "location_geocoder_local".equals(geocoder));
            locationsDbHelper.setNoLocationFound(getBaseContext());
            Utils.getAndWriteAddressFromGeocoder(new Geocoder(this, new Locale(PreferenceUtil.getLanguage(this))),
                    address,
                    location.getLatitude(),
                    location.getLongitude(),
                    resolveAddressByOS,
                    this);
        }
        appendLog(getBaseContext(), TAG, "send intent to get weather, updateSource " + currentLocation.getLocationSource());
        sendIntentToGetWeather(currentLocation);
    }

    Handler lastKnownLocationTimerHandler = new Handler();
    Runnable lastKnownLocationTimerRunnable = new Runnable() {

        @Override
        public void run() {
            appendLog(getBaseContext(), TAG, "send update source to N - update location by network, lastKnownLocation timeouted");
            updateNetworkLocationByNetwork(null, false);
        }
    };

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            requestWeatherCheck("-");
        }
    };

    Handler timerHandlerGpsLocation = new Handler();
    Runnable timerRunnableGpsLocation = new Runnable() {

        @Override
        public void run() {
            locationManager.removeUpdates(gpsLocationListener);
            setNoLocationFound();
            stopRefreshRotation();
        }
    };

    final LocationListener gpsLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            locationManager.removeUpdates(gpsLocationListener);
            timerHandlerGpsLocation.removeCallbacksAndMessages(null);
            Intent sendIntent = new Intent("android.intent.action.START_LOCATION_UPDATE");
            if ("location_geocoder_unifiednlp".equals(AppPreference.getLocationGeocoderSource(getBaseContext()))) {
                sendIntent.setPackage("org.microg.nlp");
            } else {
                sendIntent.setPackage("org.thosp.yourlocalweather");
            }
            sendIntent.putExtra("destinationPackageName", "org.thosp.yourlocalweather");
            sendIntent.putExtra("inputLocation", location);
            sendIntent.putExtra("resolveAddress", true);
            startService(sendIntent);
            appendLog(getBaseContext(), TAG, "send intent START_LOCATION_UPDATE:locationSource G:" + sendIntent);
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "G");
            timerHandler.postDelayed(timerRunnable, LOCATION_TIMEOUT_IN_MS);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {
            locationManager.removeUpdates(gpsLocationListener);
        }
    };
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        removeUpdates(this);
    }

    private void gpsRequestLocation() {
        boolean isGPSEnabled = AppPreference.isGpsEnabledByPreferences(getBaseContext()) &&
                locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Looper locationLooper = Looper.myLooper();
            appendLog(getBaseContext(), TAG, "get location from GPS");
            timerHandlerGpsLocation.postDelayed(timerRunnableGpsLocation, GPS_LOCATION_TIMEOUT_IN_MS);
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, gpsLocationListener, locationLooper);
        }
    }

    private void setNoLocationFound() {
        final LocationsDbHelper locationDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long lastLocationUpdate = locationDbHelper.getLastUpdateLocationTime();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -5);
        if (lastLocationUpdate > now.getTimeInMillis()) {
            return;
        }
        locationDbHelper.setNoLocationFound(this);
        updateWidgets();
    }

    private boolean updateNetworkLocation(boolean bylastLocationOnly) {

        if (!PermissionUtil.checkPermissionsAndSettings(this)) {
            return false;
        }
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean isGPSEnabled = AppPreference.isGpsEnabledByPreferences(getBaseContext()) &&
                locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        String geocoder = AppPreference.getLocationGeocoderSource(getBaseContext());
        boolean networkNotEnabled = !isNetworkEnabled && "location_geocoder_system".equals(geocoder);

        if (networkNotEnabled && isGPSEnabled && !bylastLocationOnly) {
            startRefreshRotation();
            gpsRequestLocation();
            return true;
        }
        AppWakeUpManager.getInstance(getBaseContext()).wakeUp();
        startRefreshRotation();
        try {

            Location lastLocation = null;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                lastKnownLocationTimerHandler.postDelayed(lastKnownLocationTimerRunnable, LOCATION_TIMEOUT_IN_MS);
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                lastKnownLocationTimerHandler.removeCallbacksAndMessages(null);
            }
            return updateNetworkLocationByNetwork(lastLocation, bylastLocationOnly);
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception during update of network location", e);
        }
        return false;
    }

    private void startRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.START_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
    }

    private void stopRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.STOP_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
    }

    private boolean updateNetworkLocationByNetwork(Location lastLocation,
                                                   boolean bylastLocationOnly) {
        Intent sendIntent = new Intent("android.intent.action.START_LOCATION_UPDATE");
        sendIntent.putExtra("destinationPackageName", "org.thosp.yourlocalweather");

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -5);

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        long lastLocationUpdate = autoLocation.getLastLocationUpdate();
        if ("location_geocoder_unifiednlp".equals(AppPreference.getLocationGeocoderSource(this))) {
            sendIntent.setPackage("org.microg.nlp");
        } else {
            sendIntent.setPackage("org.thosp.yourlocalweather");
        }

        long gpsLastLocationTime = getLocationTimeInMilis(lastLocation);
        appendLog(getBaseContext(), TAG, "Comparison of last location from GPS time = " +
                gpsLastLocationTime +
                ", and location last update time = " +
                lastLocationUpdate);
        if ((lastLocation != null) && gpsLastLocationTime > lastLocationUpdate) {
            sendIntent.putExtra("inputLocation", lastLocation);
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "G");
        } else if (bylastLocationOnly) {
            return false;
        }

        sendIntent.putExtra("resolveAddress", true);
        startService(sendIntent);
        appendLog(getBaseContext(), TAG, "send intent START_LOCATION_UPDATE:updatesource is N or G:" + sendIntent);
        timerHandler.postDelayed(timerRunnable, LOCATION_TIMEOUT_IN_MS);
        return true;
    }

    private void removeUpdates(LocationListener locationListener) {
        if("location_geocoder_system".equals(AppPreference.getLocationGeocoderSource(this))) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void detectLocation() {
        if (!PermissionUtil.checkPermissionsAndSettings(this)) {
            updateWidgets();
            stopSelf();
            return;
        }
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetworkEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            final Looper locationLooper = Looper.myLooper();
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, locationLooper);
            final LocationListener locationListener = this;
            final Handler locationHandler = new Handler(locationLooper);
            locationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    locationManager.removeUpdates(locationListener);
                    if ((System.currentTimeMillis() - (2 * LOCATION_TIMEOUT_IN_MS)) < lastLocationUpdateTime) {
                        return;
                    }
                    LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
                    org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
                    locationsDbHelper.updateLocationSource(currentLocation.getId(), "-");
                    if (ContextCompat.checkSelfPermission(LocationUpdateService.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        Location lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if ((lastGpsLocation == null) && (lastNetworkLocation != null)) {
                            locationsDbHelper.updateLocationSource(currentLocation.getId(), "N");
                            locationListener.onLocationChanged(lastNetworkLocation);
                        } else if ((lastGpsLocation != null) && (lastNetworkLocation == null)) {
                            locationsDbHelper.updateLocationSource(currentLocation.getId(), "G");
                            locationListener.onLocationChanged(lastGpsLocation);
                        } else if (AppPreference.isGpsEnabledByPreferences(getBaseContext())){
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                            new CountDownTimer(30000, 10000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                }

                                @Override
                                public void onFinish() {
                                    locationManager.removeUpdates(LocationUpdateService.this);
                                    stopSelf();
                                }
                            }.start();
                        }
                    }
                    requestWeatherCheck(null);
                }
            }, LOCATION_TIMEOUT_IN_MS);
        }
    }

    private void requestWeatherCheck(String locationSource) {
        startRefreshRotation();
        boolean updateLocationInProcess = updateNetworkLocation(true);
        appendLog(getBaseContext(), TAG, "requestWeatherCheck, updateLocationInProcess=" +
                updateLocationInProcess);
        if (updateLocationInProcess) {
            updateWidgets();
            return;
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (locationSource != null) {
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "-");
            currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        }
        sendIntentToGetWeather(currentLocation);
    }

    private void sendIntentToGetWeather(org.thosp.yourlocalweather.model.Location currentLocation) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        currentWeatherDbHelper.updateLastUpdatedTime(currentLocation.getId(), System.currentTimeMillis());
        Intent intentToCheckWeather = new Intent(getBaseContext(), CurrentWeatherService.class);
        intentToCheckWeather.putExtra("location", locationsDbHelper.getLocationById(currentLocation.getId()));
        startService(intentToCheckWeather);
    }
    
    private void updateWidgets() {
        stopRefreshRotation();
        startService(new Intent(getBaseContext(), LessWidgetService.class));
        startService(new Intent(getBaseContext(), MoreWidgetService.class));
        startService(new Intent(getBaseContext(), ExtLocationWidgetService.class));
        if (updateSource != null) {
            switch (updateSource) {
                case "MAIN":
                    sendIntentToMain();
                    break;
                case "NOTIFICATION":
                    startService(new Intent(getBaseContext(), NotificationService.class));
                    break;
            }
        }
    }
    
    private void sendIntentToMain() {
        Intent intent = new Intent(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        intent.putExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT, CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void processSensorEvent(SensorEvent sensorEvent) {
        double countedtLength = 0;
        double countedAcc = 0;
        long now = sensorEvent.timestamp;
        try {
            final float dT = (float) (now - lastUpdate) / 1000000000.0f;
            lastUpdate = now;

            if (lastMovement != null) {
                countedAcc = (float) Math.sqrt((lastMovement.getX() * lastMovement.getX()) + (lastMovement.getY() * lastMovement.getY()) + (lastMovement.getZ() * lastMovement.getZ()));
                countedtLength = countedAcc * dT *dT;

                float lowPassConst = 0.1f;

                if (countedAcc < lowPassConst) {
                    if (dT > 1.0f) {
                        appendLog(getBaseContext(), TAG, "acc under limit, currentLength = " + String.format("%.8f", currentLength) +
                                ":counted length = " + String.format("%.8f", countedtLength) + ":countedAcc = " + countedAcc +
                                ", dT = " + String.format("%.8f", dT));
                    }
                    currentLengthLowPassed += countedtLength;
                    lastMovement = highPassFilter(sensorEvent);
                    return;
                }
                currentLength += countedtLength;
            } else {
                countedtLength = 0;
                countedAcc = 0;
            }
            lastMovement = highPassFilter(sensorEvent);

            if ((lastUpdate%1000 < 5) || (countedtLength > 10)) {
                appendLog(getBaseContext(), TAG, "current currentLength = " + String.format("%.8f", currentLength) +
                        ":counted length = " + String.format("%.8f", countedtLength) + ":countedAcc = " + countedAcc +
                        ", dT = " + String.format("%.8f", dT));
            }
            float absCurrentLength = Math.abs(currentLength);

            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);

            if (((lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SPAN)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT))
                    && ((lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SECOND_SPAN)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_SECOND_LIMIT))
                    && (currentLocation.isAddressFound() || (lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION))) {
                return;
            }

            appendLog(getBaseContext(), TAG, "end currentLength = " + String.format("%.8f", absCurrentLength) + ", currentLengthLowPassed = " + String.format("%.8f", currentLengthLowPassed));
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception when processSensorQueue", e);
            return;
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        locationsDbHelper.setNoLocationFound(getBaseContext());
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        lastUpdatedPossition = lastUpdate;
        currentLength = 0;
        currentLengthLowPassed = 0;

        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        locationsDbHelper.updateLocationSource(currentLocation.getId(), "-");
        updateNetworkLocation(false);
    }

    private MoveVector highPassFilter(SensorEvent sensorEvent) {
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

        return new MoveVector(sensorEvent.values[0] - gravity[0], sensorEvent.values[1] - gravity[1], sensorEvent.values[2] - gravity[2]);
    }

    private class MoveVector {
        private final float x;
        private final float y;
        private final float z;

        public MoveVector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() {
            return x;
        }
        public float getY() {
            return y;
        }
        public float getZ() {
            return z;
        }
    }

    private long getLocationTimeInMilis(Location location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
             return System.currentTimeMillis()
                - SystemClock.elapsedRealtime()
                + (location.getElapsedRealtimeNanos() / 1000000);
        } else {
            return location.getTime();
        }

    }
}
