package org.thosp.yourlocalweather.settings.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.textfield.TextInputEditText;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.SettingsActivity;
import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.service.ReconciliationDbService;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class GeneralPreferenceFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "GeneralPreferenceFragment";

    private final String[] SUMMARIES_TO_UPDATE = {
            Constants.KEY_PREF_HIDE_DESCRIPTION,
            Constants.PREF_LANGUAGE,
            Constants.PREF_THEME,
            Constants.KEY_PREF_WEATHER_ICON_SET,
            Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY,
            Constants.KEY_PREF_WEATHER_FORECAST_FEATURES
    };

    public static void restartApp(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        EditTextPreference openWeatherMapApiKey =
                (EditTextPreference) findPreference(Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY);
        openWeatherMapApiKey.setSummary(ApiKeys.getOpenweathermapApiKeyForPreferences(getActivity()));
        checkApiKeyMenuOptionPresence();
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
    }

    private void updateSummary(String key, boolean changing) {
        switch (key) {
            case Constants.KEY_PREF_HIDE_DESCRIPTION:
                if (changing) {
                    Intent intent = new Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
                    intent.setPackage("org.thosp.yourlocalweather");
                    getActivity().sendBroadcast(intent);
                }
                break;
            case Constants.PREF_LANGUAGE:
                entrySummary(key);
                if (changing) {
                    AppPreference appPreference = AppPreference.getInstance();
                    appPreference.clearLanguage();
                    String newLocale = appPreference.getLanguage(getActivity().getApplicationContext());
                    LanguageUtil.setLanguage(getActivity().getApplication(), newLocale);
                    updateLocationsLocale(newLocale);
                    WidgetUtils.updateWidgets(getActivity());
                    DialogFragment dialog = new SettingsActivity.SettingsAlertDialog().newInstance(R.string.update_locale_dialog_message);
                    dialog.show(getActivity().getFragmentManager(), "restartApp");
                }
                break;
            case Constants.PREF_THEME:
                entrySummary(key);
                if (changing) {
                    YourLocalWeather app = (YourLocalWeather) getActivity().getApplication();
                    app.reloadTheme();
                    app.applyTheme(getActivity());
                    restartApp(getActivity());
                }
                break;
            case Constants.KEY_PREF_WEATHER_ICON_SET:
                entrySummary(key);
                break;
            case Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY:
                findPreference(key).setSummary(ApiKeys.getOpenweathermapApiKeyForPreferences(getActivity()));
                checkAndDeleteLocations();
                break;
            case Constants.KEY_PREF_WEATHER_FORECAST_FEATURES:
                entrySummary(key);
                checkApiKeyMenuOptionPresence();
                break;
            case Constants.KEY_PREF_WEATHER_LICENSE_KEY:
                calculateInitialToken(key);
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

    private void calculateInitialToken(String key) {
        EditTextPreference inputLicenceKey = (EditTextPreference) findPreference(key);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit().putString(Constants.KEY_PREF_WEATHER_INITIAL_TOKEN, encryptKey(inputLicenceKey.getText())).apply();
    }

    private String encryptKey(String key) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            KeySpec spec = new PBEKeySpec(key.toCharArray(), salt, 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String h = Integer.toHexString(0xFF & hash[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void updateLocationsLocale(String newLocale) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getActivity());
        for (Location location : locationsDbHelper.getAllRows()) {
            locationsDbHelper.updateLocale(location.getId(), newLocale);
        }
    }

    private void setDefaultValues() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (!preferences.contains(Constants.PREF_LANGUAGE)) {
            preferences.edit().putString(Constants.PREF_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage()).apply();
            entrySummary(Constants.PREF_LANGUAGE);
        }
    }

    private void updateSummaries() {
        for (String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
        }
    }

    private void checkAndDeleteLocations() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getActivity());
        List<Location> allLocations = locationsDbHelper.getAllRows();
        if (allLocations.size() <= ApiKeys.getAvailableLocations(getActivity())) {
            return;
        }
        for (Location location : allLocations) {
            if (location.getOrderId() >= ApiKeys.getAvailableLocations(getActivity())) {
                locationsDbHelper.deleteRecordFromTable(location);
            }
        }
        sendMessageToReconciliationDbService(true);
    }

    private void checkApiKeyMenuOptionPresence() {
        if (ApiKeys.isWeatherForecastFeaturesFree(getActivity())) {
            findPreference(Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY).setEnabled(true);
            findPreference(Constants.KEY_PREF_WEATHER_LICENSE_KEY).setEnabled(false);
        } else {
            findPreference(Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY).setEnabled(false);
            findPreference(Constants.KEY_PREF_WEATHER_LICENSE_KEY).setEnabled(true);
        }
    }

    protected void sendMessageToReconciliationDbService(boolean force) {
        appendLog(getActivity(),
                TAG,
                "going run reconciliation DB service");
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_RECONCILIATION");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("force", force);
        getActivity().startService(intent);
    }
}
