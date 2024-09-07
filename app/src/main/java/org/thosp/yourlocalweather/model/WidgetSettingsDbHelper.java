package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static org.thosp.yourlocalweather.model.WidgetSettingsContract.SQL_CREATE_TABLE_WIDGET_SETTINGS;
import static org.thosp.yourlocalweather.model.WidgetSettingsContract.SQL_DELETE_TABLE_WIDGET_SETTINGS;

public class WidgetSettingsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
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
        db.execSQL(SQL_CREATE_TABLE_WIDGET_SETTINGS);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_WIDGET_SETTINGS);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void deleteRecordFromTable(Integer widgetId) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                String selection = WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + " = ?";
                String[] selectionArgs = {widgetId.toString()};
                db.delete(WidgetSettingsContract.WidgetSettings.TABLE_NAME, selection, selectionArgs);
            }
        }).start();
    }

    public void saveParamString(int widgetId, String paramName, String value) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();

                String oldValue = getParamString(widgetId, paramName);

                ContentValues values = new ContentValues();
                values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_STRING, value);
                if (oldValue == null) {
                    values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME, paramName);
                    values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID, widgetId);
                    db.insert(WidgetSettingsContract.WidgetSettings.TABLE_NAME, null, values);
                } else {
                    db.updateWithOnConflict(WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                            values,
                            WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId +
                                    " AND " + WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + "=\"" + paramName + "\"",
                            null,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        }).start();
    }

    public void saveParamBoolean(int widgetId, String paramName, Boolean value) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();

                Boolean oldValue = getParamBoolean(widgetId, paramName);

                ContentValues values = new ContentValues();
                Long valueToStore;
                if (value == null) {
                    valueToStore = null;
                } else if (value) {
                    valueToStore = 1l;
                } else {
                    valueToStore = 0l;
                }
                values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG, valueToStore);
                if (oldValue == null) {
                    values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME, paramName);
                    values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID, widgetId);
                    db.insert(WidgetSettingsContract.WidgetSettings.TABLE_NAME, null, values);
                } else {
                    db.updateWithOnConflict(WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                            values,
                            WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId +
                                    " AND " + WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + "=\"" + paramName + "\"",
                            null,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        }).start();
    }

    public void saveParamLong(int widgetId, String paramName, long value) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();

                Long oldValue = getParamLong(widgetId, paramName);

                ContentValues values = new ContentValues();
                values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG, value);
                if (oldValue == null) {
                    values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME, paramName);
                    values.put(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID, widgetId);
                    db.insert(WidgetSettingsContract.WidgetSettings.TABLE_NAME, null, values);
                } else {
                    db.updateWithOnConflict(WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                            values,
                            WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId +
                            " AND " + WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + "=\"" + paramName + "\"",
                            null,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        }).start();
    }

    public Long getParamLong(int widgetId, String paramName) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                    projection,
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId +
                    " AND " + WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + "=\"" + paramName + "\"",
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
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getParamString(int widgetId, String paramName) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_STRING
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                    projection,
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId +
                            " AND " + WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + "=\"" + paramName + "\"",
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_STRING));
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Boolean getParamBoolean(int widgetId, String paramName) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    WidgetSettingsContract.WidgetSettings.TABLE_NAME,
                    projection,
                    WidgetSettingsContract.WidgetSettings.COLUMN_NAME_WIDGET_ID + "=" + widgetId +
                            " AND " + WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_NAME + "=\"" + paramName + "\"",
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                Long longValue = cursor.getLong(cursor.getColumnIndexOrThrow(WidgetSettingsContract.WidgetSettings.COLUMN_NAME_PARAM_LONG));
                if (longValue == null) {
                    return null;
                } else return longValue > 0;
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
