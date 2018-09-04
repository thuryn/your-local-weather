package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;

public class ApiKeys {

    public static final String DEFAULT_OPEN_WEATHER_MAP_API_KEY =
            "97878c1d0af280fe1bfc5441880d0c6f";

    public static String getOpenweathermapApiKey(Context context) {
        String openweathermapApiKey = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY,
                        ""
                );
        if ((openweathermapApiKey == null) || "".equals(openweathermapApiKey)) {
            openweathermapApiKey = DEFAULT_OPEN_WEATHER_MAP_API_KEY;
        }
        return openweathermapApiKey;
    }

    public static String getOpenweathermapApiKeyForPreferences(Context context) {
        String openweathermapApiKey = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY,
                        ""
                );
        if ((openweathermapApiKey == null) || "".equals(openweathermapApiKey)) {
            openweathermapApiKey = context.getString(R.string.open_weather_map_api_default_key);
        }
        return openweathermapApiKey;
    }

    public static boolean isDefaultOpenweatherApiKey(Context context) {
        String openweathermapApiKey = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY,
                        ""
                );
        return ((openweathermapApiKey == null) || "".equals(openweathermapApiKey));
    }
}
