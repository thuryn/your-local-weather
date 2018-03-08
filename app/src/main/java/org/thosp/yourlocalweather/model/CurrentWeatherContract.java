package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class CurrentWeatherContract {

    private CurrentWeatherContract() {}

    protected static final String SQL_CREATE_TABLE_CURRENT_WEATHER =
            "CREATE TABLE " + CurrentWeatherContract.CurrentWeather.TABLE_NAME + " (" +
                    CurrentWeatherContract.CurrentWeather._ID + " INTEGER PRIMARY KEY," +
                    CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID + " integer," +
                    CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LAST_UPDATED_IN_MS + " integer," +
                    CurrentWeatherContract.CurrentWeather.COLUMN_NAME_WEATHER + " blob)";

    protected static final String SQL_DELETE_TABLE_CURRENT_WEATHER =
            "DROP TABLE IF EXISTS " + CurrentWeatherContract.CurrentWeather.TABLE_NAME;

    public static class CurrentWeather implements BaseColumns {
        public static final String TABLE_NAME = "current_weather";
        public static final String COLUMN_NAME_LOCATION_ID = "location_id";
        public static final String COLUMN_NAME_WEATHER = "weather";
        public static final String COLUMN_NAME_LAST_UPDATED_IN_MS = "last_updated_in_ms";
    }
}
