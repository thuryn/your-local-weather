package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Parcel;

import static org.thosp.yourlocalweather.model.CurrentWeatherContract.SQL_CREATE_TABLE_CURRENT_WEATHER;
import static org.thosp.yourlocalweather.model.CurrentWeatherContract.SQL_DELETE_TABLE_CURRENT_WEATHER;

public class CurrentWeatherDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "CurrentWeather.db";
    private static final int GET_READABLE_DATABASE_RETRIES = 3;
    private static final int GET_READABLE_DATABASE_WAIT_TIME_MS = 500;
    private static CurrentWeatherDbHelper instance;

    public synchronized static CurrentWeatherDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new CurrentWeatherDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private CurrentWeatherDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_CURRENT_WEATHER);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_CURRENT_WEATHER);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        int retryCounter = 0;
        do {
            try {
                return super.getReadableDatabase();
            } catch (SQLiteDatabaseLockedException dbLockException) {
                retryCounter++;
                if (retryCounter > GET_READABLE_DATABASE_RETRIES) {
                    return null;
                }
                try {
                    Thread.currentThread();
                    Thread.sleep(GET_READABLE_DATABASE_WAIT_TIME_MS);
                } catch (InterruptedException e) {
                    //
                }
            }
        } while (retryCounter <= GET_READABLE_DATABASE_RETRIES);
        return null;
    }

    public void deleteRecordByLocation(Location location) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID + " = ?";
        String[] selectionArgs = {location.getId().toString()};
        db.delete(CurrentWeatherContract.CurrentWeather.TABLE_NAME, selection, selectionArgs);
    }

    public void deleteRecordFromTable(Integer recordId) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = CurrentWeatherContract.CurrentWeather._ID + " = ?";
        String[] selectionArgs = {recordId.toString()};
        db.delete(CurrentWeatherContract.CurrentWeather.TABLE_NAME, selection, selectionArgs);
    }

    public static Weather getWeatherFromBytes(byte[] addressBytes) {
        if ((addressBytes == null) || (addressBytes.length == 0)) {
            return null;
        }
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(addressBytes, 0, addressBytes.length);
        parcel.setDataPosition(0);
        Weather weather = Weather.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return weather;
    }

    public byte[] getWeatherAsBytes(Weather weather) {
        final Parcel parcel = Parcel.obtain();
        weather.writeToParcel(parcel, 0);
        byte[] weatherBytes = parcel.marshall();
        parcel.recycle();
        return weatherBytes;
    }

    public void saveWeather(final long locationId,
                            final long weatherUpdateTime,
                            final long nextAllowedAttemptToUpdateTime,
                            final Weather weather) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();

                WeatherRecord oldWeather = getWeather(locationId);

                ContentValues values = new ContentValues();
                values.put(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_WEATHER, getWeatherAsBytes(weather));
                values.put(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID, locationId);
                values.put(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LAST_UPDATED_IN_MS, weatherUpdateTime);
                values.put(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS, nextAllowedAttemptToUpdateTime);
                if (oldWeather == null) {
                    db.insert(CurrentWeatherContract.CurrentWeather.TABLE_NAME, null, values);
                } else {
                    db.updateWithOnConflict(CurrentWeatherContract.CurrentWeather.TABLE_NAME,
                            values,
                            CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                            null,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        }).start();
    }

    public void updateNextAllowedAttemptToUpdateTime(final long locationId, final long nextAllowedAttemptToUpdateTime) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS, nextAllowedAttemptToUpdateTime);
                WeatherRecord oldWeather = getWeather(locationId);
                if (oldWeather == null) {
                    values.put(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID, locationId);
                    db.insert(CurrentWeatherContract.CurrentWeather.TABLE_NAME, null, values);
                } else {
                    db.updateWithOnConflict(
                            CurrentWeatherContract.CurrentWeather.TABLE_NAME,
                            values,
                            CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                            null,
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }
        }).start();
    }

    public WeatherRecord getWeather(long locationId) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                CurrentWeatherContract.CurrentWeather.COLUMN_NAME_WEATHER,
                CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LAST_UPDATED_IN_MS,
                CurrentWeatherContract.CurrentWeather.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    CurrentWeatherContract.CurrentWeather.TABLE_NAME,
                    projection,
                    CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LOCATION_ID + "=" + locationId,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                Weather weather = getWeatherFromBytes(cursor.getBlob(cursor.getColumnIndexOrThrow(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_WEATHER)));
                return new WeatherRecord(
                        cursor.getLong(cursor.getColumnIndexOrThrow(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_LAST_UPDATED_IN_MS)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(CurrentWeatherContract.CurrentWeather.COLUMN_NAME_NEXT_ALLOWED_ATTEMPT_TO_UPDATE_TIME_IN_MS)),
                        weather);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public class WeatherRecord {
        long lastUpdatedTime;
        long nextAllowedAttemptToUpdateTime;
        Weather weather;

        public WeatherRecord(long lastUpdatedTime, long nextAllowedAttemptToUpdateTime, Weather weather) {
            this.lastUpdatedTime = lastUpdatedTime;
            this.nextAllowedAttemptToUpdateTime = nextAllowedAttemptToUpdateTime;
            this.weather = weather;
        }

        public long getLastUpdatedTime() {
            return lastUpdatedTime;
        }

        public Weather getWeather() {
            return weather;
        }

        public long getNextAllowedAttemptToUpdateTime() {
            return nextAllowedAttemptToUpdateTime;
        }
    }
}
