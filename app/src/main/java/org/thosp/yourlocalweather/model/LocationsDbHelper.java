package org.thosp.yourlocalweather.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.os.Parcel;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.service.LocationUpdateService;

import java.util.ArrayList;
import java.util.List;

import static org.thosp.yourlocalweather.model.LocationsContract.SQL_CREATE_TABLE_LOCATIONS;
import static org.thosp.yourlocalweather.model.LocationsContract.SQL_DELETE_TABLE_LOCATIONS;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LocationsDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "LocationsDbHelper";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Locations.db";
    private Context context;
    private static LocationsDbHelper instance;

    public synchronized static LocationsDbHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new LocationsDbHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private LocationsDbHelper(Context context) {
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

    public Address getAddressFromBytes(byte[] addressBytes) {
        if ((addressBytes == null) || (addressBytes.length == 0)) {
            return null;
        }
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(addressBytes, 0, addressBytes.length);
        parcel.setDataPosition(0);
        Address address = Address.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return address;
    }

    public byte[] getAddressAsBytes(Address address) {
        if (address == null) {
            return null;
        }
        final Parcel parcel = Parcel.obtain();
        address.writeToParcel(parcel, 0);
        byte[] addressBytes = parcel.marshall();
        parcel.recycle();
        return addressBytes;
    }

    public int getMaxOrderId() {
        SQLiteDatabase db = getReadableDatabase();

        int maxOrderId = 0;
        Cursor cursor = null;
        try {
            cursor = db.query(
                LocationsContract.Locations.TABLE_NAME,
                new String[]{"MAX(order_id)"},
                null,
                null,
                null,
                null,
                null);

            cursor.moveToNext();
            maxOrderId = cursor.getInt(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return maxOrderId;
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
                    address = getAddressFromBytes(cachedAddressBytes);
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

    public Location getLocationByOrderId(int orderId) {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LocationsContract.Locations.COLUMN_NAME_ADDRESS,
                LocationsContract.Locations._ID,
                LocationsContract.Locations.COLUMN_NAME_LONGITUDE,
                LocationsContract.Locations.COLUMN_NAME_LATITUDE,
                LocationsContract.Locations.COLUMN_NAME_LOCALE,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY,
                LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS,
                LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE,
                LocationsContract.Locations.COLUMN_NAME_ENABLED,
                LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                LocationsContract.Locations.TABLE_NAME,
                projection,
                LocationsContract.Locations.COLUMN_NAME_ORDER_ID + "=" + orderId,
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
                address = getAddressFromBytes(cachedAddressBytes);
            }

            int itemId = cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations._ID));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LONGITUDE));
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LATITUDE));
            String locale = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCALE));
            String nickname = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME));
            float accuracy = cursor.getFloat(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY));
            long locationUpdateTime = cursor.getLong(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS));
            String locationSource = cursor.getString(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE));
            boolean addressFound = 1 == cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND));
            boolean enabled = 1 == cursor.getInt(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_ENABLED));

            return new Location(
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
                    address);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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
                address = getAddressFromBytes(cachedAddressBytes);
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

    public Long getLocationIdByCoordinates(float lat, float lon) {

        float latLow = lat - 0.01f;
        float latHigh = lat + 0.01f;
        float lonLow = lon - 0.01f;
        float lonHigh = lon + 0.01f;
        String latLowTxt = String.valueOf(latLow).replace(",", ".");
        String latHighTxt = String.valueOf(latHigh).replace(",", ".");
        String lonLowTxt = String.valueOf(lonLow).replace(",", ".");
        String lonHighTxt = String.valueOf(lonHigh).replace(",", ".");

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LocationsContract.Locations._ID
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    LocationsContract.Locations.TABLE_NAME,
                    projection,
                    LocationsContract.Locations.COLUMN_NAME_LATITUDE +
                            ">? and " +
                            LocationsContract.Locations.COLUMN_NAME_LATITUDE +
                            "<? and " +
                            LocationsContract.Locations.COLUMN_NAME_LONGITUDE +
                            ">? and " +
                            LocationsContract.Locations.COLUMN_NAME_LONGITUDE +
                            "<?",
                    new String[] {latLowTxt, latHighTxt, lonLowTxt, lonHighTxt},
                    null,
                    null,
                    null
            );

            if (!cursor.moveToNext()) {
                return null;
            }
            return cursor.getLong(cursor.getColumnIndexOrThrow(LocationsContract.Locations._ID));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void updateNickname(int locationOrderId, String locationNickname) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME, locationNickname);
        db.updateWithOnConflict(
                LocationsContract.Locations.TABLE_NAME,
                values,
                LocationsContract.Locations.COLUMN_NAME_ORDER_ID +"=" + locationOrderId,
                null,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void updateLocale(final long locationId, final String locale) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCALE, locale);
        db.updateWithOnConflict(
                LocationsContract.Locations.TABLE_NAME,values,
                LocationsContract.Locations._ID +"=" + locationId,
                null,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void updateAutoLocationAddress(final String locale, final Address address) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS, getAddressAsBytes(address));
                values.put(LocationsContract.Locations.COLUMN_NAME_LOCALE, locale);
                values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND, 1);
                values.put(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS, System.currentTimeMillis());
                db.updateWithOnConflict(
                        LocationsContract.Locations.TABLE_NAME,values,
                        LocationsContract.Locations.COLUMN_NAME_ORDER_ID +"=0",
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
                LocationUpdateService.autolocationForSensorEventAddressFound = true;
            }
        }).start();
    }

    public void updateAutoLocationGeoLocation(final double latitude,
                                              final double longitude,
                                              final String locationSource,
                                              final float accuracy,
                                              final long locationTime) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(LocationsContract.Locations.COLUMN_NAME_LONGITUDE, longitude);
                values.put(LocationsContract.Locations.COLUMN_NAME_LATITUDE, latitude);
                values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE, locationSource);
                values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY, accuracy);
                values.put(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS, locationTime);
                db.updateWithOnConflict(
                        LocationsContract.Locations.TABLE_NAME,values,
                        LocationsContract.Locations.COLUMN_NAME_ORDER_ID +"=0",
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        }).start();
    }

    public void setNoLocationFound() {
        appendLog(context, TAG, "setNoLocationFound:entered");
        new Thread(new Runnable() {
            public void run() {
                appendLog(context, TAG, "setNoLocationFound:run");
                SQLiteDatabase db = getWritableDatabase();
                appendLog(context, TAG, "setNoLocationFound:writableDB");
                ContentValues values = new ContentValues();
                values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND, 0);
                values.put(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS, System.currentTimeMillis());

                db.updateWithOnConflict(
                        LocationsContract.Locations.TABLE_NAME,values,
                        LocationsContract.Locations.COLUMN_NAME_ORDER_ID +"=0",
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
                appendLog(context, TAG, "setNoLocationFound:updated");
                LocationUpdateService.autolocationForSensorEventAddressFound = false;
                appendLog(context, TAG, "setNoLocationFound:finished");
            }
        }).start();
    }

    public void updateLocationSource(final long locationId, final String updateSource) {
        appendLog(context, TAG, "updateLocationSource:entered:" + locationId + ":" + updateSource);
        new Thread(new Runnable() {
            public void run() {
                appendLog(context, TAG, "updateLocationSource:run");
                SQLiteDatabase db = getWritableDatabase();
                appendLog(context, TAG, "updateLocationSource:writableDB");
                ContentValues values = new ContentValues();
                values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE, updateSource);

                db.updateWithOnConflict(
                        LocationsContract.Locations.TABLE_NAME,
                        values,
                        LocationsContract.Locations._ID + "=" + locationId,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
                appendLog(context, TAG, "updateLocationSource:updated");
            }
        }).start();
    }

    public void updateEnabled(long locationId, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationsContract.Locations.COLUMN_NAME_ENABLED, enabled);

        db.updateWithOnConflict(
                LocationsContract.Locations.TABLE_NAME, values,
                LocationsContract.Locations._ID + "=" + locationId,
                null,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void updateLastUpdatedAndLocationSource(final long locationId,
                                                   final long updateTime,
                                                   final String updateSource) {
        new Thread(new Runnable() {
            public void run() {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE, updateSource);
                values.put(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS, updateTime);

                db.updateWithOnConflict(
                        LocationsContract.Locations.TABLE_NAME,
                        values,
                        LocationsContract.Locations._ID + "=" + locationId,
                        null,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }
        }).start();
    }

    public long getLastUpdateLocationTime() {
        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {
                LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS
        };

        Cursor cursor = null;
        try {
            cursor = db.query(
                LocationsContract.Locations.TABLE_NAME,
                projection,
                LocationsContract.Locations.COLUMN_NAME_ORDER_ID + "=0",
                null,
                null,
                null,
                null
            );

            if (cursor.moveToNext()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS));
            } else {
                return 0;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
