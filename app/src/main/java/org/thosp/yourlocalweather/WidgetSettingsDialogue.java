package org.thosp.yourlocalweather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;

import org.osmdroid.config.Configuration;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;
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

    private Set<Integer> combinedGraphValues = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        String settingOption = getIntent().getStringExtra("settings_option");
        ArrayList<String> widgetActionPlaces = getIntent().getStringArrayListExtra("widget_action_places");

        switch (settingOption) {
            case "graphSetting": createGraphSettingDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "forecastSettings": createForecastSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "locationSettings": createLocationSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "widgetActionSettings": createWidgetActionSettingsDialog(
                    getIntent().getIntExtra("widgetId", 0),
                    widgetActionPlaces); break;
        }
    }

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
            if (!currentLocation.isEnabled()) {
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
            locationId = 0l;
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
        numberOfDaysSpinner.setSelection(currentLocation.getOrderId());
        final LocationsListener locationListener = new LocationsListener(currentLocation.getOrderId());
        numberOfDaysSpinner.setOnItemSelectedListener(locationListener);

        builder.setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Location location = locationsDbHelper.getLocationByOrderId(locationListener.getLocationOrderId());
                        widgetSettingsDbHelper.saveParamLong(widgetId, "locationId", location.getId());
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
            if (!currentLocation.isEnabled()) {
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
                        predefinedSelection = 3;
                        break;
                    case 4:
                        predefinedSelection = 4;
                        break;
                    case 5:
                        predefinedSelection = 5;
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
                }
            }
        } else {
            storedDays = 5l;
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

    private void createGraphSettingDialog(final int widgetId) {
        final Set<Integer> mSelectedItems = new HashSet<>();

        combinedGraphValues = GraphUtils.getCombinedGraphValuesFromSettings(this, widgetId);

        boolean[] checkedItems = new boolean[4];
        for (Integer visibleColumn: combinedGraphValues) {
            mSelectedItems.add(visibleColumn);
            checkedItems[visibleColumn] = true;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.forecast_settings_combined_values)
                .setMultiChoiceItems(R.array.pref_combined_graph_values, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                ListView dialogListView = ((AlertDialog) dialog).getListView();
                                if (isChecked) {
                                    mSelectedItems.add(which);
                                    if (which == 2) {
                                        if (mSelectedItems.contains(3)) {
                                            mSelectedItems.remove(3);
                                        }
                                        dialogListView.getChildAt(3).setEnabled(false);
                                        dialogListView.getChildAt(3).setClickable(true);
                                    } else if (which == 3) {
                                        if (mSelectedItems.contains(2)) {
                                            mSelectedItems.remove(2);
                                        }
                                        dialogListView.getChildAt(2).setEnabled(false);
                                        dialogListView.getChildAt(2).setClickable(true);
                                    }
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                    if ((which == 2) || (which == 3)) {
                                        dialogListView.getChildAt(3).setEnabled(true);
                                        dialogListView.getChildAt(3).setClickable(false);
                                        dialogListView.getChildAt(2).setEnabled(true);
                                        dialogListView.getChildAt(2).setClickable(false);
                                    }
                                }
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        combinedGraphValues = new HashSet<>();
                        for (Integer selectedItem: mSelectedItems) {
                            combinedGraphValues.add(selectedItem);
                        }
                        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(WidgetSettingsDialogue.this);
                        StringBuilder valuesToStore = new StringBuilder();
                        for (int selectedValue: combinedGraphValues) {
                            valuesToStore.append(selectedValue);
                            valuesToStore.append(",");
                        }
                        widgetSettingsDbHelper.saveParamString(widgetId, "combinedGraphValues", valuesToStore.toString());
                        GraphUtils.invalidateGraph();
                        Intent refreshWidgetIntent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE);
                        refreshWidgetIntent.setPackage("org.thosp.yourlocalweather");
                        sendBroadcast(refreshWidgetIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
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
                case 3: numberOfDays = 3; hoursForecast = true; break;
                case 4: numberOfDays = 4; hoursForecast = true; break;
                case 5: numberOfDays = 5; hoursForecast = true; break;
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
}
