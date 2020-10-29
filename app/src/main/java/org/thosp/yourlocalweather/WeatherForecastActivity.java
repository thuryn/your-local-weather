package org.thosp.yourlocalweather;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.thosp.yourlocalweather.adapter.LongWeatherForecastAdapter;
import org.thosp.yourlocalweather.adapter.WeatherForecastAdapter;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastActivity extends ForecastingActivity {

    private final String TAG = "WeatherForecastActivity";

    private RecyclerView mRecyclerView;
    private Set<Integer> visibleColumns = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWeatherForecastReceiver(UpdateWeatherService.ACTION_FORECAST_UPDATE_RESULT);
        setContentView(R.layout.activity_weather_forecast);

        mRecyclerView = findViewById(R.id.forecast_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        localityView = findViewById(R.id.forecast_locality);
        visibleColumns = AppPreference.getForecastActivityColumns(this);
        connectionDetector = new ConnectionDetector(WeatherForecastActivity.this);
        forecastType = findViewById(R.id.forecast_forecastType);

        forecastType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppPreference.setForecastType(getBaseContext(), isChecked ? 2 : 1);
                updateUI();
            }
        });

        updateUI();

        mRecyclerView.setOnTouchListener(new ActivityTransitionTouchListener(
                MainActivity.class,
                GraphsActivity.class, this));
    }

    @Override
    protected void updateUI() {
        View switchPanel = findViewById(R.id.forecast_switch_panel);
        if (ApiKeys.isWeatherForecastFeaturesFree(this)) {
            switchPanel.setVisibility(View.INVISIBLE);
        } else {
            switchPanel.setVisibility(View.VISIBLE);
        }

        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        long locationId = AppPreference.getCurrentLocationId(this);
        currentLocation = locationsDbHelper.getLocationById(locationId);
        if (currentLocation == null) {
            return;
        }
        forecastType.setChecked(2 == AppPreference.getForecastType(getBaseContext()));
        int forecastTypeRecord = (forecastType.isChecked() ? 2 : 1);
        appendLog(getBaseContext(), TAG, "updateUI with forecastType:", forecastTypeRecord);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId, forecastTypeRecord);
        appendLog(getBaseContext(), TAG, "Weather forecast record: ", weatherForecastRecord, ", forecastType: ", forecastType);
        if (weatherForecastRecord != null) {
            weatherForecastList.put(locationId, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList());
            locationWeatherForecastLastUpdate.put(locationId, weatherForecastRecord.getLastUpdatedTime());
        } else if (ForecastUtil.shouldUpdateForecast(this, locationId,
                forecastType.isChecked() ? UpdateWeatherService.LONG_WEATHER_FORECAST_TYPE : UpdateWeatherService.WEATHER_FORECAST_TYPE)) {
            if (forecastType.isChecked()) {
                updateLongWeatherForecastFromNetwork("FORECAST");
            } else {
                updateWeatherForecastFromNetwork("FORECAST");
            }
            return;
        }

        localityView.setText(Utils.getCityAndCountry(this, currentLocation.getOrderId()));

        if (weatherForecastList.get(locationId) == null) {
            return;
        }

        ImageView android = findViewById(R.id.android);
        if (weatherForecastList.isEmpty()) {
            mRecyclerView.setVisibility(View.INVISIBLE);
            android.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            android.setVisibility(View.GONE);
        }
        if (forecastType.isChecked()) {
            LongWeatherForecastAdapter adapter = new LongWeatherForecastAdapter(this,
                    weatherForecastList.get(locationId),
                    currentLocation.getLatitude(),
                    currentLocation.getLocale(),
                    visibleColumns);
            mRecyclerView.setAdapter(adapter);
        } else {
            WeatherForecastAdapter adapter = new WeatherForecastAdapter(this,
                    weatherForecastList.get(locationId),
                    currentLocation.getLatitude(),
                    currentLocation.getLocale(),
                    visibleColumns);
            mRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWeatherUpdateReceiver,
                new IntentFilter(
                        UpdateWeatherService.ACTION_FORECAST_UPDATE_RESULT));
        updateUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.weather_forecast_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_forecast_refresh:
                if (forecastType.isChecked()) {
                    updateLongWeatherForecastFromNetwork("FORECAST");
                } else {
                    updateWeatherForecastFromNetwork("FORECAST");
                }
                return true;
            case R.id.menu_forecast_settings:
                showSettings();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                        AppPreference.setForecastActivityColumns(context, visibleColumns);
                        updateUI();
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
