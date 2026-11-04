package org.thosp.yourlocalweather.settings.fragments;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.CustomWeatherUrlAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

public class UpdatesPreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String GEMMA_DOWNLOAD_URL = "https://huggingface.co/AIFrenz/Gemma-2b-it-gpu-int4/resolve/main/gemma-2b-it-gpu-int4.bin";

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (AppPreference.getGemmaDownloadId(context) == id) {
                installGemmaModel(context);
            }
        }
    };

    private void installGemmaModel(Context context) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) return;
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(AppPreference.getGemmaDownloadId(context));
        android.database.Cursor cursor = downloadManager.query(query);

        if (cursor != null && cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusIndex != -1 && DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(statusIndex)) {
                int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                if (uriIndex != -1) {
                    String downloadedPackageUriString = cursor.getString(uriIndex);
                    if (downloadedPackageUriString != null) {
                        File downloadedFile = new File(Uri.parse(downloadedPackageUriString).getPath());
                        File internalFile = new File(context.getFilesDir(), "gemma-2b-it-gpu-int4.bin");

                        try {
                            copyFile(downloadedFile, internalFile);
                            downloadedFile.delete();
                            AppPreference.setGemmaDownloadId(context, 0);
                            Toast.makeText(context, "Gemma model installed successfully", Toast.LENGTH_SHORT).show();
                            updateAIModelStatus();
                        } catch (Exception e) {
                            Toast.makeText(context, "Failed to install model: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
            cursor.close();
        }
    }

    private void copyFile(File sourceFile, File destFile) throws Exception {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
             FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    private final String[] SUMMARIES_TO_UPDATE = {
            Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD,
            Constants.KEY_PREF_LOCATION_UPDATE_PERIOD,
            Constants.KEY_PREF_LOCATION_GEOCODER_SOURCE,
            Constants.KEY_PREF_WEATHER_PROVIDER,
            Constants.KEY_PREF_CUSTOM_WEATHER_URL
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
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_updates, rootKey);
        ContextCompat.registerReceiver(requireContext(), onDownloadComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED);

        SensorManager senSensorManager = (SensorManager) getActivity()
                .getSystemService(Context.SENSOR_SERVICE);
        if (senSensorManager != null) {
            Sensor senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            boolean deviceHasAccelerometer = senSensorManager.registerListener(
                    sensorListener,
                    senAccelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST);
            senSensorManager.unregisterListener(sensorListener);

            Preference updateWidgetUpdatePref = findPreference(Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD);
            if (updateWidgetUpdatePref instanceof ListPreference) {
                ListPreference updateListPref = (ListPreference) updateWidgetUpdatePref;
                int accIndex = updateListPref.findIndexOfValue("0");

                if (!deviceHasAccelerometer && updateListPref.getEntries() != null) {
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
                } else if (updateListPref.getValue() == null && accIndex != -1) {
                    updateListPref.setValueIndex(accIndex);
                }
            }
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
            ListPreference locationPreference = findPreference("location_update_period_pref_key");
            if (locationPreference != null) {
                locationPreference.setEnabled(false);
            }
        }

        ListPreference locationAutoPreference = findPreference("location_auto_update_period_pref_key");
        if (locationAutoPreference != null && locationsDbHelper.getLocationByOrderId(0) != null) {
            locationAutoPreference.setEnabled(locationsDbHelper.getLocationByOrderId(0).isEnabled());
        }

        updateAIModelStatus();
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
        if (Constants.KEY_PREF_LOCATION_AUTO_UPDATE_NIGHT.equals(key) ||
                Constants.KEY_PREF_LOCATION_UPDATE_NIGHT.equals(key)) {
            return;
        }
        Preference preference = findPreference(key);
        if (!(preference instanceof ListPreference)) {
            return;
        }
        ListPreference listPreference = (ListPreference) preference;
        listPreference.setSummary(listPreference.getEntry());
        if (Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD.equals(key)) {
            if ("0".equals(listPreference.getValue())) {
                AppPreference.getInstance().setNotificationEnabled(getActivity(), true);
                AppPreference.setNotificationPresence(getActivity(), "permanent");
                AppPreference.setRegularOnlyInterval(getActivity());
            } else {
                AppPreference.getInstance().setNotificationEnabled(getActivity(), false);
                AppPreference.setNotificationPresence(getActivity(), "when_updated");
                NotificationManager notificationManager =
                        (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancelAll();
                }
            }
        }
    }

    private void updateSummary(String key, boolean changing) {
        if (Constants.KEY_PREF_CUSTOM_WEATHER_URL.equals(key)) {
            Preference customUrlPref = findPreference(key);
            if (customUrlPref != null) {
                String value = AppPreference.getCustomWeatherUrl(getContext());
                customUrlPref.setSummary(value.isEmpty() ? getString(R.string.pref_title_custom_weather_url) : value);
            }
            return;
        }
        entrySummary(key);
        Context context = requireContext();
        switch (key) {
            case Constants.KEY_PREF_WEATHER_PROVIDER:
                ListPreference providerPref = findPreference(key);
                Preference customUrlPref = findPreference(Constants.KEY_PREF_CUSTOM_WEATHER_URL);
                Preference analyzeUrlPref = findPreference("analyze_custom_url_pref_key");
                boolean isCustom = providerPref != null && "custom_url".equals(providerPref.getValue());
                if (customUrlPref != null) {
                    customUrlPref.setVisible(isCustom);
                }
                if (analyzeUrlPref != null) {
                    analyzeUrlPref.setVisible(isCustom);
                }
                break;
            case Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD:
                if (changing) {
                    AppPreference.getInstance().clearLocationAutoUpdatePeriod();
                    Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
                    intentToStartUpdate.setPackage(context.getPackageName());
                    getActivity().startService(intentToStartUpdate);
                }
                break;
            case Constants.KEY_PREF_LOCATION_UPDATE_PERIOD:
                if (changing) {
                    AppPreference.getInstance().clearLocationUpdatePeriod();
                    Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
                    intentToStartUpdate.setPackage(context.getPackageName());
                    getActivity().startService(intentToStartUpdate);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if ("analyze_custom_url_pref_key".equals(preference.getKey())) {
            analyzeCustomUrl();
            return true;
        } else if ("download_gemma_pref_key".equals(preference.getKey())) {
            downloadGemmaModel();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void downloadGemmaModel() {
        Toast.makeText(getContext(), "Starting Gemma model download...", Toast.LENGTH_LONG).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(GEMMA_DOWNLOAD_URL))
                .setTitle("Gemma Model Download")
                .setDescription("Downloading AI model for on-device weather analysis")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(getContext(), null, "gemma-2b-it-gpu-int4.bin")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        request.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");

        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            try {
                long id = downloadManager.enqueue(request);
                AppPreference.setGemmaDownloadId(getContext(), id);
                updateAIModelStatus();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to start download: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            requireContext().unregisterReceiver(onDownloadComplete);
        } catch (Exception e) {
            // ignore
        }
    }

    private void updateAIModelStatus() {
        Preference statusPref = findPreference("ai_model_status_pref_key");
        if (statusPref != null) {
            boolean nanoAvailable = false;
            if (nanoAvailable) {
                statusPref.setSummary(R.string.pref_summary_gemini_nano_available);
            } else {
                statusPref.setSummary(R.string.pref_summary_gemini_nano_not_available);
            }
        }

        Preference gemmaPref = findPreference("download_gemma_pref_key");
        if (gemmaPref != null) {
            if (CustomWeatherUrlAnalyzer.isGemmaModelDownloaded(requireContext())) {
                gemmaPref.setSummary(R.string.pref_summary_gemma_downloaded);
                gemmaPref.setEnabled(false);
            } else {
                checkDownloadStatus(gemmaPref);
            }
        }
    }

    private void checkDownloadStatus(Preference gemmaPref) {
        long downloadId = AppPreference.getGemmaDownloadId(requireContext());
        if (downloadId == 0) {
            gemmaPref.setSummary(R.string.pref_summary_gemma_not_downloaded);
            gemmaPref.setEnabled(true);
            return;
        }

        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) return;
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        android.database.Cursor cursor = downloadManager.query(query);

        if (cursor != null && cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusIndex != -1) {
                int status = cursor.getInt(statusIndex);
                switch (status) {
                    case DownloadManager.STATUS_RUNNING:
                        int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        long downloaded = (downloadedIndex != -1) ? cursor.getLong(downloadedIndex) : 0;
                        long total = (totalIndex != -1) ? cursor.getLong(totalIndex) : 0;
                        if (total > 0) {
                            int progress = (int) (downloaded * 100L / total);
                            gemmaPref.setSummary("Downloading Gemma... " + progress + "%");
                        } else {
                            gemmaPref.setSummary("Downloading Gemma...");
                        }
                        gemmaPref.setEnabled(false);
                        new Handler(Looper.getMainLooper()).postDelayed(this::updateAIModelStatus, 2000);
                        break;
                    case DownloadManager.STATUS_PENDING:
                        gemmaPref.setSummary("Download pending...");
                        gemmaPref.setEnabled(false);
                        new Handler(Looper.getMainLooper()).postDelayed(this::updateAIModelStatus, 2000);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int reason = (reasonIndex != -1) ? cursor.getInt(reasonIndex) : -1;
                        String errorMsg = "Download failed (Error " + reason + ")";
                        if (reason == 403 || reason == 401) {
                            errorMsg = "Access Denied (403). The model might require accepting terms on HuggingFace.";
                        } else if (reason == 1006) {
                            errorMsg = "Download failed: Insufficient space on device.";
                        }
                        gemmaPref.setSummary(errorMsg + ". Click to retry.");
                        gemmaPref.setEnabled(true);
                        AppPreference.setGemmaDownloadId(requireContext(), 0);
                        break;
                    default:
                        gemmaPref.setSummary(R.string.pref_summary_gemma_not_downloaded);
                        gemmaPref.setEnabled(true);
                        break;
                }
            }
            cursor.close();
        }
    }

    private void analyzeCustomUrl() {
        String customUrl = AppPreference.getCustomWeatherUrl(requireContext());
        if (customUrl.isEmpty()) {
            Toast.makeText(getContext(), "Zadejte nejprve vlastností URL v nastavení", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomWeatherUrlAnalyzer.fetchAndAnalyzeUrl(requireContext(), customUrl, new CustomWeatherUrlAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(String result) {
                if (getContext() != null) {
                    AppPreference.setCustomWeatherParsingRules(getContext(), result);
                    Toast.makeText(getContext(), "Struktura rozponána a pravidla uložena!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Analýza selhala: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onStatusUpdate(String status) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }
        updateSummaries();
        updateAIModelStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPreferenceScreen() != null && getPreferenceScreen().getSharedPreferences() != null) {
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    private void updateSummaries() {
        for (String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
        }
    }
}