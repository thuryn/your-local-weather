package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Parcel;

import java.util.concurrent.ExecutionException;

import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_CREATE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.model.WeatherForecastContract.SQL_DELETE_TABLE_WEATHER_FORECAST;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "WeatherForecastDbHelper";

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "WeatherForecast.db";
    private static WeatherForecastDbHelper instance;
    private final Context context;

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
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                String selection = WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + " = ?";
                String[] selectionArgs = {location.getId().toString()};
                db.delete(WeatherForecastContract.WeatherForecast.TABLE_NAME, selection, selectionArgs);
            }
        }).start();
    }

    public void saveWeatherForecast(long locationId, int forecastType, long weatherUpdateTime, final long nextAllowedAttemptToUpdateTime, CompleteWeatherForecast completeWeatherForecast) {
        new Thread(new Runnable() {
            public void run() {
                appendLog(context, TAG, "saveWeatherForecast: locationId:", locationId, "forecastType:", forecastType);
                SQLiteDatabase db = getWritableDatabase();

                WeatherForecastRecord oldWeatherForecast = getWeatherForecast(locationId, forecastType);

                appendLog(context, TAG, "saveWeatherForecast: oldWeatherForecast:", oldWeatherForecast);

                ContentValues values = new ContentValues();
                values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                        getCompleteWeatherForecastAsBytes(completeWeatherForecast));
                values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID, locationId);
                values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS, weatherUpdateTime);
                values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS, nextAllowedAttemptToUpdateTime);
                values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE, forecastType);
                if (oldWeatherForecast == null) {
                    db.insertOrThrow(WeatherForecastContract.WeatherForecast.TABLE_NAME, null, values);
                    appendLog(context, TAG, "saveWeatherForecast: inserted new record");
                } else {
                    db.updateWithOnConflict(WeatherForecastContract.WeatherForecast.TABLE_NAME,
                            values,
                            WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId +
                                    " AND " + WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE + "=" + forecastType,
                            null,
                            SQLiteDatabase.CONFLICT_REPLACE);
                    appendLog(context, TAG, "saveWeatherForecast: updated old record");
                }
            }
        }).start();
    }

    public WeatherForecastRecord getWeatherForecast(long locationId) {
        return getWeatherForecast(locationId, 1);
    }

    public WeatherForecastRecord getWeatherForecast(final long locationId, final int forecastType) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_WEATHER_FORECAST,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_LAST_UPDATED_IN_MS,
                WeatherForecastContract.WeatherForecast.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    WeatherForecastContract.WeatherForecast.TABLE_NAME,
                    projection,
                    WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId +
                            " AND " + WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE + "=" + forecastType,
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
                        cursor.getLong(cursor.getColumnIndexOrThrow(WeatherForecastContract.WeatherForecast.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS)),
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

    public void updateNextAllowedAttemptToUpdateTime(final long locationId, final int forecastType, final long nextAllowedAttemptToUpdateTime) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS, nextAllowedAttemptToUpdateTime);
                WeatherForecastRecord oldWeatherForecast = getWeatherForecast(locationId, forecastType);
                if (oldWeatherForecast == null) {
                    values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID, locationId);
                    values.put(WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE, forecastType);
                    db.insert(WeatherForecastContract.WeatherForecast.TABLE_NAME, null, values);
                } else {
                    db.updateWithOnConflict(
                            WeatherForecastContract.WeatherForecast.TABLE_NAME,
                            values,
                            WeatherForecastContract.WeatherForecast.COLUMN_NAME_LOCATION_ID + "=" + locationId +
                                    " AND " + WeatherForecastContract.WeatherForecast.COLUMN_NAME_FORECAST_TYPE + "=" + forecastType,
                            null,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        }).start();
    }

    public class WeatherForecastRecord {
        long lastUpdatedTime;
        long nextAllowedAttemptToUpdateTime;
        CompleteWeatherForecast completeWeatherForecast;

        public WeatherForecastRecord(long lastUpdatedTime, long nextAllowedAttemptToUpdateTime, CompleteWeatherForecast completeWeatherForecast) {
            this.lastUpdatedTime = lastUpdatedTime;
            this.nextAllowedAttemptToUpdateTime = nextAllowedAttemptToUpdateTime;
            this.completeWeatherForecast = completeWeatherForecast;
        }

        public long getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public CompleteWeatherForecast getCompleteWeatherForecast() {
            return completeWeatherForecast;
        }

        public long getNextAllowedAttemptToUpdateTime() {
            return nextAllowedAttemptToUpdateTime;
        }
    }
}
