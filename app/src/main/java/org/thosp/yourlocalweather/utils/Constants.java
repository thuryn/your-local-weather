package org.thosp.yourlocalweather.utils;

public class Constants {

    /**
     * SharedPreference names
     */
    public static final String APP_SETTINGS_NAME = "config";
    public static final String PREF_WEATHER_NAME = "weather_pref";
    public static final String PREF_FORECAST_NAME = "weather_forecast";
    public static final String APP_INITIAL_GUIDE_VERSION = "initial_guide_version";

    /**
     * Preferences constants
     */
    public static final String APP_SETTINGS_LATITUDE = "latitude";
    public static final String APP_SETTINGS_LONGITUDE = "longitude";
    public static final String APP_SETTINGS_LOCATION_ACCURACY = "location_accuracy";
    public static final String APP_SETTINGS_ADDRESS_FOUND = "address_found";
    public static final String APP_SETTINGS_CITY = "city";
    public static final String APP_SETTINGS_COUNTRY_CODE = "country_code";
    public static final String APP_SETTINGS_GEO_COUNTRY_NAME = "geo_country_name";
    public static final String APP_SETTINGS_GEO_DISTRICT_OF_CITY = "geo_district_name";
    public static final String APP_SETTINGS_GEO_DISTRICT_OF_COUNTRY = "geo_district_country";
    public static final String APP_SETTINGS_GEO_CITY = "geo_city_name";
    public static final String APP_SETTINGS_UPDATE_SOURCE = "update_source";
    public static final String APP_SETTINGS_LOCATION_CACHE_LASTING_HOURS = "location.cache.lasting";
    public static final String APP_SETTINGS_LOCATION_CACHE_ENABLED = "location.cache.enabled";
    public static final String APP_SETTINGS_VOICE_BT_PERMISSION_PASSED = "voice.setting.permission.passed";
    public static final String LAST_UPDATE_TIME_IN_MS = "last_update";
    public static final String LAST_FORECAST_UPDATE_TIME_IN_MS = "last_forecast_update";
    public static final String LAST_LOCATION_UPDATE_TIME_IN_MS = "last_location_update";
    public static final String LAST_WEATHER_UPDATE_TIME_IN_MS = "last_weather_update";
    public static final String LAST_NOTIFICATION_TIME_IN_MS = "last_weather_update";
    public static final String LAST_SENSOR_SERVICES_CHECK_TIME_IN_MS = "last_weather_update";
    public static final String CURRENT_LOCATION_ID = "current_location_id";
    public static final String KEY_PREF_OPEN_WEATHER_MAP_API_KEY = "open_weather_map_api_key";
    public static final String KEY_PREF_WEATHER_FORECAST_FEATURES = "weather_forecast_features_pref_key";
    public static final String KEY_PREF_WEATHER_LICENSE_KEY = "weather_forecast_license_key_pref_key";
    public static final String KEY_PREF_WEATHER_INITIAL_TOKEN = "weather_forecast_initial token_pref_key";

    public static final String KEY_PREF_TIME_STYLE = "time_style_pref_key";
    public static final String KEY_PREF_DATE_STYLE = "date_style_pref_key";
    public static final String KEY_PREF_IS_NOTIFICATION_ENABLED = "notification_pref_key";
    public static final String KEY_PREF_TEMPERATURE_UNITS = "temperature_units_pref_key";
    public static final String KEY_PREF_TEMPERATURE_TYPE = "temperature_type_pref_key";
    public static final String KEY_PREF_WIND_UNITS = "wind_units_pref_key";
    public static final String KEY_PREF_WIND_DIRECTION = "wind_direction_pref_key";
    public static final String KEY_PREF_RAIN_SNOW_UNITS = "rain_snow_units_pref_key";
    public static final String KEY_PREF_PRESSURE_UNITS = "pressure_units_pref_key";
    public static final String KEY_PREF_HIDE_DESCRIPTION = "hide_desc_pref_key";
    public static final String KEY_PREF_INTERVAL_NOTIFICATION = "notification_interval_pref_key";
    public static final String KEY_PREF_NOTIFICATION_PRESENCE = "notification_presence_pref_key";
    public static final String KEY_PREF_NOTIFICATION_STATUS_ICON = "notification_status_icon_pref_key";
    public static final String KEY_PREF_NOTIFICATION_VISUAL_STYLE = "notification_visual_style_pref_key";
    public static final String KEY_PREF_VIBRATE = "notification_vibrate_pref_key";
    public static final String KEY_PREF_WIDGET_SHOW_LABELS = "widget_show_labels_pref_key";
    public static final String KEY_PREF_WIDGET_TEXT_COLOR = "widget_text_color_pref_key";
    public static final String KEY_PREF_WIDGET_THEME = "widget_theme_pref_key";
    public static final String KEY_PREF_LOCATION_UPDATE_PERIOD = "location_update_period_pref_key";
    public static final String KEY_PREF_LOCATION_UPDATE_NIGHT = "location_update_period_night_pref_key";
    public static final String KEY_PREF_LOCATION_AUTO_UPDATE_PERIOD = "location_auto_update_period_pref_key";
    public static final String KEY_PREF_LOCATION_AUTO_UPDATE_NIGHT = "location_auto_update_period_night_pref_key";
    public static final String PREF_LANGUAGE = "language_pref_key";
    public static final String PREF_OS_LANGUAGE = "os_language_pref_key";
    public static final String KEY_PREF_LOCATION_GEOCODER_SOURCE = "location_geocoder_source";
    public static final String KEY_PREF_WEATHER_ICON_SET = "weather_icon_set_pref_key";
    public static final String KEY_PREF_LOCATION_UPDATE_STRATEGY = "location_update_strategy";
    public static final String KEY_PREF_LOCATION_GPS_ENABLED = "location_gps_enabled";
    public static final String KEY_PREF_UPDATE_DETAIL = "widget_update_details_pref_key";
    public static final String PREF_THEME = "theme_pref_key";
    public static final String KEY_DEBUG_URI_SCHEME = "debug.log.scheme";
    public static final String KEY_DEBUG_URI_AUTHORITY = "debug.log.authority";
    public static final String KEY_DEBUG_URI_PATH = "debug.log.path";
    public static final String KEY_DEBUG_FILE = "debug.log.file";
    public static final String KEY_DEBUG_TO_FILE = "debug.to.file";
    public static final String KEY_DEBUG_FILE_LASTING_HOURS = "debug.file.lasting.hours";
    public static final String KEY_WAKE_UP_STRATEGY = "wake.up.strategy";
    public static final String KEY_PREF_FORECAST_TYPE = "forecast_type_pref_key";
    public static final String KEY_PREF_FORECAST_ACTIVITY_COLUMNS = "forecast_activity_columns_pref_key";
    public static final String KEY_PREF_GRAPHS_ACTIVITY_VISIBLE_GRAPHS = "graphs_activity_visible_graphs";
    public static final String KEY_PREF_COMBINED_GRAPH_VALUES = "graphs_activity_combined_values";
    public static final String KEY_PREF_WIDGET_GRAPH_NATIVE_SCALE = "widget_graphs_native_scale";
    public static final String KEY_PREF_WIDGET_SHOW_CONTROLS = "widget_show_controls";
    public static final String CONNECTED_BT_DEVICES = "connected_bt_devices";

    /**
     * About preference screen constants
     */
    public static final String KEY_PREF_ABOUT_VERSION = "about_version_pref_key";
    public static final String KEY_PREF_ABOUT_F_DROID = "about_f_droid_pref_key";
    public static final String KEY_PREF_ABOUT_GOOGLE_PLAY = "about_google_play_pref_key";
    public static final String KEY_PREF_ABOUT_OPEN_SOURCE_LICENSES =
            "about_open_source_licenses_pref_key";

    public static final String WEATHER_DATA_WEATHER_ID = "weatherId";
    public static final String WEATHER_DATA_TEMPERATURE = "temperature";
    public static final String WEATHER_DATA_DESCRIPTION = "description";
    public static final String WEATHER_DATA_PRESSURE = "pressure";
    public static final String WEATHER_DATA_HUMIDITY = "humidity";
    public static final String WEATHER_DATA_WIND_SPEED = "wind_speed";
    public static final String WEATHER_DATA_CLOUDS = "clouds";
    public static final String WEATHER_DATA_ICON = "icon";
    public static final String WEATHER_DATA_SUNRISE = "sunrise";
    public static final String WEATHER_DATA_SUNSET = "sunset";

    /**
     * Widget action constants
     */
    public static final String ACTION_FORCED_APPWIDGET_UPDATE =
            "org.thosp.yourlocalweather.action.FORCED_APPWIDGET_UPDATE";
    public static final String ACTION_APPWIDGET_CHANGE_GRAPH_SCALE =
            "org.thosp.yourlocalweather.action.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE";
    public static final String ACTION_APPWIDGET_THEME_CHANGED =
            "org.thosp.yourlocalweather.action.APPWIDGET_THEME_CHANGED";
    public static final String ACTION_APPWIDGET_UPDATE_PERIOD_CHANGED =
            "org.thosp.yourlocalweather.action.APPWIDGET_UPDATE_PERIOD_CHANGED";
    public static final String ACTION_APPWIDGET_CHANGE_LOCATION =
            "org.thosp.yourlocalweather.action.ACTION_APPWIDGET_CHANGE_LOCATION";
    public static final String ACTION_APPWIDGET_SETTINGS_OPENED =
            "org.thosp.yourlocalweather.action.ACTION_APPWIDGET_SETTINGS_OPENED";
    public static final String ACTION_APPWIDGET_SETTINGS_SHOW_CONTROLS =
            "org.thosp.yourlocalweather.action.ACTION_APPWIDGET_SETTINGS_SHOW_CONTROLS";
    public static final String ACTION_APPWIDGET_CHANGE_SETTINGS =
            "org.thosp.yourlocalweather.action.ACTION_APPWIDGET_CHANGE_SETTINGS";
    public static final String ACTION_APPWIDGET_START_ACTIVITY =
            "org.thosp.yourlocalweather.action.ACTION_APPWIDGET_START_ACTIVITY";
    /**
     * URIs constants
     */
    public static final String SOURCE_CODE_URI = "https://github.com/thuryn/your-local-weather";
    public static final String GOOGLE_PLAY_APP_URI = "market://details?id=%s";
    public static final String GOOGLE_PLAY_WEB_URI =
            "https://play.google.com/store/apps/details?id=%s";
    public static final String F_DROID_WEB_URI = "https://f-droid.org/repository/browse/?fdid=%s";
    public static final String WEATHER_ENDPOINT = "https://api.open-meteo.com/v1/forecast?latitude={0}&longitude={1}&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m&hourly=temperature_2m,relative_humidity_2m,rain,snowfall,weather_code,pressure_msl,cloud_cover,wind_speed_10m,wind_direction_10m&daily=sunrise,sunset&timezone=auto&forecast_days=16";

    public static final int PARSE_RESULT_SUCCESS = 0;
    public static final int TASK_RESULT_ERROR = -1;
    public static final int PARSE_RESULT_ERROR = -2;
    public static final int TOO_EARLY_UPDATE_ERROR = -3;
}
