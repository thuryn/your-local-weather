package org.thosp.yourlocalweather.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeather;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppPreference {

    public static String getLocalizedTime(Context context, Date inputTime, Locale locale) {
        String timeStylePreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TIME_STYLE, "system");
        if ("system".equals(timeStylePreferences)) {
            return DateFormat.getTimeFormat(context).format(inputTime);
        } else if ("12h".equals(timeStylePreferences)) {
            SimpleDateFormat sdf_12 = new SimpleDateFormat("hh:mm aaaa", locale);
            return sdf_12.format(inputTime);
        } else {
            SimpleDateFormat sdf_24 = new SimpleDateFormat("HH:mm", locale);
            return sdf_24.format(inputTime);
        }
    }

    public static String getLocalizedDateTime(Context context, Date inputTime, boolean showYear, Locale locale) {
        String dateStylePreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_DATE_STYLE, "system");
        switch (dateStylePreferences) {
            case "date_style_dots": return getSimpleDateFormatForDate("dd.MM.", "dd.MM.yy", showYear, locale).format(inputTime) +
                                        " " + getLocalizedTime(context, inputTime, locale);
            case "date_style_slash_month_first": return getSimpleDateFormatForDate("MM/dd", "yy/MM/dd", showYear, locale).format(inputTime) +
                                                    " " + getLocalizedTime(context, inputTime, locale);
            case "date_style_slash_day_first": return getSimpleDateFormatForDate("dd/MM", "dd/MM/yy", showYear, locale).format(inputTime) +
                                                    " " + getLocalizedTime(context, inputTime, locale);
            case "system":
            default: return DateFormat.getDateFormat(context).format(inputTime) + " " +
                    getLocalizedTime(context, inputTime, locale);
        }
    }

    private static SimpleDateFormat getSimpleDateFormatForDate(String datePattern, String yearPattern, boolean showYear, Locale locale) {
        if (showYear) {
            return new SimpleDateFormat(yearPattern, locale);
        } else {
            return new SimpleDateFormat(datePattern, locale);
        }
    }

    public static boolean is12TimeStyle(Context context) {
        String timeStylePreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TIME_STYLE, "system");
        if ("system".equals(timeStylePreferences)) {
            return !DateFormat.is24HourFormat(context);
        } else if ("12h".equals(timeStylePreferences)) {
            return true;
        } else {
            return false;
        }
    }

    public static String getLocalizedHour(Context context, Date inputTime, Locale locale) {
        String timeStylePreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TIME_STYLE, "system");
        if ("system".equals(timeStylePreferences)) {
            return DateFormat.getTimeFormat(context).format(inputTime);
        } else if ("12h".equals(timeStylePreferences)) {
            SimpleDateFormat sdf_12 = new SimpleDateFormat("hh", locale);
            return sdf_12.format(inputTime);
        } else {
            SimpleDateFormat sdf_24 = new SimpleDateFormat("HH", locale);
            return sdf_24.format(inputTime);
        }
    }

    public static WindWithUnit getWindWithUnit(Context context, float value, float direction, Locale locale) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
        if (unitsFromPreferences.contains("km_per_hour") ) {
            double kmhValue = 3.6d * value;
            return new WindWithUnit(context, kmhValue, context.getString(R.string.wind_speed_kilometers), direction, locale);
        } else if (unitsFromPreferences.contains("miles_per_hour") ) {
            double mhValue = 2.2369d * value;
            return new WindWithUnit(context, mhValue, context.getString(R.string.wind_speed_miles), direction, locale);
        } else if (unitsFromPreferences.contains("knots") ) {
            double knotsValue = 1.9438445d * value;
            return new WindWithUnit(context, knotsValue, context.getString(R.string.wind_speed_knots), direction, locale);
        } else {
            return new WindWithUnit(context, value, context.getString(R.string.wind_speed_meters), direction, locale);
        }
    }

    public static double getRainOrSnow(Context context, double value) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_RAIN_SNOW_UNITS, "mm");
        if (unitsFromPreferences.contains("inches") ) {
            return 0.03937007874d * value;
        } else {
            return value;
        }
    }

    public static int getGraphFormatterForRainOrSnow(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_RAIN_SNOW_UNITS, "mm");
        if (unitsFromPreferences.contains("inches") ) {
            return 3;
        } else {
            return 2;
        }
    }

    public static String getFormatedRainOrSnow(Context context, double value, Locale locale) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_RAIN_SNOW_UNITS, "mm");
        String format;
        if (unitsFromPreferences.contains("inches") ) {
            format = "%.3f";
        } else {
            format = "%.1f";
        }
        return String.format(locale, format, getRainOrSnow(context, value));
    }

    public static int getRainOrSnowUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_RAIN_SNOW_UNITS, "mm");
        if (unitsFromPreferences.contains("inches") ) {
            return R.string.inches_label;
        } else {
            return R.string.millimetre_label;
        }
    }

    public static int getRainOrSnowForecastWeadherWidth(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_RAIN_SNOW_UNITS, "mm");
        if (unitsFromPreferences.contains("inches") ) {
            return 60;
        } else {
            return 40;
        }
    }

    public static String getWindDirection(Context context, double windDirection, Locale locale) {
        return new WindWithUnit(context, windDirection, locale).getWindDirection();
    }

    public static String getWindInString(Context context, double stringValue, Locale locale) {
        return String.format(locale, "%.1f", getWind(context, stringValue));
    }

    public static double getWind(Context context, double windSpeed) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
        if (unitsFromPreferences.contains("km_per_hour") ) {
            return 3.6d * windSpeed;
        } else if (unitsFromPreferences.contains("miles_per_hour") ) {
            return 2.2369d * windSpeed;
        } else if (unitsFromPreferences.contains("knots") ) {
            return 1.9438445d * windSpeed;
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
        } else if (unitsFromPreferences.contains("knots") ) {
            return context.getString(R.string.wind_speed_knots);
        } else {
            return context.getString(R.string.wind_speed_meters);
        }
    }

    public static String getPressureUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
        switch (unitsFromPreferences) {
            case "mmhg": return context.getString(R.string.pressure_measurement_mmhg);
            case "inhg": return context.getString(R.string.pressure_measurement_inhg);
            case "mbar": return context.getString(R.string.pressure_measurement_mbar);
            default: return context.getString(R.string.pressure_measurement);
        }
    }

    public static int getPressureDecimalPlaces(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
        switch (unitsFromPreferences) {
            case "mmhg": return 2;
            case "inhg": return 2;
            case "mbar": return 0;
            default: return 0;
        }
    }

    public static String getPressureInString(Context context, double stringValue, Locale locale) {
        return String.format(locale, "%." + getPressureDecimalPlaces(context)
                + "f", getPressureWithUnit(context, stringValue, locale).getPressure());
    }

    public static PressureWithUnit getPressureWithUnit(Context context, double value, Locale locale) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
        switch (unitsFromPreferences) {
            case "mmhg": return new PressureWithUnit(value * 0.75f,
                                                 context.getString(R.string.pressure_measurement_mmhg), locale);
            case "inhg": return new PressureWithUnit(value * 0.029529983071445f,
                                                 context.getString(R.string.pressure_measurement_inhg), locale);
            case "mbar": return new PressureWithUnit(value,
                                                 context.getString(R.string.pressure_measurement_mbar), locale);
            default: return new PressureWithUnit(value, context.getString(R.string.pressure_measurement), locale);
        }
    }

    public static boolean hideDescription(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_HIDE_DESCRIPTION, false);
    }

    public static long getLastNotificationTimeInMs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(Constants.LAST_NOTIFICATION_TIME_IN_MS, 0);
    }

    public static void setLastNotificationTimeInMs(Context context, long lastNotificationTimeInMs) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(Constants.LAST_NOTIFICATION_TIME_IN_MS, lastNotificationTimeInMs).apply();
    }

    public static long getLastSensorServicesCheckTimeInMs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(Constants.LAST_SENSOR_SERVICES_CHECK_TIME_IN_MS, 0);
    }

    public static void setLastSensorServicesCheckTimeInMs(Context context, long lastSensorServicesCheckTimeInMs) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(Constants.LAST_SENSOR_SERVICES_CHECK_TIME_IN_MS, lastSensorServicesCheckTimeInMs).apply();
    }

    public static String getInterval(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                                .getString(Constants.KEY_PREF_INTERVAL_NOTIFICATION, "60");
    }

    public static String getNotificationPresence(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.KEY_PREF_NOTIFICATION_PRESENCE, "when_updated");
    }

    public static String getNotificationStatusIconStyle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.KEY_PREF_NOTIFICATION_STATUS_ICON, "icon_sun");
    }

    public static String getNotificationVisualStyle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.KEY_PREF_NOTIFICATION_VISUAL_STYLE, "build_in");
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

    public static void setNotificationEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
                Constants.KEY_PREF_IS_NOTIFICATION_ENABLED, enabled).apply();
    }

    public static boolean isWidgetGraphNativeScaled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_WIDGET_GRAPH_NATIVE_SCALE, false);
    }

    public static boolean isShowControls(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_WIDGET_SHOW_CONTROLS, false);
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
        defaultVisibleColumns.add("5");
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

    public static Set<Integer> getGraphsActivityVisibleGraphs(Context context) {
        Set<String> defaultVisibleGraphs = new HashSet<>();
        defaultVisibleGraphs.add("0");
        defaultVisibleGraphs.add("1");
        defaultVisibleGraphs.add("2");;
        defaultVisibleGraphs.add("4");
        defaultVisibleGraphs.add("6");
        defaultVisibleGraphs.add("7");
        Set<String> visibleColumns = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
                Constants.KEY_PREF_GRAPHS_ACTIVITY_VISIBLE_GRAPHS, defaultVisibleGraphs);
        Set<Integer> result = new HashSet<>();
        for (String visibleColumn: visibleColumns) {
            result.add(Integer.valueOf(visibleColumn));
        }
        return result;
    }

    public static void setGraphsActivityVisibleGraphs(Context context, Set<Integer> visibleGraphs) {
        Set<String> columnsToStore = new HashSet<>();
        for (Integer visibleColumn: visibleGraphs) {
            columnsToStore.add(String.valueOf(visibleColumn));
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(
                Constants.KEY_PREF_GRAPHS_ACTIVITY_VISIBLE_GRAPHS, columnsToStore).apply();
    }

    public static Set<Integer> getCombinedGraphValues(Context context) {
        Set<String> defaultVisibleGraphs = new HashSet<>();
        defaultVisibleGraphs.add("0");
        defaultVisibleGraphs.add("1");
        defaultVisibleGraphs.add("2");
        Set<String> visibleColumns = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
                Constants.KEY_PREF_COMBINED_GRAPH_VALUES, defaultVisibleGraphs);
        Set<Integer> result = new HashSet<>();
        for (String visibleColumn: visibleColumns) {
            result.add(Integer.valueOf(visibleColumn));
        }
        return result;
    }

    public static void setCombinedGraphValues(Context context, Set<Integer> visibleGraphs) {
        Set<String> columnsToStore = new HashSet<>();
        for (Integer visibleColumn: visibleGraphs) {
            columnsToStore.add(String.valueOf(visibleColumn));
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(
                Constants.KEY_PREF_COMBINED_GRAPH_VALUES, columnsToStore).apply();
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

    public static int getGraphGridColor(Context context) {
        String theme = getTheme(context);
        if (null == theme) {
            return Color.parseColor("#333333");
        } else switch (theme) {
            case "dark":
                return Color.WHITE;
            case "light":
                return Color.parseColor("#333333");
            default:
                return Color.WHITE;
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
                Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD, "60");
    }

    public static String getLocationUpdatePeriod(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_PERIOD, "60");
    }

    public static void setCurrentLocationId(Context context, Location currentLocation) {
        Long currentLocationId = null;
        if (currentLocation != null) {
            currentLocationId = currentLocation.getId();
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(
                Constants.CURRENT_LOCATION_ID, currentLocationId).apply();
    }

    public static long getCurrentLocationId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(
                Constants.CURRENT_LOCATION_ID, 0);
    }
}
