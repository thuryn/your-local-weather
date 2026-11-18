package org.thosp.yourlocalweather.settings.fragments;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.TemperatureUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NotificationPreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String[] SUMMARIES_TO_UPDATE = {
            Constants.KEY_PREF_IS_NOTIFICATION_ENABLED,
            Constants.KEY_PREF_INTERVAL_NOTIFICATION,
            Constants.KEY_PREF_NOTIFICATION_PRESENCE,
            Constants.KEY_PREF_NOTIFICATION_STATUS_ICON,
            Constants.KEY_PREF_NOTIFICATION_VISUAL_STYLE,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_CONDITION_ENABLED,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_CONDITIONS,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_LOOKAHEAD_HOURS,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_WIND_THRESHOLD,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_COOLDOWN_MINUTES,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_ENABLED,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_START,
            Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_END
    };
    private final String[] ENABLED_TO_UPDATE = {
            Constants.KEY_PREF_INTERVAL_NOTIFICATION,
            Constants.KEY_PREF_NOTIFICATION_PRESENCE,
            Constants.KEY_PREF_NOTIFICATION_STATUS_ICON,
            Constants.KEY_PREF_NOTIFICATION_VISUAL_STYLE
    };
    private boolean updateBySensor;
    Preference.OnPreferenceChangeListener notificationListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    boolean isEnabled = (boolean) o;
                    AppPreference.getInstance().setNotificationEnabled(getActivity(), isEnabled);
                    Context context = requireContext();
                    Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_NOTIFICATION_ALARM_SERVICE");
                    intentToStartUpdate.setPackage(context.getPackageName());
                    getActivity().startService(intentToStartUpdate);
                    updateSummaries(isEnabled);
                    NotificationManager notificationManager =
                            (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                    return true;
                }
            };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_notification, rootKey);
        final SwitchPreference notificationSwitch = (SwitchPreference) findPreference(
                Constants.KEY_PREF_IS_NOTIFICATION_ENABLED);
        notificationSwitch.setOnPreferenceChangeListener(notificationListener);
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

    private void entrySummary(String key, boolean changing) {
        if (Constants.KEY_PREF_IS_NOTIFICATION_ENABLED.equals(key)) {
            SwitchPreference switchPreference = (SwitchPreference) findPreference(key);
            if (switchPreference == null) {
                return;
            }
            switchPreference.setEnabled(!updateBySensor);
        } else if (Constants.KEY_PREF_VIBRATE.equals(key)) {
            AppPreference.getInstance().clearVibrateEnabled();
        } else {
            SwitchPreference switchPreference = (SwitchPreference) findPreference(Constants.KEY_PREF_IS_NOTIFICATION_ENABLED);

            ListPreference preference = (ListPreference) findPreference(key);
            if (preference == null) {
                return;
            }
            preference.setSummary(preference.getEntry());
            if (Constants.KEY_PREF_NOTIFICATION_PRESENCE.equals(key)) {
                if (updateBySensor || !switchPreference.isChecked()) {
                    preference.setValue("permanent");
                    preference.setEnabled(false);
                } else {
                    preference.setEnabled(true);
                }
                if ("permanent".equals(preference.getValue()) || "on_lock_screen".equals(preference.getValue())) {
                    SwitchPreference vibrate = (SwitchPreference) findPreference(Constants.KEY_PREF_VIBRATE);
                    vibrate.setEnabled(false);
                    vibrate.setChecked(false);
                    AppPreference.getInstance().clearVibrateEnabled();
                } else {
                    SwitchPreference vibrate = (SwitchPreference) findPreference(Constants.KEY_PREF_VIBRATE);
                    vibrate.setEnabled(true);
                    AppPreference.getInstance().clearVibrateEnabled();
                }
                if (!"permanent".equals(preference.getValue())) {
                    NotificationManager notificationManager =
                            (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                }
            } else if (Constants.KEY_PREF_INTERVAL_NOTIFICATION.equals(key)) {
                preference.setEnabled(!updateBySensor && switchPreference.isChecked());
            } else {
                preference.setEnabled(true);
            }
        }
        if (AppPreference.getInstance().isNotificationEnabled(getActivity()) &&
                "permanent".equals(AppPreference.getNotificationPresence(getActivity()))) {
            Location locationForNotification = NotificationUtils.getLocationForNotification(getActivity());
            if (locationForNotification != null) {
                NotificationUtils.weatherNotification(getActivity(),
                        locationForNotification.getId());
            }
        }
        if (TemperatureUtil.isTemperatureUnitKelvin(getActivity()) &&
                Constants.KEY_PREF_NOTIFICATION_STATUS_ICON.equals(key)) {

            ListPreference statusIconPreference = (ListPreference) findPreference(key);
            statusIconPreference.setEntries(R.array.notification_status_icon_entries_without_temperature);
            statusIconPreference.setEntryValues(R.array.notification_status_icon_values_without_temperature);
        }
    }

    private void updateSummary(String key, boolean changing) {
        switch (key) {
            case Constants.KEY_PREF_IS_NOTIFICATION_ENABLED:
            case Constants.KEY_PREF_INTERVAL_NOTIFICATION:
                entrySummary(key, changing);
                if (changing) {
                    Context context = requireContext();
                    Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
                    intentToStartUpdate.setPackage(context.getPackageName());
                    getActivity().startService(intentToStartUpdate);
                }
                break;
            case Constants.KEY_PREF_NOTIFICATION_PRESENCE:
            case Constants.KEY_PREF_NOTIFICATION_STATUS_ICON:
            case Constants.KEY_PREF_NOTIFICATION_VISUAL_STYLE:
                entrySummary(key, changing);
                updateForecastFilterState();
                break;
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_CONDITION_ENABLED:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_CONDITIONS:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_LOOKAHEAD_HOURS:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_WIND_THRESHOLD:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_COOLDOWN_MINUTES:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_ENABLED:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_START:
            case Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_END:
                updateForecastFilterState();
                break;
        }
    }

    private void updateForecastFilterState() {
        SwitchPreference notificationSwitch = (SwitchPreference) findPreference(Constants.KEY_PREF_IS_NOTIFICATION_ENABLED);
        ListPreference presencePref = (ListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_PRESENCE);
        SwitchPreference forecastEnabledPref = (SwitchPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_CONDITION_ENABLED);
        MultiSelectListPreference conditionsPref = (MultiSelectListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_CONDITIONS);
        ListPreference lookaheadPref = (ListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_LOOKAHEAD_HOURS);
        ListPreference windThresholdPref = (ListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_WIND_THRESHOLD);
        ListPreference cooldownPref = (ListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_COOLDOWN_MINUTES);
        SwitchPreference quietHoursEnabledPref = (SwitchPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_ENABLED);
        ListPreference quietStartPref = (ListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_START);
        ListPreference quietEndPref = (ListPreference) findPreference(Constants.KEY_PREF_NOTIFICATION_FORECAST_QUIET_HOURS_END);

        boolean notificationActive = notificationSwitch != null && notificationSwitch.isChecked() && !updateBySensor;
        boolean notPermanent = presencePref == null || !"permanent".equals(presencePref.getValue());
        boolean canEnableFilter = notificationActive && notPermanent;

        if (forecastEnabledPref != null) {
            forecastEnabledPref.setEnabled(canEnableFilter);
        }
        boolean filterActive = canEnableFilter && forecastEnabledPref != null && forecastEnabledPref.isChecked();

        if (conditionsPref != null) {
            conditionsPref.setEnabled(filterActive);
            Set<String> selectedValues = conditionsPref.getValues();
            conditionsPref.setSummary(buildConditionsSummary(conditionsPref, selectedValues));
            boolean windSelected = selectedValues != null && selectedValues.contains("wind");
            if (windThresholdPref != null) {
                windThresholdPref.setEnabled(filterActive && windSelected);
                windThresholdPref.setSummary(windThresholdPref.getEntry());
            }
        }
        if (lookaheadPref != null) {
            lookaheadPref.setEnabled(filterActive);
            lookaheadPref.setSummary(lookaheadPref.getEntry());
        }
        if (cooldownPref != null) {
            cooldownPref.setEnabled(filterActive);
            cooldownPref.setSummary(cooldownPref.getEntry());
        }
        if (quietHoursEnabledPref != null) {
            quietHoursEnabledPref.setEnabled(filterActive);
        }
        boolean quietActive = filterActive && quietHoursEnabledPref != null && quietHoursEnabledPref.isChecked();
        if (quietStartPref != null) {
            quietStartPref.setEnabled(quietActive);
            quietStartPref.setSummary(quietStartPref.getEntry());
        }
        if (quietEndPref != null) {
            quietEndPref.setEnabled(quietActive);
            quietEndPref.setSummary(quietEndPref.getEntry());
        }
    }

    private String buildConditionsSummary(MultiSelectListPreference preference, Set<String> selectedValues) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return getString(R.string.pref_summary_notification_forecast_conditions_none);
        }
        CharSequence[] entries = preference.getEntries();
        CharSequence[] entryValues = preference.getEntryValues();
        List<CharSequence> selectedLabels = new ArrayList<>();
        for (int i = 0; i < entryValues.length; i++) {
            if (selectedValues.contains(entryValues[i].toString())) {
                selectedLabels.add(entries[i]);
            }
        }
        if (selectedLabels.isEmpty()) {
            return getString(R.string.pref_summary_notification_forecast_conditions_none);
        }
        return android.text.TextUtils.join(", ", selectedLabels);
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
        String updateAutoPeriodStr = AppPreference.getInstance().getLocationAutoUpdatePeriod(getActivity());
        updateBySensor = "0".equals(updateAutoPeriodStr);
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

    private void updateSummaries(boolean isNotificationEnabled) {
        for (String key : ENABLED_TO_UPDATE) {
            ListPreference preference = (ListPreference) findPreference(key);
            if (preference == null) {
                return;
            }
            preference.setSummary(preference.getEntry());
            if (Constants.KEY_PREF_NOTIFICATION_PRESENCE.equals(key)) {
                if (updateBySensor || !isNotificationEnabled) {
                    preference.setValue("permanent");
                    preference.setEnabled(false);
                } else {
                    preference.setEnabled(true);
                }
                if ("permanent".equals(preference.getValue()) || "on_lock_screen".equals(preference.getValue())) {
                    SwitchPreference vibrate = (SwitchPreference) findPreference(Constants.KEY_PREF_VIBRATE);
                    vibrate.setEnabled(false);
                    vibrate.setChecked(false);
                } else {
                    SwitchPreference vibrate = (SwitchPreference) findPreference(Constants.KEY_PREF_VIBRATE);
                    vibrate.setEnabled(true);
                }
                if (!"permanent".equals(preference.getValue())) {
                    NotificationManager notificationManager =
                            (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancelAll();
                }
            } else if (Constants.KEY_PREF_INTERVAL_NOTIFICATION.equals(key)) {
                preference.setEnabled(!updateBySensor && isNotificationEnabled);
            } else {
                preference.setEnabled(true);
            }
        }
        updateForecastFilterState();
    }
}
