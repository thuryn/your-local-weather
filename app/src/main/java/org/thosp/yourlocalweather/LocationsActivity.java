package org.thosp.yourlocalweather;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.thosp.yourlocalweather.databinding.ActivityLocationsBinding;
import org.thosp.yourlocalweather.databinding.CityItemBinding;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.List;

public class LocationsActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    // 1. Deklarujeme hlavní binding pro layout aktivity
    private ActivityLocationsBinding binding;
    private LocationsAdapter locationsAdapter;
    private boolean addLocationDisabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // 2. Inicializujeme View Binding
        binding = ActivityLocationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActionBar();
        setupRecyclerView();
        locationsDbHelper = LocationsDbHelper.getInstance(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        List<Location> allLocations = locationsDbHelper.getAllRows();
        updateAddLocationButton(allLocations);
        locationsAdapter = new LocationsAdapter(allLocations);
        binding.searchRecyclerView.setAdapter(locationsAdapter);
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
        // Použijeme root view bindingu namísto hledání android.R.id.content
        Snackbar.make(
                binding.getRoot(),
                getString(R.string.snackbar_add_location_disabled, ApiKeys.getAvailableLocations(this)),
                Snackbar.LENGTH_LONG).show();
    }

    private void setupActionBar() {
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        binding.searchRecyclerView.setLayoutManager(new LinearLayoutManager(this));

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

        binding.searchRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });

        itemTouchhelper.attachToRecyclerView(binding.searchRecyclerView);
    }

    private void disableEnableLocation() {
        Location location = locationsAdapter.locations.get(0);
        locationsDbHelper.updateEnabled(location.getId(), !location.isEnabled());
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
        intentToStartUpdate.setPackage(getBaseContext().getPackageName());
        startService(intentToStartUpdate);
        List<Location> allLocations = locationsDbHelper.getAllRows();
        updateAddLocationButton(allLocations);
        locationsAdapter = new LocationsAdapter(allLocations);
        binding.searchRecyclerView.setAdapter(locationsAdapter);
        sendMessageToReconciliationDbService();
    }

    private void deleteLocation(int position) {
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        Location location = locationsAdapter.locations.get(position);
        int locatonOrder = location.getOrderId();
        currentWeatherDbHelper.deleteRecordByLocation(location);
        weatherForecastDbHelper.deleteRecordByLocation(location);
        locationsDbHelper.deleteRecordFromTable(location);

        if (locatonOrder == 1) {
            Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE");
            intentToStartUpdate.setPackage(getBaseContext().getPackageName());
            this.startService(intentToStartUpdate);
        }

        locationsAdapter.locations.remove(position);
        locationsAdapter.notifyItemRemoved(position);
        locationsAdapter.notifyItemRangeChanged(position, locationsAdapter.getItemCount());
        List<Location> allLocations = locationsDbHelper.getAllRows();
        updateAddLocationButton(allLocations);
        locationsAdapter = new LocationsAdapter(allLocations);
        binding.searchRecyclerView.setAdapter(locationsAdapter);
        sendMessageToReconciliationDbService();
    }

    private void updateAddLocationButton(List<Location> allLocations) {
        int locationsCount = allLocations.size();
        if (!allLocations.get(0).isEnabled()) {
            locationsCount--;
        }
        if (locationsCount >= ApiKeys.getAvailableLocations(this)) {
            binding.addLocation.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            addLocationDisabled = true;
        } else {
            binding.addLocation.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(0xde, 0x44, 0x4e)));
            addLocationDisabled = false;
        }
    }

    @Override
    protected void updateUI() {
    }

    // 3. Upravený Holder s využitím specifického CityItemBinding
    public class LocationHolder extends RecyclerView.ViewHolder {

        private Location location;
        private final CityItemBinding itemBinding;

        LocationHolder(CityItemBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        void bindLocation(Location location) {
            this.location = location;
            if (location == null) {
                return;
            }
            String orderId = Integer.toString(location.getOrderId());
            itemBinding.cityName.setText(orderId + getLocationNickname(getBaseContext(), location));
            if (location.getAddress() != null) {
                itemBinding.countryCode.setText(Utils.getCityAndCountryFromAddress(location.getAddress()));
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
            // Použijeme vygenerovaný layout binding pro položku seznamu
            CityItemBinding itemBinding = CityItemBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new LocationHolder(itemBinding);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}