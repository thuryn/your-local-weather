package org.thosp.yourlocalweather.settings.fragments;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract;
import org.thosp.yourlocalweather.model.ReverseGeocodingCacheDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PowerSavePreferenceFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("en"));

    private final String[] SUMMARIES_TO_UPDATE = {
            Constants.KEY_WAKE_UP_STRATEGY,
            Constants.KEY_PREF_LOCATION_GPS_ENABLED,
            Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_powersave, rootKey);
        initLocationCache();
        initWakeUpStrategy();
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
        if (!Constants.KEY_PREF_LOCATION_GPS_ENABLED.equals(key)) {
            ListPreference preference = (ListPreference) findPreference(key);
            if (preference == null) {
                return;
            }
            preference.setSummary(preference.getEntry());
        }
    }

    private void updateSummary(String key, boolean changing) {
        switch (key) {
            case Constants.KEY_PREF_LOCATION_GPS_ENABLED:
                entrySummary(key);
                break;
            case Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED:
                AppPreference.getInstance().clearLocationCacheEnabled();
                break;
        }
    }

    private void updateSummaries() {
        for (String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
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

    private void initLocationCache() {

        Preference locationCacheEnabled = findPreference(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED);
        locationCacheEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                boolean enabled = (Boolean) value;
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                preferences.edit().putBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, enabled).apply();
                return true;
            }
        });

        Preference locationLasting = findPreference(Constants.APP_SETTINGS_LOCATION_CACHE_LASTING_HOURS);
        locationLasting.setSummary(
                getLocationLastingLabel(Integer.parseInt(
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Constants.APP_SETTINGS_LOCATION_CACHE_LASTING_HOURS, "720"))
                )
        );
        locationLasting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference locationLasting, Object value) {
                String locationRowLastingHoursTxt = (String) value;
                Integer locationRowLastingHours = Integer.valueOf(locationRowLastingHoursTxt);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                preferences.edit().putString(Constants.APP_SETTINGS_LOCATION_CACHE_LASTING_HOURS, locationRowLastingHoursTxt).apply();
                locationLasting.setSummary(getString(getLocationLastingLabel(locationRowLastingHours)));
                return true;
            }
        });


        Preference button = findPreference("clear_cache_button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ReverseGeocodingCacheDbHelper mDbHelper = ReverseGeocodingCacheDbHelper.getInstance(preference.getContext());
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                mDbHelper.onUpgrade(db, 0, 0);
                return true;
            }
        });

        Preference dbInfo = findPreference("db_info");
        dbInfo.setSummary(getDataFromCacheDB());
    }

    private int getLocationLastingLabel(int locationLastingValue) {
        int locationLastingLastingId;
        switch (locationLastingValue) {
            case 12:
                locationLastingLastingId = R.string.location_cache_12_label;
                break;
            case 24:
                locationLastingLastingId = R.string.location_cache_24_label;
                break;
            case 168:
                locationLastingLastingId = R.string.location_cache_168_label;
                break;
            case 2190:
                locationLastingLastingId = R.string.location_cache_2190_label;
                break;
            case 4380:
                locationLastingLastingId = R.string.location_cache_4380_label;
                break;
            case 8760:
                locationLastingLastingId = R.string.location_cache_8760_label;
                break;
            case 88888:
                locationLastingLastingId = R.string.location_cache_88888_label;
                break;
            case 720:
            default:
                locationLastingLastingId = R.string.location_cache_720_label;
                break;
        }
        return locationLastingLastingId;
    }

    private void initWakeUpStrategy() {
        Preference wakeUpStrategy = findPreference(Constants.KEY_WAKE_UP_STRATEGY);
        wakeUpStrategy.setSummary(
                getWakeUpStrategyLabel(
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(Constants.KEY_WAKE_UP_STRATEGY, "nowakeup")
                )
        );
        wakeUpStrategy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference wakeUpStrategy, Object value) {
                String wakeUpStrategyValue = (String) value;
                wakeUpStrategy.setSummary(getString(getWakeUpStrategyLabel(wakeUpStrategyValue)));
                return true;
            }
        });
    }

    private int getWakeUpStrategyLabel(String wakeUpStrategyValue) {
        int wakeUpStrategyId;
        switch (wakeUpStrategyValue) {
            case "wakeuppartial":
                wakeUpStrategyId = R.string.wakeuppartial_label;
                break;
            case "wakeupfull":
                wakeUpStrategyId = R.string.wakeupfull_label;
                break;
            case "nowakeup":
            default:
                wakeUpStrategyId = R.string.nowakeup_label;
                break;
        }
        return wakeUpStrategyId;
    }

    private String getDataFromCacheDB() {

        ReverseGeocodingCacheDbHelper mDbHelper = ReverseGeocodingCacheDbHelper.getInstance(getActivity());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        long numberOfRowsInAddress = DatabaseUtils.queryNumEntries(db, ReverseGeocodingCacheContract.LocationAddressCache.TABLE_NAME);

        StringBuilder lastRowsFromDB = new StringBuilder();

        lastRowsFromDB.append("There are ");
        lastRowsFromDB.append(numberOfRowsInAddress);
        lastRowsFromDB.append(" of rows in cache.\n\n");

        String[] projection = {
                ReverseGeocodingCacheContract.LocationAddressCache.COLUMN_NAME_ADDRESS,
                ReverseGeocodingCacheContract.LocationAddressCache.COLUMN_NAME_CREATED,
                ReverseGeocodingCacheContract.LocationAddressCache._ID
        };

        String sortOrder = ReverseGeocodingCacheContract.LocationAddressCache.COLUMN_NAME_CREATED + " DESC";

        Cursor cursor = db.query(
                ReverseGeocodingCacheContract.LocationAddressCache.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        int rowsCounter = 0;

        while (cursor.moveToNext()) {

            if (!cursor.isFirst()) {
                lastRowsFromDB.append("\n");
            }

            byte[] cachedAddressBytes = cursor.getBlob(
                    cursor.getColumnIndexOrThrow(ReverseGeocodingCacheContract.LocationAddressCache.COLUMN_NAME_ADDRESS));
            Address address = ReverseGeocodingCacheDbHelper.getAddressFromBytes(cachedAddressBytes);

            long recordCreatedinMilis = cursor.getLong(cursor.getColumnIndexOrThrow(ReverseGeocodingCacheContract.LocationAddressCache.COLUMN_NAME_CREATED));
            String recordCreatedTxt = iso8601Format.format(new Date(recordCreatedinMilis));

            int itemId = cursor.getInt(cursor.getColumnIndexOrThrow(ReverseGeocodingCacheContract.LocationAddressCache._ID));

            lastRowsFromDB.append(itemId);
            lastRowsFromDB.append(" : ");
            lastRowsFromDB.append(recordCreatedTxt);
            lastRowsFromDB.append(" : ");
            if (address.getLocality() != null) {
                lastRowsFromDB.append(address.getLocality());
                if (!address.getLocality().equals(address.getSubLocality())) {
                    lastRowsFromDB.append(" - ");
                    lastRowsFromDB.append(address.getSubLocality());
                }
            }

            rowsCounter++;
            if (rowsCounter > 7) {
                break;
            }
        }
        cursor.close();

        return lastRowsFromDB.toString();
    }
}
