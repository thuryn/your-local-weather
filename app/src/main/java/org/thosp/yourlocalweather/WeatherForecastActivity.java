package org.thosp.yourlocalweather;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.thosp.yourlocalweather.adapter.WeatherForecastAdapter;
import org.thosp.yourlocalweather.databinding.ActivityWeatherForecastBinding;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.AppPreference.getForecastMinMaxOnly;
import static org.thosp.yourlocalweather.utils.AppPreference.setForecastMinMaxOnly;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastActivity extends ForecastingActivity {

    private final String TAG = "WeatherForecastActivity";

    private volatile boolean inited;

    // 1. Deklarujeme instanční proměnnou bindingu
    private ActivityWeatherForecastBinding binding;
    private Set<Integer> visibleColumns = new HashSet<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWeatherForecastReceiver(UpdateWeatherService.ACTION_FORECAST_UPDATE_RESULT);

        // 2. Inicializujeme View Binding a nastavíme root view
        binding = ActivityWeatherForecastBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 3. Přístup k RecyclerView a naplnění proměnných z parent třídy přes binding
        binding.forecastRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        localityView = binding.forecastLocality;
        switchLocationButton = binding.forecastSwitchLocation;

        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(), "fonts/Roboto-Light.ttf");
        localityView.setTypeface(robotoLight);

        appendLog(getBaseContext(), TAG, "Start processing forecast");

        final Context appContext = this.getApplicationContext();
        final java.lang.ref.WeakReference<WeatherForecastActivity> activityRef = new java.lang.ref.WeakReference<>(this);

        YourLocalWeather.executor.submit(() -> {
            WeatherForecastActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            Set<Integer> cols = AppPreference.getInstance().getForecastActivityColumns(appContext);
            ConnectionDetector detector = new ConnectionDetector(appContext);
            LocationsDbHelper dbHelper = LocationsDbHelper.getInstance(appContext);
            String pressUnit = AppPreference.getPressureUnitFromPreferences(appContext);
            String rainSnowUnit = AppPreference.getRainSnowUnitFromPreferences(appContext);
            String tempUnit = AppPreference.getTemperatureUnitFromPreferences(appContext);

            activity.runOnUiThread(() -> {
                WeatherForecastActivity act = activityRef.get();
                if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                    act.visibleColumns = cols;
                    act.connectionDetector = detector;
                    act.locationsDbHelper = dbHelper;
                    act.pressureUnitFromPreferences = pressUnit;
                    act.rainSnowUnitFromPreferences = rainSnowUnit;
                    act.temperatureUnitFromPreferences = tempUnit;

                    act.updateUI();
                    act.inited = true;
                }
            });
        });

        binding.forecastRecyclerView.setOnTouchListener(new ActivityTransitionTouchListener(
                MainActivity.class,
                GraphsActivity.class, this));
        appendLog(getBaseContext(), TAG, "Finished processing forecast");
    }

    @Override
    protected void updateUI() {
        // Pojistka pro případ, že asynchronní exekutor zavolá updateUI po onDestroy()
        if (binding == null) {
            return;
        }

        appendLog(getBaseContext(), TAG, "UpdateUI start processing");
        int maxOrderId = locationsDbHelper.getMaxOrderId();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;

                // Použití bindingu namísto findViewById v celém UI threadu
                binding.forecastSwitchPanel.setVisibility(View.INVISIBLE);
                if ((maxOrderId > 1) ||
                        ((maxOrderId == 1) && (locationsDbHelper.getLocationByOrderId(0).isEnabled()))) {
                    switchLocationButton.setVisibility(View.VISIBLE);
                } else {
                    switchLocationButton.setVisibility(View.GONE);
                }
            }
        });

        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        long locationId = AppPreference.getCurrentLocationId(WeatherForecastActivity.this);
        currentLocation = locationsDbHelper.getLocationById(locationId);
        if (currentLocation == null) {
            return;
        }
        appendLog(getBaseContext(), TAG, "locationId:", locationId, "currentLocation:", currentLocation.getOrderId());
        appendLog(getBaseContext(), TAG, "updateUI with forecastType:", UpdateWeatherService.WEATHER_FORECAST_TYPE);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId, UpdateWeatherService.WEATHER_FORECAST_TYPE);
        appendLog(getBaseContext(), TAG, "Weather forecast record: ", weatherForecastRecord);
        if (weatherForecastRecord != null) {
            weatherForecastList.put(locationId, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList());
            locationWeatherForecastLastUpdate.put(locationId, weatherForecastRecord.getLastUpdatedTime());
        } else {
            sendMessageToCurrentWeatherService(currentLocation, "FORECAST");
            return;
        }

        String cityAndCountry = Utils.getCityAndCountry(this, currentLocation);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (localityView != null) {
                    localityView.setText(cityAndCountry);
                }
            }
        });

        if (weatherForecastList.get(locationId) == null) {
            return;
        }

        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(this);
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
        String timeStylePreference = AppPreference.getTimeStylePreference(this);
        boolean showMinMaxOnly = getForecastMinMaxOnly(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;

                if (weatherForecastList.isEmpty()) {
                    binding.forecastRecyclerView.setVisibility(View.INVISIBLE);
                    binding.android.setVisibility(View.VISIBLE);
                } else {
                    binding.forecastRecyclerView.setVisibility(View.VISIBLE);
                    binding.android.setVisibility(View.GONE);
                }
                appendLog(getBaseContext(), TAG, "UpdateUI create adapter processing");
                WeatherForecastAdapter adapter = new WeatherForecastAdapter(WeatherForecastActivity.this,
                        weatherForecastList.get(locationId),
                        currentLocation.getLatitude(),
                        currentLocation.getLocale(),
                        pressureUnitFromPreferences,
                        rainSnowUnitFromPreferences,
                        windUnitFromPreferences,
                        temperatureUnitFromPreferences,
                        timeStylePreference,
                        visibleColumns,
                        showMinMaxOnly);
                binding.forecastRecyclerView.setAdapter(adapter);
            }
        });
        appendLog(getBaseContext(), TAG, "UpdateUI finished processing");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(
                this,
                mWeatherUpdateReceiver,
                new IntentFilter(UpdateWeatherService.ACTION_FORECAST_UPDATE_RESULT),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        if (inited) {
            YourLocalWeather.executor.submit(() -> {
                updateUI();
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.weather_forecast_menu, menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_forecast_min_max);
        boolean showOnlyMinMax = getForecastMinMaxOnly(this);
        toggleItem.setChecked(showOnlyMinMax);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_forecast_refresh:
                sendMessageToCurrentWeatherService(currentLocation, "FORECAST");
                return true;
            case R.id.menu_forecast_settings:
                showSettings();
                return true;
            case R.id.menu_forecast_min_max:
                processMinMaxSettings(item);
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processMinMaxSettings(MenuItem item) {
        boolean newState = !item.isChecked();
        item.setChecked(newState);
        setForecastMinMaxOnly(this, newState);

        YourLocalWeather.executor.submit(() -> {
            updateUI();
        });
    }

    private void showSettings() {
        final Set<Integer> mSelectedItems = new HashSet<>();
        boolean[] checkedItems = new boolean[9];
        for (Integer visibleColumn: visibleColumns) {
            if (visibleColumn == 1) {
                continue;
            }
            mSelectedItems.add(visibleColumn - 2);
            checkedItems[visibleColumn - 2] = true;
        }
        final Context context = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.forecast_settings_columns)
                .setMultiChoiceItems(R.array.pref_forecast_columns, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    mSelectedItems.add(which);
                                } else mSelectedItems.remove(which);
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        visibleColumns = new HashSet<>();
                        visibleColumns.add(1);
                        for (Integer selectedItem: mSelectedItems) {
                            visibleColumns.add(selectedItem + 2);
                        }
                        YourLocalWeather.executor.submit(() -> {
                            AppPreference.getInstance().setForecastActivityColumns(context, visibleColumns);
                            updateUI();
                        });
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}