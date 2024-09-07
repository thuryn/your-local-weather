package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class WeatherForecastContract {

    private WeatherForecastContract() {}

    static final String SQL_CREATE_TABLE_WEATHER_FORECAST =
            "CREATE TABLE " + WeatherForecastContract.WeatherForecast.TABLE_NAME + " (" +
                    WeatherForecastContract.WeatherForecast._ID + " INTEGER PRIMARY KEY," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + " integer," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS + " integer," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE + " integer," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS + " integer," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST + " blob)";

    static final String SQL_DELETE_TABLE_WEATHER_FORECAST =
            "DROP TABLE IF EXISTS " + WeatherForecastContract.WeatherForecast.TABLE_NAME;

    public static class WeatherForecast implements BaseColumns {
        public static final String TABLE_NAME = "weather_forecast";
        public static final String COLUMN_NAME_LOCATION_ID = "location_id";
        public static final String COLUMN_NAME_WEATHER_FORECAST = "weather_forecast";
        public static final String COLUMN_NAME_FORECAST_TYPE = "forecast_type";
        public static final String COLUMN_NAME_LAST_UPDATED_IN_MS = "last_updated_in_ms";
        public static final String COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS = "next_allowed_attempt_to_update_time_in_ms";
    }
}
