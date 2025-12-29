package org.thosp.yourlocalweather.settings.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import org.thosp.yourlocalweather.R;

public class MainSettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_main_menu, rootKey);
    }
}