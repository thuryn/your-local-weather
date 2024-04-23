package org.thosp.yourlocalweather.utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.LicenseKey;
import org.thosp.yourlocalweather.model.LicenseKeysDbHelper;

public class ApiKeys {

    private static final String TAG = "ApiKeys";

    public static final int DEFAULT_AVAILABLE_LOCATIONS = 2;
    public static final int MAX_AVAILABLE_LOCATIONS = 20;

    public static int getAvailableLocations(Context context) {
        return DEFAULT_AVAILABLE_LOCATIONS;
    }
}
