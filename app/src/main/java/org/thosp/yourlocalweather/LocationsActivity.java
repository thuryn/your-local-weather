package org.thosp.yourlocalweather;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.List;

public class LocationsActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    private LocationsAdapter locationsAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton addLocationButton;
    private boolean addLocationDisabled;

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
        addLocationButton = findViewById(R.id.add_location);
    }

    @Override
    public void onResume(){
        super.onResume();
        List<Location> allLocations = locationsDbHelper.getAllRows();
        updateAddLocationButton(allLocations);
        locationsAdapter = new LocationsAdapter(allLocations);
        recyclerView.setAdapter(locationsAdapter);
    }

    public void addLocation(View view) {
        if (addLocationDisabled) {
            notifyUserAboutMaxAllowedLocations();
        } else {
            Intent intent = new Intent(LocationsActivity.this, SearchActivity.class);
            startActivity(intent);
        }
    }

    private void notifyUserAboutMaxAllowedLocations() {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.snackbar_add_location_disabled, ApiKeys.getAvailableLocations(this)),
                Snackbar.LENGTH_LONG).show();
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.search_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(org.thosp.yourlocalweather.LocationsActivity.this));

        final LocationsSwipeController swipeController = new LocationsSwipeController(new LocationsSwipeControllerActions() {
            @Override
            public void onRightClicked(int position) {
                if (position == 0) {
                    Location location = locationsAdapter.locations.get(0);
                    if (addLocationDisabled && !location.isEnabled()) {
                        notifyUserAboutMaxAllowedLocations();
                    } else {
                        disableEnableLocation();
                    }
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
        updateAddLocationButton(allLocations);
        locationsAdapter = new LocationsAdapter(allLocations);
        recyclerView.setAdapter(locationsAdapter);
        sendMessageToReconciliationDbService(true);
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
        updateAddLocationButton(allLocations);
        locationsAdapter = new LocationsAdapter(allLocations);
        recyclerView.setAdapter(locationsAdapter);
        sendMessageToReconciliationDbService(true);
    }

    private void updateAddLocationButton(List<Location> allLocations) {
        int locationsCount = allLocations.size();
        if (!allLocations.get(0).isEnabled()) {
            locationsCount--;
        }
        if (locationsCount >= ApiKeys.getAvailableLocations(this)) {
            addLocationButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            addLocationDisabled = true;
        } else {
            addLocationButton.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(0xde, 0x44, 0x4e)));
            addLocationDisabled = false;
        }
    }

    @Override
    protected void updateUI() {
    }

    public class LocationHolder extends RecyclerView.ViewHolder {

        private Location location;
        private final TextView mCityName;
        private final TextView mCountryName;

        LocationHolder(View itemView) {
            super(itemView);
            mCityName = itemView.findViewById(R.id.city_name);
            mCountryName = itemView.findViewById(R.id.country_code);
        }

        void bindLocation(Location location) {
            this.location = location;
            if (location == null) {
                return;
            }
            String orderId = Integer.toString(location.getOrderId());
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

        private final List<Location> locations;

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

