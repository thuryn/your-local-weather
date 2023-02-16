package org.thosp.yourlocalweather.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.service.MozillaLocationService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AppPreference {

    private static AppPreference instance;

    private AppPreference() {
    }

    public synchronized static AppPreference getInstance() {
        if (instance == null) {
            instance = new AppPreference();
        }
        return instance;
    }

    private Set<Integer> forecastActivityColumns;
    private String language;
    private Boolean vibrateEnabled;
    private String locationAutoUpdatePeriod;
    private String locationUpdatePeriod;
    private Boolean notificationEnabled;
    private Boolean locationCacheEnabled;
    private Boolean voiceBtPermissionPassed;

    public boolean getVoiceBtPermissionPassed(Context context) {
        if (voiceBtPermissionPassed != null) {
            return voiceBtPermissionPassed;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.APP_SETTINGS_VOICE_BT_PERMISSION_PASSED, false);
    }

    public void setVoiceBtPermissionPassed(Context context, boolean newValue) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
                Constants.APP_SETTINGS_VOICE_BT_PERMISSION_PASSED, newValue).apply();
    }

    public boolean getLocationCacheEnabled(Context context) {
        if (locationCacheEnabled != null) {
            return locationCacheEnabled;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, false);
    }

    public void clearLocationCacheEnabled() {
        locationCacheEnabled = null;
    }

    public String getLanguage(Context context) {
        if (language != null) {
            return language;
        }
        language = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage());
        if ("default".equals(language)) {
            language = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_OS_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage());
        }
        return language;
    }

    public void clearLanguage() {
        language = null;
    }

    public static String getTimeStylePreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TIME_STYLE, "system");
    }

    public static String getLocalizedTime(Context context, Date inputTime, String timeStylePreference, Locale locale) {
        if ("system".equals(timeStylePreference)) {
            return DateFormat.getTimeFormat(context).format(inputTime);
        } else if ("12h".equals(timeStylePreference)) {
            SimpleDateFormat sdf_12 = new SimpleDateFormat("hh:mm aaaa", locale);
            return sdf_12.format(inputTime);
        } else {
            SimpleDateFormat sdf_24 = new SimpleDateFormat("HH:mm", locale);
            return sdf_24.format(inputTime);
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

    public static String getTemperatureUnitFromPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
    }

    public static String getPressureUnitFromPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
    }

    public static String getLocalizedDateTime(Context context, Date inputTime, boolean showYear, String timeStylePreference, Locale locale) {
        String dateStylePreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_DATE_STYLE, "system");
        switch (dateStylePreferences) {
            case "date_style_dots": return getSimpleDateFormatForDate("dd.MM.", "dd.MM.yy", showYear, locale).format(inputTime) +
                                        " " + getLocalizedTime(context, inputTime, timeStylePreference, locale);
            case "date_style_slash_month_first": return getSimpleDateFormatForDate("MM/dd", "yy/MM/dd", showYear, locale).format(inputTime) +
                                                    " " + getLocalizedTime(context, inputTime, timeStylePreference, locale);
            case "date_style_slash_day_first": return getSimpleDateFormatForDate("dd/MM", "dd/MM/yy", showYear, locale).format(inputTime) +
                                                    " " + getLocalizedTime(context, inputTime, timeStylePreference, locale);
            case "system":
            default: return DateFormat.getDateFormat(context).format(inputTime) + " " +
                    getLocalizedTime(context, inputTime, timeStylePreference, locale);
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

    private static short mpsToBft(double speed) {
        if (speed < 0.51d) {
            return 0;
        } else if (speed < 2.06d) {
            return 1;
        } else if (speed < 3.60d) {
            return 2;
        } else if (speed < 5.66d) {
            return 3;
        } else if (speed < 8.23d) {
            return 4;
        } else if (speed < 11.32d) {
            return 5;
        } else if (speed < 14.40d) {
            return 6;
        } else if (speed < 17.49d) {
            return 7;
        } else if (speed < 21.09d) {
            return 8;
        } else if (speed < 24.69d) {
            return 9;
        } else if (speed < 28.81d) {
            return 10;
        } else if (speed < 32.92d) {
            return 11;
        } else {
            return 12;
        }
    }

    public static WindWithUnit getWindWithUnit(Context context, float value, float direction, String windUnitFromPreferences, Locale locale) {
        if (windUnitFromPreferences.contains("km_per_hour") ) {
            double kmhValue = 3.6d * value;
            return new WindWithUnit(context, kmhValue, context.getString(R.string.wind_speed_kilometers), direction, locale);
        } else if (windUnitFromPreferences.contains("miles_per_hour") ) {
            double mhValue = 2.2369d * value;
            return new WindWithUnit(context, mhValue, context.getString(R.string.wind_speed_miles), direction, locale);
        } else if (windUnitFromPreferences.contains("knots") ) {
            double knotsValue = 1.9438445d * value;
            return new WindWithUnit(context, knotsValue, context.getString(R.string.wind_speed_knots), direction, locale);
        } else if (windUnitFromPreferences.contains("beaufort") ) {
            double beaufortValue = mpsToBft(value);
            return new WindWithUnit(context, beaufortValue, context.getString(R.string.wind_speed_beaufort), direction, locale);
        } else {
            return new WindWithUnit(context, value, context.getString(R.string.wind_speed_meters), direction, locale);
        }
    }

    public static String getRainSnowUnitFromPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_RAIN_SNOW_UNITS, "mm");
    }

    public static double getRainOrSnow(String rainSnowUnitFromPreferences, double value) {
        if (rainSnowUnitFromPreferences.contains("inches") ) {
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

    public static String getFormatedRainOrSnow(String rainSnowUnitFromPreferences, double value, Locale locale) {
        String format;
        if (rainSnowUnitFromPreferences.contains("inches") ) {
            format = "%.3f";
        } else {
            format = "%.1f";
        }
        return String.format(locale, format, getRainOrSnow(rainSnowUnitFromPreferences, value));
    }

    public static int getRainOrSnowUnit(String rainSnowUnitFromPreferences) {
        if (rainSnowUnitFromPreferences.contains("inches") ) {
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

    private static String getWindFormat(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
        if (unitsFromPreferences.contains("beaufort") ) {
            return "%.0f";
        } else {
            return "%.1f";
        }
    }

    public static String getWindInString(Context context, String windUnitFromPreferences, double stringValue, Locale locale) {
        return String.format(locale, getWindFormat(context), getWind(windUnitFromPreferences, stringValue));
    }

    public static String getWindUnitFromPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_UNITS, "m_per_second");
    }

    public static double getWind(String windUnitFromPreferences, double windSpeed) {
        if (windUnitFromPreferences.contains("km_per_hour") ) {
            return 3.6d * windSpeed;
        } else if (windUnitFromPreferences.contains("miles_per_hour") ) {
            return 2.2369d * windSpeed;
        } else if (windUnitFromPreferences.contains("knots") ) {
            return 1.9438445d * windSpeed;
        } else if (windUnitFromPreferences.contains("beaufort") ) {
            return mpsToBft(windSpeed);
        } else {
            return windSpeed;
        }
    }

    public static String getWindUnit(Context context, String windUnitFromPreferences) {
        if (windUnitFromPreferences.contains("km_per_hour") ) {
            return context.getString(R.string.wind_speed_kilometers);
        } else if (windUnitFromPreferences.contains("miles_per_hour") ) {
            return context.getString(R.string.wind_speed_miles);
        } else if (windUnitFromPreferences.contains("knots") ) {
            return context.getString(R.string.wind_speed_knots);
        } else if (windUnitFromPreferences.contains("beaufort") ) {
            return context.getString(R.string.wind_speed_beaufort);
        } else {
            return context.getString(R.string.wind_speed_meters);
        }
    }

    public static String getPressureUnit(Context context, String pressureUnitFromPreferences) {
        switch (pressureUnitFromPreferences) {
            case "mmhg": return context.getString(R.string.pressure_measurement_mmhg);
            case "inhg": return context.getString(R.string.pressure_measurement_inhg);
            case "mbar": return context.getString(R.string.pressure_measurement_mbar);
            case "psi": return context.getString(R.string.pressure_measurement_psi);
            case "kpa": return context.getString(R.string.pressure_measurement_kpa);
            default: return context.getString(R.string.pressure_measurement);
        }
    }

    public static int getPressureDecimalPlaces(String pressureUnitFromPreferences) {
        switch (pressureUnitFromPreferences) {
            case "mmhg": return 2;
            case "inhg": return 2;
            case "mbar": return 0;
            case "psi" : return 2;
            case "kpa" : return 1;
            default: return 0;
        }
    }

    public static String getTemeratureTypeFromPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
    }

    public static String getPressureInString(Context context, double stringValue, String pressureUnitFromPreferences, Locale locale) {
        return String.format(locale, "%." + getPressureDecimalPlaces(pressureUnitFromPreferences)
                + "f", getPressureWithUnit(context, stringValue, pressureUnitFromPreferences, locale).getPressure());
    }

    public static PressureWithUnit getPressureWithUnit(Context context, double value, String unitsFromPreferences, Locale locale) {
        switch (unitsFromPreferences) {
            case "mmhg": return new PressureWithUnit(value * 0.75f,
                                                 context.getString(R.string.pressure_measurement_mmhg), locale);
            case "inhg": return new PressureWithUnit(value * 0.029529983071445f,
                                                 context.getString(R.string.pressure_measurement_inhg), locale);
            case "mbar": return new PressureWithUnit(value,
                                                 context.getString(R.string.pressure_measurement_mbar), locale);
            case "psi": return new PressureWithUnit(value * 0.0145037738f,
                                                 context.getString(R.string.pressure_measurement_psi), locale);
            case "kpa": return new PressureWithUnit(value / 10f,
                    context.getString(R.string.pressure_measurement_kpa), locale);
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

    public static void setRegularOnlyInterval(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(Constants.KEY_PREF_INTERVAL_NOTIFICATION, "regular_only").apply();
    }

    public static String getNotificationPresence(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.KEY_PREF_NOTIFICATION_PRESENCE, "when_updated");
    }
    
    public static void setNotificationPresence(Context context, String notificationPresence) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(Constants.KEY_PREF_NOTIFICATION_PRESENCE, notificationPresence).apply();
    }

    public static String getNotificationStatusIconStyle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.KEY_PREF_NOTIFICATION_STATUS_ICON, "icon_sun");
    }

    public static String getNotificationVisualStyle(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Constants.KEY_PREF_NOTIFICATION_VISUAL_STYLE, "build_in");
    }

    public boolean isVibrateEnabled(Context context) {
        if (vibrateEnabled != null) {
            return vibrateEnabled;
        }
        vibrateEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_VIBRATE,
                false);
        return vibrateEnabled;
    }

    public void clearVibrateEnabled() {
        vibrateEnabled = null;
    }

    public boolean isNotificationEnabled(Context context) {
        if (notificationEnabled != null) {
            return notificationEnabled;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_IS_NOTIFICATION_ENABLED, false);
    }

    public void setNotificationEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
                Constants.KEY_PREF_IS_NOTIFICATION_ENABLED, enabled).apply();
        this.notificationEnabled = enabled;
    }

    public static void setNotificationIconStyle(Context context, String iconStyle) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(
                Constants.KEY_PREF_NOTIFICATION_STATUS_ICON, iconStyle).apply();
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

    public static String getWidgetTheme(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIDGET_THEME, "dark");
    }

    public Set<Integer> getForecastActivityColumns(Context context) {
        if (forecastActivityColumns != null) {
            return forecastActivityColumns;
        }
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
        forecastActivityColumns = result;
        return result;
    }

    public void setForecastActivityColumns(Context context, Set<Integer> visibleColumns) {
        Set<String> columnsToStore = new HashSet<>();
        for (Integer visibleColumn: visibleColumns) {
            columnsToStore.add(String.valueOf(visibleColumn));
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(
                Constants.KEY_PREF_FORECAST_ACTIVITY_COLUMNS, columnsToStore).apply();
        forecastActivityColumns = visibleColumns;
    }

    public static int getForecastType(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                Constants.KEY_PREF_FORECAST_TYPE, 1);
    }

    public static void setForecastType(Context context, int forecastType) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(
                Constants.KEY_PREF_FORECAST_TYPE, forecastType).apply();
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
        defaultVisibleGraphs.add("2");
        Set<String> combinedGraphValues = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(
                Constants.KEY_PREF_COMBINED_GRAPH_VALUES, defaultVisibleGraphs);
        Set<Integer> result = new HashSet<>();
        for (String visibleColumn: combinedGraphValues) {
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

    public static int getWidgetTextColor(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPreferences.contains(Constants.KEY_PREF_WIDGET_TEXT_COLOR)) {
            return getTextColor(context);
        }
        return sharedPreferences.getInt(Constants.KEY_PREF_WIDGET_TEXT_COLOR, 0);
    }

    public static int getTextColor(Context context) {
        String theme = getWidgetTheme(context);
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

    public static GraphGridColors getWidgetGraphGridColor(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPreferences.contains(Constants.KEY_PREF_WIDGET_TEXT_COLOR)) {
            String theme = getWidgetTheme(context);
            if (null == theme) {
                return new GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
            } else switch (theme) {
                case "dark":
                    return new GraphGridColors(Color.WHITE, Color.GRAY);
                case "light":
                    return new GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
                default:
                    return new GraphGridColors(Color.WHITE, Color.GRAY);
            }
        } else {
            return new GraphGridColors(sharedPreferences.getInt(Constants.KEY_PREF_WIDGET_TEXT_COLOR, 0), Color.GRAY);
        }
    }

    public static int getWidgetBackgroundColor(Context context) {
        String theme = getWidgetTheme(context);
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
        String theme = getWidgetTheme(context);
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

    public String getLocationAutoUpdatePeriod(Context context) {
        if (locationAutoUpdatePeriod != null) {
            return locationAutoUpdatePeriod;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD, "60");
    }

    public void clearLocationAutoUpdatePeriod() {
        locationAutoUpdatePeriod = null;
    }

    public static boolean getLocationAutoUpdateNight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_LOCATION_AUTO_UPDATE_NIGHT, false);
    }

    public String getLocationUpdatePeriod(Context context) {
        if (locationUpdatePeriod != null) {
            return locationUpdatePeriod;
        }
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_PERIOD, "60");
    }

    public void clearLocationUpdatePeriod() {
        locationUpdatePeriod = null;
    }

    public static boolean getLocationUpdateNight(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_LOCATION_UPDATE_NIGHT, false);
    }

    public static void setCurrentLocationId(Context context, Location currentLocation) {
        Long currentLocationId = null;
        if (currentLocation != null) {
            currentLocationId = currentLocation.getId();
        }
        if (currentLocationId == null) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(
                Constants.CURRENT_LOCATION_ID, currentLocationId).apply();
    }

    public static long getCurrentLocationId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(
                Constants.CURRENT_LOCATION_ID, 0);
    }

    public static class GraphGridColors {
        private int mainGridColor;
        private int secondaryGridColor;

        public GraphGridColors(int mainGridColor, int secondaryGridColor) {
            this.mainGridColor = mainGridColor;
            this.secondaryGridColor = secondaryGridColor;
        }

        public int getMainGridColor() {
            return mainGridColor;
        }

        public int getSecondaryGridColor() {
            return secondaryGridColor;
        }
    }
}
