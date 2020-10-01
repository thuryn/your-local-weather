package org.thosp.yourlocalweather.settings.fragments;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;

import java.util.List;

public class UpdatesPreferenceFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String[] SUMMARIES_TO_UPDATE = {
            Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD,
            Constants.KEY_PREF_LOCATION_UPDATE_PERIOD,
            Constants.KEY_PREF_LOCATION_GEOCODER_SOURCE
    };
    private final SensorEventListener sensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_updates);

        SensorManager senSensorManager = (SensorManager) getActivity()
                .getSystemService(Context.SENSOR_SERVICE);
        Sensor senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean deviceHasAccelerometer = senSensorManager.registerListener(
                sensorListener,
                senAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        senSensorManager.unregisterListener(sensorListener);

        Preference updateWidgetUpdatePref = findPreference(Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD);
        ListPreference updateListPref = (ListPreference) updateWidgetUpdatePref;
        int accIndex = updateListPref.findIndexOfValue("0");

        if (!deviceHasAccelerometer) {
            CharSequence[] entries = updateListPref.getEntries();
            CharSequence[] newEntries = new CharSequence[entries.length - 1];
            int i = 0;
            int j = 0;
            for (CharSequence entry : entries) {
                if (i != accIndex) {
                    newEntries[j] = entries[i];
                    j++;
                }
                i++;
            }
            updateListPref.setEntries(newEntries);
            if (updateListPref.getValue() == null) {
                updateListPref.setValueIndex(updateListPref.findIndexOfValue("60") - 1);
            }
        } else if (updateListPref.getValue() == null) {
            updateListPref.setValueIndex(accIndex);
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getActivity());
        List<Location> availableLocations = locationsDbHelper.getAllRows();
        boolean oneNoautoLocationAvailable = false;
        for (Location location : availableLocations) {
            if (location.getOrderId() != 0) {
                oneNoautoLocationAvailable = true;
                break;
            }
        }
        if (!oneNoautoLocationAvailable) {
            ListPreference locationPreference = (ListPreference) findPreference("location_update_period_pref_key");
            locationPreference.setEnabled(false);
        }

        ListPreference locationAutoPreference = (ListPreference) findPreference("location_auto_update_period_pref_key");
        locationAutoPreference.setEnabled(locationsDbHelper.getLocationByOrderId(0).isEnabled());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        int horizontalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());

        if (view != null) {
            view.setPadding(horizontalMargin, topMargin, horizontalMargin, verticalMargin);
        }
        return view;
    }

    private void entrySummary(String key) {
        ListPreference preference = (ListPreference) findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setSummary(preference.getEntry());
        if (Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD.equals(key)) {
            if ("0".equals(preference.getValue())) {
                AppPreference.setNotificationEnabled(getActivity(), true);
                AppPreference.setNotificationPresence(getActivity(), "permanent");
                AppPreference.setRegularOnlyInterval(getActivity());
            } else {
                AppPreference.setNotificationEnabled(getActivity(), false);
                AppPreference.setNotificationPresence(getActivity(), "when_updated");
                NotificationManager notificationManager =
                        (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
            }
        }
    }

    private void updateSummary(String key, boolean changing) {
        entrySummary(key);
        switch (key) {
            case Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD:
            case Constants.KEY_PREF_LOCATION_UPDATE_PERIOD:
                if (changing) {
                    Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
                    intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
                    getActivity().startService(intentToStartUpdate);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        updateSummaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateSummaries() {
        for (String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
        }
    }
}