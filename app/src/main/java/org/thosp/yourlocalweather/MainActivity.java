package org.thosp.yourlocalweather;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.CurrentWeatherService;
import org.thosp.yourlocalweather.service.StartAutoLocationJob;
import org.thosp.yourlocalweather.service.WeatherRequestDataHolder;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PermissionUtil;
import org.thosp.yourlocalweather.utils.PressureWithUnit;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WindWithUnit;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class MainActivity extends BaseActivity
                          implements AppBarLayout.OnOffsetChangedListener,
                                     ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";

    private ImageView mIconWeatherView;
    private TextView mTemperatureView;
    private TextView dewPointView;
    private TextView secondTemperatureView;
    private TextView mDescriptionView;
    private TextView mHumidityView;
    private TextView mWindSpeedView;
    private TextView mPressureView;
    private TextView mCloudinessView;
    private TextView mLastUpdateView;
    private TextView mSunriseView;
    private TextView mSunsetView;
    private AppBarLayout mAppBarLayout;
    private TextView iconSecondTemperatureView;
    private TextView mIconWindView;
    private TextView mIconHumidityView;
    private TextView mIconPressureView;
    private TextView mIconCloudinessView;
    private TextView mIconSunriseView;
    private TextView mIconSunsetView;
    private TextView mIconDewPointView;

    private ConnectionDetector connectionDetector;
    private Boolean isNetworkAvailable;
    public static ProgressDialog mProgressDialog;
    private SwipeRefreshLayout mSwipeRefresh;
    private Menu mToolbarMenu;
    private BroadcastReceiver mWeatherUpdateReceiver;
    private CurrentWeatherDbHelper currentWeatherDbHelper;
    private WeatherForecastDbHelper weatherForecastDbHelper;
    private Messenger currentWeatherService;
    private Lock currentWeatherServiceLock = new ReentrantLock();
    private Queue<Message> currentWeatherUnsentMessages = new LinkedList<>();

    private WindWithUnit windWithUnit;
    private String iconSecondTemperature;
    private String mIconWind;
    private String mIconHumidity;
    private String mIconPressure;
    private String mIconCloudiness;
    private String mIconSunrise;
    private String mIconSunset;
    private String mIconDewPoint;
    private String mPercentSign;

    private static final int REQUEST_LOCATION = 0;
    public Context storedContext;
    private Handler refreshDialogHandler;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        locationsDbHelper = LocationsDbHelper.getInstance(this);
        weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        setContentView(R.layout.activity_main);

        weatherConditionsIcons();
        initializeTextView();
        initializeWeatherReceiver();

        connectionDetector = new ConnectionDetector(MainActivity.this);
        setTitle( R.string.label_activity_main);

        /**
         * Configure SwipeRefreshLayout
         */
        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.main_swipe_refresh);
        int top_to_padding = 150;
        mSwipeRefresh.setProgressViewOffset(false, 0, top_to_padding);
        mSwipeRefresh.setColorSchemeResources(R.color.swipe_red, R.color.swipe_green,
                R.color.swipe_blue);
        mSwipeRefresh.setOnRefreshListener(swipeRefreshListener);

        NestedScrollView main_scroll_view = (NestedScrollView) findViewById(R.id.main_scroll_view);
        main_scroll_view.setOnTouchListener(new ActivityTransitionTouchListener(
                null,
                WeatherForecastActivity.class, this));

        updateUI();
        /**
         * Share weather fab
         */
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        this.storedContext = this;
        fab.setOnClickListener(fabListener);
        checkSettingsAndPermisions();
        startAlarms();
        StartAlarmsTask startAlarmsTask = new StartAlarmsTask();
        startAlarmsTask.execute(new Integer[0]);
    }

    class StartAlarmsTask extends AsyncTask<Integer[], Integer, Long> {
        @Override
        protected Long doInBackground(Integer[]... params) {
            synchronized (this) {
                startAlarms();
            }
            return 0l;
        }
    }

    private void startAlarms() {
        appendLog(this, TAG, "scheduleStart at boot, SDK=", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            boolean scheduled = false;
            for (JobInfo jobInfo: jobScheduler.getAllPendingJobs()) {
                if (jobInfo.getId() > 0) {
                    appendLog(this, TAG, "scheduleStart does not start - it's scheduled already");
                    scheduled = true;
                    break;
                }
            }
            if (!scheduled) {
                appendLog(this, TAG, "scheduleStart at MainActivity");
                AppPreference.setLastSensorServicesCheckTimeInMs(this, 0);
                jobScheduler.cancelAll();
                ComponentName serviceComponent = new ComponentName(this, StartAutoLocationJob.class);
                JobInfo.Builder builder = new JobInfo.Builder(StartAutoLocationJob.JOB_ID, serviceComponent);
                builder.setMinimumLatency(1 * 1000); // wait at least
                builder.setOverrideDeadline(3 * 1000); // maximum delay
                jobScheduler.schedule(builder.build());
            }
        } else {
            Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_ALARM_SERVICE");
            intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
            startService(intentToStartUpdate);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCurrentLocationAndButtonVisibility();
        checkSettingsAndPermisions();
        updateUI();
        mAppBarLayout.addOnOffsetChangedListener(this);
        registerReceiver(mWeatherUpdateReceiver,
                new IntentFilter(
                        CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindCurrentWeatherService();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mAppBarLayout.removeOnOffsetChangedListener(this);
        unregisterReceiver(mWeatherUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mToolbarMenu = menu;
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_main_menu, menu);
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoLocation.isEnabled()) {
            menu.findItem(R.id.main_menu_detect_location).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_refresh:
                if (connectionDetector.isNetworkAvailableAndConnected()) {
                    locationsDbHelper.updateLastUpdatedAndLocationSource(
                            currentLocation.getId(),
                            System.currentTimeMillis(),
                            getString(R.string.location_weather_update_status_update_started));
                    if ((currentLocation.getLatitude() == 0.0) && (currentLocation.getLongitude() == 0.0)) {
                        Toast.makeText(MainActivity.this,
                                R.string.location_not_initialized,
                                Toast.LENGTH_LONG).show();
                        return true;
                    }
                    currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
                    sendMessageToCurrentWeatherService(currentLocation, "MAIN");
                    setUpdateButtonState(true);
                } else {
                    Toast.makeText(MainActivity.this,
                            R.string.connection_not_found,
                            Toast.LENGTH_SHORT).show();
                    setUpdateButtonState(false);
                }
                return true;
            case R.id.main_menu_detect_location:
                requestLocation();
                return true;
            case R.id.main_menu_search_city:
                Intent intent = new Intent(MainActivity.this, LocationsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    isNetworkAvailable = connectionDetector.isNetworkAvailableAndConnected();
                    if (isNetworkAvailable) {
                        locationsDbHelper.updateLastUpdatedAndLocationSource(
                                currentLocation.getId(),
                                System.currentTimeMillis(),
                                getString(R.string.location_weather_update_status_update_started));
                        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
                        sendMessageToCurrentWeatherService(currentLocation, "MAIN");
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.connection_not_found,
                                Toast.LENGTH_SHORT).show();
                        mSwipeRefresh.setRefreshing(false);
                    }
                }
            };

    private void switchToNextLocationWhenCurrentIsAutoAndIsDisabled() {
        if (currentLocation == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
        }
        if ((currentLocation.getOrderId() == 0) && !currentLocation.isEnabled() && (locationsDbHelper.getAllRows().size() > 1)) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
    }

    private void updateLocationCityTimeAndSource() {
        if (currentLocation == null) {
            return;
        }
        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        String lastUpdate = Utils.getLastUpdateTime(
                this,
                currentWeatherDbHelper.getWeather(currentLocation.getId()),
                weatherForecastDbHelper.getWeatherForecast(currentLocation.getId()),
                currentLocation);
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        localityView.setText(Utils.getCityAndCountry(this, currentLocation.getOrderId()));
    }

    @Override
    protected void updateUI() {
        long locationId = AppPreference.getCurrentLocationId(this);
        currentLocation = locationsDbHelper.getLocationById(locationId);
        if (currentLocation == null) {
            return;
        }

        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());

        if (weatherRecord == null) {
            mTemperatureView.setText(getString(R.string.temperature_with_degree,""));
            dewPointView.setText(getString(R.string.dew_point_label, ""));
            String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(this).getString(
                    Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
            if ("measured_only".equals(temperatureTypeFromPreferences) ||
                    "appearance_only".equals(temperatureTypeFromPreferences)) {
                secondTemperatureView.setVisibility(View.GONE);
                iconSecondTemperatureView.setVisibility(View.GONE);
            } else {
                secondTemperatureView.setVisibility(View.VISIBLE);
                iconSecondTemperatureView.setVisibility(View.VISIBLE);
                secondTemperatureView.setText(getString(R.string.label_apparent_temperature,""));
            }
            mDescriptionView.setText(R.string.location_not_found);
            mLastUpdateView.setText(getString(R.string.last_update_label, ""));
            mHumidityView.setText(getString(R.string.humidity_label,
                    "",
                    ""));
            mPressureView.setText(getString(R.string.pressure_label,
                    "",
                    ""));
            mWindSpeedView.setText(getString(R.string.wind_label,"", "", ""));
            mCloudinessView.setText(getString(R.string.cloudiness_label,
                    "",
                    ""));
            mSunriseView.setText(getString(R.string.sunrise_label, ""));
            mSunsetView.setText(getString(R.string.sunset_label, ""));
            return;
        }

        Weather weather = weatherRecord.getWeather();

        String lastUpdate = Utils.getLastUpdateTime(this, weatherRecord, weatherForecastRecord, currentLocation);
        windWithUnit = AppPreference.getWindWithUnit(this,
                                                     weather.getWindSpeed(),
                                                     weather.getWindDirection(),
                                                     currentLocation.getLocale());
        PressureWithUnit pressure = AppPreference.getPressureWithUnit(this,
                                                                      weather.getPressure(),
                                                                      currentLocation.getLocale());
        String sunrise = Utils.unixTimeToFormatTime(this, weather.getSunrise(), currentLocation.getLocale());
        String sunset = Utils.unixTimeToFormatTime(this, weather.getSunset(), currentLocation.getLocale());

        Utils.setWeatherIcon(mIconWeatherView, this, weatherRecord);
        mTemperatureView.setText(getString(R.string.temperature_with_degree,
                TemperatureUtil.getTemperatureWithUnit(this,
                        weather,
                        currentLocation.getLatitude(),
                        weatherRecord.getLastUpdatedTime(),
                        currentLocation.getLocale())));
        dewPointView.setText(getString(R.string.dew_point_label, 
                             TemperatureUtil.getDewPointWithUnit(this, weather, currentLocation.getLocale())));
        String secondTemperature = TemperatureUtil.getSecondTemperatureWithLabel(this,
                weather,
                currentLocation.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                currentLocation.getLocale());
        if (secondTemperature != null) {
            secondTemperatureView.setText(secondTemperature);
            secondTemperatureView.setVisibility(View.VISIBLE);
            iconSecondTemperatureView.setVisibility(View.VISIBLE);
        } else {
            secondTemperatureView.setVisibility(View.GONE);
            iconSecondTemperatureView.setVisibility(View.GONE);
        }
        mDescriptionView.setText(Utils.getWeatherDescription(this, currentLocation.getLocaleAbbrev(), weather));
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        mHumidityView.setText(getString(R.string.humidity_label,
                String.valueOf(weather.getHumidity()),
                mPercentSign));
        mPressureView.setText(getString(R.string.pressure_label,
                pressure.getPressure(AppPreference.getPressureDecimalPlaces(this)),
                pressure.getPressureUnit()));
        mWindSpeedView.setText(getString(R.string.wind_label,
                                         windWithUnit.getWindSpeed(1),
                                         windWithUnit.getWindUnit(),
                                         windWithUnit.getWindDirection()));
        mCloudinessView.setText(getString(R.string.cloudiness_label,
                String.valueOf(weather.getClouds()),
                mPercentSign));
        mSunriseView.setText(getString(R.string.sunrise_label, sunrise));
        mSunsetView.setText(getString(R.string.sunset_label, sunset));
        localityView.setText(Utils.getCityAndCountry(this, currentLocation.getOrderId()));
    }

    private void initializeTextView() {
        /**
         * Create typefaces from Asset
         */
        Typeface weatherFontIcon = Typeface.createFromAsset(this.getAssets(),
                "fonts/weathericons-regular-webfont.ttf");
        Typeface robotoThin = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Thin.ttf");
        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Light.ttf");

        mIconWeatherView = (ImageView) findViewById(R.id.main_weather_icon);
        mTemperatureView = (TextView) findViewById(R.id.main_temperature);
	    dewPointView = (TextView) findViewById(R.id.main_dew_point);
        secondTemperatureView = (TextView) findViewById(R.id.main_second_temperature);
        mDescriptionView = (TextView) findViewById(R.id.main_description);
        mPressureView = (TextView) findViewById(R.id.main_pressure);
        mHumidityView = (TextView) findViewById(R.id.main_humidity);
        mWindSpeedView = (TextView) findViewById(R.id.main_wind_speed);
        mCloudinessView = (TextView) findViewById(R.id.main_cloudiness);
        mLastUpdateView = (TextView) findViewById(R.id.main_last_update);
        mSunriseView = (TextView) findViewById(R.id.main_sunrise);
        mSunsetView = (TextView) findViewById(R.id.main_sunset);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.main_app_bar);
        localityView = (TextView) findViewById(R.id.main_locality);

        mTemperatureView.setTypeface(robotoThin);
        dewPointView.setTypeface(robotoLight);
        secondTemperatureView.setTypeface(robotoLight);
        mWindSpeedView.setTypeface(robotoLight);
        mHumidityView.setTypeface(robotoLight);
        mPressureView.setTypeface(robotoLight);
        mCloudinessView.setTypeface(robotoLight);
        mSunriseView.setTypeface(robotoLight);
        mSunsetView.setTypeface(robotoLight);

        /**
         * Initialize and configure weather icons
         */
        iconSecondTemperatureView = (TextView) findViewById(R.id.main_second_temperature_icon);
        iconSecondTemperatureView.setTypeface(weatherFontIcon);
        iconSecondTemperatureView.setText(iconSecondTemperature);
        mIconWindView = (TextView) findViewById(R.id.main_wind_icon);
        mIconWindView.setTypeface(weatherFontIcon);
        mIconWindView.setText(mIconWind);
        mIconHumidityView = (TextView) findViewById(R.id.main_humidity_icon);
        mIconHumidityView.setTypeface(weatherFontIcon);
        mIconHumidityView.setText(mIconHumidity);
        mIconPressureView = (TextView) findViewById(R.id.main_pressure_icon);
        mIconPressureView.setTypeface(weatherFontIcon);
        mIconPressureView.setText(mIconPressure);
        mIconCloudinessView = (TextView) findViewById(R.id.main_cloudiness_icon);
        mIconCloudinessView.setTypeface(weatherFontIcon);
        mIconCloudinessView.setText(mIconCloudiness);
        mIconSunriseView = (TextView) findViewById(R.id.main_sunrise_icon);
        mIconSunriseView.setTypeface(weatherFontIcon);
        mIconSunriseView.setText(mIconSunrise);
        mIconSunsetView = (TextView) findViewById(R.id.main_sunset_icon);
        mIconSunsetView.setTypeface(weatherFontIcon);
        mIconSunsetView.setText(mIconSunset);
        mIconDewPointView = (TextView) findViewById(R.id.main_dew_point_icon);
        mIconDewPointView.setTypeface(weatherFontIcon);
        mIconDewPointView.setText(mIconDewPoint);
    }

    private void weatherConditionsIcons() {
        mIconWind = getString(R.string.icon_wind);
        mIconHumidity = getString(R.string.icon_humidity);
        mIconPressure = getString(R.string.icon_barometer);
        mIconCloudiness = getString(R.string.icon_cloudiness);
        mPercentSign = getString(R.string.percent_sign);
        mIconSunrise = getString(R.string.icon_sunrise);
        mIconSunset = getString(R.string.icon_sunset);
        iconSecondTemperature = getString(R.string.icon_thermometer);
        mIconDewPoint = getString(R.string.icon_dew_point);
    }

    private void setUpdateButtonState(boolean isUpdate) {
        if (mToolbarMenu != null) {
            MenuItem updateItem = mToolbarMenu.findItem(R.id.main_menu_refresh);
            ProgressBar progressUpdate = (ProgressBar) findViewById(R.id.toolbar_progress_bar);
            if (isUpdate) {
                updateItem.setVisible(false);
                progressUpdate.setVisibility(View.VISIBLE);
            } else {
                progressUpdate.setVisibility(View.GONE);
                updateItem.setVisible(true);
            }
        }
    }

    private void initializeWeatherReceiver() {
        mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                appendLog(context, TAG, "BroadcastReceiver:", intent);
                if ((mProgressDialog != null) && (refreshDialogHandler != null)) {
                    refreshDialogHandler.post(new Runnable() {
                        public void run() {
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                            }
                        }
                    });
                }
                switch (intent.getStringExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT)) {
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_OK:
                        mSwipeRefresh.setRefreshing(false);
                        setUpdateButtonState(false);

                        updateUI();
                        break;
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                        mSwipeRefresh.setRefreshing(false);
                        setUpdateButtonState(false);
                        updateLocationCityTimeAndSource();
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_parse_error),
                                Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        mSwipeRefresh.setEnabled(verticalOffset == 0);
    }

    FloatingActionButton.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(MainActivity.this);
            CurrentWeatherDbHelper.WeatherRecord currentWeatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

            if (currentWeatherRecord == null) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.current_weather_has_not_been_fetched),
                        Toast.LENGTH_LONG).show();
                return;
            }

            Weather weather = currentWeatherRecord.getWeather();

            String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                    MainActivity.this,
                    weather,
                    currentLocation.getLatitude(),
                    currentWeatherRecord.getLastUpdatedTime(),
                    currentLocation.getLocale());
            windWithUnit = AppPreference.getWindWithUnit(
                    MainActivity.this,
                    weather.getWindSpeed(),
                    weather.getWindDirection(),
                    currentLocation.getLocale());
            String description;
            String sunrise;
            String sunset;
            description = Utils.getWeatherDescription(MainActivity.this, currentLocation.getLocaleAbbrev(), weather);
            sunrise = Utils.unixTimeToFormatTime(MainActivity.this, weather.getSunrise(), currentLocation.getLocale());
            sunset = Utils.unixTimeToFormatTime(MainActivity.this, weather.getSunset(), currentLocation.getLocale());
            String weatherDescription = getString(R.string.share_weather_descritpion,
                                                  getCityNameFromAddress(),
                                                  temperatureWithUnit,
                                                  description,
                                                  windWithUnit.getWindSpeed(1),
                                                  windWithUnit.getWindUnit(),
                                                  sunrise,
                                                  sunset);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, weatherDescription);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_weather_title)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.share_weather_app_not_found),
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    private String getCityNameFromAddress() {
        return (currentLocation.getAddress() != null)?currentLocation.getAddress().getLocality():getString(R.string.location_not_found);
    }

    private void detectLocation() {
        if (WidgetRefreshIconService.isRotationActive) {
            return;
        }
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage(getString(R.string.progressDialog_gps_locate));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                } catch (SecurityException e) {
                    appendLog(MainActivity.this, TAG, "Cancellation error", e);
                }
            }
        });

        updateNetworkLocation();
        mProgressDialog.show();
        refreshDialogHandler = new Handler(Looper.getMainLooper());
    }

    private volatile boolean permissionsAndSettingsRequested = false;

    public boolean checkPermissionsSettingsAndShowAlert() {
        if (permissionsAndSettingsRequested) {
            return true;
        }
        permissionsAndSettingsRequested = true;
        Location autoUpdateLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoUpdateLocation.isEnabled()) {
            return true;
        }
        AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_location_permission_title);

        LocationManager locationManager = (LocationManager) getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        String geocoder = AppPreference.getLocationGeocoderSource(getBaseContext());

        boolean gpsNotEnabled = !isGPSEnabled && AppPreference.isGpsEnabledByPreferences(getBaseContext());
        boolean networkNotEnabled = !isNetworkEnabled && "location_geocoder_system".equals(geocoder);

        if (gpsNotEnabled || networkNotEnabled) {
            settingsAlert.setMessage(R.string.alertDialog_location_permission_message_location_phone_settings);
            settingsAlert.setPositiveButton(R.string.alertDialog_location_permission_positiveButton_settings,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            permissionsAndSettingsRequested = false;
                            Intent goToSettings = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(goToSettings);
                        }
                    });
        } else {
            List<String> permissions = new ArrayList<>();
            StringBuilder notificationMessage = new StringBuilder();
            if (AppPreference.isGpsEnabledByPreferences(getBaseContext()) &&
                    isGPSEnabled &&
                    ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_phone_settings) + "\n\n");
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if ("location_geocoder_local".equals(geocoder) && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_phone_permission));
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            } else if (isNetworkEnabled && "location_geocoder_system".equals(geocoder) && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_network_permission));
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (permissions.isEmpty()) {
                return true;
            }
            settingsAlert.setMessage(notificationMessage.toString());
            final String[] permissionsArray = permissions.toArray(new String[permissions.size()]);
            final Activity mainActivity = this;
            settingsAlert.setPositiveButton(R.string.alertDialog_location_permission_positiveButton_permissions,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(mainActivity,
                                    permissionsArray,
                                    123);
                        }
                    });
        }

        settingsAlert.setNegativeButton(R.string.alertDialog_location_permission_negativeButton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionsAndSettingsRequested = false;
                        dialog.cancel();
                    }
                });
        settingsAlert.show();
        return false;
    }

    private void checkBatteryOptimization() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion < 3) {
            SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(this).edit();
            preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 3);
            preferences.apply();
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_battery_optimization_title);
        settingsAlert.setMessage(R.string.alertDialog_battery_optimization_message);
        settingsAlert.setPositiveButton(R.string.alertDialog_battery_optimization_proceed,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                return;
                            }
                            Intent intent = new Intent();
                            String packageName = getPackageName();
                            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            if (pm.isIgnoringBatteryOptimizations(packageName))
                                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            else {
                                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + packageName));
                            }
                            startActivity(intent);
                        }
                    });
        settingsAlert.setNegativeButton(R.string.alertDialog_battery_optimization_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionsAndSettingsRequested = false;
                        dialog.cancel();
                    }
                });
        settingsAlert.show();
    }

    private volatile boolean initialGuideCompleted;
    private volatile int initialGuidePage;
    private int selectedUpdateLocationStrategy;
    private int selectedLocationAndAddressSourceStrategy;
    private int selectedWakeupStrategyStrategy;
    private int selectedCacheLocationStrategy;

    private void checkSettingsAndPermisions() {
        if (initialGuideCompleted) {
            return;
        }
        checkAndShowInitialGuide();
    }

    private void checkAndShowInitialGuide() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion > 0) {
            initialGuideCompleted = true;
            if (initialGuideVersion < 3) {
                checkBatteryOptimization();
            }
            return;
        }
        if (initialGuidePage > 0) {
            return;
        }
        initialGuidePage = 1;
        showInitialGuidePage(initialGuidePage);
    }

    private void showInitialGuidePage(int pageNumber) {
        final AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        switch (pageNumber) {
            case 1:
                settingsAlert.setTitle(R.string.initial_guide_title_1);
                settingsAlert.setMessage(R.string.initial_guide_paragraph_1);
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_close);
                break;
            case 2:
                settingsAlert.setTitle(R.string.initial_guide_title_2);
                selectedUpdateLocationStrategy = 1;
                settingsAlert.setSingleChoiceItems(R.array.location_update_strategy, selectedUpdateLocationStrategy,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedOption) {
                                selectedUpdateLocationStrategy = selectedOption;
                                if (selectedOption == 0) {
                                    initialGuidePage = 8; //skip to the last page
                                }
                            }
                        });
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 3:
                settingsAlert.setTitle(R.string.initial_guide_title_3);
                settingsAlert.setMessage(R.string.initial_guide_paragraph_3);
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 4:
                settingsAlert.setTitle(R.string.initial_guide_title_4);
                selectedLocationAndAddressSourceStrategy = 0;
                settingsAlert.setSingleChoiceItems(R.array.location_geocoder_source_entries, selectedLocationAndAddressSourceStrategy,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedOption) {
                                selectedLocationAndAddressSourceStrategy = selectedOption;
                            }
                        });
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 5:
                settingsAlert.setTitle(R.string.initial_guide_title_5);
                settingsAlert.setMessage(R.string.initial_guide_paragraph_5);
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 6:
                settingsAlert.setTitle(R.string.initial_guide_title_6);
                selectedWakeupStrategyStrategy = 2;
                settingsAlert.setSingleChoiceItems(R.array.wake_up_strategy_entries, selectedWakeupStrategyStrategy,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedOption) {
                                selectedWakeupStrategyStrategy = selectedOption;
                            }
                        });
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 7:
                settingsAlert.setTitle(R.string.initial_guide_title_7);
                settingsAlert.setMessage(R.string.initial_guide_paragraph_7);
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 8:
                settingsAlert.setTitle(R.string.initial_guide_title_8);
                selectedCacheLocationStrategy = 1;
                settingsAlert.setSingleChoiceItems(R.array.location_cache_entries, selectedCacheLocationStrategy,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedOption) {
                                selectedCacheLocationStrategy = selectedOption;
                            }
                        });
                setNextButton(settingsAlert, R.string.initial_guide_next);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
            case 9:
                settingsAlert.setTitle(R.string.initial_guide_title_9);
                settingsAlert.setMessage(R.string.initial_guide_paragraph_9);
                setNextButton(settingsAlert, R.string.initial_guide_finish);
                setPreviousButton(settingsAlert, R.string.initial_guide_previous);
                break;
        }
        settingsAlert.show();
    }

    private void setNextButton(AlertDialog.Builder settingsAlert, int labelId) {
        settingsAlert.setPositiveButton(labelId,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        initialGuidePage++;
                        if (initialGuidePage > 9) {
                            initialGuideCompleted = true;
                            permissionsAndSettingsRequested = false;
                            saveInitialPreferences();
                            updateCurrentLocationAndButtonVisibility();
                            checkPermissionsSettingsAndShowAlert();
                        } else {
                            showInitialGuidePage(initialGuidePage);
                        }
                    }
                });
    }

    private void setPreviousButton(AlertDialog.Builder settingsAlert, final int labelId) {
        settingsAlert.setNegativeButton(labelId,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (labelId == R.string.initial_guide_close) {
                            closeInitialGuideAndCheckPermission();
                        } else {
                            initialGuidePage--;
                            showInitialGuidePage(initialGuidePage);
                        }
                    }
                });
    }

    private void closeInitialGuideAndCheckPermission() {
        permissionsAndSettingsRequested = false;
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(this).edit();
        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 3);
        preferences.apply();
        initialGuideCompleted = true;
        checkPermissionsSettingsAndShowAlert();
        updateCurrentLocationAndButtonVisibility();
    }

    private void saveInitialPreferences() {
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(this).edit();

        boolean gpsEnabled = true;
        boolean locationUpdateEnabled = true;
        switch (selectedUpdateLocationStrategy) {
            case 0: locationUpdateEnabled = false; gpsEnabled = false; break;
            case 1: locationUpdateEnabled = true; gpsEnabled = true; break;
            case 2: locationUpdateEnabled = true; gpsEnabled = false; break;
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        locationsDbHelper.updateEnabled(autoLocation.getId(), locationUpdateEnabled);
        preferences.putBoolean(Constants.KEY_PREF_LOCATION_GPS_ENABLED, gpsEnabled);

        String selectedWakeupStrategyStrategyString = "nowakeup";
        switch (selectedWakeupStrategyStrategy) {
            case 0: selectedWakeupStrategyStrategyString = "nowakeup"; break;
            case 1: selectedWakeupStrategyStrategyString = "wakeuppartial"; break;
            case 2: selectedWakeupStrategyStrategyString = "wakeupfull"; break;
        }
        preferences.putString(Constants.KEY_WAKE_UP_STRATEGY, selectedWakeupStrategyStrategyString);

        String selectedLocationAndAddressSourceStrategyString = "location_geocoder_local";
        switch (selectedLocationAndAddressSourceStrategy) {
            case 0: selectedLocationAndAddressSourceStrategyString = "location_geocoder_local"; break;
            case 1: selectedLocationAndAddressSourceStrategyString = "location_geocoder_system"; break;
        }
        preferences.putString(Constants.KEY_PREF_LOCATION_GEOCODER_SOURCE, selectedLocationAndAddressSourceStrategyString);

        boolean selectedCacheLocationStrategyBoolean = false;
        switch (selectedCacheLocationStrategy) {
            case 0: selectedCacheLocationStrategyBoolean = false; break;
            case 1: selectedCacheLocationStrategyBoolean = true; break;
        }
        preferences.putBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, selectedCacheLocationStrategyBoolean);

        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 3);
        preferences.apply();
    }

    private void updateNetworkLocation() {
        Intent startLocationUpdateIntent = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
        startLocationUpdateIntent.setPackage("org.thosp.yourlocalweather");
        startLocationUpdateIntent.putExtra("updateSource", "MAIN");
        startLocationUpdateIntent.putExtra("locationId", currentLocation.getId());
        storedContext.startService(startLocationUpdateIntent);
    }
    
    private void requestLocation() {
        if (checkPermissionsSettingsAndShowAlert()) {
            detectLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (PermissionUtil.verifyPermissions(grantResults)) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.permission_available_location, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                checkBatteryOptimization();
                break;
        }
    }

    private void updateCurrentLocationAndButtonVisibility() {
        currentLocation = locationsDbHelper.getLocationById(AppPreference.getCurrentLocationId(getApplicationContext()));
        if (currentLocation == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
        }
        switchToNextLocationWhenCurrentIsAutoAndIsDisabled();
        if (mToolbarMenu != null) {
            if ((currentLocation.getOrderId() == 0) && !currentLocation.isEnabled()) {
                mToolbarMenu.findItem(R.id.main_menu_refresh).setVisible(false);
            } else {
                mToolbarMenu.findItem(R.id.main_menu_refresh).setVisible(true);
            }
            Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!autoLocation.isEnabled()) {
                mToolbarMenu.findItem(R.id.main_menu_detect_location).setVisible(false);
            } else {
                mToolbarMenu.findItem(R.id.main_menu_detect_location).setVisible(true);
            }
        }
        AppPreference.setCurrentLocationId(this, currentLocation);
    }

    protected void sendMessageToCurrentWeatherService(Location location, String updateSource) {
        if ((location.getLongitude() == 0) && (location.getLatitude() == 0) && (location.getLastLocationUpdate() == 0)) {
            detectLocation();
            return;
        }
        currentWeatherServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    CurrentWeatherService.START_CURRENT_WEATHER_UPDATE,
                    new WeatherRequestDataHolder(location.getId(), updateSource)
            );
            if (checkIfCurrentWeatherServiceIsNotBound()) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                currentWeatherUnsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            currentWeatherService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            currentWeatherServiceLock.unlock();
        }
        sendMessageToWeatherForecastService(location.getId());
    }

    private boolean checkIfCurrentWeatherServiceIsNotBound() {
        if (currentWeatherService != null) {
            return false;
        }
        try {
            bindCurrentWeatherService();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return (currentWeatherService == null);
    }

    private void bindCurrentWeatherService() {
        getApplicationContext().bindService(
                new Intent(getApplicationContext(), CurrentWeatherService.class),
                currentWeatherServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindCurrentWeatherService() {
        if (currentWeatherService == null) {
            return;
        }
        try {
            getApplicationContext().unbindService(currentWeatherServiceConnection);
        } catch (Exception e) {
            appendLog(this, "TAG", e.getMessage(), e);
        }
    }

    private ServiceConnection currentWeatherServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            currentWeatherService = new Messenger(binderService);
            currentWeatherServiceLock.lock();
            try {
                while (!currentWeatherUnsentMessages.isEmpty()) {
                    currentWeatherService.send(currentWeatherUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                currentWeatherServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            currentWeatherService = null;
        }
    };
}
