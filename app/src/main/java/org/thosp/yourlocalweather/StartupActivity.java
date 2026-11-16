package org.thosp.yourlocalweather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.thosp.yourlocalweather.databinding.ActivityStartupBinding;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.Constants;

public class StartupActivity extends BaseActivity {

    private ActivityStartupBinding binding;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                updateButtonsState();
                triggerInitialLocationFetch();
            });

    private final ActivityResultLauncher<String> bgLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                updateButtonsState();
                triggerInitialLocationFetch();
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> updateButtonsState());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        updateButtonsState();

        binding.startupLocationButton.setOnClickListener(v -> requestForegroundLocationPermission());
        binding.startupBgLocationButton.setOnClickListener(v -> requestBackgroundLocationPermission());
        binding.startupNotificationButton.setOnClickListener(v -> requestNotificationPermission());
        binding.startupBatteryButton.setOnClickListener(v -> requestBatteryOptimizationExemption());
        binding.startupFinishButton.setOnClickListener(v -> finishStartup());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateButtonsState();
        triggerInitialLocationFetch();
    }

    private void triggerInitialLocationFetch() {
        boolean hasForeground = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (hasForeground) {
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
            Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
            if (autoLocation != null) {
                locationsDbHelper.updateEnabled(autoLocation.getId(), true);
                Intent startLocationUpdateIntent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
                startLocationUpdateIntent.putExtra("updateSource", "MAIN");
                startLocationUpdateIntent.putExtra("locationId", autoLocation.getId());
                org.thosp.yourlocalweather.utils.LocationServiceDispatcher.startLocationAndWeatherUpdate(this, startLocationUpdateIntent);
            }
        }
    }

    private void requestForegroundLocationPermission() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void requestBackgroundLocationPermission() {
        boolean hasForeground = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasForeground) {
            Toast.makeText(this, R.string.startup_grant_foreground_first, Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            Toast.makeText(this, R.string.startup_notifications_granted, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
        }
    }

    private void updateButtonsState() {
        if (binding == null) return;

        boolean hasForeground = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean hasBg = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isIgnoringBattery = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName()));

        if (hasForeground) {
            binding.startupLocationButton.setText(R.string.startup_location_granted);
            binding.startupLocationButton.setEnabled(false);
        } else {
            binding.startupLocationButton.setText(R.string.startup_btn_location_fg);
            binding.startupLocationButton.setEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.startupBgLocationButton.setVisibility(View.VISIBLE);
            if (hasBg) {
                binding.startupBgLocationButton.setText(R.string.startup_bg_location_granted);
                binding.startupBgLocationButton.setEnabled(false);
            } else {
                binding.startupBgLocationButton.setText(R.string.startup_btn_location_bg);
                binding.startupBgLocationButton.setEnabled(true);
            }
        } else {
            binding.startupBgLocationButton.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.startupNotificationButton.setVisibility(View.VISIBLE);
            if (hasNotifications) {
                binding.startupNotificationButton.setText(R.string.startup_notifications_granted);
                binding.startupNotificationButton.setEnabled(false);
            } else {
                binding.startupNotificationButton.setText(R.string.startup_btn_notifications);
                binding.startupNotificationButton.setEnabled(true);
            }
        } else {
            binding.startupNotificationButton.setText(R.string.startup_notifications_granted);
            binding.startupNotificationButton.setEnabled(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.startupBatteryButton.setVisibility(View.VISIBLE);
            if (isIgnoringBattery) {
                binding.startupBatteryButton.setText(R.string.startup_battery_granted);
                binding.startupBatteryButton.setEnabled(false);
            } else {
                binding.startupBatteryButton.setText(R.string.startup_btn_battery);
                binding.startupBatteryButton.setEnabled(true);
            }
        } else {
            binding.startupBatteryButton.setVisibility(View.GONE);
        }

        // Highlight Finish Button in colorPrimary when all setup steps are completed, gray otherwise
        boolean allCompleted = hasForeground && hasBg && hasNotifications && isIgnoringBattery;
        if (allCompleted) {
            binding.startupFinishButton.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            binding.startupFinishButton.setTextColor(android.graphics.Color.WHITE);
        } else {
            binding.startupFinishButton.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
            binding.startupFinishButton.setTextColor(android.graphics.Color.parseColor("#333333"));
        }
        binding.startupFinishButton.setEnabled(true);
    }

    private void finishStartup() {
        SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(this).edit();
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (autoLocation != null) {
            locationsDbHelper.updateEnabled(autoLocation.getId(), true);
        }
        preferences.putBoolean(Constants.KEY_PREF_LOCATION_GPS_ENABLED, true);
        preferences.putString(Constants.KEY_WAKE_UP_STRATEGY, "wakeuppartial");
        preferences.putString(Constants.KEY_PREF_LOCATION_GEOCODER_SOURCE, "location_geocoder_local");
        preferences.putBoolean(Constants.APP_SETTINGS_LOCATION_CACHE_ENABLED, true);
        preferences.putInt(Constants.APP_INITIAL_GUIDE_VERSION, 7);
        preferences.apply();

        // Immediately trigger location detection & weather update if auto-location is enabled
        if (autoLocation != null) {
            Intent startLocationUpdateIntent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
            startLocationUpdateIntent.putExtra("updateSource", "MAIN");
            startLocationUpdateIntent.putExtra("locationId", autoLocation.getId());
            org.thosp.yourlocalweather.utils.LocationServiceDispatcher.startLocationAndWeatherUpdate(this, startLocationUpdateIntent);
        }

        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    @Override
    protected void updateUI() {
    }
}
