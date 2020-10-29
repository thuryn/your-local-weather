package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcel;

import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_CREATE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_DELETE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "WeatherForecastDbHelper";

    public static final int DATABASE_VERSION = 2;
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

    public void saveWeatherForecast(long locationId, int forecastType, long weatherUpdateTime, CompleteWeatherForecast completeWeatherForecast) {
        SQLiteDatabase db = getWritableDatabase();

        WeatherForecastRecord oldWeatherForecast = getWeatherForecast(locationId, forecastType);

        ContentValues values = new ContentValues();
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                   getCompleteWeatherForecastAsBytes(completeWeatherForecast));
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID, locationId);
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS, weatherUpdateTime);
        values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE, forecastType);
        if (oldWeatherForecast == null) {
            db.insert(WeatherForecastContract.WeatherForecast.TABLE_NAME, null, values);
        } else {
            db.updateWithOnConflict(WeatherForecastContract.WeatherForecast.TABLE_NAME,
                    values,
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId +
                    " AND " + WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE + "=" + forecastType,
                    null,
                    SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public WeatherForecastRecord getWeatherForecast(long locationId) {
        return getWeatherForecast(locationId, 1);
    }

    public WeatherForecastRecord getWeatherForecast(long locationId, int forecastType) {

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS
        };

        try (Cursor cursor = db.query(
                WeatherForecastContract.WeatherForecast.TABLE_NAME,
                projection,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId +
                    " AND " + WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE + "=" + forecastType,
                null,
                null,
                null,
                null
        )) {

            if (cursor.moveToNext()) {
                CompleteWeatherForecast completeWeatherForecast = getCompleteWeatherForecastFromBytes(
                        cursor.getBlob(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST)));
                if (completeWeatherForecast == null) {
                    return null;
                }
                return new WeatherForecastRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS)),
                        completeWeatherForecast);
            }
            return null;
        }
    }

    public CompleteWeatherForecast getCompleteWeatherForecastFromBytes(byte[] addressBytes) {
        if ((addressBytes == null) || (addressBytes.length == 0)) {
            return null;
        }
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(addressBytes, 0, addressBytes.length);
        parcel.setDataPosition(0);
        CompleteWeatherForecast completeWeatherForecast = null;
        try {
            completeWeatherForecast = CompleteWeatherForecast.CREATOR.createFromParcel(parcel);
        } catch (Exception e) {
            appendLog(context, TAG, e);
        }
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
}
