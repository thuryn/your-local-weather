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
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.thosp.yourlocalweather.databinding.ActivityMainBinding;
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

    private WearActivityManager wearServiceManager = new WearActivityManagerImpl(MainActivity.this);

    private volatile boolean inited;

    // 1. Definujeme vygenerovanou třídu bindingu
    private ActivityMainBinding binding;

    private ConnectionDetector connectionDetector;
    public static ProgressDialog mProgressDialog;
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
    public Context storedContext;
    private Handler refreshDialogHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        locationsDbHelper = LocationsDbHelper.getInstance(MainActivity.this);
        weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(MainActivity.this);
        currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(MainActivity.this);

        final Context appContext = this.getApplicationContext();
        final java.lang.ref.WeakReference<MainActivity> activityRef = new java.lang.ref.WeakReference<>(this);

        YourLocalWeather.executor.submit(() -> {
            ConnectionDetector detector = new ConnectionDetector(appContext);
            String timeStyle = AppPreference.getTimeStylePreference(appContext);
            String pressUnit = AppPreference.getPressureUnitFromPreferences(appContext);
            String windUnit = AppPreference.getWindUnitFromPreferences(appContext);
            String tempUnit = AppPreference.getTemperatureUnitFromPreferences(appContext);
            String tempType = AppPreference.getTemeratureTypeFromPreferences(appContext);

            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            activity.runOnUiThread(() -> {
                MainActivity act = activityRef.get();
                if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                    act.connectionDetector = detector;
                    act.timeStylePreference = timeStyle;
                    act.pressureUnitFromPreferences = pressUnit;
                    act.windUnitFromPreferences = windUnit;
                    act.temperatureUnitFromPreferences = tempUnit;
                    act.temeratureTypeFromPreferences = tempType;

                    act.initializeWeatherReceiver();
                    act.checkSettingsAndPermisions();
                    act.inited = true;
                    act.updateActivityOnResume();
                }
            });
        });

        // 2. Nafoukneme layout pomocí bindingu a nastavíme Root View
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //setTitle(R.string.label_activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        weatherConditionsIcons();
        initializeTextView();

        StartAlarmsTask startAlarmsTask = new StartAlarmsTask();
        startAlarmsTask.execute(new Integer[0]);
        Intent intentToStartUpdate = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        intentToStartUpdate.setPackage(getBaseContext().getPackageName());
        startService(intentToStartUpdate);

        // 3. Konfigurace SwipeRefreshLayout a ScrollView přes binding
        int top_to_padding = 150;
        binding.mainContent.mainSwipeRefresh.setProgressViewOffset(false, 0, top_to_padding);
        binding.mainContent.mainSwipeRefresh.setColorSchemeResources(R.color.swipe_red, R.color.swipe_green, R.color.swipe_blue);
        binding.mainContent.mainSwipeRefresh.setOnRefreshListener(swipeRefreshListener);

        binding.mainContent.mainScrollView.setOnTouchListener(new ActivityTransitionTouchListener(
                null,
                WeatherForecastActivity.class, this));

        this.storedContext = this;
        binding.fab.setOnClickListener(fabListener);
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
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_ALARM_SERVICE");
        intentToStartUpdate.setPackage(getBaseContext().getPackageName());
        startService(intentToStartUpdate);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mainAppBar.addOnOffsetChangedListener(this);
        ContextCompat.registerReceiver(this, mWeatherUpdateReceiver,
                new IntentFilter(
                        UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        if (inited) {
            YourLocalWeather.executor.submit(() -> {
                updateActivityOnResume();
            });
        }
    }

    private void updateActivityOnResume() {
        updateCurrentLocationAndButtonVisibility();
        checkSettingsAndPermisions();
        if (inited) {
            updateUI();
        }
        wearServiceManager.checkWearables();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mProgressDialog != null) {
            runOnUiThread(() -> {
                mProgressDialog.dismiss();
            });
        }
        if (binding != null) {
            binding.mainAppBar.removeOnOffsetChangedListener(this);
        }
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
        binding = null; // 4. Uvolnění reference
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
                    if (binding == null) return;
                    Boolean isNetworkAvailable = connectionDetector.isNetworkAvailableAndConnected();
                    if (isNetworkAvailable && (currentLocation != null)) {
                        currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
                        sendMessageToCurrentWeatherService(currentLocation, "MAIN");
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.connection_not_found,
                                Toast.LENGTH_SHORT).show();
                        binding.mainContent.mainSwipeRefresh.setRefreshing(false);
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
            if (binding != null) {
                binding.mainHeader.mainLastUpdate.setText(MainActivity.this.getString(R.string.last_update_label, lastUpdate));
                localityView.setText(cityAndCountry);
            }
        });
    }

    @Override
    protected void updateUI() {
        if (binding == null || currentLocation == null) {
            appendLog(MainActivity.this, TAG, "updateUI skipping - view template not ready or no currentLocation");
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
        String weatherDescription = Utils.getWeatherDescription(MainActivity.this, weather);
        String pressureValue = pressure.getPressure(AppPreference.getPressureDecimalPlaces(pressureUnitFromPreferences));
        String cityAndCountry = Utils.getCityAndCountry(MainActivity.this, currentLocation);
        boolean fontBasedIconSet = "weather_icon_set_fontbased".equals(AppPreference.getIconSet(MainActivity.this));
        int textColor = AppPreference.getTextColor(MainActivity.this);

        runOnUiThread(() -> {
            if (binding == null) return;
            try {
                Utils.setWeatherIcon(binding.mainHeader.mainWeatherIcon, MainActivity.this, weatherRecord, textColor, fontBasedIconSet);
                binding.mainHeader.mainTemperature.setText(MainActivity.this.getString(R.string.temperature_with_degree, temperatureWithUnit));
                binding.mainContent.mainDewPoint.setText(MainActivity.this.getString(R.string.dew_point_label, dewPointWithUnit));
                if (secondTemperature != null) {
                    binding.mainContent.mainSecondTemperature.setText(secondTemperature);
                    binding.mainContent.mainSecondTemperature.setVisibility(View.VISIBLE);
                    binding.mainContent.mainSecondTemperatureIcon.setVisibility(View.VISIBLE);
                } else {
                    binding.mainContent.mainSecondTemperature.setVisibility(View.GONE);
                    binding.mainContent.mainSecondTemperatureIcon.setVisibility(View.GONE);
                }
                binding.mainHeader.mainDescription.setText(weatherDescription);
                binding.mainHeader.mainLastUpdate.setText(MainActivity.this.getString(R.string.last_update_label, lastUpdate));
                binding.mainContent.mainHumidity.setText(MainActivity.this.getString(R.string.humidity_label,
                        String.valueOf(weather.getHumidity()),
                        mPercentSign));
                binding.mainContent.mainPressure.setText(MainActivity.this.getString(R.string.pressure_label,
                        pressureValue,
                        pressure.getPressureUnit()));
                binding.mainContent.mainWindSpeed.setText(MainActivity.this.getString(R.string.wind_label,
                        windWithUnit.getWindSpeed(1),
                        windWithUnit.getWindUnit(),
                        windWithUnit.getWindDirection()));
                binding.mainContent.mainCloudiness.setText(MainActivity.this.getString(R.string.cloudiness_label,
                        String.valueOf(weather.getClouds()),
                        mPercentSign));
                binding.mainContent.mainSunrise.setText(MainActivity.this.getString(R.string.sunrise_label, sunrise));
                binding.mainContent.mainSunset.setText(MainActivity.this.getString(R.string.sunset_label, sunset));
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
                if (binding == null) return;
                binding.mainHeader.mainTemperature.setText(MainActivity.this.getString(R.string.temperature_with_degree, ""));
                binding.mainContent.mainDewPoint.setText(MainActivity.this.getString(R.string.dew_point_label, ""));

                if ("measured_only".equals(temperatureTypeFromPreferences) ||
                        "appearance_only".equals(temperatureTypeFromPreferences)) {
                    binding.mainContent.mainSecondTemperature.setVisibility(View.GONE);
                    binding.mainContent.mainSecondTemperatureIcon.setVisibility(View.GONE);
                } else {
                    binding.mainContent.mainSecondTemperature.setVisibility(View.VISIBLE);
                    binding.mainContent.mainSecondTemperatureIcon.setVisibility(View.VISIBLE);
                    binding.mainContent.mainSecondTemperature.setText(MainActivity.this.getString(R.string.label_apparent_temperature, ""));
                }
                binding.mainHeader.mainDescription.setText(R.string.location_not_found);
                binding.mainHeader.mainLastUpdate.setText(MainActivity.this.getString(R.string.last_update_label, ""));
                binding.mainContent.mainHumidity.setText(MainActivity.this.getString(R.string.humidity_label, "", ""));
                binding.mainContent.mainPressure.setText(MainActivity.this.getString(R.string.pressure_label, "", ""));
                binding.mainContent.mainWindSpeed.setText(MainActivity.this.getString(R.string.wind_label, "", "", ""));
                binding.mainContent.mainCloudiness.setText(MainActivity.this.getString(R.string.cloudiness_label, "", ""));
                binding.mainContent.mainSunrise.setText(MainActivity.this.getString(R.string.sunrise_label, ""));
                binding.mainContent.mainSunset.setText(MainActivity.this.getString(R.string.sunset_label, ""));
            }
        });
    }

    private void initializeTextView() {
        Typeface weatherFontIcon = ResourcesCompat.getFont(this, R.font.weathericons);
        Typeface robotoThin = Typeface.createFromAsset(this.getAssets(), "fonts/Roboto-Thin.ttf");
        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(), "fonts/Roboto-Light.ttf");

        localityView = binding.mainHeader.mainLocality;

        binding.mainHeader.mainTemperature.setTypeface(robotoThin);
        binding.mainContent.mainDewPoint.setTypeface(robotoLight);
        binding.mainContent.mainSecondTemperature.setTypeface(robotoLight);
        binding.mainContent.mainWindSpeed.setTypeface(robotoLight);
        binding.mainContent.mainHumidity.setTypeface(robotoLight);
        binding.mainContent.mainPressure.setTypeface(robotoLight);
        binding.mainContent.mainCloudiness.setTypeface(robotoLight);
        binding.mainContent.mainSunrise.setTypeface(robotoLight);
        binding.mainContent.mainSunset.setTypeface(robotoLight);
        localityView.setTypeface(robotoLight);

        binding.mainContent.mainSecondTemperatureIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainSecondTemperatureIcon.setText(iconSecondTemperature);

        binding.mainContent.mainWindIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainWindIcon.setText(mIconWind);

        binding.mainContent.mainHumidityIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainHumidityIcon.setText(mIconHumidity);

        binding.mainContent.mainPressureIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainPressureIcon.setText(mIconPressure);

        binding.mainContent.mainCloudinessIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainCloudinessIcon.setText(mIconCloudiness);

        binding.mainContent.mainSunriseIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainSunriseIcon.setText(mIconSunrise);

        binding.mainContent.mainSunsetIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainSunsetIcon.setText(mIconSunset);

        binding.mainContent.mainDewPointIcon.setTypeface(weatherFontIcon);
        binding.mainContent.mainDewPointIcon.setText(mIconDewPoint);
    }

    private void weatherConditionsIcons() {
        mIconWind = getString(R.string.wi_windy);
        mIconHumidity = getString(R.string.wi_humidity);
        mIconPressure = getString(R.string.wi_barometer);
        mIconCloudiness = getString(R.string.wi_cloud);
        mPercentSign = getString(R.string.percent_sign);
        mIconSunrise = getString(R.string.wi_sunrise);
        mIconSunset = getString(R.string.wi_sunset);
        iconSecondTemperature = getString(R.string.wi_thermometer);
        mIconDewPoint = getString(R.string.wi_raindrop);
    }

    private void setUpdateButtonState(boolean isUpdate) {
        if (mToolbarMenu != null && binding != null) {
            MenuItem updateItem = mToolbarMenu.findItem(R.id.main_menu_refresh);
            if (isUpdate) {
                updateItem.setVisible(false);
                binding.toolbarProgressBar.setVisibility(View.VISIBLE);
            } else {
                binding.toolbarProgressBar.setVisibility(View.GONE);
                updateItem.setVisible(true);
            }
        }
    }

    private void initializeWeatherReceiver() {
        final java.lang.ref.WeakReference<MainActivity> activityRef = new java.lang.ref.WeakReference<>(this);

        mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity activity = activityRef.get();
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                if ((mProgressDialog != null) && (activity.refreshDialogHandler != null)) {
                    activity.refreshDialogHandler.post(() -> {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                        }
                    });
                }

                final String actionResult = intent.getStringExtra(UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT);

                YourLocalWeather.executor.submit(() -> {
                    MainActivity actInside = activityRef.get();
                    if (actInside == null || !actInside.initialGuideCompleted) {
                        return;
                    }

                    if (actionResult == null) return;

                    switch (actionResult) {
                        case UpdateWeatherService.ACTION_WEATHER_UPDATE_OK:
                            actInside.runOnUiThread(() -> {
                                MainActivity act = activityRef.get();
                                if (act != null && act.binding != null) {
                                    act.binding.mainContent.mainSwipeRefresh.setRefreshing(false);
                                    act.setUpdateButtonState(false);
                                    act.updateLocationCityTimeAndSource();
                                    YourLocalWeather.executor.submit(() -> act.updateUI());
                                }
                            });
                            break;

                        case UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                            actInside.runOnUiThread(() -> {
                                MainActivity act = activityRef.get();
                                if (act != null && act.binding != null) {
                                    act.binding.mainContent.mainSwipeRefresh.setRefreshing(false);
                                    act.setUpdateButtonState(false);
                                    act.updateLocationCityTimeAndSource();
                                    Toast.makeText(act, act.getString(R.string.toast_parse_error), Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                    }
                });
            }
        };
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (binding != null) {
            binding.mainContent.mainSwipeRefresh.setEnabled(verticalOffset == 0);
        }
    }

    FloatingActionButton.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final java.lang.ref.WeakReference<MainActivity> activityRef = new java.lang.ref.WeakReference<>(MainActivity.this);
            final Context appContext = MainActivity.this.getApplicationContext();

            YourLocalWeather.executor.submit(() -> {
                MainActivity activity = activityRef.get();
                if (activity == null || activity.currentLocation == null) {
                    return;
                }

                CurrentWeatherDbHelper dbHelper = CurrentWeatherDbHelper.getInstance(appContext);
                CurrentWeatherDbHelper.WeatherRecord currentWeatherRecord = dbHelper.getWeather(activity.currentLocation.getId());

                if (currentWeatherRecord == null || currentWeatherRecord.getWeather() == null) {
                    activity.runOnUiThread(() -> {
                        MainActivity act = activityRef.get();
                        if (act != null) Toast.makeText(act, act.getString(R.string.current_weather_has_not_been_fetched), Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Weather weather = currentWeatherRecord.getWeather();

                String tempType = activity.temeratureTypeFromPreferences;
                String windUnit = activity.windUnitFromPreferences;
                String timeStyle = activity.timeStylePreference;
                String tempUnit = activity.temperatureUnitFromPreferences;

                String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                        appContext, weather, activity.currentLocation.getLatitude(),
                        currentWeatherRecord.getLastUpdatedTime(), tempType, tempUnit, activity.currentLocation.getLocale());

                WindWithUnit localWindWithUnit = AppPreference.getWindWithUnit(
                        appContext, weather.getWindSpeed(), weather.getWindDirection(), windUnit, activity.currentLocation.getLocale());

                String description = Utils.getWeatherDescription(appContext, weather);
                String sunrise = Utils.unixTimeToFormatTime(appContext, weather.getSunrise(), timeStyle, activity.currentLocation.getLocale());
                String sunset = Utils.unixTimeToFormatTime(appContext, weather.getSunset(), timeStyle, activity.currentLocation.getLocale());

                String weatherDescription = appContext.getString(R.string.share_weather_descritpion,
                        Utils.getLocationForSharingFromAddress(activity.currentLocation.getAddress()),
                        temperatureWithUnit, description, localWindWithUnit.getWindSpeed(1),
                        localWindWithUnit.getWindUnit(), sunrise, sunset);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, weatherDescription);

                activity.runOnUiThread(() -> {
                    MainActivity act = activityRef.get();
                    if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                        try {
                            Intent chooser = Intent.createChooser(shareIntent, act.getString(R.string.share_weather_title));
                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            act.startActivity(chooser);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(act, act.getString(R.string.share_weather_app_not_found), Toast.LENGTH_LONG).show();
                        }
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
                                if (binding != null) {
                                    Snackbar.make(binding.getRoot(), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
                                }
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
                        if (binding != null) Snackbar.make(binding.getRoot(), R.string.permission_available_location, Snackbar.LENGTH_SHORT).show();
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
                if (binding != null) Snackbar.make(binding.getRoot(), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
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
            if (isNetworkEnabled && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                                        if (binding != null) Snackbar.make(binding.getRoot(), R.string.permission_available_location, Snackbar.LENGTH_SHORT).show();
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
                    if (binding != null) Snackbar.make(binding.getRoot(), R.string.permission_available_notification, Snackbar.LENGTH_SHORT).show();
                } else {
                    if (binding != null) Snackbar.make(binding.getRoot(), R.string.permission_not_granted, Snackbar.LENGTH_SHORT).show();
                }
            });

    private void checkBatteryOptimization() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion != 2) {
            return;
        }
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        startLocationUpdateIntent.setPackage(getBaseContext().getPackageName());
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
        runOnUiThread(() -> {
            if (mToolbarMenu != null) {
                mToolbarMenu.findItem(R.id.main_menu_refresh).setVisible((currentLocation.getOrderId() != 0) || currentLocation.isEnabled());
                mToolbarMenu.findItem(R.id.main_menu_detect_location).setVisible(autoLocation.isEnabled());
            }
            if ((maxOrderId > 1) ||
                    ((maxOrderId == 1) && autoLocation.isEnabled())) {
                binding.mainHeader.mainSwitchLocation.setVisibility(View.VISIBLE);
            } else {
                binding.mainHeader.mainSwitchLocation.setVisibility(View.GONE);
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