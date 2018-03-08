package org.thosp.yourlocalweather;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.thosp.yourlocalweather.model.CitySearch;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsContract;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.service.NominatimLocationService;
import org.thosp.yourlocalweather.service.SearchActivityProcessResultFromAddressResolution;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.List;
import java.util.Locale;

public class SearchActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    public static final String ACTION_ADDRESS_RESOLUTION_RESULT = "org.thosp.yourlocalweather.action.ADDRESS_RESOLUTION_RESULT";

    private MapView map;
    private TextView resolvedLocationAddress;
    private Context mContext;
    private BroadcastReceiver mWeatherUpdateReceiver;
    private double longitude;
    private double latitude;
    private Address address;
    private String locale;
    private Button addLocatonButton;

    private void initializeWeatherReceiver() {
        mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                address = (Address) intent.getExtras().getParcelable("addresses");
                resolvedLocationAddress.setText(Utils.getCityAndCountryFromAddress(address));
                addLocatonButton.setVisibility(View.VISIBLE);
            }
        };
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());
        Configuration.getInstance().setUserAgentValue(String.format("YourLocalWeather/%s (Linux; Android %s)",
                BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE));

        setContentView(R.layout.activity_search);
        setupActionBar();

        addLocatonButton = (Button) findViewById(R.id.search_add_location_button);
        addLocatonButton.setVisibility(View.GONE);

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        List<Location> currentLocations = locationsDbHelper.getAllRows();
        Location lastLocation = currentLocations.get(currentLocations.size() - 1);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        IMapController mapController = map.getController();
        mapController.setZoom(11);
        GeoPoint startPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
        mapController.setCenter(startPoint);

        resolvedLocationAddress = (TextView) findViewById(R.id.resolved_location_address);
        resolvedLocationAddress.setText(R.string.search_location_info);
        mContext = this;

        MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                latitude = p.getLatitude();
                longitude = p.getLongitude();
                locale = Locale.getDefault().getLanguage();
                Intent resultionResult = new Intent(ACTION_ADDRESS_RESOLUTION_RESULT);
                resultionResult.setPackage("org.thosp.yourlocalweather");
                NominatimLocationService.getInstance().getFromLocation(
                        mContext,
                        p.getLatitude(),
                        p.getLongitude(),
                        1,
                        locale,
                        new SearchActivityProcessResultFromAddressResolution(mContext, resultionResult));
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };


        MapEventsOverlay overlayEvents = new MapEventsOverlay(mReceive);
        map.getOverlays().add(overlayEvents);

        initializeWeatherReceiver();
    }

    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mWeatherUpdateReceiver,
                new IntentFilter(ACTION_ADDRESS_RESOLUTION_RESULT));
        map.onResume();
    }

    public void onPause(){
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWeatherUpdateReceiver);
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void addToLocations(View arg0) {
        storeLocation();
        finish();
    }

    private void storeLocation() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);

        int currentMaxOrderId = locationsDbHelper.getMaxOrderId();
        SQLiteDatabase db = locationsDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS, locationsDbHelper.getAddressAsBytes(address));
        values.put(LocationsContract.Locations.COLUMN_NAME_LONGITUDE, longitude);
        values.put(LocationsContract.Locations.COLUMN_NAME_LATITUDE, latitude);
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCALE, locale);
        values.put(LocationsContract.Locations.COLUMN_NAME_ORDER_ID, currentMaxOrderId + 1);
        values.put(LocationsContract.Locations.COLUMN_NAME_LOCATION_UPDATE_SOURCE, "");
        values.put(LocationsContract.Locations.COLUMN_NAME_ADDRESS_FOUND, 1);

        long newLocationRowId = db.insert(LocationsContract.Locations.TABLE_NAME, null, values);
    }
}
