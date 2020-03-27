package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Calendar;

import static org.thosp.yourlocalweather.model.LicenseKeysContract.SQL_CREATE_TABLE_LICENSES;
import static org.thosp.yourlocalweather.model.LicenseKeysContract.SQL_DELETE_TABLE_LICENSES;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LicenseKeysDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "LicenseKeysDbHelper";

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Licenses.db";
    private Context context;
    private static LicenseKeysDbHelper instance;

    public synchronized static LicenseKeysDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new LicenseKeysDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private LicenseKeysDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_LICENSES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_LICENSES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private void createLicenseKey(LicenseKey licenseKey) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LicenseKeysContract.LicenseKeys.COLUMN_NAME_INITIAL_LICENSE,
                licenseKey.getInitialLicense());
        values.put(LicenseKeysContract.LicenseKeys.COLUMN_NAME_REQUEST_URI,
                licenseKey.getRequestUri());
        values.put(LicenseKeysContract.LicenseKeys.COLUMN_NAME_TOKEN,
                licenseKey.getToken());

        long newLocationRowId = db.insert(LicenseKeysContract.LicenseKeys.TABLE_NAME, null, values);
        appendLog(context, TAG, "LicenseKey created: ", newLocationRowId);
    }

    public LicenseKey getLicenseKeyByLocationRequestId(String requestUri) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LicenseKeysContract.LicenseKeys._ID,
                LicenseKeysContract.LicenseKeys.COLUMN_NAME_INITIAL_LICENSE,
                LicenseKeysContract.LicenseKeys.COLUMN_NAME_TOKEN,
                LicenseKeysContract.LicenseKeys.COLUMN_NAME_LAST_CALL_TIME_IN_MS
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                LicenseKeysContract.LicenseKeys.TABLE_NAME,
                projection,
                LicenseKeysContract.LicenseKeys.COLUMN_NAME_REQUEST_URI + "='" + requestUri + "'",
                null,
                null,
                null,
                null
            );

            if (!cursor.moveToNext()) {
                return null;
            }

            Long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(LicenseKeysContract.LicenseKeys._ID));
            String initialLicense = cursor.getString(cursor.getColumnIndexOrThrow(LicenseKeysContract.LicenseKeys.COLUMN_NAME_INITIAL_LICENSE));
            String token = cursor.getString(cursor.getColumnIndexOrThrow(LicenseKeysContract.LicenseKeys.COLUMN_NAME_TOKEN));
            Long lastCallTimeInMs = cursor.getLong(cursor.getColumnIndexOrThrow(LicenseKeysContract.LicenseKeys.COLUMN_NAME_LAST_CALL_TIME_IN_MS));

            return new LicenseKey(
                    itemId,
                    requestUri,
                    initialLicense,
                    token,
                    lastCallTimeInMs);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void updateToken(String requestUri, String token) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LicenseKeysContract.LicenseKeys.COLUMN_NAME_TOKEN, token);
        values.put(LicenseKeysContract.LicenseKeys.COLUMN_NAME_LAST_CALL_TIME_IN_MS, System.currentTimeMillis());
        if (!dbRecordExists(requestUri)) {
            values.put(LicenseKeysContract.LicenseKeys.COLUMN_NAME_REQUEST_URI, requestUri);
            db.insert(LicenseKeysContract.LicenseKeys.TABLE_NAME, null, values);
        } else {
            db.updateWithOnConflict(
                    LicenseKeysContract.LicenseKeys.TABLE_NAME,
                    values,
                    LicenseKeysContract.LicenseKeys.COLUMN_NAME_REQUEST_URI + "='" + requestUri + "'",
                    null,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private boolean dbRecordExists(String requestUri) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LicenseKeysContract.LicenseKeys._ID
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    LicenseKeysContract.LicenseKeys.TABLE_NAME,
                    projection,
                    LicenseKeysContract.LicenseKeys.COLUMN_NAME_REQUEST_URI + "='" + requestUri + "'",
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
