package org.thosp.yourlocalweather.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.os.Parcel;

import static org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract.SQL_CREATE_TABLE_LOCATION_ADDRESS_CACHE;
import static org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract.SQL_DELETE_TABLE_LOCATION_ADDRESS_CACHE;

public class ReverseGeocodingCacheDbHelper extends SQLiteOpenHelper {
    
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ReverseGeocodingCache.db";

    public ReverseGeocodingCacheDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_LOCATION_ADDRESS_CACHE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE_LOCATION_ADDRESS_CACHE);
        onCreate(db);
    }
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void deleteRecordFromTable(Integer recordId) {
        SQLiteDatabase db = getWritableDatabase();
        String selection = ReverseGeocodingCacheContract.LocationAddressCache._ID + " = ?";
        String[] selectionArgs = { recordId.toString() };
        db.delete(ReverseGeocodingCacheContract.LocationAddressCache.TABLE_NAME, selection, selectionArgs);
    }

    public static Address getAddressFromBytes(byte[] addressBytes) {
        final Parcel parcel = Parcel.obtain();
        parcel.unmarshall(addressBytes, 0, addressBytes.length);
        parcel.setDataPosition(0);
        Address address = Address.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return address;
    }
}
