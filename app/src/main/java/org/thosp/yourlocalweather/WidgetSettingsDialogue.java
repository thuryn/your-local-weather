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

import org.osmdroid.config.Configuration;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

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

        switch (settingOption) {
            case "graphSetting": createGraphSettingDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "forecastSettings": createForecastSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
            case "locationSettings": createLocationSettingsDialog(getIntent().getIntExtra("widgetId", 0)); break;
        }
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
        if (storedDays != null) {
            switch (storedDays.intValue()) {
                case 3: predefinedSelection = 0;break;
                case 4: predefinedSelection = 1;break;
                case 5: predefinedSelection = 2;break;
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

        public NumberOfDaysListener(long initialValue) {
            numberOfDays = initialValue;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0: numberOfDays = 3;break;
                case 1: numberOfDays = 4;break;
                case 2: numberOfDays = 5;break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

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
}
