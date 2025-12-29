package org.thosp.yourlocalweather;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.adapter.WeatherForecastAdapter;
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

    private RecyclerView mRecyclerView;
    private Set<Integer> visibleColumns = new HashSet<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWeatherForecastReceiver(UpdateWeatherService.ACTION_FORECAST_UPDATE_RESULT);
        setContentView(R.layout.activity_weather_forecast);

        mRecyclerView = findViewById(R.id.forecast_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        localityView = findViewById(R.id.forecast_locality);
        switchLocationButton = findViewById(R.id.forecast_switch_location);
        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Light.ttf");
        localityView.setTypeface(robotoLight);

        appendLog(getBaseContext(), TAG, "Start processing forecast");
        YourLocalWeather.executor.submit(() -> {
                    visibleColumns = AppPreference.getInstance().getForecastActivityColumns(this);
                    connectionDetector = new ConnectionDetector(WeatherForecastActivity.this);
                    connectionDetector = new ConnectionDetector(this);
                    locationsDbHelper = LocationsDbHelper.getInstance(this);
                    pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(this);
                    rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(this);
                    temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
                    updateUI();
                    inited = true;
                });

        mRecyclerView.setOnTouchListener(new ActivityTransitionTouchListener(
                MainActivity.class,
                GraphsActivity.class, this));
        appendLog(getBaseContext(), TAG, "Finished processing forecast");
    }

    @Override
    protected void updateUI() {
        appendLog(getBaseContext(), TAG, "UpdateUI start processing");
        int maxOrderId = locationsDbHelper.getMaxOrderId();
        runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  View switchPanel = findViewById(R.id.forecast_switch_panel);
                  switchPanel.setVisibility(View.INVISIBLE);
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
                localityView.setText(cityAndCountry);
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
                ImageView android = findViewById(R.id.android);
                if (weatherForecastList.isEmpty()) {
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    android.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    android.setVisibility(View.GONE);
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
                mRecyclerView.setAdapter(adapter);
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
                                // Else, if the item is already in the array, remove it
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
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
}
