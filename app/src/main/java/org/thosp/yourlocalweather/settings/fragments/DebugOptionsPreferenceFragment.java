package org.thosp.yourlocalweather.settings.fragments;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.LogToFile;

import java.io.File;

import static org.thosp.yourlocalweather.utils.Constants.KEY_DEBUG_FILE;
import static org.thosp.yourlocalweather.utils.Constants.KEY_DEBUG_FILE_LASTING_HOURS;
import static org.thosp.yourlocalweather.utils.Constants.KEY_DEBUG_TO_FILE;

public class DebugOptionsPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_debug);
        initLogFileChooser();
        initLogFileLasting();
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
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Preference preference = findPreference(KEY_DEBUG_FILE);
        preference.setSummary(preferences.getString(KEY_DEBUG_FILE, ""));
    }

    private void initLogFileChooser() {

        Preference logToFileCheckbox = findPreference(KEY_DEBUG_TO_FILE);
        logToFileCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object value) {
                if (!checkWriteToSdcardPermission()) {
                    return false;
                }
                boolean logToFile = (Boolean) value;
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                preferences.edit().putBoolean(KEY_DEBUG_TO_FILE, logToFile).apply();
                LogToFile.logToFileEnabled = logToFile;
                return true;
            }
        });

        Preference buttonFileLog = findPreference(KEY_DEBUG_FILE);
        buttonFileLog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                new ChooserDialog(getActivity())
                        .withFilter(true, false)
                        .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                String logFileName = path + "/log-yourlocalweather.txt";
                                LogToFile.logFilePathname = logFileName;
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                preferences.edit().putString(KEY_DEBUG_FILE, logFileName).apply();
                                preference.setSummary(preferences.getString(KEY_DEBUG_FILE, ""));
                            }
                        })
                        .build()
                        .show();
                return true;
            }
        });
    }

    private boolean checkWriteToSdcardPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        123456);
            }
            return false;
        }
        return true;
    }

    private void initLogFileLasting() {
        Preference logFileLasting = findPreference(KEY_DEBUG_FILE_LASTING_HOURS);
        logFileLasting.setSummary(
                getLogFileLastingLabel(Integer.parseInt(
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(KEY_DEBUG_FILE_LASTING_HOURS, "24"))
                )
        );
        logFileLasting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference logFileLasting, Object value) {
                String logFileLastingHoursTxt = (String) value;
                Integer logFileLastingHours = Integer.valueOf(logFileLastingHoursTxt);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                preferences.edit().putString(KEY_DEBUG_FILE_LASTING_HOURS, logFileLastingHoursTxt).apply();
                logFileLasting.setSummary(getString(getLogFileLastingLabel(logFileLastingHours)));
                LogToFile.logFileHoursOfLasting = logFileLastingHours;
                return true;
            }
        });
    }

    private int getLogFileLastingLabel(int logFileLastingValue) {
        int logFileLastingId;
        switch (logFileLastingValue) {
            case 12:
                logFileLastingId = R.string.log_file_12_label;
                break;
            case 48:
                logFileLastingId = R.string.log_file_48_label;
                break;
            case 72:
                logFileLastingId = R.string.log_file_72_label;
                break;
            case 168:
                logFileLastingId = R.string.log_file_168_label;
                break;
            case 720:
                logFileLastingId = R.string.log_file_720_label;
                break;
            case 24:
            default:
                logFileLastingId = R.string.log_file_24_label;
                break;
        }
        return logFileLastingId;
    }
}
