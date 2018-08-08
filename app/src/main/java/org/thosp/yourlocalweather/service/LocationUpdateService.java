package org.thosp.yourlocalweather.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
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

import org.thosp.yourlocalweather.ConnectionDetector;
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

public class LocationUpdateService extends AbstractCommonService implements LocationListener {

    private static final String TAG = "LocationUpdateService";

    private static final long LOCATION_TIMEOUT_IN_MS = 30000L;
    private static final long GPS_LOCATION_TIMEOUT_IN_MS = 30000L;

    private LocationManager locationManager;

    private volatile long lastLocationUpdateTime;
    public static volatile boolean updateLocationInProcess;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return ret;
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():" + intent.getAction());
        switch (intent.getAction()) {
            case "android.intent.action.LOCATION_UPDATE": startLocationUpdateOnly(intent); return ret;
            case "android.intent.action.START_LOCATION_AND_WEATHER_UPDATE": startLocationAndWeatherUpdate(intent); return ret;
            case "android.intent.action.START_LOCATION_ONLY_UPDATE": updateNetworkLocation(intent); return ret;
            default: return ret;
        }
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
            updateSource = networkSourceBuilder.toString();
            appendLog(getBaseContext(), TAG, "send update source to " + updateSource);
        } else if ("-".equals(currentLocation.getLocationSource())) {
            updateSource = "N";
        }
        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        locationsDbHelper.updateAutoLocationGeoLocation(location.getLatitude(), location.getLongitude(), updateSource, location.getAccuracy(), getLocationTimeInMilis(location));
        appendLog(getBaseContext(), TAG, "put new location from location update service, latitude=" + location.getLatitude() + ", longitude=" + location.getLongitude());
        if (address != null) {
            locationsDbHelper.updateAutoLocationAddress(PreferenceUtil.getLanguage(getBaseContext()), address);
        } else {
            String geocoder = AppPreference.getLocationGeocoderSource(this);
            boolean resolveAddressByOS = !("location_geocoder_unifiednlp".equals(geocoder) || "location_geocoder_local".equals(geocoder));
            locationsDbHelper.setNoLocationFound();
            Utils.getAndWriteAddressFromGeocoder(new Geocoder(this, new Locale(PreferenceUtil.getLanguage(this))),
                    address,
                    location.getLatitude(),
                    location.getLongitude(),
                    resolveAddressByOS,
                    this);
        }
        appendLog(getBaseContext(), TAG, "send intent to get weather, updateSource " + updateSource);
        updateLocationInProcess = false;
        sendIntentToGetWeather(currentLocation, false);
    }

    Handler lastKnownLocationTimerHandler = new Handler();
    Runnable lastKnownLocationTimerRunnable = new Runnable() {

        @Override
        public void run() {
            appendLog(getBaseContext(), TAG, "send update source to N - update location by network, lastKnownLocation timeouted");
            updateNetworkLocationByNetwork(null, false, null, false);
        }
    };

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            appendLog(getBaseContext(), TAG, "timerRunnable:requestWeatherCheck");
            requestWeatherCheck("-", false);
        }
    };

    Handler timerHandlerGpsLocation = new Handler();
    Runnable timerRunnableGpsLocation = new Runnable() {

        @Override
        public void run() {
            locationManager.removeUpdates(gpsLocationListener);
            setNoLocationFound(false);
            stopRefreshRotation(false);
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
            startBackgroundService(sendIntent, false);
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

    private void startLocationUpdateOnly(Intent intent) {
        if (intent.getExtras() == null) {
            return;
        }
        Location inputLocation = (Location) intent.getExtras().getParcelable("inputLocation");
        Address addresses = (Address) intent.getExtras().getParcelable("addresses");
        appendLog(getBaseContext(), TAG, "LOCATION_UPDATE recieved:" + inputLocation + ":" + addresses);
        onLocationChanged(inputLocation, addresses);
    }

    private void startLocationAndWeatherUpdate(Intent intent) {
        appendLog(getBaseContext(), TAG, "startLocationAndWeatherUpdate:" + intent);
        if (intent.getExtras() == null) {
            return;
        }
        boolean isInteractive = intent.getBooleanExtra("isInteractive", false);
        boolean isGPSEnabled = AppPreference.isGpsEnabledByPreferences(this) &&
                locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        appendLog(getBaseContext(), TAG, "startLocationAndWeatherUpdate:isGPSEnabled=" +
                                        isGPSEnabled + ", isNetworkEnabled=" + isNetworkEnabled);
        updateSource = intent.getStringExtra("updateSource");
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
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
                updateNetworkLocation(false, intent, isInteractive);
            } else {
                detectLocation(isInteractive);
            }
        } else {
            appendLog(getBaseContext(), TAG, "startLocationAndWeatherUpdate:requestWeatherCheck");
            requestWeatherCheck("-", isInteractive);
        }
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

    private void setNoLocationFound(boolean isInteractive) {
        final LocationsDbHelper locationDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long lastLocationUpdate = locationDbHelper.getLastUpdateLocationTime();
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -5);
        if (lastLocationUpdate > now.getTimeInMillis()) {
            return;
        }
        locationDbHelper.setNoLocationFound();
        updateWidgets(isInteractive);
    }

    private void updateNetworkLocation(Intent intent) {
        if (intent.getExtras() == null) {
            return;
        }
        boolean byLastLocationOnly = intent.getExtras().getBoolean("byLastLocationOnly");
        boolean isInteractive = intent.getExtras().getBoolean("isInteractive");
        updateNetworkLocation(byLastLocationOnly, null, isInteractive);
    }

    private void updateNetworkLocation(boolean bylastLocationOnly,
                                          Intent originalIntent,
                                          boolean isInteractive) {
        updateLocationInProcess = true;
        boolean permissionsGranted = PermissionUtil.checkPermissionsAndSettings(this);
        appendLog(getBaseContext(), TAG, "updateNetworkLocation:" + permissionsGranted);
        if (!permissionsGranted) {
            updateLocationInProcess = false;
            return;
        }
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean isGPSEnabled = AppPreference.isGpsEnabledByPreferences(getBaseContext()) &&
                locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        String geocoder = AppPreference.getLocationGeocoderSource(getBaseContext());
        boolean networkNotEnabled = !isNetworkEnabled && "location_geocoder_system".equals(geocoder);

        appendLog(getBaseContext(), TAG, "updateNetworkLocation:networkNotEnabled=" + networkNotEnabled +
                                        ", isGPSEnabled=" + isGPSEnabled + ", bylastLocationOnly=" + bylastLocationOnly +
                                        ", isNetworkEnabled=" + isNetworkEnabled);
        if (networkNotEnabled && isGPSEnabled && !bylastLocationOnly) {
            appendLog(getBaseContext(), TAG, "updateNetworkLocation:request GPS and start rotation");
            startRefreshRotation(isInteractive);
            gpsRequestLocation();
            return;
        }
        AppWakeUpManager.getInstance(getBaseContext()).wakeUp();
        appendLog(getBaseContext(), TAG, "updateNetworkLocation:wakeup and start rotation");
        startRefreshRotation(isInteractive);
        try {

            Location lastLocation = null;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                lastKnownLocationTimerHandler.postDelayed(lastKnownLocationTimerRunnable, LOCATION_TIMEOUT_IN_MS);
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                lastKnownLocationTimerHandler.removeCallbacksAndMessages(null);
            }
            updateNetworkLocationByNetwork(lastLocation, bylastLocationOnly, originalIntent, isInteractive);
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception during update of network location", e);
        }
        stopRefreshRotation(isInteractive);
        updateLocationInProcess = false;
    }

    private void updateNetworkLocationByNetwork(Location lastLocation,
                                                   boolean bylastLocationOnly,
                                                   Intent originalIntent,
                                                   boolean isInteractive) {
        updateLocationInProcess = true;
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!connectionDetector.isNetworkAvailableAndConnected()) {
            if (originalIntent == null) {
                updateLocationInProcess = false;
                return;
            }
            int numberOfAttempts = originalIntent.getIntExtra("attempts", 0);
            if (numberOfAttempts > 2) {
                locationsDbHelper.updateLastUpdatedAndLocationSource(
                        currentLocation.getId(),
                        System.currentTimeMillis(),
                        ".");
                updateLocationInProcess = false;
                return;
            }
            originalIntent.putExtra("attempts", ++numberOfAttempts);
            resendTheIntentInSeveralSeconds(20, originalIntent);
            updateLocationInProcess = false;
        }
        Intent sendIntent = new Intent("android.intent.action.START_LOCATION_UPDATE");
        sendIntent.putExtra("destinationPackageName", "org.thosp.yourlocalweather");

        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, -5);

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
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "G");
        } else if (bylastLocationOnly) {
            updateLocationInProcess = false;
            stopRefreshRotation(isInteractive);
            return;
        }

        sendIntent.putExtra("resolveAddress", true);
        startBackgroundService(sendIntent, isInteractive);
        appendLog(getBaseContext(), TAG, "send intent START_LOCATION_UPDATE:updatesource is N or G:" + sendIntent);
        timerHandler.postDelayed(timerRunnable, LOCATION_TIMEOUT_IN_MS);
        updateLocationInProcess = true;
    }

    private void removeUpdates(LocationListener locationListener) {
        if("location_geocoder_system".equals(AppPreference.getLocationGeocoderSource(this))) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void detectLocation(final boolean isInteractive) {
        if (!PermissionUtil.checkPermissionsAndSettings(this)) {
            updateWidgets(isInteractive);
            stopSelf();
            return;
        }
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        appendLog(getBaseContext(), TAG, "detectLocation:isNetworkEnabled=" + isNetworkEnabled);
        if (isNetworkEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            appendLog(getBaseContext(), TAG, "detectLocation:afterCheckSelfPermission");
            final Looper locationLooper = Looper.myLooper();
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, locationLooper);
            final LocationListener locationListener = this;
            final Handler locationHandler = new Handler(locationLooper);
            locationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    locationManager.removeUpdates(locationListener);
                    appendLog(getBaseContext(), TAG, "detectLocation:lastLocationUpdateTime=" + lastLocationUpdateTime);
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
                    appendLog(getBaseContext(), TAG, "detectLocation:requestWeatherCheck");
                    requestWeatherCheck(null, isInteractive);
                }
            }, LOCATION_TIMEOUT_IN_MS);
        }
    }

    private long getLocationTimeInMilis(Location location) {
        if (location == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
             return System.currentTimeMillis()
                - SystemClock.elapsedRealtime()
                + (location.getElapsedRealtimeNanos() / 1000000);
        } else {
            return location.getTime();
        }

    }

    private void resendTheIntentInSeveralSeconds(int seconds, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (1000 + seconds), pendingIntent);
    }
}
