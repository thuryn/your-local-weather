package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcel;

import static org.thosp.yourlocalweather.model.WidgetSettingsContract.SQL_CREATE_TABLE_WEATHER_SETTINGS;
import static org.thosp.yourlocalweather.model.WidgetSettingsContract.SQL_DELETE_TABLE_WEATHER_SETTINGS;

public class WidgetSettingsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WidgetSettings.db";
    private static WidgetSettingsDbHelper instance;

    public static WidgetSettingsDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new WidgetSettingsDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private WidgetSettingsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_WEATHER_SETTINGS);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_WEATHER_SETTINGS);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void deleteRecordFromTable(Integer widgetId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            String selection = WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + " = ?";
            String[] selectionArgs = {widgetId.toString()};
            db.delete(WidgetSettingsContract.WidgetSettings.TABLE_NAME, selection, selectionArgs);
        } finally {
        }
    }

    public void saveParamLong(int widgetId, String paramName, long value) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            Long oldValue = getParamLong(widgetId, paramName);

            ContentValues values = new ContentValues();
            values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG, value);
            if (oldValue == null) {
                values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME, paramName);
                values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID, widgetId);
                db.insert(WidgetSettingsContract.WidgetSettings.TABLE_NAME, null, values);
            } else {
                db.update(WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                        values,
                        WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId,
                        null);
            }
        } finally {
        }
    }

    public Long getParamLong(int widgetId, String paramName) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG
        };

        try {
            Cursor cursor = db.query(
                    WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                    projection,
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG));
            } else {
                return null;
            }
        } finally {
        }
    }
}
