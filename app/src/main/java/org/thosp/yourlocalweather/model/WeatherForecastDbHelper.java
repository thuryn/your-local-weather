package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.os.Parcel;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.utils.Constants;

import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_CREATE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_DELETE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "WeatherForecastDbHelper";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "WeatherForecast.db";
    private static WeatherForecastDbHelper instance;
    private Context context;

    public synchronized static WeatherForecastDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new WeatherForecastDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private WeatherForecastDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_WEATHER_FORECAST);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_WEATHER_FORECAST);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void deleteRecordByLocation(Location location) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + " = ?";
        String[] selectionArgs = {location.getId().toString()};
        db.delete(WeatherForecastContract.WeatherForecast.TABLE_NAME, selection, selectionArgs);
    }

    public void deleteRecordFromTable(Integer recordId) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = WeatherForecastContract.WeatherForecast._ID + " = ?";
        String[] selectionArgs = {recordId.toString()};
        db.delete(WeatherForecastContract.WeatherForecast.TABLE_NAME, selection, selectionArgs);
    }

    public void saveWeatherForecast(long locationId, long weatherUpdateTime, CompleteWeatherForecast completeWeatherForecast) {
        SQLiteDatabase db = getWritableDatabase();

        WeatherForecastRecord oldWeatherForecast = getWeatherForecast(locationId);

        ContentValues values = new ContentValues();
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                   getCompleteWeatherForecastAsBytes(completeWeatherForecast));
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID, locationId);
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS, weatherUpdateTime);
        if (oldWeatherForecast == null) {
            db.insert(WeatherForecastContract.WeatherForecast.TABLE_NAME, null, values);
        } else {
            db.updateWithOnConflict(WeatherForecastContract.WeatherForecast.TABLE_NAME,
                    values,
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                    null,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public WeatherForecastRecord getWeatherForecast(long locationId) {

        checkVersionOfStoredForecastInDb();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                WeatherForecastContract.WeatherForecast.TABLE_NAME,
                projection,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                null,
                null,
                null,
                null
            );

            if (cursor.moveToNext()) {
                CompleteWeatherForecast completeWeatherForecast = getCompleteWeatherForecastFromBytes(
                        cursor.getBlob(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST)));
                if (completeWeatherForecast == null) {
                    return null;
                }
                return new WeatherForecastRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS)),
                        completeWeatherForecast);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static CompleteWeatherForecast getCompleteWeatherForecastFromBytes(byte[] addressBytes) {
        if ((addressBytes == null) || (addressBytes.length == 0)) {
            return null;
        }
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(addressBytes, 0, addressBytes.length);
        parcel.setDataPosition(0);
        CompleteWeatherForecast completeWeatherForecast = CompleteWeatherForecast.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return completeWeatherForecast;
    }

    public byte[] getCompleteWeatherForecastAsBytes(CompleteWeatherForecast completeWeatherForecast) {
        final Parcel parcel = Parcel.obtain();
        completeWeatherForecast.writeToParcel(parcel, 0);
        byte[] completeWeatherForecastBytes = parcel.marshall();
        parcel.recycle();
        return completeWeatherForecastBytes;
    }


    public class WeatherForecastRecord {
        long lastUpdatedTime;
        CompleteWeatherForecast completeWeatherForecast;

        public WeatherForecastRecord(long lastUpdatedTime, CompleteWeatherForecast completeWeatherForecast) {
            this.lastUpdatedTime = lastUpdatedTime;
            this.completeWeatherForecast = completeWeatherForecast;
        }

        public long getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public CompleteWeatherForecast getCompleteWeatherForecast() {
            return completeWeatherForecast;
        }
    }

    private void checkVersionOfStoredForecastInDb() {

        int initialGuideVersion = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Constants.APP_INITIAL_GUIDE_VERSION, 0);
        if (initialGuideVersion != 1) {
            return;
        }
        appendLog(context, TAG, "Old version of stored forecast, clearing forecast DB");
        SQLiteDatabase db = getWritableDatabase();
        onUpgrade(db, 0, 1);
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 2);
        preferences.apply();
    }
}
