package org.thosp.yourlocalweather;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PressureWithUnit;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WindWithUnit;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity
                          implements AppBarLayout.OnOffsetChangedListener,
                                     ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";

    private volatile boolean inited;

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
    private AppCompatImageButton switchLocationButton;

    private ConnectionDetector connectionDetector;
    private Boolean isNetworkAvailable;
    public static ProgressDialog mProgressDialog;
    private SwipeRefreshLayout mSwipeRefresh;
    private Menu mToolbarMenu;
    private BroadcastReceiver mWeatherUpdateReceiver;
    private CurrentWeatherDbHelper currentWeatherDbHelper;
    private WeatherForecastDbHelper weatherForecastDbHelper;
    private String timeStylePreference;
    private String pressureUnitFromPreferences;
    private String windUnitFromPreferences;
    private String temperatureUnitFromPreferences;
    private String temeratureTypeFromPreferences;


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

        YourLocalWeather.executor.submit(() -> {
            locationsDbHelper = LocationsDbHelper.getInstance(MainActivity.this);
            weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(MainActivity.this);
            currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(MainActivity.this);
            connectionDetector = new ConnectionDetector(MainActivity.this);
            timeStylePreference = AppPreference.getTimeStylePreference(MainActivity.this);
            pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(MainActivity.this);
            windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(MainActivity.this);
            temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(MainActivity.this);
            temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(MainActivity.this);

            initializeWeatherReceiver();
            updateActivityOnResume();
            checkSettingsAndPermisions();
            inited = true;
        });

        setContentView(R.layout.activity_main);
        setTitle( R.string.label_activity_main);
        weatherConditionsIcons();
        initializeTextView();

        StartAlarmsTask startAlarmsTask = new StartAlarmsTask();
        startAlarmsTask.execute(new Integer[0]);
        Intent intentToStartUpdate = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        startService(intentToStartUpdate);

        /**
         * Configure SwipeRefreshLayout
         */
        mSwipeRefresh = findViewById(R.id.main_swipe_refresh);
        int top_to_padding = 150;
        mSwipeRefresh.setProgressViewOffset(false, 0, top_to_padding);
        mSwipeRefresh.setColorSchemeResources(R.color.swipe_red, R.color.swipe_green,
                R.color.swipe_blue);
        mSwipeRefresh.setOnRefreshListener(swipeRefreshListener);

        NestedScrollView main_scroll_view = findViewById(R.id.main_scroll_view);
        main_scroll_view.setOnTouchListener(new ActivityTransitionTouchListener(
                null,
                WeatherForecastActivity.class, this));

        FloatingActionButton fab = findViewById(R.id.fab);
        this.storedContext = this;
        fab.setOnClickListener(fabListener);
    }

    class StartAlarmsTask extends AsyncTask<Integer[], Integer, Long> {
        @Override
        protected Long doInBackground(Integer[]... params) {
            synchronized (this) {
                startAlarms();
            }
            return 0L;
        }
    }

    private void startAlarms() {
        appendLog(this, TAG, "scheduleStart at boot, SDK=", Build.VERSION.SDK_INT);
        /*if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
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
        } else {*/
            Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_ALARM_SERVICE");
            intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
            startService(intentToStartUpdate);
        //}
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppBarLayout.addOnOffsetChangedListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mWeatherUpdateReceiver,
                    new IntentFilter(
                            UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT),
                    RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mWeatherUpdateReceiver,
                    new IntentFilter(
                            UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT));
        }
        if (inited) {
            YourLocalWeather.executor.submit(() -> {
                updateActivityOnResume();
            });
        }
    }

    private void updateActivityOnResume() {
        updateCurrentLocationAndButtonVisibility();
        checkSettingsAndPermisions();
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mProgressDialog != null) {
            runOnUiThread(() -> {
                mProgressDialog.dismiss();
            });
        }
        mAppBarLayout.removeOnOffsetChangedListener(this);
        if (mWeatherUpdateReceiver != null) {
            try {
                unregisterReceiver(mWeatherUpdateReceiver);
            } catch (Exception e) {
                //
            }
        }
        inited = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null) {
            runOnUiThread(() -> {
                mProgressDialog.dismiss();
            });
            mProgressDialog = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mToolbarMenu = menu;
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_main_menu, menu);
        if (locationsDbHelper == null) {
            return false;
        }
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
                    if ((currentLocation == null) || (currentLocation.getLatitude() == 0.0) && (currentLocation.getLongitude() == 0.0)) {
                        Toast.makeText(MainActivity.this,
                                R.string.location_not_initialized,
                                Toast.LENGTH_LONG).show();
                        return true;
                    }
                    YourLocalWeather.executor.submit(() -> {
                        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
                        sendMessageToCurrentWeatherService(currentLocation, "MAIN");
                    });
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

    private final SwipeRefreshLayout.OnRefreshListener swipeRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    isNetworkAvailable = connectionDetector.isNetworkAvailableAndConnected();
                    if (isNetworkAvailable && (currentLocation != null)) {
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
        if ((currentLocation == null) || ((currentLocation.getOrderId() == 0) && !currentLocation.isEnabled() && (locationsDbHelper.getAllRows().size() > 1))) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
    }

    private void updateLocationCityTimeAndSource() {
        if (currentLocation == null) {
            return;
        }
        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        String lastUpdate = Utils.getLastUpdateTime(
                MainActivity.this,
                currentWeatherDbHelper.getWeather(currentLocation.getId()),
                weatherForecastDbHelper.getWeatherForecast(currentLocation.getId()),
                timeStylePreference,
                currentLocation);
        String cityAndCountry = Utils.getCityAndCountry(MainActivity.this, currentLocation);
        runOnUiThread(() -> {
            mLastUpdateView.setText(MainActivity.this.getString(R.string.last_update_label, lastUpdate));
            localityView.setText(cityAndCountry);
        });
    }

    @Override
    protected void updateUI() {
        if (currentLocation == null) {
            appendLog(MainActivity.this, TAG, "updateUI no currentLocation found");
            return;
        }
        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());

        if (weatherRecord == null) {
            renderTextsWithNoWeather();
            return;
        }

        Weather weather = weatherRecord.getWeather();

        if (weather == null) {
            renderTextsWithNoWeather();
            return;
        }

        String lastUpdate = Utils.getLastUpdateTime(MainActivity.this, weatherRecord, weatherForecastRecord, timeStylePreference, currentLocation);
        windWithUnit = AppPreference.getWindWithUnit(MainActivity.this,
                                                     weather.getWindSpeed(),
                                                     weather.getWindDirection(),
                                                    windUnitFromPreferences,
                                                     currentLocation.getLocale());
        PressureWithUnit pressure = AppPreference.getPressureWithUnit(MainActivity.this,
                                                                      weather.getPressure(),
                pressureUnitFromPreferences,
                currentLocation.getLocale());
        String sunrise = Utils.unixTimeToFormatTime(MainActivity.this, weather.getSunrise(), timeStylePreference, currentLocation.getLocale());
        String sunset = Utils.unixTimeToFormatTime(MainActivity.this, weather.getSunset(), timeStylePreference, currentLocation.getLocale());
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(MainActivity.this);

        String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(MainActivity.this,
                weather,
                currentLocation.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                temeratureTypeFromPreferences,
                temperatureUnitFromPreferences,
                currentLocation.getLocale());
        String dewPointWithUnit = TemperatureUtil.getDewPointWithUnit(MainActivity.this, weather, temperatureUnitFromPreferences, currentLocation.getLocale());
        String secondTemperature = TemperatureUtil.getSecondTemperatureWithLabel(MainActivity.this,
                weather,
                currentLocation.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                temperatureUnitFromPreferences,
                currentLocation.getLocale());
        String weatherDescription = Utils.getWeatherDescription(MainActivity.this, currentLocation.getLocaleAbbrev(), weather);
        String pressureValue = pressure.getPressure(AppPreference.getPressureDecimalPlaces(pressureUnitFromPreferences));
        String cityAndCountry = Utils.getCityAndCountry(MainActivity.this, currentLocation);
        boolean fontBasedIconSet = "weather_icon_set_fontbased".equals(AppPreference.getIconSet(MainActivity.this));
        int textColor = AppPreference.getTextColor(MainActivity.this);

        runOnUiThread(() -> {
                try {
                    Utils.setWeatherIcon(mIconWeatherView, MainActivity.this, weatherRecord, textColor, fontBasedIconSet);
                    mTemperatureView.setText(MainActivity.this.getString(R.string.temperature_with_degree, temperatureWithUnit));
                    dewPointView.setText(MainActivity.this.getString(R.string.dew_point_label, dewPointWithUnit));
                    if (secondTemperature != null) {
                        secondTemperatureView.setText(secondTemperature);
                        secondTemperatureView.setVisibility(View.VISIBLE);
                        iconSecondTemperatureView.setVisibility(View.VISIBLE);
                    } else {
                        secondTemperatureView.setVisibility(View.GONE);
                        iconSecondTemperatureView.setVisibility(View.GONE);
                    }
                    mDescriptionView.setText(weatherDescription);
                    mLastUpdateView.setText(MainActivity.this.getString(R.string.last_update_label, lastUpdate));
                    mHumidityView.setText(MainActivity.this.getString(R.string.humidity_label,
                            String.valueOf(weather.getHumidity()),
                            mPercentSign));
                    mPressureView.setText(MainActivity.this.getString(R.string.pressure_label,
                            pressureValue,
                            pressure.getPressureUnit()));
                    mWindSpeedView.setText(MainActivity.this.getString(R.string.wind_label,
                            windWithUnit.getWindSpeed(1),
                            windWithUnit.getWindUnit(),
                            windWithUnit.getWindDirection()));
                    mCloudinessView.setText(MainActivity.this.getString(R.string.cloudiness_label,
                            String.valueOf(weather.getClouds()),
                            mPercentSign));
                    mSunriseView.setText(MainActivity.this.getString(R.string.sunrise_label, sunrise));
                    mSunsetView.setText(MainActivity.this.getString(R.string.sunset_label, sunset));
                    localityView.setText(cityAndCountry);
                } catch (Exception e) {
                    appendLog(MainActivity.this, TAG, e);
                }
        });
    }

    private void renderTextsWithNoWeather() {
        sendMessageToCurrentWeatherService(currentLocation, "MAIN");
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTemperatureView.setText(MainActivity.this.getString(R.string.temperature_with_degree, ""));
                dewPointView.setText(MainActivity.this.getString(R.string.dew_point_label, ""));

                if ("measured_only".equals(temperatureTypeFromPreferences) ||
                        "appearance_only".equals(temperatureTypeFromPreferences)) {
                    secondTemperatureView.setVisibility(View.GONE);
                    iconSecondTemperatureView.setVisibility(View.GONE);
                } else {
                    secondTemperatureView.setVisibility(View.VISIBLE);
                    iconSecondTemperatureView.setVisibility(View.VISIBLE);
                    secondTemperatureView.setText(MainActivity.this.getString(R.string.label_apparent_temperature, ""));
                }
                mDescriptionView.setText(R.string.location_not_found);
                mLastUpdateView.setText(MainActivity.this.getString(R.string.last_update_label, ""));
                mHumidityView.setText(MainActivity.this.getString(R.string.humidity_label,
                        "",
                        ""));
                mPressureView.setText(MainActivity.this.getString(R.string.pressure_label,
                        "",
                        ""));
                mWindSpeedView.setText(MainActivity.this.getString(R.string.wind_label, "", "", ""));
                mCloudinessView.setText(MainActivity.this.getString(R.string.cloudiness_label,
                        "",
                        ""));
                mSunriseView.setText(MainActivity.this.getString(R.string.sunrise_label, ""));
                mSunsetView.setText(MainActivity.this.getString(R.string.sunset_label, ""));
            }
        });
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

        mIconWeatherView = findViewById(R.id.main_weather_icon);
        mTemperatureView = findViewById(R.id.main_temperature);
	    dewPointView = findViewById(R.id.main_dew_point);
        secondTemperatureView = findViewById(R.id.main_second_temperature);
        mDescriptionView = findViewById(R.id.main_description);
        mPressureView = findViewById(R.id.main_pressure);
        mHumidityView = findViewById(R.id.main_humidity);
        mWindSpeedView = findViewById(R.id.main_wind_speed);
        mCloudinessView = findViewById(R.id.main_cloudiness);
        mLastUpdateView = findViewById(R.id.main_last_update);
        mSunriseView = findViewById(R.id.main_sunrise);
        mSunsetView = findViewById(R.id.main_sunset);
        mAppBarLayout = findViewById(R.id.main_app_bar);
        localityView = findViewById(R.id.main_locality);
        switchLocationButton = findViewById(R.id.main_switch_location);

        mTemperatureView.setTypeface(robotoThin);
        dewPointView.setTypeface(robotoLight);
        secondTemperatureView.setTypeface(robotoLight);
        mWindSpeedView.setTypeface(robotoLight);
        mHumidityView.setTypeface(robotoLight);
        mPressureView.setTypeface(robotoLight);
        mCloudinessView.setTypeface(robotoLight);
        mSunriseView.setTypeface(robotoLight);
        mSunsetView.setTypeface(robotoLight);
        localityView.setTypeface(robotoLight);

        /**
         * Initialize and configure weather icons
         */
        iconSecondTemperatureView = findViewById(R.id.main_second_temperature_icon);
        iconSecondTemperatureView.setTypeface(weatherFontIcon);
        iconSecondTemperatureView.setText(iconSecondTemperature);
        TextView mIconWindView = findViewById(R.id.main_wind_icon);
        mIconWindView.setTypeface(weatherFontIcon);
        mIconWindView.setText(mIconWind);
        TextView mIconHumidityView = findViewById(R.id.main_humidity_icon);
        mIconHumidityView.setTypeface(weatherFontIcon);
        mIconHumidityView.setText(mIconHumidity);
        TextView mIconPressureView = findViewById(R.id.main_pressure_icon);
        mIconPressureView.setTypeface(weatherFontIcon);
        mIconPressureView.setText(mIconPressure);
        TextView mIconCloudinessView = findViewById(R.id.main_cloudiness_icon);
        mIconCloudinessView.setTypeface(weatherFontIcon);
        mIconCloudinessView.setText(mIconCloudiness);
        TextView mIconSunriseView = findViewById(R.id.main_sunrise_icon);
        mIconSunriseView.setTypeface(weatherFontIcon);
        mIconSunriseView.setText(mIconSunrise);
        TextView mIconSunsetView = findViewById(R.id.main_sunset_icon);
        mIconSunsetView.setTypeface(weatherFontIcon);
        mIconSunsetView.setText(mIconSunset);
        TextView mIconDewPointView = findViewById(R.id.main_dew_point_icon);
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
            ProgressBar progressUpdate = findViewById(R.id.toolbar_progress_bar);
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
                if ((mProgressDialog != null) && (refreshDialogHandler != null)) {
                    refreshDialogHandler.post(new Runnable() {
                        public void run() {
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                            }
                        }
                    });
                }
                YourLocalWeather.executor.submit(() -> {
                    if (!initialGuideCompleted) {
                        return;
                    }
                    switch (intent.getStringExtra(UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT)) {
                        case UpdateWeatherService.ACTION_WEATHER_UPDATE_OK:
                            mSwipeRefresh.setRefreshing(false);
                            runOnUiThread(() -> {
                                setUpdateButtonState(false);
                            });

                            YourLocalWeather.executor.submit(() -> {
                                updateUI();
                            });
                            break;
                        case UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                            mSwipeRefresh.setRefreshing(false);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setUpdateButtonState(false);
                                }
                            });
                            updateLocationCityTimeAndSource();
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this,
                                        getString(R.string.toast_parse_error),
                                        Toast.LENGTH_SHORT).show();
                            });
                            break;
                    }
                });
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
            YourLocalWeather.executor.submit(() -> {
                CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(MainActivity.this);
                CurrentWeatherDbHelper.WeatherRecord currentWeatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

                if (currentWeatherRecord == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.current_weather_has_not_been_fetched),
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Weather weather = currentWeatherRecord.getWeather();

                if (weather == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.current_weather_has_not_been_fetched),
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(MainActivity.this);
                String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(MainActivity.this);
                String timeStylePreference = AppPreference.getTimeStylePreference(MainActivity.this);
                String temeratureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(MainActivity.this);
                String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                        MainActivity.this,
                        weather,
                        currentLocation.getLatitude(),
                        currentWeatherRecord.getLastUpdatedTime(),
                        temeratureTypeFromPreferences,
                        temeratureUnitFromPreferences,
                        currentLocation.getLocale());
                windWithUnit = AppPreference.getWindWithUnit(
                        MainActivity.this,
                        weather.getWindSpeed(),
                        weather.getWindDirection(),
                        windUnitFromPreferences,
                        currentLocation.getLocale());
                String description;
                String sunrise;
                String sunset;
                description = Utils.getWeatherDescription(MainActivity.this, currentLocation.getLocaleAbbrev(), weather);
                sunrise = Utils.unixTimeToFormatTime(MainActivity.this, weather.getSunrise(), timeStylePreference, currentLocation.getLocale());
                sunset = Utils.unixTimeToFormatTime(MainActivity.this, weather.getSunset(), timeStylePreference, currentLocation.getLocale());
                String weatherDescription = getString(R.string.share_weather_descritpion,
                        Utils.getLocationForSharingFromAddress(currentLocation.getAddress()),
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
                runOnUiThread(() -> {
                    try {
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_weather_title)));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.share_weather_app_not_found),
                                Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    };

    private void detectLocation() {
        if (!locationsDbHelper.getLocationByOrderId(0).isEnabled()) {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
            return;
        }
        updateNetworkLocation();
        runOnUiThread(() -> {
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

            mProgressDialog.show();
            refreshDialogHandler = new Handler(Looper.getMainLooper());
        });
    }

    private volatile boolean permissionsAndSettingsRequested = false;

    private final static int BACKGROUND_LOCATION_PERMISSION_CODE = 333;
    private final static int LOCATION_PERMISSION_CODE = 222;

    private void askPermissionForBackgroundUsage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            runOnUiThread(() -> {
                    String message = getString(R.string.alertDialog_background_location_permission_message);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        message += getPackageManager().getBackgroundPermissionOptionLabel();
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.alertDialog_background_location_permission_title)
                            .setMessage(message)
                            .setPositiveButton(R.string.alertDialog_location_permission_positiveButton_settings, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
                                }
                            })
                            .setNegativeButton(R.string.alertDialog_location_permission_negativeButton, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Snackbar.make(findViewById(android.R.id.content), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
                                }
                            })
                            .create().show();
            });
            } else {
                runOnUiThread(() -> {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_CODE);
                });
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if((grantResults == null) || (grantResults.length == 0)) {
            appendLog(getBaseContext(), TAG, "onRequestPermissionsResult:grantResults is null or zero in length");
            return;
        }

        if (requestCode == LOCATION_PERMISSION_CODE) {
            boolean preciseOrCoarseLocation = false;
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                    preciseOrCoarseLocation |= (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                    preciseOrCoarseLocation |= (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
            }
            if (preciseOrCoarseLocation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Snackbar.make(findViewById(android.R.id.content), R.string.permission_available_location, Snackbar.LENGTH_SHORT).show();
                    } else {
                        askPermissionForBackgroundUsage();
                    }
                } else {
                    SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                    preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 2);
                    preferences.apply();
                }
            } else {
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 2);
                preferences.apply();
                Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
                locationsDbHelper.updateEnabled(autoLocation.getId(), false);
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 2);
                preferences.apply();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
            }
        }

    }

    public boolean checkPermissionsSettingsAndShowAlert() {
        if (permissionsAndSettingsRequested) {
            return true;
        }
        permissionsAndSettingsRequested = true;
        Location autoUpdateLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoUpdateLocation.isEnabled() ||
                (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            return true;
        }

        LocationManager locationManager = (LocationManager) getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        String geocoder = AppPreference.getLocationGeocoderSource(getBaseContext());

        boolean gpsNotEnabled = !isGPSEnabled && AppPreference.isGpsEnabledByPreferences(getBaseContext());
        boolean networkNotEnabled = !isNetworkEnabled && "location_geocoder_system".equals(geocoder);

        AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_location_permission_title);
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
            /*if ("location_geocoder_local".equals(geocoder) && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_phone_permission));
                permissions.add(Manifest.permission.READ_PHONE_STATE);
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            } else*/ if (isNetworkEnabled /*&& "location_geocoder_system".equals(geocoder)*/ && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_network_permission));
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (permissions.isEmpty()) {
                return true;
            }
            settingsAlert.setMessage(notificationMessage.toString());
            final String[] permissionsArray = permissions.toArray(new String[permissions.size()]);
            settingsAlert.setPositiveButton(R.string.alertDialog_location_permission_positiveButton_permissions,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
                                    (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        Snackbar.make(findViewById(android.R.id.content), R.string.permission_available_location, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        askPermissionForBackgroundUsage();
                                    }
                                }
                            } else {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        permissionsArray,
                                        LOCATION_PERMISSION_CODE);
                            }
                        }
                    });
        }

        settingsAlert.setNegativeButton(R.string.alertDialog_location_permission_negativeButton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        permissionsAndSettingsRequested = false;
                        dialog.cancel();
                        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 2);
                        preferences.apply();
                        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
                        locationsDbHelper.updateEnabled(autoLocation.getId(), false);
                        checkSettingsAndPermisions();
                    }
                });
        runOnUiThread(() -> {
            settingsAlert.show();
        });
        return false;
    }

    private void showVoiceAndSourcesDisclaimer() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion != 3) {
            return;
        }
        final Context localContext = getBaseContext();
        final AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_voice_disclaimer_title);
        settingsAlert.setMessage(R.string.alertDialog_voice_disclaimer_message);
        settingsAlert.setNeutralButton(R.string.alertDialog_battery_optimization_proceed,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(localContext).edit();
                        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 4);
                        preferences.apply();
                        checkAndShowInitialGuide();
                    }
                });
        runOnUiThread(() -> {
            settingsAlert.show();
        });
    }

    private void showWeatherProviderChangedDisclaimer() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion != 6) {
            return;
        }
        final Context localContext = getBaseContext();
        final AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_weather_changed_title);
        settingsAlert.setMessage(R.string.alertDialog_weather_changed_message);
        settingsAlert.setNeutralButton(R.string.alertDialog_battery_optimization_proceed,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(localContext).edit();
                        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 7);
                        preferences.apply();
                        checkAndShowInitialGuide();
                    }
                });
        runOnUiThread(() -> {
            settingsAlert.show();
        });
    }

    private void checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
            preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 6);
            preferences.apply();
            initialGuideCompleted = true;
            detectLocation();
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS)) {
            runOnUiThread(() -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else {
            runOnUiThread(() -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            });
        }
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 5);
        preferences.apply();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.permission_available_notification, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
                }
            });


    private void checkBatteryOptimization() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion != 2) {
            return;
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            SharedPreferences.Editor initGuidePreferences = preferences.edit();
            initGuidePreferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 3);
            initGuidePreferences.apply();
            return;
        }
        AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_battery_optimization_title);
        settingsAlert.setMessage(R.string.alertDialog_battery_optimization_message);
        settingsAlert.setPositiveButton(R.string.alertDialog_battery_optimization_proceed,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor initGuidePreferences = preferences.edit();
                            initGuidePreferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 3);
                            initGuidePreferences.apply();
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                return;
                            }
                            inited = true;
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
                        SharedPreferences.Editor initGuidePreferences = preferences.edit();
                        initGuidePreferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 3);
                        initGuidePreferences.apply();
                        checkSettingsAndPermisions();
                    }
                });
        runOnUiThread(() -> {
            settingsAlert.show();
        });
    }

    private volatile boolean initialGuideCompleted;

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
            if (initialGuideVersion == 2) {
                checkBatteryOptimization();
                return;
            } else if (initialGuideVersion == 3) {
                showVoiceAndSourcesDisclaimer();
                return;
            } else if (initialGuideVersion == 4) {
                checkNotificationPermission();
                return;
            } else if (initialGuideVersion == 5) {
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 7);
                preferences.apply();
                initialGuideCompleted = true;
                detectLocation();
                return;
            } else if (initialGuideVersion == 6) {
                showWeatherProviderChangedDisclaimer();
                initialGuideCompleted = true;
            } else if (initialGuideVersion == 7) {
                initialGuideCompleted = true;
            }
            checkPermissionsSettingsAndShowAlert();
        } else {
            saveInitialPreferences();
            checkPermissionsSettingsAndShowAlert();
        }
    }

    private void saveInitialPreferences() {
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(this).edit();
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        locationsDbHelper.updateEnabled(autoLocation.getId(), true);
        preferences.putBoolean(Constants.KEY_PREF_LOCATION_GPS_ENABLED, true);

        preferences.putString(Constants.KEY_WAKE_UP_STRATEGY, "wakeuppartial");
        preferences.putString(Constants.KEY_PREF_LOCATION_GEOCODER_SOURCE, "location_geocoder_local");
        preferences.putBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, true);

        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 1);
        preferences.apply();
    }

    private void updateNetworkLocation() {
        if (!initialGuideCompleted || (currentLocation == null)) {
            return;
        }
        Intent startLocationUpdateIntent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        startLocationUpdateIntent.setPackage("org.thosp.yourlocalweather");
        startLocationUpdateIntent.putExtra("updateSource", "MAIN");
        startLocationUpdateIntent.putExtra("locationId", currentLocation.getId());
        ContextCompat.startForegroundService(getBaseContext(), startLocationUpdateIntent);
    }
    
    private void requestLocation() {
        if (checkPermissionsSettingsAndShowAlert()) {
            detectLocation();
        }
    }

    private void updateCurrentLocationAndButtonVisibility() {
        currentLocation = locationsDbHelper.getLocationById(AppPreference.getCurrentLocationId(MainActivity.this));
        if (currentLocation == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
        }
        if (currentLocation == null) {
            return;
        }
        appendLog(getBaseContext(), TAG, "updateCurrentLocationAndButtonVisibility:currentLocation:", currentLocation);
        switchToNextLocationWhenCurrentIsAutoAndIsDisabled();
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        int maxOrderId = locationsDbHelper.getMaxOrderId();
        appendLog(getBaseContext(), TAG, "updateCurrentLocationAndButtonVisibility:maxOrderId:", maxOrderId);
        AppPreference.setCurrentLocationId(MainActivity.this, currentLocation);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToolbarMenu != null) {
                    mToolbarMenu.findItem(R.id.main_menu_refresh).setVisible((currentLocation.getOrderId() != 0) || currentLocation.isEnabled());
                    mToolbarMenu.findItem(R.id.main_menu_detect_location).setVisible(autoLocation.isEnabled());
                }
                if ((maxOrderId > 1) ||
                        ((maxOrderId == 1) && autoLocation.isEnabled())) {
                    switchLocationButton.setVisibility(View.VISIBLE);
                } else {
                    switchLocationButton.setVisibility(View.GONE);
                }
            }
        });
    }

    protected void sendMessageToCurrentWeatherService(Location location, String updateSource) {
        if (!initialGuideCompleted) {
            return;
        }
        super.sendMessageToCurrentWeatherService(location, updateSource);
    }
}
