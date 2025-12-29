package org.thosp.yourlocalweather;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.thosp.yourlocalweather.settings.fragments.MainSettingsFragment;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.LanguageUtil;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new MainSettingsFragment()) // <-- Startujeme hlavní menu
                    .commit();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageUtil.setLanguage(base, AppPreference.getInstance().getLanguage(base)));
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();

        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());

        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(null)
                .commit();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsAlertDialog extends DialogFragment {

        private static final String ARG_MESSAGE_RES_ID = "org.thosp.yourlocalweather.message_res_id";

        public SettingsAlertDialog newInstance(int messageResId) {
            SettingsAlertDialog fragment = new SettingsAlertDialog();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE_RES_ID, messageResId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int messageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(messageResId);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent returnToMainActivity = new Intent(getActivity().getApplicationContext(), MainActivity.class);
                    startActivity(returnToMainActivity);
                }
            });
            return builder.create();
        }
    }

}
