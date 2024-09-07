package org.thosp.yourlocalweather;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.settings.fragments.AboutPreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.DebugOptionsPreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.GeneralPreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.NotificationPreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.PowerSavePreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.UnitsPreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.UpdatesPreferenceFragment;
import org.thosp.yourlocalweather.settings.fragments.WidgetPreferenceFragment;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        super.onCreate(savedInstanceState);
        setupActionBar();

        int horizontalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
        getListView().setPadding(horizontalMargin, topMargin, horizontalMargin, verticalMargin);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageUtil.setLanguage(base, AppPreference.getInstance().getLanguage(base)));
    }

    private void setupActionBar() {
        getLayoutInflater().inflate(R.layout.activity_settings, findViewById(android.R.id.content));
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || UnitsPreferenceFragment.class.getName().equals(fragmentName)
                || UpdatesPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || PowerSavePreferenceFragment.class.getName().equals(fragmentName)
                || DebugOptionsPreferenceFragment.class.getName().equals(fragmentName)
                || WidgetPreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName);
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
