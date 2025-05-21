package org.thosp.yourlocalweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.settings.CurrentWeatherDetailSwitchListener;
import org.thosp.yourlocalweather.settings.GraphValuesSwitchListener;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.MoreWidgetProvider;
import org.thosp.yourlocalweather.widget.WeatherForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.WeatherGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.WidgetActions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WidgetSettingsDialogue extends Activity {

    private static final String TAG = "WidgetSettingsDialogue";

    private static final int NUMBER_OF_WEATHER_DETAIL_OPTIONS = 7;
    private static final int DEFAULT_NUMBER_OF_AVAILABLE_DETAIL_OPTIONS_IN_WIDGET = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        String settingOption = getIntent().getStringExtra("settings_option");
        ArrayList<String> widgetActionPlaces = getIntent().getStringArrayListExtra("widget_action_places");

        switch (settingOption) {
            case "detailsSetting": createDetailsSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "graphSetting": createGraphSettingDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "forecastSettings": createForecastSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "locationSettings": createLocationSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "widgetActionSettings": createWidgetActionSettingsDialog(
                    getIntent().getIntExtra("widgetId", 0),
                    widgetActionPlaces); break;
        }
    }

    private WidgetDefaultDetailsResult getNumberOfCurrentWeatherDetails(int widgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, ExtLocationWithForecastGraphWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int currentWidgetId: widgetIds) {
            if (currentWidgetId == widgetId) {
                return new WidgetDefaultDetailsResult(
                        ExtLocationWithForecastGraphWidgetProvider.getNumberOfCurrentWeatherDetails(),
                        ExtLocationWithForecastGraphWidgetProvider.getDefaultCurrentWeatherDetails());
            }
        }
        widgetComponent = new ComponentName(this, ExtLocationWidgetProvider.class);
        widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int currentWidgetId: widgetIds) {
            if (currentWidgetId == widgetId) {
                return new WidgetDefaultDetailsResult(
                        ExtLocationWidgetProvider.getNumberOfCurrentWeatherDetails(),
                        ExtLocationWidgetProvider.getDefaultCurrentWeatherDetails());
            }
        }
        widgetComponent = new ComponentName(this, ExtLocationWithForecastWidgetProvider.class);
        widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int currentWidgetId: widgetIds) {
            if (currentWidgetId == widgetId) {
                return new WidgetDefaultDetailsResult(
                        ExtLocationWithForecastWidgetProvider.getNumberOfCurrentWeatherDetails(),
                        ExtLocationWithForecastWidgetProvider.getDefaultCurrentWeatherDetails());
            }
        }
        widgetComponent = new ComponentName(this, ExtLocationWithGraphWidgetProvider.class);
        widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int currentWidgetId: widgetIds) {
            if (currentWidgetId == widgetId) {
                return new WidgetDefaultDetailsResult(
                        ExtLocationWithGraphWidgetProvider.getNumberOfCurrentWeatherDetails(),
                        ExtLocationWithGraphWidgetProvider.getDefaultCurrentWeatherDetails());
            }
        }
        widgetComponent = new ComponentName(this, MoreWidgetProvider.class);
        widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int currentWidgetId: widgetIds) {
            if (currentWidgetId == widgetId) {
                return new WidgetDefaultDetailsResult(
                        MoreWidgetProvider.getNumberOfCurrentWeatherDetails(),
                        MoreWidgetProvider.getDefaultCurrentWeatherDetails());
            }
        }
        return new WidgetDefaultDetailsResult(
                DEFAULT_NUMBER_OF_AVAILABLE_DETAIL_OPTIONS_IN_WIDGET,
                "0,1,2,3");
    }

    @SuppressLint("MissingInflatedId")
    private void createDetailsSettingsDialog(final int widgetId) {
        final Set<Integer> mSelectedItems = new HashSet<>();
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(WidgetSettingsDialogue.this);

        WidgetDefaultDetailsResult currentWeatherDetailsAvailableInWidget = getNumberOfCurrentWeatherDetails(widgetId);

        String storedCurrentWeatherDetails = widgetSettingsDbHelper.getParamString(widgetId, "currentWeatherDetails");
        if (storedCurrentWeatherDetails == null) {
            storedCurrentWeatherDetails = currentWeatherDetailsAvailableInWidget.getDefaultDetails();
        }

        Set<Integer> currentWeatherDetailValues = WidgetUtils.getCurrentWeatherDetailsFromSettings(
                storedCurrentWeatherDetails);

        boolean[] checkedItems = new boolean[NUMBER_OF_WEATHER_DETAIL_OPTIONS];
        for (Integer visibleDetail: currentWeatherDetailValues) {
            mSelectedItems.add(visibleDetail);
            checkedItems[visibleDetail] = true;
        }

        boolean fullSetOfOptions = currentWeatherDetailValues.size() >= currentWeatherDetailsAvailableInWidget.getMaxNumberOfDetails();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View forecastSettingView = inflater.inflate(R.layout.widget_setting_weather_detail, null);
        Switch[] switches = new Switch[NUMBER_OF_WEATHER_DETAIL_OPTIONS];
        final CurrentWeatherDetailSwitchListener[] switchListeners = new CurrentWeatherDetailSwitchListener[NUMBER_OF_WEATHER_DETAIL_OPTIONS];
        switches[0] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_wind_switch);
        switches[1] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_humidity_switch);
        switches[2] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_pressure_switch);
        switches[3] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_cloudiness_switch);
        switches[4] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_dew_point_switch);
        switches[5] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_sunrise_switch);
        switches[6] = forecastSettingView.findViewById(R.id.widget_setting_weather_detail_sunset_switch);
        for (int i = 0; i < NUMBER_OF_WEATHER_DETAIL_OPTIONS; i++) {
            switches[i].setEnabled(checkedItems[i] || !fullSetOfOptions);
            switches[i].setChecked(checkedItems[i]);
            switchListeners[i] = new CurrentWeatherDetailSwitchListener(
                    checkedItems[i],
                    switches,
                    i,
                    currentWeatherDetailsAvailableInWidget.getMaxNumberOfDetails());
            switches[i].setOnCheckedChangeListener(switchListeners[i]);
        }
        
        builder.setTitle(R.string.widget_details_setting_button)
                .setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        StringBuilder valuesToStore = new StringBuilder();

                        for (int i = 0; i < NUMBER_OF_WEATHER_DETAIL_OPTIONS; i++) {
                            if (switchListeners[i].isChecked()) {
                                valuesToStore.append(i);
                                valuesToStore.append(",");
                            }
                        }

                        widgetSettingsDbHelper.saveParamString(widgetId, "currentWeatherDetails", valuesToStore.toString());

                        Intent refreshWidgetIntent = new Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
                        refreshWidgetIntent.setPackage("org.thosp.yourlocalweather");
                        sendBroadcast(refreshWidgetIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    @SuppressLint("MissingInflatedId")
    private void createWidgetActionSettingsDialog(final int widgetId, final ArrayList<String> widgetActionPlaces) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View forecastSettingView = inflater.inflate(R.layout.widget_setting_actions, null);

        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);

        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.widget_actions, android.R.layout.simple_spinner_item);

        Spinner cityActions = forecastSettingView.findViewById(R.id.widget_setting_actions_city);
        TextView cityActionLabel = forecastSettingView.findViewById(R.id.widget_setting_actions_city_label);
        final WidgetActionListener cityActionsListener;
        if (widgetActionPlaces.contains("action_city")) {
            cityActions.setVisibility(View.VISIBLE);
            cityActionLabel.setVisibility(View.VISIBLE);
            WidgetActions cityAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_city"), "action_city");
            cityActions.setAdapter(adapter);
            cityActions.setSelection(cityAction.getComboSelection());
            cityActionsListener = new WidgetActionListener(cityAction);
            cityActions.setOnItemSelectedListener(cityActionsListener);
        } else {
            cityActions.setVisibility(View.GONE);
            cityActionLabel.setVisibility(View.GONE);
            cityActionsListener = null;
        }

        Spinner mainIconActions = forecastSettingView.findViewById(R.id.widget_setting_actions_main_icon);
        TextView mainIconActionsLabel = forecastSettingView.findViewById(R.id.widget_setting_actions_main_icon_label);
        final WidgetActionListener mainIconActionsListener;
        if (widgetActionPlaces.contains("action_current_weather_icon")) {
            mainIconActions.setVisibility(View.VISIBLE);
            mainIconActionsLabel.setVisibility(View.VISIBLE);
            WidgetActions mainIconAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_current_weather_icon"), "action_current_weather_icon");
            mainIconActions.setAdapter(adapter);
            mainIconActions.setSelection(mainIconAction.getComboSelection());
            mainIconActionsListener = new WidgetActionListener(mainIconAction);
            mainIconActions.setOnItemSelectedListener(mainIconActionsListener);
        } else {
            mainIconActions.setVisibility(View.GONE);
            mainIconActionsLabel.setVisibility(View.GONE);
            mainIconActionsListener = null;
        }

        Spinner forecastActions = forecastSettingView.findViewById(R.id.widget_setting_actions_forecast);
        TextView forecastActionsLabel = forecastSettingView.findViewById(R.id.widget_setting_actions_forecast_label);
        final WidgetActionListener forecastActionsListener;
        if (widgetActionPlaces.contains("action_forecast")) {
            forecastActions.setVisibility(View.VISIBLE);
            forecastActionsLabel.setVisibility(View.VISIBLE);
            WidgetActions forecastAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_forecast"), "action_forecast");
            forecastActions.setAdapter(adapter);
            forecastActions.setSelection(forecastAction.getComboSelection());
            forecastActionsListener = new WidgetActionListener(forecastAction);
            forecastActions.setOnItemSelectedListener(forecastActionsListener);
        } else {
            forecastActions.setVisibility(View.GONE);
            forecastActionsLabel.setVisibility(View.GONE);
            forecastActionsListener = null;
        }

        Spinner graphActions = forecastSettingView.findViewById(R.id.widget_setting_actions_graph);
        TextView graphActionsLabel = forecastSettingView.findViewById(R.id.widget_setting_actions_graph_label);
        final WidgetActionListener graphActionsListener;
        if (widgetActionPlaces.contains("action_graph")) {
            graphActions.setVisibility(View.VISIBLE);
            graphActionsLabel.setVisibility(View.VISIBLE);
            WidgetActions graphAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_graph"), "action_graph");
            graphActions.setAdapter(adapter);
            graphActions.setSelection(graphAction.getComboSelection());
            graphActionsListener = new WidgetActionListener(graphAction);
            graphActions.setOnItemSelectedListener(graphActionsListener);
        } else {
            graphActions.setVisibility(View.GONE);
            graphActionsLabel.setVisibility(View.GONE);
            graphActionsListener = null;
        }

        builder.setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (cityActionsListener != null) {
                            widgetSettingsDbHelper.saveParamLong(
                                    widgetId,
                                    "action_city",
                                    cityActionsListener.getWidgetAction().getId());
                        }
                        if (mainIconActionsListener != null) {
                            widgetSettingsDbHelper.saveParamLong(
                                    widgetId,
                                    "action_current_weather_icon",
                                    mainIconActionsListener.getWidgetAction().getId());
                        }
                        if (forecastActionsListener != null) {
                            widgetSettingsDbHelper.saveParamLong(
                                    widgetId,
                                    "action_forecast",
                                    forecastActionsListener.getWidgetAction().getId());
                        }
                        if (graphActionsListener != null) {
                            widgetSettingsDbHelper.saveParamLong(
                                    widgetId,
                                    "action_graph",
                                    graphActionsListener.getWidgetAction().getId());
                        }
                        Intent intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_SETTINGS);
                        intent.setPackage("org.thosp.yourlocalweather");
                        intent.putExtra("widgetId", widgetId);
                        sendBroadcast(intent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("MissingInflatedId")
    private void createLocationSettingsDialog(final int widgetId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View forecastSettingView = inflater.inflate(R.layout.widget_setting_location, null);

        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);

        Long locationId = widgetSettingsDbHelper.getParamLong(widgetId, "locationId");

        Location currentLocation;
        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if ((currentLocation == null) || !currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
                if ((currentLocation != null) && currentLocation.isEnabled()) {
                    locationId = currentLocation.getId();
                }
            } else {
                locationId = currentLocation.getId();
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }

        if (locationId == null) {
            locationId = 0L;
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }

        List<Location> allLocations = locationsDbHelper.getAllRows();

        List<String> locationLabels = new ArrayList<>();
        for (Location location: allLocations) {
            StringBuilder locationLabel = new StringBuilder();
            locationLabel.append(location.getOrderId());
            if (location.getAddress() != null) {
                locationLabel.append(" - ");
                locationLabel.append(Utils.getCityAndCountryFromAddress(location.getAddress()));
            }
            locationLabels.add(locationLabel.toString());
        }

        Spinner numberOfDaysSpinner = forecastSettingView.findViewById(R.id.widget_setting_location_locations);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, locationLabels);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberOfDaysSpinner.setAdapter(adapter);
        final LocationsListener locationListener;

        if (currentLocation == null) {
            appendLog(getBaseContext(), TAG, "No enabled location found to show");
            locationListener = new LocationsListener(0);
        } else {
            numberOfDaysSpinner.setSelection(currentLocation.getOrderId());
            locationListener = new LocationsListener(currentLocation.getOrderId());
        }
        numberOfDaysSpinner.setOnItemSelectedListener(locationListener);
        boolean hasLocationToHide = false;
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetGraphComponent = new ComponentName(this, WeatherGraphWidgetProvider.class);
        int[] graphWidgets = widgetManager.getAppWidgetIds(widgetGraphComponent);
        for (int widgetIdToSearch: graphWidgets) {
            if (widgetIdToSearch == widgetId) {
                hasLocationToHide = true;
                break;
            }
        }
        if (!hasLocationToHide) {
            ComponentName widgetForecastComponent = new ComponentName(this, WeatherForecastWidgetProvider.class);
            int[] forecastWidgets = widgetManager.getAppWidgetIds(widgetForecastComponent);
            for (int widgetIdToSearch: forecastWidgets) {
                if (widgetIdToSearch == widgetId) {
                    hasLocationToHide = true;
                    break;
                }
            }
        }
        final boolean saveLocationSetting = hasLocationToHide;
        final Switch showLocationSwitch = forecastSettingView.findViewById(R.id.widget_setting_show_location);
        Boolean showLocation = widgetSettingsDbHelper.getParamBoolean(widgetId, "showLocation");
        if (showLocation == null) {
            showLocation = false;
        }
        final GraphValuesSwitchListener showLocationSwitchListener = new GraphValuesSwitchListener(showLocation);
        if (hasLocationToHide) {
            showLocationSwitch.setVisibility(View.VISIBLE);
            showLocationSwitch.setChecked(showLocation);
            showLocationSwitch.setOnCheckedChangeListener(showLocationSwitchListener);
        } else {
            showLocationSwitch.setVisibility(View.GONE);
        }

        builder.setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Location location = locationsDbHelper.getLocationByOrderId(locationListener.getLocationOrderId());
                        widgetSettingsDbHelper.saveParamLong(widgetId, "locationId", location.getId());
                        if (saveLocationSetting) {
                            widgetSettingsDbHelper.saveParamBoolean(widgetId, "showLocation", showLocationSwitchListener.isChecked());
                        }
                        GraphUtils.invalidateGraph();
                        Intent intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_SETTINGS);
                        intent.setPackage("org.thosp.yourlocalweather");
                        intent.putExtra("widgetId", widgetId);
                        sendBroadcast(intent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("MissingInflatedId")
    private void createForecastSettingsDialog(final int widgetId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View forecastSettingView = inflater.inflate(R.layout.widget_setting_forecast, null);
        final Switch dayNameSwitch = forecastSettingView.findViewById(R.id.widget_setting_forecast_day_name_switch);

        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);

        Long locationId = widgetSettingsDbHelper.getParamLong(widgetId, "locationId");

        Location currentLocation;
        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if ((currentLocation == null) || !currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }
        Locale locale = (currentLocation != null)?currentLocation.getLocale(): Locale.getDefault();
        Date dateForSetting = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", locale);
        final String textOn = sdf.format(dateForSetting);
        dayNameSwitch.setTextOn(textOn);
        sdf = new SimpleDateFormat("EEEE", locale);
        final String textOff = sdf.format(dateForSetting);
        dayNameSwitch.setTextOff(textOff);
        Boolean dayAbbrev = widgetSettingsDbHelper.getParamBoolean(widgetId, "forecast_day_abbrev");
        boolean dayAbbrevChecked = (dayAbbrev != null)?dayAbbrev:false;
        dayNameSwitch.setChecked(dayAbbrevChecked);
        dayNameSwitch.setText(getString(R.string.widget_setting_forecast_day_name_style) + " (" + (dayNameSwitch.isChecked()?textOn:textOff) + ")");
        final DayNameSwitchListener dayNameSwitchListener = new DayNameSwitchListener(dayAbbrevChecked, dayNameSwitch, textOn, textOff);
        dayNameSwitch.setOnCheckedChangeListener(dayNameSwitchListener);

        Spinner numberOfDaysSpinner = forecastSettingView.findViewById(R.id.widget_setting_forecast_number_of_days_hours);
        int predefinedSelection = 0;
        Long storedDays = widgetSettingsDbHelper.getParamLong(widgetId, "forecastDaysCount");
        Boolean hoursForecast = widgetSettingsDbHelper.getParamBoolean(widgetId, "hoursForecast");
        if (hoursForecast == null) {
            hoursForecast = false;
        }
        if (storedDays != null) {
            if (hoursForecast) {
                switch (storedDays.intValue()) {
                    case 3:
                        predefinedSelection = 6;
                        break;
                    case 4:
                        predefinedSelection = 7;
                        break;
                    case 5:
                        predefinedSelection = 8;
                        break;
                    case 6:
                        predefinedSelection = 9;
                        break;
                    case 7:
                        predefinedSelection = 10;
                        break;
                    case 8:
                        predefinedSelection = 11;
                        break;
                }
            } else {
                switch (storedDays.intValue()) {
                    case 3:
                        predefinedSelection = 0;
                        break;
                    case 4:
                        predefinedSelection = 1;
                        break;
                    case 5:
                        predefinedSelection = 2;
                        break;
                    case 6:
                        predefinedSelection = 3;
                        break;
                    case 7:
                        predefinedSelection = 4;
                        break;
                    case 8:
                        predefinedSelection = 5;
                        break;
                }
            }
        } else {
            storedDays = 5L;
        }
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.forecast_number_of_days_hours, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numberOfDaysSpinner.setAdapter(adapter);
        numberOfDaysSpinner.setSelection(predefinedSelection);
        final NumberOfDaysListener numberOfDaysListener = new NumberOfDaysListener(storedDays);
        numberOfDaysSpinner.setOnItemSelectedListener(numberOfDaysListener);

        builder.setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        widgetSettingsDbHelper.saveParamBoolean(widgetId, "forecast_day_abbrev", dayNameSwitchListener.isChecked());
                        widgetSettingsDbHelper.saveParamLong(widgetId, "forecastDaysCount", numberOfDaysListener.getNumberOfDays());
                        widgetSettingsDbHelper.saveParamBoolean(widgetId, "hoursForecast", numberOfDaysListener.isHoursForecast());
                        Intent intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_SETTINGS);
                        intent.setPackage("org.thosp.yourlocalweather");
                        intent.putExtra("widgetId", widgetId);
                        sendBroadcast(intent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("MissingInflatedId")
    private void createGraphSettingDialog(final int widgetId) {
        final Set<Integer> mSelectedItems = new HashSet<>();
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(WidgetSettingsDialogue.this);
        Set<Integer> combinedGraphValuesFromPreferences = AppPreference.getCombinedGraphValues(WidgetSettingsDialogue.this);
        Set<Integer> combinedGraphValues = GraphUtils.getCombinedGraphValuesFromSettings(combinedGraphValuesFromPreferences, widgetSettingsDbHelper, widgetId);

        boolean[] checkedItems = new boolean[4];
        for (Integer visibleColumn: combinedGraphValues) {
            mSelectedItems.add(visibleColumn);
            checkedItems[visibleColumn] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View forecastSettingView = inflater.inflate(R.layout.widget_setting_graph, null);
        final Switch temperatureSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_temperatre_switch);
        final Switch rainsnowSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_rain_switch);
        final Switch windSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_wind_switch);
        final Switch pressureSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_pressure_switch);
        temperatureSwitch.setChecked(checkedItems[0]);
        final GraphValuesSwitchListener temperatureSwitchListener = new GraphValuesSwitchListener(checkedItems[0]);
        temperatureSwitch.setOnCheckedChangeListener(temperatureSwitchListener);
        rainsnowSwitch.setChecked(checkedItems[1]);
        final GraphValuesSwitchListener rainsnowSwitchListener = new GraphValuesSwitchListener(checkedItems[1]);
        rainsnowSwitch.setOnCheckedChangeListener(rainsnowSwitchListener);
        windSwitch.setChecked(checkedItems[2]);
        final GraphValuesSwitchListener windSwitchListener = new GraphValuesSwitchListener(checkedItems[2], pressureSwitch);
        windSwitch.setOnCheckedChangeListener(windSwitchListener);
        pressureSwitch.setChecked(checkedItems[3]);
        final GraphValuesSwitchListener pressureSwitchListener = new GraphValuesSwitchListener(checkedItems[3], windSwitch);
        pressureSwitch.setOnCheckedChangeListener(pressureSwitchListener);
        if (windSwitch.isChecked()) {
            pressureSwitch.setEnabled(false);
        } else if (pressureSwitch.isChecked()) {
            windSwitch.setEnabled(false);
        }

        Boolean showLegend = widgetSettingsDbHelper.getParamBoolean(widgetId, "combinedGraphShowLegend");
        if (showLegend == null) {
            showLegend = true;
        }
        final Switch showLegendSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_show_legend);
        showLegendSwitch.setChecked(showLegend);
        final GraphValuesSwitchListener showLegendSwitchListener = new GraphValuesSwitchListener(showLegend);
        showLegendSwitch.setOnCheckedChangeListener(showLegendSwitchListener);

        builder.setTitle(R.string.forecast_settings_combined_values)
                .setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        StringBuilder valuesToStore = new StringBuilder();
                        if (temperatureSwitchListener.isChecked()) {
                            valuesToStore.append(0);
                            valuesToStore.append(",");
                        }
                        if (rainsnowSwitchListener.isChecked()) {
                            valuesToStore.append(1);
                            valuesToStore.append(",");
                        }
                        if (windSwitchListener.isChecked()) {
                            valuesToStore.append(2);
                            valuesToStore.append(",");
                        }
                        if (pressureSwitchListener.isChecked()) {
                            valuesToStore.append(3);
                        }

                        widgetSettingsDbHelper.saveParamString(widgetId, "combinedGraphValues", valuesToStore.toString());
                        widgetSettingsDbHelper.saveParamBoolean(widgetId, "combinedGraphShowLegend", showLegendSwitchListener.isChecked());
                        GraphUtils.invalidateGraph();
                        Intent refreshWidgetIntent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE);
                        refreshWidgetIntent.setPackage("org.thosp.yourlocalweather");
                        sendBroadcast(refreshWidgetIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public class NumberOfDaysListener implements AdapterView.OnItemSelectedListener {

        private long numberOfDays;
        private boolean hoursForecast;

        public NumberOfDaysListener(long initialValue) {
            numberOfDays = initialValue;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0: numberOfDays = 3; hoursForecast = false; break;
                case 1: numberOfDays = 4; hoursForecast = false; break;
                case 2: numberOfDays = 5; hoursForecast = false; break;
                case 3: numberOfDays = 6; hoursForecast = false; break;
                case 4: numberOfDays = 7; hoursForecast = false; break;
                case 5: numberOfDays = 8; hoursForecast = false; break;
                case 6: numberOfDays = 3; hoursForecast = true; break;
                case 7: numberOfDays = 4; hoursForecast = true; break;
                case 8: numberOfDays = 5; hoursForecast = true; break;
                case 9: numberOfDays = 6; hoursForecast = true; break;
                case 10: numberOfDays = 7; hoursForecast = true; break;
                case 11: numberOfDays = 8; hoursForecast = true; break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        public boolean isHoursForecast() {
            return hoursForecast;
        }

        public long getNumberOfDays() {
            return numberOfDays;
        }
    }

    public class LocationsListener implements AdapterView.OnItemSelectedListener {

        private int locationOrderId;

        public LocationsListener(int initialValue) {
            locationOrderId = initialValue;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            locationOrderId = position;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        public int getLocationOrderId() {
            return locationOrderId;
        }
    }

    public class DayNameSwitchListener implements CompoundButton.OnCheckedChangeListener {

        boolean checked;
        Switch dayNameSwitch;
        String textOn;
        String textOff;

        public DayNameSwitchListener(boolean initialValue, Switch dayNameSwitch, String textOn, String textOff) {
            checked = initialValue;
            this.dayNameSwitch = dayNameSwitch;
            this.textOff = textOff;
            this.textOn = textOn;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            checked = isChecked;
            dayNameSwitch.setText(getString(R.string.widget_setting_forecast_day_name_style) + " (" + (isChecked ? textOn : textOff) + ")");
        }

        public boolean isChecked() {
            return checked;
        }
    }

    public class WidgetActionListener implements AdapterView.OnItemSelectedListener {
        private WidgetActions widgetAction;

        public WidgetActionListener(WidgetActions initialValue) {
            widgetAction = initialValue;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            widgetAction = WidgetActions.getByComboSelection(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

        public WidgetActions getWidgetAction() {
            return widgetAction;
        }
    }

    private class WidgetDefaultDetailsResult {
        int maxNumberOfDetails;
        String defaultDetails;

        public WidgetDefaultDetailsResult(int maxNumberOfDetails, String defaultDetails) {
            this.maxNumberOfDetails = maxNumberOfDetails;
            this.defaultDetails = defaultDetails;
        }

        public int getMaxNumberOfDetails() {
            return maxNumberOfDetails;
        }

        public String getDefaultDetails() {
            return defaultDetails;
        }
    }
}
