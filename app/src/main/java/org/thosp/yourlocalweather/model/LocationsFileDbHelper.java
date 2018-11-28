package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.os.Parcel;

import org.thosp.yourlocalweather.service.SensorLocationUpdateService;
import org.thosp.yourlocalweather.utils.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;

import static org.thosp.yourlocalweather.model.LocationsContract.SQL_CREATE_TABLE_LOCATIONS;
import static org.thosp.yourlocalweather.model.LocationsContract.SQL_DELETE_TABLE_LOCATIONS;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LocationsFileDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "LocationsFileDbHelper";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Locations.db";
    private Context context;
    private static LocationsFileDbHelper instance;

    public synchronized static LocationsFileDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new LocationsFileDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private LocationsFileDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_LOCATIONS);
        ContentValues values = new ContentValues();
        values.put(LocationsContract.Locations.COLUMN_NAME_ORDER_ID, 0);
        values.put(LocationsContract.Locations.COLUMN_NAME_ENABLED, true);
        db.insert(LocationsContract.Locations.TABLE_NAME, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_LOCATIONS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public Location getLocationById(long id) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LocationsContract.Locations.COLUMN_NAME_ADDRESS,
                LocationsContract.Locations.COLUMN_NAME_ORDER_ID,
                LocationsContract.Locations.COLUMN_NAME_LONGITUDE,
                LocationsContract.Locations.COLUMN_NAME_LATITUDE,
                LocationsContract.Locations.COLUMN_NAME_LOCALE,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY,
                LocationsContract.Locations.COLUMN_NAME_ENABLED,
                LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE,
                LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                LocationsContract.Locations.TABLE_NAME,
                projection,
                LocationsContract.Locations._ID + "=" + id,
                null,
                null,
                null,
                null
            );

            if (!cursor.moveToNext()) {
                return null;
            }

            byte[] cachedAddressBytes = cursor.getBlob(
                    cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ADDRESS));
            Address address = null;
            if (cachedAddressBytes != null) {
                address = LocationsDbHelper.getAddressFromBytes(cachedAddressBytes);
            }

            int orderId = cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ORDER_ID));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LONGITUDE));
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LATITUDE));
            String locale = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCALE));
            String nickname = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME));
            float accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY));
            long locationUpdateTime = cursor.getLong(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS));
            String locationSource = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE));
            boolean addressFound = 1 == cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND));
            boolean enabled = 1 == cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ENABLED));

            if (locale == null) {
                locale = PreferenceUtil.getLanguage(context);
            }

            return new Location(
                    id,
                    orderId,
                    nickname,
                    locale,
                    longitude,
                    latitude,
                    accuracy,
                    locationSource,
                    locationUpdateTime,
                    addressFound,
                    enabled,
                    address);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Location> getAllRows() {

        List<Location> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LocationsContract.Locations.COLUMN_NAME_ADDRESS,
                LocationsContract.Locations._ID,
                LocationsContract.Locations.COLUMN_NAME_LONGITUDE,
                LocationsContract.Locations.COLUMN_NAME_LATITUDE,
                LocationsContract.Locations.COLUMN_NAME_ORDER_ID,
                LocationsContract.Locations.COLUMN_NAME_LOCALE,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY,
                LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE,
                LocationsContract.Locations.COLUMN_NAME_ENABLED,
                LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND
        };

        String sortOrder = LocationsContract.Locations.COLUMN_NAME_ORDER_ID;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    LocationsContract.Locations.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );

            while (cursor.moveToNext()) {

                byte[] cachedAddressBytes = cursor.getBlob(
                        cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ADDRESS));
                Address address = null;
                if (cachedAddressBytes != null) {
                    address = LocationsDbHelper.getAddressFromBytes(cachedAddressBytes);
                }

                long itemId = cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations._ID));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LONGITUDE));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LATITUDE));
                int orderId = cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ORDER_ID));
                String locale = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCALE));
                String nickname = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME));
                float accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY));
                long locationUpdateTime = cursor.getLong(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS));
                String locationSource = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE));
                boolean addressFound = 1 == cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND));
                boolean enabled = 1 == cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ENABLED));

                if (locale == null) {
                    locale = PreferenceUtil.getLanguage(context);
                }

                result.add(new Location(
                        itemId,
                        orderId,
                        nickname,
                        locale,
                        longitude,
                        latitude,
                        accuracy,
                        locationSource,
                        locationUpdateTime,
                        addressFound,
                        enabled,
                        address));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    public void deleteRecordFromTable(Location location) {
        int deletedOrderId = location.getOrderId();
        SQLiteDatabase db = getWritableDatabase();
        String selection = LocationsContract.Locations._ID + " = ?";
        String[] selectionArgs = {location.getId().toString()};
        db.delete(LocationsContract.Locations.TABLE_NAME, selection, selectionArgs);

        String[] projection = {
                LocationsContract.Locations._ID,
                LocationsContract.Locations.COLUMN_NAME_ORDER_ID
        };

        String sortOrder = LocationsContract.Locations.COLUMN_NAME_ORDER_ID;

        Cursor cursor = null;
        try {
            cursor = db.query(
                    LocationsContract.Locations.TABLE_NAME,
                    projection,
                    LocationsContract.Locations.COLUMN_NAME_ORDER_ID + ">" + deletedOrderId,
                    null,
                    null,
                    null,
                    sortOrder
            );

            while (cursor.moveToNext()) {
                long itemId = cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations._ID));
                int orderId = cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ORDER_ID));
                ContentValues values = new ContentValues();
                values.put(LocationsContract.Locations.COLUMN_NAME_ORDER_ID, orderId - 1);
                db.updateWithOnConflict(
                        LocationsContract.Locations.TABLE_NAME,
                        values,
                        LocationsContract.Locations._ID +"=" + itemId,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
