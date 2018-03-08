package org.thosp.yourlocalweather.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeather;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppPreference {

    public static String getTemperatureWithUnit(Context context, double value) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return String.format(Locale.getDefault(), "%d",
                    Math.round(fahrenheitValue)) + "°F";
        } else {
            return String.format(Locale.getDefault(), "%d",
                    Math.round(value)) + "°C";
        }
    }

    public static String getTemperatureUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return "°F";
        } else {
            return "°C";
        }
    }

    public static double getTemperature(Context context, String value) {
        return getTemperature(context, Double.parseDouble(value.replace(",", ".")));
    }

    public static double getTemperature(Context context, double value) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return (value * 1.8d) + 32;
        } else {
            return value;
        }
    }

    public static WindWithUnit getWindWithUnit(Context context, String value) {
        return getWindWithUnit(context, Float.parseFloat(value.replace(",", ".")));
    }

    public static WindWithUnit getWindWithUnit(Context context, float value) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
        if (unitsFromPreferences.contains("km_per_hour") ) {
            double kmhValue = 3.6d * value;
            return new WindWithUnit(kmhValue, context.getString(R.string.wind_speed_kilometers));
        } else if (unitsFromPreferences.contains("miles_per_hour") ) {
            double mhValue = 2.2369d * value;
            return new WindWithUnit(mhValue, context.getString(R.string.wind_speed_miles));
        } else {
            return new WindWithUnit(value, context.getString(R.string.wind_speed_meters));
        }
    }

    public static String getWindInString(Context context, double stringValue) {
        return String.format(Locale.getDefault(), "%.1f", getWind(context, stringValue));
    }

    public static double getWind(Context context, String stringValue) {
        double value = Double.parseDouble(stringValue.replace(",", "."));
        return getWind(context, value);
    }

    public static double getWind(Context context, double windSpeed) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
        if (unitsFromPreferences.contains("km_per_hour") ) {
            return 3.6d * windSpeed;
        } else if (unitsFromPreferences.contains("miles_per_hour") ) {
            return 2.2369d * windSpeed;
        } else {
            return windSpeed;
        }
    }

    public static String getWindUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
        if (unitsFromPreferences.contains("km_per_hour") ) {
            return context.getString(R.string.wind_speed_kilometers);
        } else if (unitsFromPreferences.contains("miles_per_hour") ) {
            return context.getString(R.string.wind_speed_miles);
        } else {
            return context.getString(R.string.wind_speed_meters);
        }
    }

    public static String getPressureUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
        if (unitsFromPreferences.contains("mmhg") ) {
            return context.getString(R.string.pressure_measurement_mmhg);
        } else {
            return context.getString(R.string.pressure_measurement);
        }
    }

    public static String getPressureInString(Context context, double stringValue) {
        return String.format(Locale.getDefault(), "%.0f", getPressureWithUnit(context, stringValue).getWindSpeed());
    }

    public static WindWithUnit getPressureWithUnit(Context context, double value) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
        if (unitsFromPreferences.contains("mmhg") ) {
            double mmhgValue = value / 1.33322387415f;
            return new WindWithUnit(mmhgValue, context.getString(R.string.pressure_measurement_mmhg));
        } else {
            return new WindWithUnit(value, context.getString(R.string.pressure_measurement));
        }
    }

    public static boolean hideDescription(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_HIDE_DESCRIPTION, false);
    }

    public static String getInterval(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                                .getString(Constants.KEY_PREF_INTERVAL_NOTIFICATION, "60");
    }

    public static boolean isVibrateEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_VIBRATE,
                false);
    }

    public static boolean isNotificationEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_IS_NOTIFICATION_ENABLED, false);
    }

    public static boolean isUpdateLocationEnabled(Context context, Location currentLocation) {
        if ((currentLocation == null) || (currentLocation.getOrderId() != 0) || !currentLocation.isEnabled()) {
            return false;
        }
        return true;
    }
        
    public static String getLocationGeocoderSource(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_GEOCODER_SOURCE, "location_geocoder_local");
    }

    public static boolean isGpsEnabledByPreferences(Context context) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location locationAuto = locationsDbHelper.getLocationByOrderId(0);
        return locationAuto.isEnabled() && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_LOCATION_GPS_ENABLED, true);
    }

    public static String getIconSet(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WEATHER_ICON_SET, "weather_icon_set_merlin_the_red");
    }

    public static boolean showLabelsOnWidget(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_PREF_WIDGET_SHOW_LABELS, false);
    }

    public static String getTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIDGET_THEME, "dark");
    }

    public static Set<Integer> getForecastActivityColumns(Context context) {
        Set<String> defaultVisibleColumns = new HashSet<>();
        defaultVisibleColumns.add("1");
        defaultVisibleColumns.add("2");
        defaultVisibleColumns.add("3");
        defaultVisibleColumns.add("4");
        Set<String> visibleColumns = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
                Constants.KEY_PREF_FORECAST_ACTIVITY_COLUMNS, defaultVisibleColumns);
        Set<Integer> result = new HashSet<>();
        for (String visibleColumn: visibleColumns) {
            result.add(Integer.valueOf(visibleColumn));
        }
        return result;
    }

    public static void setForecastActivityColumns(Context context, Set<Integer> visibleColumns) {
        Set<String> columnsToStore = new HashSet<>();
        for (Integer visibleColumn: visibleColumns) {
            columnsToStore.add(String.valueOf(visibleColumn));
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(
                Constants.KEY_PREF_FORECAST_ACTIVITY_COLUMNS, columnsToStore).apply();
    }

    public static int getTextColor(Context context) {
        String theme = getTheme(context);
        if (null == theme) {
            return ContextCompat.getColor(context, R.color.widget_transparentTheme_textColorPrimary);
        } else switch (theme) {
            case "dark":
                return ContextCompat.getColor(context, R.color.widget_darkTheme_textColorPrimary);
            case "light":
                return ContextCompat.getColor(context, R.color.widget_lightTheme_textColorPrimary);
            default:
                return ContextCompat.getColor(context, R.color.widget_transparentTheme_textColorPrimary);
        }
    }
    
    public static int getBackgroundColor(Context context) {
        String theme = getTheme(context);
        if (null == theme) {
            return ContextCompat.getColor(context,
                    R.color.widget_transparentTheme_colorBackground);
        } else switch (theme) {
            case "dark":
                return ContextCompat.getColor(context,
                        R.color.widget_darkTheme_colorBackground);
            case "light":
                return ContextCompat.getColor(context,
                        R.color.widget_lightTheme_colorBackground);
            default:
                return ContextCompat.getColor(context,
                        R.color.widget_transparentTheme_colorBackground);
        }
    }
    
    public static int getWindowHeaderBackgroundColorId(Context context) {
        String theme = getTheme(context);
        if (null == theme) {
            return ContextCompat.getColor(context,
                    R.color.widget_transparentTheme_window_colorBackground);
        } else switch (theme) {
            case "dark":
                return ContextCompat.getColor(context,
                        R.color.widget_darkTheme_window_colorBackground);
            case "light":
                return ContextCompat.getColor(context,
                        R.color.widget_lightTheme_window_colorBackground);
            default:
                return ContextCompat.getColor(context,
                        R.color.widget_transparentTheme_window_colorBackground);
        }
    }

    public static String getLocationAutoUpdatePeriod(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD, "0");
    }

    public static String getLocationUpdatePeriod(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_PERIOD, "60");
    }

    public static void setCurrentLocationId(Context context, long currentLocationId) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(
                Constants.CURRENT_LOCATION_ID, currentLocationId).apply();
    }

    public static long getCurrentLocationId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(
                Constants.CURRENT_LOCATION_ID, 0);
    }
}
