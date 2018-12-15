package org.thosp.yourlocalweather.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.microg.address.Formatter;
import org.thosp.yourlocalweather.model.ReverseGeocodingCacheDbHelper;
import org.thosp.yourlocalweather.utils.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

import static android.os.Build.VERSION.RELEASE;
import static org.thosp.yourlocalweather.BuildConfig.VERSION_NAME;
import static org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract.LocationAddressCache;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NominatimLocationService {

    public static final String TAG = "NominatimLocationServ";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static NominatimLocationService instance;

    private volatile long nextAlowedRequestTimestamp;

    private NominatimLocationService() {
    }

    public synchronized static NominatimLocationService getInstance() {
        if (instance == null) {
            instance = new NominatimLocationService();
            try {
                formatter = new Formatter();
            } catch (IOException e) {
                Log.w(TAG, "Could not initialize address formatter", e);
            }
            client.addHeader("User-Agent", String.format("YourLocalWeather/%s (Linux; Android %s)", VERSION_NAME, RELEASE));
        }
        return instance;
    }

    private static final String SERVICE_URL_OSM = "https://nominatim.openstreetmap.org";
    private static final String REVERSE_GEOCODE_URL =
            "%s/reverse?%sformat=json&accept-language=%s&lat=%f&lon=%f";
    private static final String WIRE_LATITUDE = "lat";
    private static final String WIRE_LONGITUDE = "lon";
    private static final String WIRE_ADDRESS = "address";
    private static final String WIRE_THOROUGHFARE = "road";
    private static final String WIRE_SUBLOCALITY = "suburb";
    private static final String WIRE_POSTALCODE = "postcode";
    private static final String WIRE_LOCALITY_CITY = "city";
    private static final String WIRE_LOCALITY_TOWN = "town";
    private static final String WIRE_LOCALITY_VILLAGE = "village";
    private static final String WIRE_SUBADMINAREA = "county";
    private static final String WIRE_ADMINAREA = "state";
    private static final String WIRE_COUNTRYNAME = "country";
    private static final String WIRE_COUNTRYCODE = "country_code";
    private static Formatter formatter;

    public void getFromLocation(final Context context,
                                            final double latitude,
                                            final double longitude,
                                            int maxResults,
                                            final String locale,
                                            final ProcessResultFromAddressResolution processResultFromAddressResolution) {

        appendLog(context, TAG, "getFromLocation:", latitude, ", ", longitude, ", ", locale);
        final ReverseGeocodingCacheDbHelper mDbHelper = ReverseGeocodingCacheDbHelper.getInstance(context);

        List<Address> addressesFromCache = retrieveLocationFromCache(context, mDbHelper, latitude, longitude, locale);
        if (addressesFromCache != null) {
            processResultFromAddressResolution.processAddresses(addressesFromCache);
            return;
        }

        long now = System.currentTimeMillis();
        if (nextAlowedRequestTimestamp > now) {
            appendLog(context, TAG,
                    "request to nominatim in less than 1.4s - nextAlowedRequestTimestamp=",
                    nextAlowedRequestTimestamp,
                    ", now=",
                    now);
            processResultFromAddressResolution.processAddresses(null);
            return;
        }

        nextAlowedRequestTimestamp = 1400 + now;
        final String url = String.format(Locale.US, REVERSE_GEOCODE_URL, SERVICE_URL_OSM, "",
                locale.split("_")[0], latitude, longitude);
        appendLog(context, TAG, "Constructed URL ", url);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                client.get(url, null, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        // called before request is started
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        try {
                            String rawResult = new String(response);
                            JSONObject result = new JSONObject(rawResult);
                            appendLog(context, TAG, "result from nominatim server:", rawResult);

                            Address address = parseResponse(localeFromLocaleString(locale), result);
                            if (address != null) {
                                List<Address> addresses = new ArrayList<>();
                                addresses.add(address);
                                storeAddressToCache(context, mDbHelper, latitude, longitude, locale, address);
                                processResultFromAddressResolution.processAddresses(addresses);
                            }
                        } catch (JSONException jsonException) {
                            appendLog(context, TAG, "jsonException:", jsonException);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        appendLog(context, TAG, "onFailure:", statusCode);
                        processResultFromAddressResolution.processAddresses(null);
                    }

                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });
            }
        };
        mainHandler.post(myRunnable);
    }

    private Address parseResponse(Locale locale, JSONObject result) throws JSONException {
        if (!result.has(WIRE_LATITUDE) || !result.has(WIRE_LONGITUDE) ||
                !result.has(WIRE_ADDRESS)) {
            return null;
        }
        Address address = new Address(locale);
        address.setLatitude(result.getDouble(WIRE_LATITUDE));
        address.setLongitude(result.getDouble(WIRE_LONGITUDE));

        JSONObject a = result.getJSONObject(WIRE_ADDRESS);

        address.setThoroughfare(a.optString(WIRE_THOROUGHFARE));
        address.setSubLocality(a.optString(WIRE_SUBLOCALITY));
        address.setPostalCode(a.optString(WIRE_POSTALCODE));
        address.setSubAdminArea(a.optString(WIRE_SUBADMINAREA));
        address.setAdminArea(a.optString(WIRE_ADMINAREA));
        address.setCountryName(a.optString(WIRE_COUNTRYNAME));
        address.setCountryCode(a.optString(WIRE_COUNTRYCODE));

        if (a.has(WIRE_LOCALITY_CITY)) {
            address.setLocality(a.getString(WIRE_LOCALITY_CITY));
        } else if (a.has(WIRE_LOCALITY_TOWN)) {
            address.setLocality(a.getString(WIRE_LOCALITY_TOWN));
        } else if (a.has(WIRE_LOCALITY_VILLAGE)) {
            address.setLocality(a.getString(WIRE_LOCALITY_VILLAGE));
        }

        if (formatter != null) {
            Map<String, String> components = new HashMap<>();
            for (String s : new IterableIterator<>(a.keys())) {
                components.put(s, String.valueOf(a.get(s)));
            }
            String[] split = formatter.formatAddress(components).split("\n");
            for (int i = 0; i < split.length; i++) {
                address.setAddressLine(i, split[i]);
            }

            address.setFeatureName(formatter.guessName(components));
        }

        return address;
    }

    private static Locale localeFromLocaleString(String localeString) {
        String[] split = localeString.split("_");
        if (split.length == 1) {
            return new Locale(split[0]);
        } else if (split.length == 2) {
            return new Locale(split[0], split[1]);
        } else if (split.length == 3) {
            return new Locale(split[0], split[1], split[2]);
        }
        throw new RuntimeException("That's not a locale: " + localeString);
    }

    private List<Address> retrieveLocationFromCache(Context context, ReverseGeocodingCacheDbHelper mDbHelper, double latitude, double longitude, String locale) {
        boolean useCache = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, false);

        if (!useCache) {
            return null;
        }

        Address addressFromCache = getResultFromCache(mDbHelper, latitude, longitude, locale);
        appendLog(context, TAG, "address retrieved from cache:", addressFromCache);
        if (addressFromCache == null) {
            return null;
        }
        List<Address> addresses = new ArrayList<>();
        addresses.add(addressFromCache);
        return addresses;
    }

    private void storeAddressToCache(Context context, ReverseGeocodingCacheDbHelper mDbHelper, double latitude, double longitude, String locale, Address address) {

        boolean useCache = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, false);

        if (!useCache) {
            return;
        }

        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(LocationAddressCache.COLUMN_NAME_ADDRESS, getAddressAsBytes(address));
        values.put(LocationAddressCache.COLUMN_NAME_LONGITUDE, longitude);
        values.put(LocationAddressCache.COLUMN_NAME_LATITUDE, latitude);
        values.put(LocationAddressCache.COLUMN_NAME_LOCALE, locale);
        values.put(LocationAddressCache.COLUMN_NAME_CREATED, new Date().getTime());

        long newLocationRowId = db.insert(LocationAddressCache.TABLE_NAME, null, values);

        appendLog(context, TAG, "storedAddress:", latitude, ", ", longitude, ", ", newLocationRowId, ", ", address);
    }

    private Address getResultFromCache(ReverseGeocodingCacheDbHelper mDbHelper, double latitude, double longitude, String locale) {

        new DeleteOldRows(mDbHelper).start();

        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                LocationAddressCache.COLUMN_NAME_ADDRESS
        };

        double latitudeLow = latitude - 0.0001;
        double latitudeHigh = latitude + 0.0001;
        double longitudeLow = longitude - 0.0001;
        double longitudeHigh = longitude + 0.0001;

        String selection = LocationAddressCache.COLUMN_NAME_LONGITUDE + " <= ? and " +
                LocationAddressCache.COLUMN_NAME_LONGITUDE + " >= ? and " +
                LocationAddressCache.COLUMN_NAME_LATITUDE + " <= ? and " +
                LocationAddressCache.COLUMN_NAME_LATITUDE + " >= ? and " +
                LocationAddressCache.COLUMN_NAME_LOCALE + " = ? ";
        String[] selectionArgs = { String.valueOf(longitudeHigh),
                String.valueOf(longitudeLow),
                String.valueOf(latitudeHigh),
                String.valueOf(latitudeLow),
                locale };

        Cursor cursor = null;
        try {
            cursor = db.query(
                    LocationAddressCache.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (!cursor.moveToNext()) {
                cursor.close();
                return null;
            }

            byte[] cachedAddressBytes = cursor.getBlob(
                    cursor.getColumnIndexOrThrow(LocationAddressCache.COLUMN_NAME_ADDRESS));
            return ReverseGeocodingCacheDbHelper.getAddressFromBytes(cachedAddressBytes);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean recordDateIsNotValidOrIsTooOld(long recordCreatedinMilis) {
        Calendar now = Calendar.getInstance();
        Calendar calendarRecordCreated = Calendar.getInstance();
        calendarRecordCreated.setTimeInMillis(recordCreatedinMilis);

        int timeToLiveRecordsInCacheInHours = 8760;/*Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.LOCATION_CACHE_LASTING_HOURS, "720"))*/;

        calendarRecordCreated.add(Calendar.HOUR_OF_DAY, timeToLiveRecordsInCacheInHours);
        return calendarRecordCreated.before(now);
    }

    private byte[] getAddressAsBytes(Address address) {
        final Parcel parcel = Parcel.obtain();
        address.writeToParcel(parcel, 0);
        byte[] addressBytes = parcel.marshall();
        parcel.recycle();
        return addressBytes;
    }

    private class DeleteOldRows extends Thread {

        private ReverseGeocodingCacheDbHelper mDbHelper;

        public DeleteOldRows(ReverseGeocodingCacheDbHelper mDbHelper) {
            this.mDbHelper = mDbHelper;
        }

        @Override
        public void run() {
            SQLiteDatabase db = mDbHelper.getWritableDatabase();

            String[] projection = {
                    LocationAddressCache.COLUMN_NAME_CREATED,
                    LocationAddressCache._ID
            };

            Cursor cursor = null;
            try {
                cursor = db.query(
                    LocationAddressCache.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null
                );

                while (cursor.moveToNext()) {
                    Integer recordId = cursor.getInt(
                            cursor.getColumnIndexOrThrow(LocationAddressCache._ID));

                    long recordCreatedInMilis = cursor.getLong(
                            cursor.getColumnIndexOrThrow(LocationAddressCache.COLUMN_NAME_CREATED));

                    if (recordDateIsNotValidOrIsTooOld(recordCreatedInMilis)) {
                        mDbHelper.deleteRecordFromTable(recordId);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    class IterableIterator<T> implements Iterable<T> {
        Iterator<T> i;

        public IterableIterator(Iterator<T> i) {
            this.i = i;
        }

        @Override
        public Iterator<T> iterator() {
            return i;
        }
    }
}
