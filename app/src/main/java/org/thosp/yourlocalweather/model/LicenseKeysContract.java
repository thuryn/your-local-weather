package org.thosp.yourlocalweather.model;

import android.provider.BaseColumns;

public final class LicenseKeysContract {

    private LicenseKeysContract() {}

    protected static final String SQL_CREATE_TABLE_LICENSES =
            "CREATE TABLE " + LicenseKeysContract.LicenseKeys.TABLE_NAME + " (" +
                    LicenseKeysContract.LicenseKeys._ID + " INTEGER PRIMARY KEY," +
                    LicenseKeysContract.LicenseKeys.COLUMN_NAME_REQUEST_URI + " text," +
                    LicenseKeysContract.LicenseKeys.COLUMN_NAME_INITIAL_LICENSE + " text," +
                    LicenseKeysContract.LicenseKeys.COLUMN_NAME_LAST_CALL_TIME_IN_MS + " integer," +
                    LicenseKeysContract.LicenseKeys.COLUMN_NAME_TOKEN + " text)";

    protected static final String SQL_DELETE_TABLE_LICENSES =
            "DROP TABLE IF EXISTS " + LicenseKeysContract.LicenseKeys.TABLE_NAME;

    public static class LicenseKeys implements BaseColumns {
        public static final String TABLE_NAME = "licenses";
        public static final String COLUMN_NAME_REQUEST_URI = "requestUri";
        public static final String COLUMN_NAME_INITIAL_LICENSE = "initialLicense";
        public static final String COLUMN_NAME_TOKEN = "token";
        public static final String COLUMN_NAME_LAST_CALL_TIME_IN_MS = "lastCallTimeInMs";
    }
}
