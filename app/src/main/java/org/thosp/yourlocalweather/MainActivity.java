package org.thosp.yourlocalweather;

import android.Manifest;
import android.app.Activity;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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

import org.thosp.yourlocalweather.model.CitySearch;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.service.CurrentWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PermissionUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WindWithUnit;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.AppPreference.getLastUpdateTimeMillis;

public class MainActivity extends BaseActivity implements AppBarLayout.OnOffsetChangedListener {

    private static final String TAG = "MainActivity";

    private static final long LOCATION_TIMEOUT_IN_MS = 30000L;

    private ImageView mIconWeatherView;
    private TextView mTemperatureView;
    private TextView mDescriptionView;
    private TextView mHumidityView;
    private TextView mWindSpeedView;
    private TextView mPressureView;
    private TextView mCloudinessView;
    private TextView mLastUpdateView;
    private TextView mSunriseView;
    private TextView mSunsetView;
    private AppBarLayout mAppBarLayout;
    private TextView mIconWindView;
    private TextView mIconHumidityView;
    private TextView mIconPressureView;
    private TextView mIconCloudinessView;
    private TextView mIconSunriseView;
    private TextView mIconSunsetView;

    private ConnectionDetector connectionDetector;
    private Boolean isNetworkAvailable;
    public static ProgressDialog mProgressDialog;
    private SwipeRefreshLayout mSwipeRefresh;
    private Menu mToolbarMenu;
    private BroadcastReceiver mWeatherUpdateReceiver;

    private WindWithUnit windWithUnit;
    private String mIconWind;
    private String mIconHumidity;
    private String mIconPressure;
    private String mIconCloudiness;
    private String mIconSunrise;
    private String mIconSunset;
    private String mPercentSign;

    private SharedPreferences mPrefWeather;
    private SharedPreferences mSharedPreferences;

    public static Weather mWeather;
    public static CitySearch mCitySearch;

    private static final int REQUEST_LOCATION = 0;
    private static final String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION,
                                                          Manifest.permission.ACCESS_FINE_LOCATION};
    public Context storedContext;
    private Handler refreshDialogHandler;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWeather = new Weather();
        mCitySearch = new CitySearch();

        weatherConditionsIcons();
        initializeTextView();
        initializeWeatherReceiver();

        connectionDetector = new ConnectionDetector(MainActivity.this);

        mPrefWeather = getSharedPreferences(Constants.PREF_WEATHER_NAME, Context.MODE_PRIVATE);
        mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                Context.MODE_PRIVATE);
        setTitle( Utils.getCityAndCountry(this));

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

        /**
         * Share weather fab
         */
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        this.storedContext = this;
        fab.setOnClickListener(fabListener);
        checkSettingsAndPermisions();
    }

    private void updateCurrentWeather() {
        windWithUnit = AppPreference.getWindWithUnit(MainActivity.this, mWeather.wind.getSpeed());
        WindWithUnit pressure = AppPreference.getPressureWithUnit(MainActivity.this, mWeather.currentCondition.getPressure());

        String lastUpdate = Utils.setLastUpdateTime(MainActivity.this,
                getLastUpdateTimeMillis(MainActivity.this));
        String sunrise = Utils.unixTimeToFormatTime(MainActivity.this, mWeather.sys.getSunrise());
        String sunset = Utils.unixTimeToFormatTime(MainActivity.this, mWeather.sys.getSunset());

        Utils.setWeatherIcon(mIconWeatherView, this);
        mTemperatureView.setText(getString(R.string.temperature_with_degree, AppPreference.getTemperatureWithUnit(getBaseContext(), mWeather.temperature.getTemp())));
        mDescriptionView.setText(Utils.getWeatherDescription(this, mWeather));
        mHumidityView.setText(getString(R.string.humidity_label,
                String.valueOf(mWeather.currentCondition.getHumidity()),
                mPercentSign));
        mPressureView.setText(getString(R.string.pressure_label, pressure.getWindSpeed(0),
                pressure.getWindUnit()));
        mWindSpeedView.setText(getString(R.string.wind_label,
                                windWithUnit.getWindSpeed(1),
                                windWithUnit.getWindUnit()));
        mCloudinessView.setText(getString(R.string.cloudiness_label,
                String.valueOf(mWeather.cloud.getClouds()),
                mPercentSign));
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        mSunriseView.setText(getString(R.string.sunrise_label, sunrise));
        mSunsetView.setText(getString(R.string.sunset_label, sunset));
        setTitle(Utils.getCityAndCountry(this));
    }

    @Override
    public void onResume() {
        super.onResume();
        checkSettingsAndPermisions();
        preLoadWeather();
        mAppBarLayout.addOnOffsetChangedListener(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mWeatherUpdateReceiver,
                new IntentFilter(
                        CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        mAppBarLayout.removeOnOffsetChangedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWeatherUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.mToolbarMenu = menu;
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_main_menu, menu);

        String locationUpdateStrategy = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_STRATEGY, "update_location_full");
        if (!"update_location_none".equals(locationUpdateStrategy)) {
            menu.findItem(R.id.main_menu_search_city).setVisible(false);
        } else {
            menu.findItem(R.id.main_menu_detect_location).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_refresh:
                if (connectionDetector.isNetworkAvailableAndConnected()) {
                    SharedPreferences mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                            Context.MODE_PRIVATE);
                    mSharedPreferences.edit().putString(Constants.APP_SETTINGS_UPDATE_SOURCE, "-").apply();
                    Intent intent = new Intent(this, CurrentWeatherService.class);
                    intent.putExtra("updateSource", "MAIN");
                    startService(intent);
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
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(intent, PICK_CITY);
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
                        SharedPreferences mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                                Context.MODE_PRIVATE);
                        mSharedPreferences.edit().putString(Constants.APP_SETTINGS_UPDATE_SOURCE, "-").apply();
                        Intent intent = new Intent(MainActivity.this, CurrentWeatherService.class);
                        intent.putExtra("updateSource", "MAIN");
                        startService(intent);
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.connection_not_found,
                                Toast.LENGTH_SHORT).show();
                        mSwipeRefresh.setRefreshing(false);
                    }
                }
            };

    private void preLoadWeather() {
        String lastUpdate = Utils.setLastUpdateTime(this,
                AppPreference.getLastUpdateTimeMillis(this));

        float temperaturePref = mPrefWeather.getFloat(Constants.WEATHER_DATA_TEMPERATURE, 0);
        int humidity = mPrefWeather.getInt(Constants.WEATHER_DATA_HUMIDITY, 0);
        float pressurePref = mPrefWeather.getFloat(Constants.WEATHER_DATA_PRESSURE, 0);
        float windPref = mPrefWeather.getFloat(Constants.WEATHER_DATA_WIND_SPEED, 0);
        int clouds = mPrefWeather.getInt(Constants.WEATHER_DATA_CLOUDS, 0);
        long sunrisePref = mPrefWeather.getLong(Constants.WEATHER_DATA_SUNRISE, -1);
        long sunsetPref = mPrefWeather.getLong(Constants.WEATHER_DATA_SUNSET, -1);

        windWithUnit = AppPreference.getWindWithUnit(this, windPref);
        WindWithUnit pressure = AppPreference.getPressureWithUnit(this, pressurePref);
        String sunrise = Utils.unixTimeToFormatTime(this, sunrisePref);
        String sunset = Utils.unixTimeToFormatTime(this, sunsetPref);

        Utils.setWeatherIcon(mIconWeatherView, this);
        mTemperatureView.setText(getString(R.string.temperature_with_degree,
                AppPreference.getTemperatureWithUnit(this, temperaturePref)));
        mDescriptionView.setText(Utils.getWeatherDescription(this));
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        mHumidityView.setText(getString(R.string.humidity_label,
                String.valueOf(humidity),
                mPercentSign));
        mPressureView.setText(getString(R.string.pressure_label,
                pressure.getWindSpeed(0),
                pressure.getWindUnit()));
        mWindSpeedView.setText(getString(R.string.wind_label,
                                         windWithUnit.getWindSpeed(1),
                                         windWithUnit.getWindUnit()));
        mCloudinessView.setText(getString(R.string.cloudiness_label,
                String.valueOf(clouds),
                mPercentSign));
        mSunriseView.setText(getString(R.string.sunrise_label, sunrise));
        mSunsetView.setText(getString(R.string.sunset_label, sunset));
        setTitle( Utils.getCityAndCountry(this));
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
        mDescriptionView = (TextView) findViewById(R.id.main_description);
        mPressureView = (TextView) findViewById(R.id.main_pressure);
        mHumidityView = (TextView) findViewById(R.id.main_humidity);
        mWindSpeedView = (TextView) findViewById(R.id.main_wind_speed);
        mCloudinessView = (TextView) findViewById(R.id.main_cloudiness);
        mLastUpdateView = (TextView) findViewById(R.id.main_last_update);
        mSunriseView = (TextView) findViewById(R.id.main_sunrise);
        mSunsetView = (TextView) findViewById(R.id.main_sunset);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.main_app_bar);

        mTemperatureView.setTypeface(robotoThin);
        mWindSpeedView.setTypeface(robotoLight);
        mHumidityView.setTypeface(robotoLight);
        mPressureView.setTypeface(robotoLight);
        mCloudinessView.setTypeface(robotoLight);
        mSunriseView.setTypeface(robotoLight);
        mSunsetView.setTypeface(robotoLight);

        /**
         * Initialize and configure weather icons
         */
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
    }

    private void weatherConditionsIcons() {
        mIconWind = getString(R.string.icon_wind);
        mIconHumidity = getString(R.string.icon_humidity);
        mIconPressure = getString(R.string.icon_barometer);
        mIconCloudiness = getString(R.string.icon_cloudiness);
        mPercentSign = getString(R.string.percent_sign);
        mIconSunrise = getString(R.string.icon_sunrise);
        mIconSunset = getString(R.string.icon_sunset);
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
                if ((mProgressDialog != null) && (refreshDialogHandler != null)) {
                    refreshDialogHandler.post(new Runnable() {
                        public void run() {
                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                                mProgressDialog = null;
                            }
                        }
                    });
                }
                switch (intent.getStringExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT)) {
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_OK:
                        mSwipeRefresh.setRefreshing(false);
                        setUpdateButtonState(false);
                        updateCurrentWeather();
                        break;
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                        mSwipeRefresh.setRefreshing(false);
                        setUpdateButtonState(false);
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
            String temperatureWithUnit = AppPreference.getTemperatureWithUnit(
                    MainActivity.this,
                    Math.round(mPrefWeather.getFloat(Constants.WEATHER_DATA_TEMPERATURE, 0)));
            windWithUnit = AppPreference.getWindWithUnit(
                    MainActivity.this,
                    mPrefWeather.getFloat(Constants.WEATHER_DATA_WIND_SPEED, 0));
            String weather;
            String city;
            String description;
            String wind;
            String sunrise;
            String sunset;
            city = mSharedPreferences.getString(Constants.APP_SETTINGS_CITY, "London");
            if(AppPreference.isUpdateLocationEnabled(storedContext)) {
                city = mSharedPreferences.getString(Constants.APP_SETTINGS_GEO_CITY, "London");
            }
            description = Utils.getWeatherDescription(MainActivity.this);
            sunrise = Utils.unixTimeToFormatTime(MainActivity.this, mPrefWeather
                    .getLong(Constants.WEATHER_DATA_SUNRISE, -1));
            sunset = Utils.unixTimeToFormatTime(MainActivity.this, mPrefWeather
                    .getLong(Constants.WEATHER_DATA_SUNSET, -1));
            weather = "City: " + city +
                    "\nTemperature: " + temperatureWithUnit +
                    "\nDescription: " + description +
                    "\nWind: " + windWithUnit.getWindSpeed(1) + " " + windWithUnit.getWindUnit() +
                    "\nSunrise: " + sunrise +
                    "\nSunset: " + sunset;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, weather);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(Intent.createChooser(shareIntent, "Share Weather"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(MainActivity.this,
                        "Communication app not found",
                        Toast.LENGTH_LONG).show();
            }
        }
    };

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
        String locationUpdateStrategy = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_STRATEGY, "update_location_full");
        if ("update_location_none".equals(locationUpdateStrategy)) {
            return true;
        }
        AlertDialog.Builder settingsAlert = new AlertDialog.Builder(MainActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_location_permission_title);

        LocationManager locationManager = (LocationManager) getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
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
            String geocoder = AppPreference.getLocationGeocoderSource(getBaseContext());
            List<String> permissions = new ArrayList<>();
            StringBuilder notificationMessage = new StringBuilder();
            if (AppPreference.isGpsEnabledByPreferences(getBaseContext()) &&
                    isGPSEnabled &&
                    ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_phone_settings) + "\n\n");
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (isNetworkEnabled) {
                if ("location_geocoder_local".equals(geocoder) && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_phone_permission));
                    permissions.add(Manifest.permission.READ_PHONE_STATE);
                } else if ("location_geocoder_system".equals(geocoder) && ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    notificationMessage.append(getString(R.string.alertDialog_location_permission_message_location_network_permission));
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
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

    private volatile boolean initialGuideCompleted;
    private volatile int initialGuidePage;
    private int selectedUpdateLocationStrategy;
    private int selectedLocationAndAddressSourceStrategy;
    private int selectedWakeupStrategyStrategy;
    private int selectedCacheLocationStrategy;

    private void checkSettingsAndPermisions() {
        if (!initialGuideCompleted) {
            checkAndShowInitialGuide();
            return;
        }
        checkPermissionsSettingsAndShowAlert();
    }

    private void checkAndShowInitialGuide() {
        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion > 0) {
            initialGuideCompleted = true;
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
                            permissionsAndSettingsRequested = false;
                        } else {
                            initialGuidePage--;
                            showInitialGuidePage(initialGuidePage);
                        }
                    }
                });
    }

    private void saveInitialPreferences() {
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(this).edit();

        String selectedUpdateLocationStrategyString = "update_location_full";
        switch (selectedUpdateLocationStrategy) {
            case 0: selectedUpdateLocationStrategyString = "update_location_none"; break;
            case 1: selectedUpdateLocationStrategyString = "update_location_full"; break;
            case 2: selectedUpdateLocationStrategyString = "update_location_network_only"; break;
        }
        preferences.putString(Constants.KEY_PREF_LOCATION_UPDATE_STRATEGY, selectedUpdateLocationStrategyString);

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

        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 1);
        preferences.apply();
    }

    private void updateNetworkLocation() {
        Intent startLocationUpdateIntent = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
        startLocationUpdateIntent.setPackage("org.thosp.yourlocalweather");
        startLocationUpdateIntent.putExtra("updateSource", "MAIN");
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
                break;
        }
    }
}
