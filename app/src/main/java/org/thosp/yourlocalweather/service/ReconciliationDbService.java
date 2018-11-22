package org.thosp.yourlocalweather.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsContract;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.LocationsFileDbHelper;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ReconciliationDbService extends IntentService {

    private static final String TAG = "ReconciliationDbService";

    private static final long MIN_RECONCILIATION_TIME_SPAN_IN_MS = 60000;

    private static volatile long nextReconciliationTime;

    public ReconciliationDbService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog(this, TAG, "onHandleIntent");
        boolean force = false;
        if (intent.hasExtra("force")) {
            force = intent.getBooleanExtra("force", false);
        }
        appendLog(this, TAG, "force:" + intent.hasExtra("force") + ":" + force);
        if (!force) {
            if (nextReconciliationTime == 0) {
                nextReconciliationTime = System.currentTimeMillis() + MIN_RECONCILIATION_TIME_SPAN_IN_MS;
                appendLog(
                        this,
                        TAG,
                        "nextReconciliationTime is 0");
            } else {
                appendLog(
                        this,
                        TAG,
                        "rescheduling with inMilis:" + nextReconciliationTime + ":" + System.currentTimeMillis());
                Intent intentToReschedule = new Intent(getBaseContext(), ReconciliationDbService.class);
                intentToReschedule.putExtra("force", true);
                WidgetUtils.startBackgroundService(getBaseContext(), intentToReschedule, MIN_RECONCILIATION_TIME_SPAN_IN_MS);
                return;
            }
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getApplicationContext());
        LocationsFileDbHelper locationsFileDbHelper = LocationsFileDbHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = locationsFileDbHelper.getWritableDatabase();
        for (Location location: locationsDbHelper.getAllRows()) {
            Location locationInFile = locationsFileDbHelper.getLocationById(location.getId());
            if (locationInFile == null) {
                insertLocation(db, location);
            } else {
                updateLocation(db, location, locationInFile);
            }
        }
        for (Location location: locationsFileDbHelper.getAllRows()) {
            Location locationInRam = locationsDbHelper.getLocationById(location.getId());
            if (locationInRam == null) {
                locationsFileDbHelper.deleteRecordFromTable(location);
            }
        }
        nextReconciliationTime = System.currentTimeMillis() + MIN_RECONCILIATION_TIME_SPAN_IN_MS;
    }

    private void insertLocation(SQLiteDatabase db, Location location) {
        ContentValues values = new ContentValues();
        values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS, LocationsDbHelper.getAddressAsBytes(location.getAddress()));
        values.put(LocationsContract.Locations.COLUMN_NAME_LONGITUDE, location.getLongitude());
        values.put(LocationsContract.Locations.COLUMN_NAME_LATITUDE, location.getLatitude());
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCALE, location.getLocaleAbbrev());
        values.put(LocationsContract.Locations.COLUMN_NAME_ORDER_ID, location.getOrderId());
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE, location.getLocationSource());
        values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND, location.isAddressFound());
        values.put(LocationsContract.Locations.COLUMN_NAME_ENABLED, location.isEnabled());
        values.put(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS, location.getLastLocationUpdate());
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY, location.getAccuracy());
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME, location.getNickname());
        values.put(LocationsContract.Locations._ID, location.getId());
        long newLocationRowId = db.insert(
                LocationsContract.Locations.TABLE_NAME,
                null,
                values);
    }

    private void updateLocation(SQLiteDatabase db, Location location, Location locationInFile) {
        ContentValues values = prepareValues(location, locationInFile);
        if (values.size() == 0) {
            return;
        }
        appendLog(
                this,
                TAG,
                "update location:" + location.getId());
        db.updateWithOnConflict(
                LocationsContract.Locations.TABLE_NAME,
                values,
                LocationsContract.Locations._ID +"=" + locationInFile.getId(),
                null,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    private ContentValues prepareValues(Location location, Location locationInFile) {
        ContentValues values = new ContentValues();
        if ((location.getAddress() != null) && !location.getAddress().equals(locationInFile.getAddress())) {
            values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS, LocationsDbHelper.getAddressAsBytes(location.getAddress()));
        }
        if (location.getLongitude() != locationInFile.getLongitude()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LONGITUDE, location.getLongitude());
        }
        if (location.getLatitude() != locationInFile.getLatitude()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LATITUDE, location.getLatitude());
        }
        if ((location.getLocale() != null) && !location.getLocale().equals(locationInFile.getLocale())) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LOCALE, location.getLocaleAbbrev());
        }
        if (location.getOrderId() != locationInFile.getOrderId()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_ORDER_ID, location.getOrderId());
        }
        if ((location.getLocationSource() != null) && !location.getLocationSource().equals(locationInFile.getLocationSource())) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE, location.getLocationSource());
        }
        if (location.isAddressFound() != locationInFile.isAddressFound()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND, location.isAddressFound());
        }
        if (location.isEnabled() != locationInFile.isEnabled()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_ENABLED, location.isEnabled());
        }
        if (location.getLastLocationUpdate() != locationInFile.getLastLocationUpdate()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LAST_UPDATE_TIME_IN_MS, location.getLastLocationUpdate());
        }
        if (location.getAccuracy() != locationInFile.getAccuracy()) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_ACCURACY, location.getAccuracy());
        }
        if ((location.getNickname() != null) && !location.getNickname().equals(locationInFile.getNickname())) {
            values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_NICKNAME, location.getNickname());
        }
        return values;
    }
}
