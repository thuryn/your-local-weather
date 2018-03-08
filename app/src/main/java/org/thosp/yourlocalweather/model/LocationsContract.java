package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class LocationsContract {

    private LocationsContract() {}

    protected static final String SQL_CREATE_TABLE_LOCATIONS =
            "CREATE TABLE " + LocationsContract.Locations.TABLE_NAME + " (" +
                    LocationsContract.Locations._ID + " INTEGER PRIMARY KEY," +
                    LocationsContract.Locations.COLUMN_NAME_LONGITUDE + " double," +
                    LocationsContract.Locations.COLUMN_NAME_LATITUDE + " double," +
                    LocationsContract.Locations.COLUMN_NAME_LOCALE + " text," +
                    LocationsContract.Locations.COLUMN_NAME_ORDER_ID + " integer," +
                    LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME + " text," +
                    LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND + " integer default 0," +
                    LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS + " integer," +
                    LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE + " text," +
                    LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY + " double," +
                    LocationsContract.Locations.COLUMN_NAME_ENABLED + " integer," +
                    LocationsContract.Locations.COLUMN_NAME_ADDRESS + " blob)";

    protected static final String SQL_DELETE_TABLE_LOCATIONS =
            "DROP TABLE IF EXISTS " + LocationsContract.Locations.TABLE_NAME;

    public static class Locations implements BaseColumns {
        public static final String TABLE_NAME = "locations";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LOCALE = "locale";
        public static final String COLUMN_NAME_ADDRESS = "address";
        public static final String COLUMN_NAME_ORDER_ID = "order_id";
        public static final String COLUMN_NAME_LOCATION_ACCURACY = "location_accuracy";
        public static final String COLUMN_NAME_LOCATION_NICKNAME = "location_nickname";
        public static final String COLUMN_NAME_ADDRESS_FOUND = "address_found";
        public static final String COLUMN_NAME_LAST_UPDATE_TIME_IN_MS = "last_update_time";
        public static final String COLUMN_NAME_LOCATION_UPDATE_SOURCE = "location_update_source";
        public static final String COLUMN_NAME_ENABLED = "location_enabled";
    }
}
