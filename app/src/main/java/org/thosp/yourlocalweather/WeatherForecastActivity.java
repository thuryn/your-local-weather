package org.thosp.yourlocalweather;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.thosp.yourlocalweather.adapter.WeatherForecastAdapter;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastResultHandler;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.WeatherForecastUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastActivity extends ForecastingActivity {

    private final String TAG = "WeatherForecastActivity";

    public static long AUTO_FORECAST_UPDATE_TIME_MILIS = 3600000; // 1h

    private Map<Long, Long> locationWeatherForecastLastUpdate = new HashMap<>();
    private RecyclerView mRecyclerView;
    private Set<Integer> visibleColumns = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);

        locationsDbHelper = LocationsDbHelper.getInstance(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.forecast_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        visibleColumns = AppPreference.getForecastActivityColumns(this);
        updateUI();


        mRecyclerView.setOnTouchListener(new ActivityTransitionTouchListener(
                MainActivity.class,
                GraphsActivity.class, this));
    }

    protected void updateUI() {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        long locationId = AppPreference.getCurrentLocationId(this);
        Location location = locationsDbHelper.getLocationById(locationId);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId);
        if (weatherForecastRecord != null) {
            weatherForecastList.put(locationId, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList());
            locationWeatherForecastLastUpdate.put(locationId, weatherForecastRecord.getLastUpdatedTime());
        }
        if ((weatherForecastRecord == null) || (locationWeatherForecastLastUpdate.get(locationId) + AUTO_FORECAST_UPDATE_TIME_MILIS) <  Calendar.getInstance().getTimeInMillis()) {
            updateWeatherForecastFromNetwork();
        }

        ImageView android = (ImageView) findViewById(R.id.android);
        if (weatherForecastList.isEmpty()) {
            mRecyclerView.setVisibility(View.INVISIBLE);
            android.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            android.setVisibility(View.GONE);
        }
        WeatherForecastAdapter adapter = new WeatherForecastAdapter(this,
                                                                    weatherForecastList.get(locationId),
                                                                    location.getLatitude(),
                                                                    visibleColumns);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
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
                updateWeatherForecastFromNetwork();
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
        boolean[] checkedItems = new boolean[8];
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
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which);
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                }
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
