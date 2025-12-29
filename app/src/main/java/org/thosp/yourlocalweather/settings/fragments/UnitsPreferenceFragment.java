package org.thosp.yourlocalweather.settings.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;

public class UnitsPreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String[] SUMMARIES_TO_UPDATE = {
            Constants.KEY_PREF_DATE_STYLE,
            Constants.KEY_PREF_TIME_STYLE,
            Constants.KEY_PREF_TEMPERATURE_TYPE,
            Constants.KEY_PREF_TEMPERATURE_UNITS,
            Constants.KEY_PREF_WIND_UNITS,
            Constants.KEY_PREF_WIND_DIRECTION,
            Constants.KEY_PREF_RAIN_SNOW_UNITS,
            Constants.KEY_PREF_PRESSURE_UNITS
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_units, rootKey);
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
        if (Constants.KEY_PREF_TEMPERATURE_UNITS.equals(key) &&
                "kelvin".equals(preference.getValue()) &&
                "icon_temperature".equals(AppPreference.getNotificationStatusIconStyle(getActivity()))) {
                AppPreference.setNotificationIconStyle(getActivity(), "icon_sun");
        }
    }

    private void updateSummary(String key, boolean changing) {
        entrySummary(key);
        if (changing) {
            Intent intent = new Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
            intent.setPackage("org.thosp.yourlocalweather");
            getActivity().sendBroadcast(intent);
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
