package org.thosp.yourlocalweather.settings.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;

public class WidgetPreferenceFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_widget);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.KEY_PREF_WIDGET_THEME:
                Intent intent = new Intent(Constants.ACTION_APPWIDGET_THEME_CHANGED);
                intent.setPackage("org.thosp.yourlocalweather");
                getActivity().sendBroadcast(intent);
                setSummary(Constants.KEY_PREF_WIDGET_THEME);
                break;
            case Constants.KEY_PREF_LOCATION_GPS_ENABLED:
                break;
            case Constants.KEY_PREF_WIDGET_SHOW_LABELS:
                intent = new Intent(Constants.ACTION_APPWIDGET_THEME_CHANGED);
                intent.setPackage("org.thosp.yourlocalweather");
                getActivity().sendBroadcast(intent);
                break;
            case Constants.KEY_PREF_WIDGET_TEXT_COLOR:
                intent = new Intent(Constants.ACTION_APPWIDGET_THEME_CHANGED);
                intent.setPackage("org.thosp.yourlocalweather");
                getActivity().sendBroadcast(intent);
                break;
            case Constants.KEY_PREF_WIDGET_GRAPH_NATIVE_SCALE:
                GraphUtils.invalidateGraph();
                intent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE);
                intent.setPackage("org.thosp.yourlocalweather");
                getActivity().sendBroadcast(intent);
                break;
            case Constants.KEY_PREF_WIDGET_SHOW_CONTROLS:
                intent = new Intent(Constants.ACTION_APPWIDGET_SETTINGS_SHOW_CONTROLS);
                intent.setPackage("org.thosp.yourlocalweather");
                getActivity().sendBroadcast(intent);
            case Constants.KEY_PREF_UPDATE_DETAIL:
                intent = new Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
                intent.setPackage("org.thosp.yourlocalweather");
                getActivity().sendBroadcast(intent);
                setDetailedSummary(Constants.KEY_PREF_UPDATE_DETAIL);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        setSummary(Constants.KEY_PREF_WIDGET_THEME);
        setDetailedSummary(Constants.KEY_PREF_UPDATE_DETAIL);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setDetailedSummary(CharSequence prefKey) {
        Preference updatePref = findPreference(prefKey);
        ListPreference updateListPref = (ListPreference) updatePref;
        switch (updateListPref.getValue()) {
            case "preference_display_update_value":
                updatePref.setSummary(R.string.preference_display_update_value_info);
                break;
            case "preference_display_update_location_source":
                updatePref.setSummary(R.string.preference_display_update_location_source_info);
                break;
            case "preference_display_update_nothing":
            default:
                updatePref.setSummary(updateListPref.getEntry());
                break;
        }
    }

    private void setSummary(CharSequence prefKey) {
        Preference updatePref = findPreference(prefKey);
        ListPreference updateListPref = (ListPreference) updatePref;
        updatePref.setSummary(updateListPref.getEntry());
    }
}
