package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.thosp.yourlocalweather.model.VoiceSettingParameterContract.SQL_CREATE_TABLE;
import static org.thosp.yourlocalweather.model.VoiceSettingParameterContract.SQL_DELETE_TABLE;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class VoiceSettingParametersDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "VoiceSettingParametersDbHelper";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "VoiceSettingParameters.db";
    private static VoiceSettingParametersDbHelper instance;
    private Context context;

    public static VoiceSettingParametersDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new VoiceSettingParametersDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private VoiceSettingParametersDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public List<Long> getAllSettingIds() {

        List<Long> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                long voiceSettingId = cursor.getInt(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID));
                if ((voiceSettingId == 0) || result.contains(voiceSettingId)) {
                    continue;
                }
                result.add(voiceSettingId);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public void deleteAllSettings(Long voiceSettingId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            String selection = VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + " = ?";
            String[] selectionArgs = {voiceSettingId.toString()};
            db.delete(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME, selection, selectionArgs);
        } finally {
        }
    }

    public void deleteRecordFromTable(Integer id) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            String selection = VoiceSettingParameterContract.VoiceSettingParameters._ID + " = ?";
            String[] selectionArgs = {id.toString()};
            db.delete(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME, selection, selectionArgs);
        } finally {
        }
    }

    public void saveStringParam(Long voiceSettingId, int paramType, String value) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE, value);
            if (!dbRecordExists(voiceSettingId, paramType)) {
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID, paramType);
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID, voiceSettingId);
                db.insert(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME, null, values);
            } else {
                db.updateWithOnConflict(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                        values,
                        VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                                " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        } catch (Exception e) {
            appendLog(context, TAG, "Error:", e);
        } finally {
        }
    }

    public void saveGeneralStringParam(int paramType, String value) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE, value);
            if (!dbRecordExists(paramType)) {
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID, paramType);
                db.insert(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME, null, values);
            } else {
                db.updateWithOnConflict(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                        values,
                        VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        } finally {
        }
    }

    public void saveBooleanParam(Long voiceSettingId, int paramType, Boolean value) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            Long valueToStore;
            if (value == null) {
                valueToStore = null;
            } else if (value) {
                valueToStore = 1l;
            } else {
                valueToStore = 0l;
            }
            values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE, valueToStore);
            if (!dbRecordExists(voiceSettingId, paramType)) {
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID, paramType);
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID, voiceSettingId);
                db.insert(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME, null, values);
            } else {
                db.updateWithOnConflict(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                        values,
                        VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                                " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        } finally {
        }
    }

    public void saveLongParam(Long voiceSettingId, int paramType, long value) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE, value);
            if (!dbRecordExists(voiceSettingId, paramType)) {
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID, paramType);
                values.put(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID, voiceSettingId);
                db.insert(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME, null, values);
            } else {
                db.updateWithOnConflict(VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                        values,
                        VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                        " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        } finally {
        }
    }

    public Long getLongParam(Long voiceSettingId, int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                    " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE));
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Map<Long, Long> getLongParam(int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        Map<Long, Long> result = new HashMap<>();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID,
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                result.put(cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public String getStringParam(Long voiceSettingId, int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                            " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE));
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Map<Long, String> getStringParam(int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID,
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE
        };

        Map<Long, String> result = new HashMap<>();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                result.put(cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public String getGeneralStringParam(int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_STRING_VALUE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public Boolean getBooleanParam(Long voiceSettingId, int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                            " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                Long longValue = cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE));
                if (longValue == null) {
                    return null;
                } else if (longValue > 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Map<Long, Boolean> getBooleanParam(int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID,
                VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE
        };

        Map<Long, Boolean> result = new HashMap<>();

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            while (cursor.moveToNext()) {
                long voiceSettingId = cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID));
                Long longValue = cursor.getLong(cursor.getColumnIndexOrThrow(VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_LONG_VALUE));
                if (longValue == null) {
                    result.put(voiceSettingId, null);
                } else if (longValue > 0) {
                    result.put(voiceSettingId, true);
                } else {
                    result.put(voiceSettingId, false);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private boolean dbRecordExists(Long voiceSettingId, int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters._ID
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_VOICE_SETTING_ID + "=" + voiceSettingId +
                            " AND " + VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            return cursor.moveToNext();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean dbRecordExists(int paramType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                VoiceSettingParameterContract.VoiceSettingParameters._ID
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    VoiceSettingParameterContract.VoiceSettingParameters.TABLE_NAME,
                    projection,
                    VoiceSettingParameterContract.VoiceSettingParameters.COLUMN_NAME_PARAM_TYPE_ID + "=" + paramType,
                    null,
                    null,
                    null,
                    null
            );

            return cursor.moveToNext();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
