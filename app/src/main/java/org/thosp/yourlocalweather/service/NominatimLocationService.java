package org.thosp.yourlocalweather.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract;
import org.thosp.yourlocalweather.model.ReverseGeocodingCacheDbHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.microg.address.Formatter;
import org.thosp.yourlocalweather.utils.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.os.Build.VERSION.RELEASE;
import static org.thosp.yourlocalweather.BuildConfig.VERSION_NAME;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.model.ReverseGeocodingCacheContract.LocationAddressCache;

public class NominatimLocationService {

    public static final String TAG = "NominatimLocationServ";

    private static NominatimLocationService instance;

    private NominatimLocationService() {
    }

    public static NominatimLocationService getInstance() {
        if (instance == null) {
            instance = new NominatimLocationService();
            try {
                formatter = new Formatter();
            } catch (IOException e) {
                Log.w(TAG, "Could not initialize address formatter", e);
            }
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

    protected List<Address> getFromLocation(Context context,
                                            double latitude,
                                            double longitude,
                                            int maxResults,
                                            String locale) {

        appendLog(context, TAG, "getFromLocation:" + latitude + ", " + longitude + ", " + locale);
        ReverseGeocodingCacheDbHelper mDbHelper = new ReverseGeocodingCacheDbHelper(context);

        List<Address> addressesFromCache = retrieveLocationFromCache(context, mDbHelper, latitude, longitude, locale);
        if (addressesFromCache != null) {
            return addressesFromCache;
        }

        String url = String.format(Locale.US, REVERSE_GEOCODE_URL, SERVICE_URL_OSM, "",
                locale.split("_")[0], latitude, longitude);
        appendLog(context, TAG, "Constructed URL " + url);
        try {
            JSONObject result = new JSONObject(new AsyncGetRequest(context,
                    url).asyncStart().retrieveString());
            appendLog(context, TAG, "result from nominatim server:" + result);

            Address address = parseResponse(localeFromLocaleString(locale), result);
            if (address != null) {
                List<Address> addresses = new ArrayList<>();
                addresses.add(address);
                storeAddressToCache(context, mDbHelper, latitude, longitude, locale, address);
                return addresses;
            }
        } catch (Exception e) {
            appendLog(context, TAG, e.getMessage(), e);
        }
        return null;
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
        appendLog(context, TAG, "address retrieved from cache:" + addressFromCache);
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

        appendLog(context, TAG, "storedAddress:" + latitude + ", " + longitude + ", " + newLocationRowId + ", " + address);
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

        Cursor cursor = db.query(
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
        cursor.close();

        return ReverseGeocodingCacheDbHelper.getAddressFromBytes(cachedAddressBytes);
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

            Cursor cursor = db.query(
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
            cursor.close();
        }
    }

    class AsyncGetRequest extends Thread {
        static final String USER_AGENT = "User-Agent";
        static final String USER_AGENT_TEMPLATE = "UnifiedNlp/%s (Linux; Android %s)";
        private final AtomicBoolean done = new AtomicBoolean(false);
        private final Context context;
        private final String url;
        private byte[] result;

        public AsyncGetRequest(Context context, String url) {
            this.context = context;
            this.url = url;
        }

        @Override
        public void run() {
            appendLog(context, TAG, "Sync key (done)" + done);
            synchronized (done) {
                try {
                    appendLog(context, TAG, "Requesting " + url);
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    appendLog(context, TAG, "Connection opened");
                    connection.setRequestProperty(USER_AGENT, String.format(USER_AGENT_TEMPLATE, VERSION_NAME, RELEASE));
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(30000);
                    connection.setDoInput(true);
                    appendLog(context, TAG, "Getting input stream");
                    InputStream inputStream = connection.getInputStream();
                    appendLog(context, TAG, "Reading input stream");
                    result = readStreamToEnd(inputStream);
                    appendLog(context, TAG, "Input stream read");
                } catch (Exception e) {
                    appendLog(context, TAG, e.getMessage(), e);
                }
                done.set(true);
                done.notifyAll();
            }
        }

        public AsyncGetRequest asyncStart() {
            start();
            return this;
        }

        byte[] retrieveAllBytes() {
            if (!done.get()) {
                synchronized (done) {
                    while (!done.get()) {
                        try {
                            done.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
            return result;
        }

        String retrieveString() {
            return new String(retrieveAllBytes());
        }

        private byte[] readStreamToEnd(InputStream is) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (is != null) {
                byte[] buff = new byte[1024];
                while (true) {
                    int nb = is.read(buff);
                    if (nb < 0) {
                        break;
                    }
                    bos.write(buff, 0, nb);
                }
                is.close();
            }
            return bos.toByteArray();
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
