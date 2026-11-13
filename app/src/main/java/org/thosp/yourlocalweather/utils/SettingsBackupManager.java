package org.thosp.yourlocalweather.utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsBackupManager {

    private static final String TAG = "SettingsBackupManager";

    public static boolean exportSettings(Context context, Uri destinationUri) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("timestamp", System.currentTimeMillis());

            // 1. Export SharedPreferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> allPrefs = prefs.getAll();
            JSONObject prefsJson = new JSONObject();

            for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (val == null) continue;

                if (val instanceof Set) {
                    JSONArray arr = new JSONArray();
                    for (Object item : (Set<?>) val) {
                        arr.put(item);
                    }
                    prefsJson.put(key, arr);
                } else {
                    prefsJson.put(key, val);
                }
            }
            root.put("preferences", prefsJson);

            // 2. Export Locations
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
            List<Location> locations = locationsDbHelper.getAllRows();
            JSONArray locationsArray = new JSONArray();

            for (Location location : locations) {
                JSONObject locJson = new JSONObject();
                locJson.put("id", location.getId());
                locJson.put("orderId", location.getOrderId());
                locJson.put("nickname", location.getNickname());
                locJson.put("locale", location.getLocaleAbbrev());
                locJson.put("longitude", location.getLongitude());
                locJson.put("latitude", location.getLatitude());
                locJson.put("accuracy", location.getAccuracy());
                locJson.put("locationSource", location.getLocationSource());
                locJson.put("lastLocationUpdate", location.getLastLocationUpdate());
                locJson.put("addressFound", location.isAddressFound());
                locJson.put("enabled", location.isEnabled());

                if (location.getAddress() != null) {
                    Address addr = location.getAddress();
                    JSONObject addrJson = new JSONObject();
                    if (addr.getLocality() != null) addrJson.put("locality", addr.getLocality());
                    if (addr.getAdminArea() != null) addrJson.put("adminArea", addr.getAdminArea());
                    if (addr.getCountryName() != null) addrJson.put("countryName", addr.getCountryName());
                    if (addr.getCountryCode() != null) addrJson.put("countryCode", addr.getCountryCode());
                    locJson.put("address", addrJson);
                }
                locationsArray.put(locJson);
            }
            root.put("locations", locationsArray);

            // Write to Uri
            try (OutputStream os = context.getContentResolver().openOutputStream(destinationUri);
                 OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write(root.toString(2));
                writer.flush();
            }

            appendLog(context, TAG, "Export settings successfully completed");
            return true;
        } catch (Exception e) {
            appendLog(context, TAG, "Failed to export settings", e);
            return false;
        }
    }

    public static boolean importSettings(Context context, Uri sourceUri) {
        try {
            StringBuilder sb = new StringBuilder();
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }

            JSONObject root = new JSONObject(sb.toString());

            // 1. Import SharedPreferences
            if (root.has("preferences")) {
                JSONObject prefsJson = root.getJSONObject("preferences");
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

                Iterator<String> keys = prefsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = prefsJson.get(key);

                    if (val instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) val);
                    } else if (val instanceof Integer) {
                        editor.putInt(key, (Integer) val);
                    } else if (val instanceof Long) {
                        editor.putLong(key, (Long) val);
                    } else if (val instanceof Double || val instanceof Float) {
                        editor.putFloat(key, ((Number) val).floatValue());
                    } else if (val instanceof String) {
                        editor.putString(key, (String) val);
                    } else if (val instanceof JSONArray) {
                        JSONArray arr = (JSONArray) val;
                        Set<String> set = new HashSet<>();
                        for (int i = 0; i < arr.length(); i++) {
                            set.add(arr.getString(i));
                        }
                        editor.putStringSet(key, set);
                    }
                }
                editor.apply();
            }

            // 2. Refresh App state
            AppPreference.getInstance().clearLanguage();
            String language = AppPreference.getInstance().getLanguage(context);
            LanguageUtil.setLanguage(context, language);
            WidgetUtils.updateWidgets(context);

            appendLog(context, TAG, "Import settings successfully completed");
            return true;
        } catch (Exception e) {
            appendLog(context, TAG, "Failed to import settings", e);
            return false;
        }
    }
}
