package org.thosp.yourlocalweather;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.ReconciliationDbService;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.List;

public class LocationsActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    private LocationsAdapter locationsAdapter;
    private LocationsDbHelper locationsDbHelper;
    private RecyclerView recyclerView;
    private FloatingActionButton addLocationButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        setContentView(R.layout.activity_locations);

        setupActionBar();
        setupRecyclerView();
        locationsDbHelper = LocationsDbHelper.getInstance(this);
        addLocationButton = (FloatingActionButton) findViewById(R.id.add_location);
    }

    public void onResume(){
        super.onResume();
        List<Location> allLocations = locationsDbHelper.getAllRows();
        if (allLocations.size() > 3) {
            addLocationButton.setVisibility(View.GONE);
        } else {
            addLocationButton.setVisibility(View.VISIBLE);
        }
        locationsAdapter = new LocationsAdapter(allLocations);
        recyclerView.setAdapter(locationsAdapter);
    }

    public void addLocation(View view) {
        Intent intent = new Intent(LocationsActivity.this, SearchActivity.class);
        startActivity(intent);
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.search_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(org.thosp.yourlocalweather.LocationsActivity.this));

        final LocationsSwipeController swipeController = new LocationsSwipeController(new LocationsSwipeControllerActions() {
            @Override
            public void onRightClicked(int position) {
                if (position == 0) {
                    disableEnableLocation();
                } else {
                    deleteLocation(position);
                }
            }
        }, this);

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);

        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });

        itemTouchhelper.attachToRecyclerView(recyclerView);
    }

    private void disableEnableLocation() {
        Location location = locationsAdapter.locations.get(0);
        locationsDbHelper.updateEnabled(location.getId(), !location.isEnabled());
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        startService(intentToStartUpdate);
        List<Location> allLocations = locationsDbHelper.getAllRows();
        locationsAdapter = new LocationsAdapter(allLocations);
        recyclerView.setAdapter(locationsAdapter);
        WidgetUtils.startBackgroundService(
                getBaseContext(),
                new Intent(getBaseContext(), ReconciliationDbService.class));
    }

    private void deleteLocation(int position) {
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(org.thosp.yourlocalweather.LocationsActivity.this);
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(org.thosp.yourlocalweather.LocationsActivity.this);
        Location location = locationsAdapter.locations.get(position);
        int locatonOrder = location.getOrderId();
        currentWeatherDbHelper.deleteRecordByLocation(location);
        weatherForecastDbHelper.deleteRecordByLocation(location);
        locationsDbHelper.deleteRecordFromTable(location);

        if (locatonOrder == 1) {
            Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
            intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
            this.startService(intentToStartUpdate);
        }

        locationsAdapter.locations.remove(position);
        locationsAdapter.notifyItemRemoved(position);
        locationsAdapter.notifyItemRangeChanged(position, locationsAdapter.getItemCount());
        List<Location> allLocations = locationsDbHelper.getAllRows();
        if (allLocations.size() > 3) {
            addLocationButton.setVisibility(View.GONE);
        } else {
            addLocationButton.setVisibility(View.VISIBLE);
        }
        locationsAdapter = new LocationsAdapter(allLocations);
        recyclerView.setAdapter(locationsAdapter);
        Intent reconciliationService = new Intent(this, ReconciliationDbService.class);
        reconciliationService.putExtra("force", true);
        WidgetUtils.startBackgroundService(
                this,
                reconciliationService);
    }

    public class LocationHolder extends RecyclerView.ViewHolder {

        private Location location;
        private TextView mCityName;
        private TextView mCountryName;

        LocationHolder(View itemView) {
            super(itemView);
            mCityName = (TextView) itemView.findViewById(R.id.city_name);
            mCountryName = (TextView) itemView.findViewById(R.id.country_code);
        }

        void bindLocation(Location location) {
            this.location = location;
            if (location == null) {
                return;
            }
            String orderId = new Integer(location.getOrderId()).toString();
            mCityName.setText(orderId + getLocationNickname(getBaseContext(), location));
            if (location.getAddress() != null) {
                mCountryName.setText(Utils.getCityAndCountryFromAddress(location.getAddress()));
            }
        }

        public Location getLocation() {
            return location;
        }
    }

    private String getLocationNickname(Context context, Location location) {
        String locationNickname = location.getNickname();
        if ((locationNickname == null) || "".equals(locationNickname)) {
            if (location.getOrderId() == 0) {
                if (!location.isEnabled()) {
                    return " - " + context.getString(R.string.locations_disabled);
                } else {
                    return " - " + context.getString(R.string.locations_automatically_discovered);
                }
            } else {
                return "";
            }
        }
        return " - " + locationNickname;
    }

    private class LocationsAdapter extends RecyclerView.Adapter<LocationHolder> {

        private List<Location> locations;

        LocationsAdapter(List<Location> locations) {
            this.locations = locations;
        }

        @Override
        public int getItemCount() {
            if (locations != null)
                return locations.size();

            return 0;
        }

        @Override
        public void onBindViewHolder(LocationHolder locationHolder, int position) {
            locationHolder.bindLocation(locations.get(position));
        }

        @Override
        public LocationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(org.thosp.yourlocalweather.LocationsActivity.this);
            View v = inflater.inflate(R.layout.city_item, parent, false);
            return new LocationHolder(v);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

