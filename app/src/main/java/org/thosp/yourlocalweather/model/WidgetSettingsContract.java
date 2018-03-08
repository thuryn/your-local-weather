package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class WidgetSettingsContract {

    private WidgetSettingsContract() {}

    protected static final String SQL_CREATE_TABLE_WEATHER_SETTINGS =
            "CREATE TABLE " + WidgetSettingsContract.WidgetSettings.TABLE_NAME + " (" +
                    WidgetSettingsContract.WidgetSettings._ID + " INTEGER PRIMARY KEY," +
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + " integer," +
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + " text," +
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG + " integer," +
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_STRING + " text," +
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_DOUBLE + " real," +
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_BLOB + " blob)";

    protected static final String SQL_DELETE_TABLE_WEATHER_SETTINGS =
            "DROP TABLE IF EXISTS " + WidgetSettingsContract.WidgetSettings.TABLE_NAME;

    public static class WidgetSettings implements BaseColumns {
        public static final String TABLE_NAME = "current_weather";
        public static final String COLUMN_NAME_WIDGET_ID = "widget_id";
        public static final String COLUMN_NAME_PARAM_NAME = "param_name";
        public static final String COLUMN_NAME_PARAM_LONG = "param_long";
        public static final String COLUMN_NAME_PARAM_STRING = "param_string";
        public static final String COLUMN_NAME_PARAM_DOUBLE = "param_double";
        public static final String COLUMN_NAME_PARAM_BLOB = "param_blob";
    }
}
