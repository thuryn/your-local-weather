package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class ReverseGeocodingCacheContract {
    
    private ReverseGeocodingCacheContract() {}
    
    protected static final String SQL_CREATE_TABLE_LOCATION_ADDRESS_CACHE =
        "CREATE TABLE " + LocationAddressCache.TABLE_NAME + " (" +
        LocationAddressCache._ID + " INTEGER PRIMARY KEY," +
        LocationAddressCache.COLUMN_NAME_LONGITUDE + " double," +
        LocationAddressCache.COLUMN_NAME_LATITUDE + " double," +
        LocationAddressCache.COLUMN_NAME_LOCALE + " text," +
        LocationAddressCache.COLUMN_NAME_CREATED + " integer," +
        LocationAddressCache.COLUMN_NAME_ADDRESS + " blob)";

    protected static final String SQL_DELETE_TABLE_LOCATION_ADDRESS_CACHE =
        "DROP TABLE IF EXISTS " + LocationAddressCache.TABLE_NAME;
        
    public static class LocationAddressCache implements BaseColumns {
        public static final String TABLE_NAME = "location_address_cache";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LOCALE = "locale";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_CREATED = "created";
    }
}
