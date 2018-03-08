package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class WeatherForecastContract {

    private WeatherForecastContract() {}

    protected static final String SQL_CREATE_TABLE_WEATHER_FORECAST =
            "CREATE TABLE " + WeatherForecastContract.WeatherForecast.TABLE_NAME + " (" +
                    WeatherForecastContract.WeatherForecast._ID + " INTEGER PRIMARY KEY," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + " integer," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS + " integer," +
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST + " blob)";

    protected static final String SQL_DELETE_TABLE_WEATHER_FORECAST =
            "DROP TABLE IF EXISTS " + WeatherForecastContract.WeatherForecast.TABLE_NAME;

    public static class WeatherForecast implements BaseColumns {
        public static final String TABLE_NAME = "weather_forecast";
        public static final String COLUMN_NAME_LOCATION_ID = "location_id";
        public static final String COLUMN_NAME_WEATHER_FORECAST = "weather_forecast";
        public static final String COLUMN_NAME_LAST_UPDATED_IN_MS = "last_updated_in_ms";
    }
}
