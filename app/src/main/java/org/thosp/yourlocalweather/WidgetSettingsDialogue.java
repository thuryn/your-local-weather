package org.thosp.yourlocalweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.Nullable;

import org.thosp.yourlocalweather.databinding.WidgetSettingActionsBinding;
import org.thosp.yourlocalweather.databinding.WidgetSettingForecastBinding;
import org.thosp.yourlocalweather.databinding.WidgetSettingGraphBinding;
import org.thosp.yourlocalweather.databinding.WidgetSettingLocationBinding;
import org.thosp.yourlocalweather.databinding.WidgetSettingWeatherDetailBinding;
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
        int widgetId = getIntent().getIntExtra("widgetId", 0);

        if (settingOption == null) {
            finish();
            return;
        }

        switch (settingOption) {
            case "detailsSetting": createDetailsSettingsDialog(widgetId); break;
            case "graphSetting": createGraphSettingDialog(widgetId); break;
            case "forecastSettings": createForecastSettingsDialog(widgetId); break;
            case "locationSettings": createLocationSettingsDialog(widgetId); break;
            case "widgetActionSettings": createWidgetActionSettingsDialog(widgetId, widgetActionPlaces); break;
            default: finish(); break;
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
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);
        WidgetDefaultDetailsResult currentWeatherDetailsAvailableInWidget = getNumberOfCurrentWeatherDetails(widgetId);

        String storedCurrentWeatherDetails = widgetSettingsDbHelper.getParamString(widgetId, "currentWeatherDetails");
        if (storedCurrentWeatherDetails == null) {
            storedCurrentWeatherDetails = currentWeatherDetailsAvailableInWidget.getDefaultDetails();
        }

        Set<Integer> currentWeatherDetailValues = WidgetUtils.getCurrentWeatherDetailsFromSettings(storedCurrentWeatherDetails);

        boolean[] checkedItems = new boolean[NUMBER_OF_WEATHER_DETAIL_OPTIONS];
        for (Integer visibleDetail: currentWeatherDetailValues) {
            if (visibleDetail < NUMBER_OF_WEATHER_DETAIL_OPTIONS) {
                checkedItems[visibleDetail] = true;
            }
        }

        boolean fullSetOfOptions = currentWeatherDetailValues.size() >= currentWeatherDetailsAvailableInWidget.getMaxNumberOfDetails();

        WidgetSettingWeatherDetailBinding dialogBinding = WidgetSettingWeatherDetailBinding.inflate(getLayoutInflater());

        Switch[] switches = new Switch[] {
                dialogBinding.widgetSettingWeatherDetailWindSwitch,
                dialogBinding.widgetSettingWeatherDetailHumiditySwitch,
                dialogBinding.widgetSettingWeatherDetailPressureSwitch,
                dialogBinding.widgetSettingWeatherDetailCloudinessSwitch,
                dialogBinding.widgetSettingWeatherDetailDewPointSwitch,
                dialogBinding.widgetSettingWeatherDetailSunriseSwitch,
                dialogBinding.widgetSettingWeatherDetailSunsetSwitch
        };

        final CurrentWeatherDetailSwitchListener[] switchListeners = new CurrentWeatherDetailSwitchListener[NUMBER_OF_WEATHER_DETAIL_OPTIONS];
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

        new AlertDialog.Builder(this)
                .setTitle(R.string.widget_details_setting_button)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    StringBuilder valuesToStore = new StringBuilder();
                    for (int i = 0; i < NUMBER_OF_WEATHER_DETAIL_OPTIONS; i++) {
                        if (switchListeners[i].isChecked()) {
                            valuesToStore.append(i).append(",");
                        }
                    }
                    widgetSettingsDbHelper.saveParamString(widgetId, "currentWeatherDetails", valuesToStore.toString());

                    Intent refreshWidgetIntent = new Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
                    refreshWidgetIntent.setPackage(getBaseContext().getPackageName());
                    sendBroadcast(refreshWidgetIntent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @SuppressLint("MissingInflatedId")
    private void createWidgetActionSettingsDialog(final int widgetId, final ArrayList<String> widgetActionPlaces) {
        WidgetSettingActionsBinding dialogBinding = WidgetSettingActionsBinding.inflate(getLayoutInflater());
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);

        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.widget_actions, android.R.layout.simple_spinner_item);

        final WidgetActionListener cityActionsListener;
        if (widgetActionPlaces != null && widgetActionPlaces.contains("action_city")) {
            dialogBinding.widgetSettingActionsCity.setVisibility(View.VISIBLE);
            dialogBinding.widgetSettingActionsCityLabel.setVisibility(View.VISIBLE);
            WidgetActions cityAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_city"), "action_city");
            dialogBinding.widgetSettingActionsCity.setAdapter(adapter);
            dialogBinding.widgetSettingActionsCity.setSelection(cityAction.getComboSelection());
            cityActionsListener = new WidgetActionListener(cityAction);
            dialogBinding.widgetSettingActionsCity.setOnItemSelectedListener(cityActionsListener);
        } else {
            dialogBinding.widgetSettingActionsCity.setVisibility(View.GONE);
            dialogBinding.widgetSettingActionsCityLabel.setVisibility(View.GONE);
            cityActionsListener = null;
        }

        final WidgetActionListener mainIconActionsListener;
        if (widgetActionPlaces != null && widgetActionPlaces.contains("action_current_weather_icon")) {
            dialogBinding.widgetSettingActionsMainIcon.setVisibility(View.VISIBLE);
            dialogBinding.widgetSettingActionsMainIconLabel.setVisibility(View.VISIBLE);
            WidgetActions mainIconAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_current_weather_icon"), "action_current_weather_icon");
            dialogBinding.widgetSettingActionsMainIcon.setAdapter(adapter);
            dialogBinding.widgetSettingActionsMainIcon.setSelection(mainIconAction.getComboSelection());
            mainIconActionsListener = new WidgetActionListener(mainIconAction);
            dialogBinding.widgetSettingActionsMainIcon.setOnItemSelectedListener(mainIconActionsListener);
        } else {
            dialogBinding.widgetSettingActionsMainIcon.setVisibility(View.GONE);
            dialogBinding.widgetSettingActionsMainIconLabel.setVisibility(View.GONE);
            mainIconActionsListener = null;
        }

        final WidgetActionListener forecastActionsListener;
        if (widgetActionPlaces != null && widgetActionPlaces.contains("action_forecast")) {
            dialogBinding.widgetSettingActionsForecast.setVisibility(View.VISIBLE);
            dialogBinding.widgetSettingActionsForecastLabel.setVisibility(View.VISIBLE);
            WidgetActions forecastAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_forecast"), "action_forecast");
            dialogBinding.widgetSettingActionsForecast.setAdapter(adapter);
            dialogBinding.widgetSettingActionsForecast.setSelection(forecastAction.getComboSelection());
            forecastActionsListener = new WidgetActionListener(forecastAction);
            dialogBinding.widgetSettingActionsForecast.setOnItemSelectedListener(forecastActionsListener);
        } else {
            dialogBinding.widgetSettingActionsForecast.setVisibility(View.GONE);
            dialogBinding.widgetSettingActionsForecastLabel.setVisibility(View.GONE);
            forecastActionsListener = null;
        }

        final WidgetActionListener graphActionsListener;
        if (widgetActionPlaces != null && widgetActionPlaces.contains("action_graph")) {
            dialogBinding.widgetSettingActionsGraph.setVisibility(View.VISIBLE);
            dialogBinding.widgetSettingActionsGraphLabel.setVisibility(View.VISIBLE);
            WidgetActions graphAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_graph"), "action_graph");
            dialogBinding.widgetSettingActionsGraph.setAdapter(adapter);
            dialogBinding.widgetSettingActionsGraph.setSelection(graphAction.getComboSelection());
            graphActionsListener = new WidgetActionListener(graphAction);
            dialogBinding.widgetSettingActionsGraph.setOnItemSelectedListener(graphActionsListener);
        } else {
            dialogBinding.widgetSettingActionsGraph.setVisibility(View.GONE);
            dialogBinding.widgetSettingActionsGraphLabel.setVisibility(View.GONE);
            graphActionsListener = null;
        }

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    if (cityActionsListener != null) {
                        widgetSettingsDbHelper.saveParamLong(widgetId, "action_city", cityActionsListener.getWidgetAction().getId());
                    }
                    if (mainIconActionsListener != null) {
                        widgetSettingsDbHelper.saveParamLong(widgetId, "action_current_weather_icon", mainIconActionsListener.getWidgetAction().getId());
                    }
                    if (forecastActionsListener != null) {
                        widgetSettingsDbHelper.saveParamLong(widgetId, "action_forecast", forecastActionsListener.getWidgetAction().getId());
                    }
                    if (graphActionsListener != null) {
                        widgetSettingsDbHelper.saveParamLong(widgetId, "action_graph", graphActionsListener.getWidgetAction().getId());
                    }
                    Intent intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_SETTINGS);
                    intent.setPackage(getBaseContext().getPackageName());
                    intent.putExtra("widgetId", widgetId);
                    sendBroadcast(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @SuppressLint("MissingInflatedId")
    private void createLocationSettingsDialog(final int widgetId) {
        WidgetSettingLocationBinding dialogBinding = WidgetSettingLocationBinding.inflate(getLayoutInflater());
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

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationLabels);
        dialogBinding.widgetSettingLocationLocations.setAdapter(adapter);

        final LocationsListener locationListener;
        if (currentLocation == null) {
            appendLog(getBaseContext(), TAG, "No enabled location found to show");
            locationListener = new LocationsListener(0);
        } else {
            dialogBinding.widgetSettingLocationLocations.setSelection(currentLocation.getOrderId());
            locationListener = new LocationsListener(currentLocation.getOrderId());
        }
        dialogBinding.widgetSettingLocationLocations.setOnItemSelectedListener(locationListener);

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
        Boolean showLocation = widgetSettingsDbHelper.getParamBoolean(widgetId, "showLocation");
        if (showLocation == null) {
            showLocation = false;
        }
        final GraphValuesSwitchListener showLocationSwitchListener = new GraphValuesSwitchListener(showLocation);
        if (hasLocationToHide) {
            dialogBinding.widgetSettingShowLocation.setVisibility(View.VISIBLE);
            dialogBinding.widgetSettingShowLocation.setChecked(showLocation);
            dialogBinding.widgetSettingShowLocation.setOnCheckedChangeListener(showLocationSwitchListener);
        } else {
            dialogBinding.widgetSettingShowLocation.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    Location location = locationsDbHelper.getLocationByOrderId(locationListener.getLocationOrderId());
                    if (location != null) {
                        widgetSettingsDbHelper.saveParamLong(widgetId, "locationId", location.getId());
                    }
                    if (saveLocationSetting) {
                        widgetSettingsDbHelper.saveParamBoolean(widgetId, "showLocation", showLocationSwitchListener.isChecked());
                    }
                    GraphUtils.invalidateGraph();
                    Intent intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_SETTINGS);
                    intent.setPackage(getBaseContext().getPackageName());
                    intent.putExtra("widgetId", widgetId);
                    sendBroadcast(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @SuppressLint("MissingInflatedId")
    private void createForecastSettingsDialog(final int widgetId) {
        WidgetSettingForecastBinding dialogBinding = WidgetSettingForecastBinding.inflate(getLayoutInflater());
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
        Locale locale = (currentLocation != null) ? currentLocation.getLocale() : Locale.getDefault();
        Date dateForSetting = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", locale);
        final String textOn = sdf.format(dateForSetting);
        dialogBinding.widgetSettingForecastDayNameSwitch.setTextOn(textOn);

        sdf = new SimpleDateFormat("EEEE", locale);
        final String textOff = sdf.format(dateForSetting);
        dialogBinding.widgetSettingForecastDayNameSwitch.setTextOff(textOff);

        Boolean dayAbbrev = widgetSettingsDbHelper.getParamBoolean(widgetId, "forecast_day_abbrev");
        boolean dayAbbrevChecked = (dayAbbrev != null) ? dayAbbrev : false;
        dialogBinding.widgetSettingForecastDayNameSwitch.setChecked(dayAbbrevChecked);
        dialogBinding.widgetSettingForecastDayNameSwitch.setText(getString(R.string.widget_setting_forecast_day_name_style) + " (" + (dayAbbrevChecked ? textOn : textOff) + ")");

        final DayNameSwitchListener dayNameSwitchListener = new DayNameSwitchListener(dayAbbrevChecked, dialogBinding.widgetSettingForecastDayNameSwitch, textOn, textOff);
        dialogBinding.widgetSettingForecastDayNameSwitch.setOnCheckedChangeListener(dayNameSwitchListener);

        int predefinedSelection = 0;
        Long storedDays = widgetSettingsDbHelper.getParamLong(widgetId, "forecastDaysCount");
        Boolean hoursForecast = widgetSettingsDbHelper.getParamBoolean(widgetId, "hoursForecast");
        if (hoursForecast == null) {
            hoursForecast = false;
        }
        if (storedDays != null) {
            int days = storedDays.intValue();
            if (hoursForecast) {
                if (days >= 3 && days <= 8) predefinedSelection = days + 3;
            } else {
                if (days >= 3 && days <= 8) predefinedSelection = days - 3;
            }
        } else {
            storedDays = 5L;
        }
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.forecast_number_of_days_hours, android.R.layout.simple_spinner_item);
        dialogBinding.widgetSettingForecastNumberOfDaysHours.setAdapter(adapter);
        dialogBinding.widgetSettingForecastNumberOfDaysHours.setSelection(predefinedSelection);
        final NumberOfDaysListener numberOfDaysListener = new NumberOfDaysListener(storedDays);
        dialogBinding.widgetSettingForecastNumberOfDaysHours.setOnItemSelectedListener(numberOfDaysListener);

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    widgetSettingsDbHelper.saveParamBoolean(widgetId, "forecast_day_abbrev", dayNameSwitchListener.isChecked());
                    widgetSettingsDbHelper.saveParamLong(widgetId, "forecastDaysCount", numberOfDaysListener.getNumberOfDays());
                    widgetSettingsDbHelper.saveParamBoolean(widgetId, "hoursForecast", numberOfDaysListener.isHoursForecast());
                    Intent intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_SETTINGS);
                    intent.setPackage(getBaseContext().getPackageName());
                    intent.putExtra("widgetId", widgetId);
                    sendBroadcast(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    @SuppressLint("MissingInflatedId")
    private void createGraphSettingDialog(final int widgetId) {
        WidgetSettingGraphBinding dialogBinding = WidgetSettingGraphBinding.inflate(getLayoutInflater());
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);
        Set<Integer> combinedGraphValuesFromPreferences = AppPreference.getCombinedGraphValues(this);
        Set<Integer> combinedGraphValues = GraphUtils.getCombinedGraphValuesFromSettings(combinedGraphValuesFromPreferences, widgetSettingsDbHelper, widgetId);

        boolean[] checkedItems = new boolean[4];
        for (Integer visibleColumn: combinedGraphValues) {
            if (visibleColumn < 4) {
                checkedItems[visibleColumn] = true;
            }
        }

        dialogBinding.widgetSettingGraphTemperatreSwitch.setChecked(checkedItems[0]);
        final GraphValuesSwitchListener temperatureSwitchListener = new GraphValuesSwitchListener(checkedItems[0]);
        dialogBinding.widgetSettingGraphTemperatreSwitch.setOnCheckedChangeListener(temperatureSwitchListener);

        dialogBinding.widgetSettingGraphRainSwitch.setChecked(checkedItems[1]);
        final GraphValuesSwitchListener rainsnowSwitchListener = new GraphValuesSwitchListener(checkedItems[1]);
        dialogBinding.widgetSettingGraphRainSwitch.setOnCheckedChangeListener(rainsnowSwitchListener);

        dialogBinding.widgetSettingGraphWindSwitch.setChecked(checkedItems[2]);
        final GraphValuesSwitchListener windSwitchListener = new GraphValuesSwitchListener(checkedItems[2], dialogBinding.widgetSettingGraphPressureSwitch);
        dialogBinding.widgetSettingGraphWindSwitch.setOnCheckedChangeListener(windSwitchListener);

        dialogBinding.widgetSettingGraphPressureSwitch.setChecked(checkedItems[3]);
        final GraphValuesSwitchListener pressureSwitchListener = new GraphValuesSwitchListener(checkedItems[3], dialogBinding.widgetSettingGraphWindSwitch);
        dialogBinding.widgetSettingGraphPressureSwitch.setOnCheckedChangeListener(pressureSwitchListener);

        if (dialogBinding.widgetSettingGraphWindSwitch.isChecked()) {
            dialogBinding.widgetSettingGraphPressureSwitch.setEnabled(false);
        } else if (dialogBinding.widgetSettingGraphPressureSwitch.isChecked()) {
            dialogBinding.widgetSettingGraphWindSwitch.setEnabled(false);
        }

        Boolean showLegend = widgetSettingsDbHelper.getParamBoolean(widgetId, "combinedGraphShowLegend");
        if (showLegend == null) {
            showLegend = true;
        }
        dialogBinding.widgetSettingGraphShowLegend.setChecked(showLegend);
        final GraphValuesSwitchListener showLegendSwitchListener = new GraphValuesSwitchListener(showLegend);
        dialogBinding.widgetSettingGraphShowLegend.setOnCheckedChangeListener(showLegendSwitchListener);

        new AlertDialog.Builder(this)
                .setTitle(R.string.forecast_settings_combined_values)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    StringBuilder valuesToStore = new StringBuilder();
                    if (temperatureSwitchListener.isChecked()) valuesToStore.append(0).append(",");
                    if (rainsnowSwitchListener.isChecked()) valuesToStore.append(1).append(",");
                    if (windSwitchListener.isChecked()) valuesToStore.append(2).append(",");
                    if (pressureSwitchListener.isChecked()) valuesToStore.append(3);

                    widgetSettingsDbHelper.saveParamString(widgetId, "combinedGraphValues", valuesToStore.toString());
                    widgetSettingsDbHelper.saveParamBoolean(widgetId, "combinedGraphShowLegend", showLegendSwitchListener.isChecked());
                    GraphUtils.invalidateGraph();
                    Intent refreshWidgetIntent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE);
                    refreshWidgetIntent.setPackage(getBaseContext().getPackageName());
                    sendBroadcast(refreshWidgetIntent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    public class NumberOfDaysListener implements AdapterView.OnItemSelectedListener {
        private long numberOfDays;
        private boolean hoursForecast;

        public NumberOfDaysListener(long initialValue) {
            numberOfDays = initialValue;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (position >= 0 && position <= 5) {
                numberOfDays = position + 3;
                hoursForecast = false;
            } else if (position >= 6 && position <= 11) {
                numberOfDays = position - 3;
                hoursForecast = true;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}

        public boolean isHoursForecast() { return hoursForecast; }
        public long getNumberOfDays() { return numberOfDays; }
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
        public void onNothingSelected(AdapterView<?> parent) {}

        public int getLocationOrderId() { return locationOrderId; }
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

        public boolean isChecked() { return checked; }
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
        public void onNothingSelected(AdapterView<?> parent) {}

        public WidgetActions getWidgetAction() { return widgetAction; }
    }

    private class WidgetDefaultDetailsResult {
        int maxNumberOfDetails;
        String defaultDetails;

        public WidgetDefaultDetailsResult(int maxNumberOfDetails, String defaultDetails) {
            this.maxNumberOfDetails = maxNumberOfDetails;
            this.defaultDetails = defaultDetails;
        }

        public int getMaxNumberOfDetails() { return maxNumberOfDetails; }
        public String getDefaultDetails() { return defaultDetails; }
    }
}