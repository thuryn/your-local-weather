package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.LicenseKey;
import org.thosp.yourlocalweather.model.LicenseKeysDbHelper;

public class ApiKeys {

    public static final int DEFAULT_AVAILABLE_LOCATIONS = 2;
    public static final int MAX_AVAILABLE_LOCATIONS = 20;

    public static final String DEFAULT_OPEN_WEATHER_MAP_API_KEY =
            "a81368c0da796214a41094ab9b63025e";

    public static String getOpenweathermapApiKey(Context context) {
        String openweathermapApiKey = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        Constants.KEY_PREF_OPEN_WEATHER_MAP_API_KEY,
                        ""
                );
        if ((openweathermapApiKey == null) || openweathermapApiKey.isEmpty()) {
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
        if ((openweathermapApiKey == null) || openweathermapApiKey.isEmpty()) {
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
        return ((openweathermapApiKey == null) || openweathermapApiKey.isEmpty());
    }

    public static int getAvailableLocations(Context context) {
        if (isDefaultOpenweatherApiKey(context)) {
            return DEFAULT_AVAILABLE_LOCATIONS;
        } else {
            return MAX_AVAILABLE_LOCATIONS;
        }
    }

    public static boolean isWeatherForecastFeaturesFree(Context context) {
        String weatherForecastFeatures = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        Constants.KEY_PREF_WEATHER_FORECAST_FEATURES,
                        "weather_forecast_features_free"
                );
        return (weatherForecastFeatures == null) ||
                weatherForecastFeatures.isEmpty() ||
                "weather_forecast_features_free".equals(weatherForecastFeatures);
    }

    public static String getInitialLicenseKey(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(
                        Constants.KEY_PREF_WEATHER_LICENSE_KEY,
                        ""
                );
    }

    public static String getLicenseKey(Context context, LicenseKey licenseKey ) {
        if (ApiKeys.isWeatherForecastFeaturesFree(context)) {
            return null;
        }
        final LicenseKeysDbHelper licenseKeysDbHelper = LicenseKeysDbHelper.getInstance(context);
        if ((licenseKey != null) && (licenseKey.getToken() != null)) {
            return licenseKey.getToken();
        } else {
            return getInitialLicenseKey(context);
        }
    }
}
