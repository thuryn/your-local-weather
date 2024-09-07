package org.thosp.yourlocalweather;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;
import androidx.core.app.NavUtils;
import androidx.core.widget.NestedScrollView;

import org.thosp.charting.charts.BarChart;
import org.thosp.charting.charts.CombinedChart;
import org.thosp.charting.charts.LineChart;
import org.thosp.charting.components.Description;
import org.thosp.charting.components.YAxis;
import org.thosp.charting.data.BarData;
import org.thosp.charting.data.BarDataSet;
import org.thosp.charting.data.BarEntry;
import org.thosp.charting.data.Entry;
import org.thosp.charting.data.LineData;
import org.thosp.charting.data.LineDataSet;
import org.thosp.charting.interfaces.datasets.IDataSet;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.settings.GraphValuesSwitchListener;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.CustomValueFormatter;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.RainSnowYAxisValueFormatter;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.YAxisValueFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class GraphsActivity extends ForecastingActivity {

    private static final String TAG = "GraphsActivity";

    private volatile boolean inited;

    private CombinedChart combinedChart;
    private CardView combinedChartCard;
    private LineChart mTemperatureChart;
    private CardView temperatureChartCard;
    private LineChart mWindChart;
    private CardView windChartCard;
    private LineChart mRainChart;
    private CardView rainChartCard;
    private LineChart mSnowChart;
    private CardView snowChartCard;
    private LineChart mPressureChart;
    private CardView pressureChartCard;
    private BarChart rainBarChart;
    private CardView rainBarCard;
    private BarChart snowBarChart;
    private CardView snowBarCard;
    private CustomValueFormatter mValueFormatter;
    private RainSnowYAxisValueFormatter rainSnowYAxisValueFormatter;
    private Set<Integer> visibleGraphs = new HashSet<>();
    private Set<Integer> combinedGraphValues = new HashSet<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWeatherForecastReceiver(UpdateWeatherService.ACTION_GRAPHS_UPDATE_RESULT);
        setContentView(R.layout.activity_graphs);
        localityView = findViewById(R.id.graph_locality);
        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Light.ttf");
        localityView.setTypeface(robotoLight);

        combinedChart = findViewById(R.id.combined_chart);
        combinedChartCard = findViewById(R.id.combined_chart_card);
        mTemperatureChart = findViewById(R.id.temperature_chart);
        temperatureChartCard = findViewById(R.id.temperature_chart_card);
        mWindChart = findViewById(R.id.wind_chart);
        windChartCard = findViewById(R.id.wind_chart_card);
        mRainChart = findViewById(R.id.rain_chart);
        rainChartCard = findViewById(R.id.rain_chart_card);
        mSnowChart = findViewById(R.id.snow_chart);
        snowChartCard = findViewById(R.id.snow_chart_card);
        mPressureChart = findViewById(R.id.pressure_chart);
        pressureChartCard = findViewById(R.id.pressure_chart_card);
        rainBarChart = findViewById(R.id.bar_rain_chart);
        rainBarCard = findViewById(R.id.rain_bar_chart_card);
        snowBarChart = findViewById(R.id.bar_snow_chart);
        snowBarCard = findViewById(R.id.snow_bar_chart_card);
        switchLocationButton = findViewById(R.id.graph_switch_location);
        YourLocalWeather.executor.submit(() -> {
                    connectionDetector = new ConnectionDetector(this);
                    locationsDbHelper = LocationsDbHelper.getInstance(this);
                    pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(this);
                    rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(this);
                    temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
                    TextView temperatureLabel = findViewById(R.id.graphs_temperature_label);
                    temperatureLabel.setText(getString(R.string.label_temperature) +
                            ", " +
                            TemperatureUtil.getTemperatureUnit(this, temperatureUnitFromPreferences));
                    TextView windLabel = findViewById(R.id.graphs_wind_label);
                    windLabel.setText(getString(R.string.label_wind) + ", " + AppPreference.getWindUnit(this, AppPreference.getWindUnitFromPreferences(this)));
                    TextView rainLabel = findViewById(R.id.graphs_rain_label);
                    rainLabel.setText(getString(R.string.label_rain) + ", " + getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences)));
                    TextView snowLabel = findViewById(R.id.graphs_snow_label);
                    snowLabel.setText(getString(R.string.label_snow) + ", " + getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences)));
                    TextView rainBarLabel = findViewById(R.id.graphs_bar_rain_label);
                    rainBarLabel.setText(getString(R.string.label_rain) + ", " + getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences)));
                    TextView snowBarLabel = findViewById(R.id.graphs_bar_snow_label);
                    snowBarLabel.setText(getString(R.string.label_snow) + ", " + getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences)));
                    TextView pressureLabel = findViewById(R.id.graphs_pressure_label);
                    pressureLabel.setText(getString(R.string.label_pressure) + ", " + AppPreference.getPressureUnit(this, pressureUnitFromPreferences));
                    visibleGraphs = AppPreference.getGraphsActivityVisibleGraphs(this);
                    combinedGraphValues = AppPreference.getCombinedGraphValues(this);
                    //forecastType = (Switch) findViewById(R.id.forecast_forecastType);

            /*forecastType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppPreference.setForecastType(getBaseContext(), isChecked ? 2 : 1);
                    updateUI();
                }
            });*/
                    updateUI();
                    inited = true;
                });
        NestedScrollView mRecyclerView = findViewById(R.id.graph_scroll_view);
        mRecyclerView.setOnTouchListener(new ActivityTransitionTouchListener(
                WeatherForecastActivity.class,
                null, this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mWeatherUpdateReceiver,
                    new IntentFilter(
                            UpdateWeatherService.ACTION_GRAPHS_UPDATE_RESULT), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mWeatherUpdateReceiver,
                    new IntentFilter(
                            UpdateWeatherService.ACTION_GRAPHS_UPDATE_RESULT));
        }
        if (inited) {
            YourLocalWeather.executor.submit(() -> {
                updateUI();
            });
        }
    }

    private void setCombinedChart(long locationId, Locale locale) {
        if (!visibleGraphs.contains(0)) {
            combinedChartCard.setVisibility(View.GONE);
            return;
        } else {
            combinedChartCard.setVisibility(View.VISIBLE);
        }
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
        String pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(this);
        String rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(this);
        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(this);
        GraphUtils.generateCombinedGraph(this,
                                        combinedChart,
                                        AppPreference.getCombinedGraphValues(this),
                                        weatherForecastList.get(locationId),
                                        locale,
                                        null,
                                        8,
                                        2,
                                        PreferenceUtil.getTextColor(this),
                                        PreferenceUtil.getBackgroundColor(this),
                                        PreferenceUtil.getGraphGridColor(this),
                            true,
                                        temperatureUnitFromPreferences,
                                        pressureUnitFromPreferences,
                                        rainSnowUnitFromPreferences,
                                        windUnitFromPreferences
        );
    }

    private void setTemperatureChart(long locationId, Locale locale) {
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
        if (!visibleGraphs.contains(1)) {
            temperatureChartCard.setVisibility(View.GONE);
            return;
        } else {
            temperatureChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        mTemperatureChart.setDescription(graphDescription);
        mTemperatureChart.setDrawGridBackground(false);
        mTemperatureChart.setTouchEnabled(true);
        mTemperatureChart.setDragEnabled(true);
        mTemperatureChart.setMaxHighlightDistance(300);
        mTemperatureChart.setPinchZoom(true);
        mTemperatureChart.getLegend().setEnabled(false);
        mTemperatureChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        mTemperatureChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                mTemperatureChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = mTemperatureChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(new YAxisValueFormatter(locale, 2, TemperatureUtil.getTemperatureUnit(this, temperatureUnitFromPreferences)));

        mTemperatureChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {

            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }

            double temperatureDay = TemperatureUtil.getTemperature(this, temperatureUnitFromPreferences, detailedWeatherForecast);
            entries.add(new Entry(detailedWeatherForecast.getDateTime(), (float) temperatureDay));
        }

        LineDataSet set;
        if (mTemperatureChart.getData() != null) {
            mTemperatureChart.getData().removeDataSet(mTemperatureChart.getData().getDataSetByIndex(
                    mTemperatureChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, getString(R.string.graph_temperature_day_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setDrawValues(false);
            set.setValueTextSize(12f);
            List<Integer> temperatureColors = new ArrayList<>();
            temperatureColors.add(Color.RED);
            temperatureColors.add(Color.BLUE);
            set.setColors(temperatureColors);
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mTemperatureChart.setData(data);
        } else {
            set = new LineDataSet(entries, getString(R.string.graph_temperature_day_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#E84E40"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mTemperatureChart.setData(data);
        }
        mTemperatureChart.invalidate();
    }
    
    private void setWindChart(long locationId, String windUnitFromPreferences, Locale locale) {
        if (!visibleGraphs.contains(2)) {
            windChartCard.setVisibility(View.GONE);
            return;
        } else {
            windChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        mWindChart.setDescription(graphDescription);
        mWindChart.setDrawGridBackground(false);
        mWindChart.setTouchEnabled(true);
        mWindChart.setDragEnabled(true);
        mWindChart.setMaxHighlightDistance(300);
        mWindChart.setPinchZoom(true);
        mWindChart.getLegend().setEnabled(false);
        mWindChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        mWindChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                mWindChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = mWindChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(new YAxisValueFormatter(locale, 2, AppPreference.getWindUnit(this, windUnitFromPreferences)));
        yLeft.setZeroLineWidth(10f);

        mWindChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }

            double wind = AppPreference.getWind(windUnitFromPreferences, detailedWeatherForecast.getWindSpeed());
            entries.add(new Entry(detailedWeatherForecast.getDateTime(), (float) wind));
        }

        LineDataSet set;
        if (mWindChart.getData() != null) {
            mWindChart.getData().removeDataSet(mWindChart.getData().getDataSetByIndex(
                    mWindChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, getString(R.string.graph_wind_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#00BCD4"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mWindChart.setData(data);
        } else {
            set = new LineDataSet(entries, getString(R.string.graph_wind_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#00BCD4"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mWindChart.setData(data);
        }
        mWindChart.invalidate();
    }

    private void setRainChart(long locationId, Locale locale) {
        if (!visibleGraphs.contains(3)) {
            rainChartCard.setVisibility(View.GONE);
            return;
        } else {
            rainChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        mRainChart.setDescription(graphDescription);
        mRainChart.setDrawGridBackground(false);
        mRainChart.setTouchEnabled(true);
        mRainChart.setDragEnabled(true);
        mRainChart.setMaxHighlightDistance(300);
        mRainChart.setPinchZoom(true);
        mRainChart.getLegend().setEnabled(false);
        mRainChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        mRainChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                mRainChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = mRainChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.setAxisMinimum(0f);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(rainSnowYAxisValueFormatter);

        mRainChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }
            entries.add(new Entry(
                    detailedWeatherForecast.getDateTime(),
                    (float) AppPreference.getRainOrSnow(
                            rainSnowUnitFromPreferences,
                            detailedWeatherForecast.getRain())
                    )
            );
        }

        LineDataSet set;
        if (mRainChart.getData() != null) {
            mRainChart.getData().removeDataSet(mRainChart.getData().getDataSetByIndex(
                    mRainChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, getString(R.string.graph_rain_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#5677FC"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mRainChart.setData(data);
        } else {
            set = new LineDataSet(entries, getString(R.string.graph_rain_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#5677FC"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mRainChart.setData(data);
        }
        mRainChart.invalidate();
    }

    private void setRainBarChart(long locationId, Locale locale) {
        if (!visibleGraphs.contains(4)) {
            rainBarCard.setVisibility(View.GONE);
            return;
        } else {
            rainBarCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        rainBarChart.setDescription(graphDescription);
        rainBarChart.setDrawGridBackground(false);
        rainBarChart.setTouchEnabled(true);
        rainBarChart.setDragEnabled(true);
        rainBarChart.setMaxHighlightDistance(300);
        rainBarChart.setPinchZoom(true);
        rainBarChart.getLegend().setEnabled(false);
        rainBarChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        rainBarChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                rainBarChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = rainBarChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.setAxisMinimum(0f);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(rainSnowYAxisValueFormatter);

        rainBarChart.getAxisRight().setEnabled(false);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }
            entries.add(new BarEntry(
                    detailedWeatherForecast.getDateTime(),
                            (float) AppPreference.getRainOrSnow(
                                    rainSnowUnitFromPreferences,
                                    detailedWeatherForecast.getRain())
                    )
            );
        }

        BarDataSet set;
        if (rainBarChart.getData() != null) {
            rainBarChart.getData().removeDataSet(rainBarChart.getData().getDataSetByIndex(
                    rainBarChart.getData().getDataSetCount() - 1));
            set = new BarDataSet(entries, getString(R.string.graph_rain_label));
            set.setValueTextSize(0f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#5677FC"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            BarData data = new BarData(set);
            data.setBarWidth(8000f);
            rainBarChart.setData(data);
        } else {
            set = new BarDataSet(entries, getString(R.string.graph_rain_label));
            set.setValueTextSize(0f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#5677FC"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            BarData data = new BarData(set);
            data.setBarWidth(8000f);
            rainBarChart.setData(data);
        }
        rainBarChart.invalidate();
    }

    private void setSnowChart(long locationId, Locale locale) {
        if (!visibleGraphs.contains(5)) {
            snowChartCard.setVisibility(View.GONE);
            return;
        } else {
            snowChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        mSnowChart.setDescription(graphDescription);
        mSnowChart.setDrawGridBackground(false);
        mSnowChart.setTouchEnabled(true);
        mSnowChart.setDragEnabled(true);
        mSnowChart.setMaxHighlightDistance(300);
        mSnowChart.setPinchZoom(true);
        mSnowChart.getLegend().setEnabled(false);
        mSnowChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        mSnowChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                mSnowChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = mSnowChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.setAxisMinimum(0f);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(rainSnowYAxisValueFormatter);

        mSnowChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }
            entries.add(new Entry(
                    detailedWeatherForecast.getDateTime(),
                            (float) AppPreference.getRainOrSnow(
                                    rainSnowUnitFromPreferences,
                                    detailedWeatherForecast.getSnow())
                    )
            );
        }

        LineDataSet set;
        if (mSnowChart.getData() != null) {
            mSnowChart.getData().removeDataSet(mSnowChart.getData().getDataSetByIndex(
                    mSnowChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, getString(R.string.graph_snow_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#009688"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mSnowChart.setData(data);
        } else {
            set = new LineDataSet(entries, getString(R.string.graph_snow_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#009688"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mSnowChart.setData(data);
        }
        mSnowChart.invalidate();
    }

    private void setSnowBarChart(long locationId, Locale locale) {
        if (!visibleGraphs.contains(6)) {
            snowBarCard.setVisibility(View.GONE);
            return;
        } else {
            snowBarCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        snowBarChart.setDescription(graphDescription);
        snowBarChart.setDrawGridBackground(false);
        snowBarChart.setTouchEnabled(true);
        snowBarChart.setDragEnabled(true);
        snowBarChart.setMaxHighlightDistance(300);
        snowBarChart.setPinchZoom(true);
        snowBarChart.getLegend().setEnabled(false);
        snowBarChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        snowBarChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                snowBarChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = snowBarChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.setAxisMinimum(0f);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(rainSnowYAxisValueFormatter);

        snowBarChart.getAxisRight().setEnabled(false);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }
            entries.add(new BarEntry(
                    detailedWeatherForecast.getDateTime(),
                            (float) AppPreference.getRainOrSnow(
                                    rainSnowUnitFromPreferences,
                                    detailedWeatherForecast.getSnow())
                    )
            );
        }

        BarDataSet set;
        if (snowBarChart.getData() != null) {
            snowBarChart.getData().removeDataSet(snowBarChart.getData().getDataSetByIndex(
                    snowBarChart.getData().getDataSetCount() - 1));
            set = new BarDataSet(entries, getString(R.string.graph_snow_label));
            set.setValueTextSize(0f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#009688"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            BarData data = new BarData(set);
            data.setBarWidth(8000f);
            snowBarChart.setData(data);
        } else {
            set = new BarDataSet(entries, getString(R.string.graph_snow_label));
            set.setValueTextSize(0f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#009688"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            BarData data = new BarData(set);
            data.setBarWidth(8000f);
            snowBarChart.setData(data);
        }
        snowBarChart.invalidate();
    }

    private void setPressureChart(long locationId, String pressureUnitFromPreferences, Locale locale) {
        if (!visibleGraphs.contains(7)) {
            pressureChartCard.setVisibility(View.GONE);
            return;
        } else {
            pressureChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        mPressureChart.setDescription(graphDescription);
        mPressureChart.setDrawGridBackground(false);
        mPressureChart.setTouchEnabled(true);
        mPressureChart.setDragEnabled(true);
        mPressureChart.setMaxHighlightDistance(300);
        mPressureChart.setPinchZoom(true);
        mPressureChart.getLegend().setEnabled(false);
        mPressureChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        mPressureChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                mPressureChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = mPressureChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(new YAxisValueFormatter(locale, 2, AppPreference.getPressureUnit(this, pressureUnitFromPreferences)));

        mPressureChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(locationId).get(i);

            if (detailedWeatherForecast == null) {
                continue;
            }
            entries.add(new Entry(
                    detailedWeatherForecast.getDateTime(),
                    (float) AppPreference.getPressureWithUnit(
                            this,
                            detailedWeatherForecast.getPressure(),
                            pressureUnitFromPreferences,
                            locale).getPressure()));
        }

        LineDataSet set;
        if (mPressureChart.getData() != null) {
            mPressureChart.getData().removeDataSet(mPressureChart.getData().getDataSetByIndex(
                    mPressureChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, getString(R.string.graph_pressure_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#20cb02"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mPressureChart.setData(data);
        } else {
            set = new LineDataSet(entries, getString(R.string.graph_pressure_label));
            set.setMode(LineDataSet.Mode.LINEAR);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#20cb02"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mPressureChart.setData(data);
        }
        mPressureChart.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_graphs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                sendMessageToCurrentWeatherService(currentLocation, "GRAPHS");
                return true;
            case R.id.action_toggle_values:
                toggleValues();
                return true;
            case R.id.action_toggle_yaxis:
                toggleYAxis();
                return true;
            case R.id.action_visible_graphs_settings:
                showSettings();
                return true;
            case R.id.action_graph_combined_settings:
                showCombinedGraphSettings();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressLint("MissingInflatedId")
    private void showCombinedGraphSettings() {
        boolean[] checkedItems = new boolean[4];
        for (Integer visibleColumn: combinedGraphValues) {
            checkedItems[visibleColumn] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View forecastSettingView = inflater.inflate(R.layout.activity_setting_graph, null);
        final Switch temperatureSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_temperatre_switch);
        final Switch rainsnowSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_rain_switch);
        final Switch windSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_wind_switch);
        final Switch pressureSwitch = forecastSettingView.findViewById(R.id.widget_setting_graph_pressure_switch);
        temperatureSwitch.setChecked(checkedItems[0]);
        final GraphValuesSwitchListener temperatureSwitchListener = new GraphValuesSwitchListener(checkedItems[0]);
        temperatureSwitch.setOnCheckedChangeListener(temperatureSwitchListener);
        rainsnowSwitch.setChecked(checkedItems[1]);
        final GraphValuesSwitchListener rainsnowSwitchListener = new GraphValuesSwitchListener(checkedItems[1]);
        rainsnowSwitch.setOnCheckedChangeListener(rainsnowSwitchListener);
        windSwitch.setChecked(checkedItems[2]);
        final GraphValuesSwitchListener windSwitchListener = new GraphValuesSwitchListener(checkedItems[2], pressureSwitch);
        windSwitch.setOnCheckedChangeListener(windSwitchListener);
        pressureSwitch.setChecked(checkedItems[3]);
        final GraphValuesSwitchListener pressureSwitchListener = new GraphValuesSwitchListener(checkedItems[3], windSwitch);
        pressureSwitch.setOnCheckedChangeListener(pressureSwitchListener);
        if (windSwitch.isChecked()) {
            pressureSwitch.setEnabled(false);
        } else if (pressureSwitch.isChecked()) {
            windSwitch.setEnabled(false);
        }

        builder.setTitle(R.string.forecast_settings_combined_values)
                .setView(forecastSettingView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        combinedGraphValues = new HashSet<>();
                        if (temperatureSwitchListener.isChecked()) {
                            combinedGraphValues.add(0);
                        }
                        if (rainsnowSwitchListener.isChecked()) {
                            combinedGraphValues.add(1);
                        }
                        if (windSwitchListener.isChecked()) {
                            combinedGraphValues.add(2);
                        }
                        if (pressureSwitchListener.isChecked()) {
                            combinedGraphValues.add(3);
                        }
                        AppPreference.setCombinedGraphValues(GraphsActivity.this, combinedGraphValues);
                        YourLocalWeather.executor.submit(() -> {
                            updateUI();
                        });
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSettings() {
        final Set<Integer> mSelectedItems = new HashSet<>();
        boolean[] checkedItems = new boolean[8];
        for (Integer visibleColumn: visibleGraphs) {
            mSelectedItems.add(visibleColumn);
            checkedItems[visibleColumn] = true;
        }
        final Context context = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.forecast_settings_visible_graphs)
                .setMultiChoiceItems(R.array.pref_graphs, checkedItems,
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
                        visibleGraphs = new HashSet<>();
                        for (Integer selectedItem: mSelectedItems) {
                            visibleGraphs.add(selectedItem);
                        }
                        AppPreference.setGraphsActivityVisibleGraphs(context, visibleGraphs);
                        YourLocalWeather.executor.submit(() -> {
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

    private void toggleValues() {
        toggleValuesForGraph(mTemperatureChart.getData());
        toggleValuesForGraph(mWindChart.getData());
        toggleValuesForGraph(mRainChart.getData());
        toggleValuesForGraph(mSnowChart.getData());
        toggleValuesForGraph(mPressureChart.getData());
        mTemperatureChart.invalidate();
        mWindChart.invalidate();
        mRainChart.invalidate();
        mSnowChart.invalidate();
        mPressureChart.invalidate();
    }

    private void toggleValuesForGraph(LineData lineData) {
        if (lineData == null) {
            return;
        }
        for (IDataSet set : lineData.getDataSets()) {
            set.setDrawValues(!set.isDrawValuesEnabled());
        }
    }

    private void toggleYAxis() {
        mTemperatureChart.getAxisLeft().setEnabled(!mTemperatureChart.getAxisLeft().isEnabled());
        mWindChart.getAxisLeft().setEnabled(!mWindChart.getAxisLeft().isEnabled());
        mRainChart.getAxisLeft().setEnabled(!mRainChart.getAxisLeft().isEnabled());
        mSnowChart.getAxisLeft().setEnabled(!mSnowChart.getAxisLeft().isEnabled());
        mPressureChart.getAxisLeft().setEnabled(!mPressureChart.getAxisLeft().isEnabled());
        mTemperatureChart.invalidate();
        mWindChart.invalidate();
        mRainChart.invalidate();
        rainBarChart.invalidate();
        mSnowChart.invalidate();
        snowBarChart.invalidate();
        mPressureChart.invalidate();
    }

    @Override
    protected void updateUI() {
        int maxOrderId = locationsDbHelper.getMaxOrderId();
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              View switchPanel = findViewById(R.id.graph_switch_panel);
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
        long locationId = AppPreference.getCurrentLocationId(GraphsActivity.this);
        currentLocation = locationsDbHelper.getLocationById(locationId);
        if (currentLocation == null) {
            return;
        }
        mValueFormatter = new CustomValueFormatter(currentLocation.getLocale());
        rainSnowYAxisValueFormatter = new RainSnowYAxisValueFormatter(this, currentLocation.getLocale());
        //forecastType.setChecked(2 == AppPreference.getForecastType(getBaseContext()));
        //appendLog(getBaseContext(), TAG, "updateUI with forecastType:", forecastType.isChecked());
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId, 1);
        if (weatherForecastRecord != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    weatherForecastList.put(locationId, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList());
                    locationWeatherForecastLastUpdate.put(locationId, weatherForecastRecord.getLastUpdatedTime());
                }
            });
        } else {
            /*if (forecastType.isChecked()) {
                updateLongWeatherForecastFromNetwork("GRAPHS");
            } else {*/
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  sendMessageToCurrentWeatherService(currentLocation, "GRAPHS");
                              }
                          });
            //}
            return;
        }

        int backgroundColor = PreferenceUtil.getBackgroundColor(this);
        int textColor = PreferenceUtil.getTextColor(this);
        String temperatureUnit = TemperatureUtil.getTemperatureUnit(this, temperatureUnitFromPreferences);
        String rainSnowUnit = getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences));
        String windUnit = AppPreference.getWindUnit(this, AppPreference.getWindUnitFromPreferences(this));
        String pressureUnit = AppPreference.getPressureUnit(this, pressureUnitFromPreferences);
        String cityAndCountry = Utils.getCityAndCountry(this, currentLocation);
        String pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(this);
        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView temperatureLabel = findViewById(R.id.graphs_temperature_label);
                temperatureLabel.setBackgroundColor(backgroundColor);
                temperatureLabel.setTextColor(textColor);
                TextView windLabel = findViewById(R.id.graphs_wind_label);
                windLabel.setBackgroundColor(backgroundColor);
                windLabel.setTextColor(textColor);
                TextView rainLabel = findViewById(R.id.graphs_rain_label);
                rainLabel.setBackgroundColor(backgroundColor);
                rainLabel.setTextColor(textColor);
                TextView snowLabel = findViewById(R.id.graphs_snow_label);
                snowLabel.setBackgroundColor(backgroundColor);
                snowLabel.setTextColor(textColor);
                TextView rainBarLabel = findViewById(R.id.graphs_bar_rain_label);
                rainBarLabel.setBackgroundColor(backgroundColor);
                rainBarLabel.setTextColor(textColor);
                TextView snowBarLabel = findViewById(R.id.graphs_bar_snow_label);
                snowBarLabel.setBackgroundColor(backgroundColor);
                snowBarLabel.setTextColor(textColor);

                TextView combinedLabel = findViewById(R.id.graphs_combined_label);
                StringBuilder combinedGraphLabel = new StringBuilder();
                if (combinedGraphValues.contains(0)) {
                    combinedGraphLabel.append(getString(R.string.label_temperature));
                    combinedGraphLabel.append(" (");
                    combinedGraphLabel.append(temperatureUnit);
                    combinedGraphLabel.append(")");
                }
                if (combinedGraphValues.contains(1)) {
                    combinedGraphLabel.append(", ");
                    combinedGraphLabel.append(getString(R.string.graph_rain_label));
                    combinedGraphLabel.append("/");
                    combinedGraphLabel.append(getString(R.string.graph_snow_label));
                    combinedGraphLabel.append(" (");
                    combinedGraphLabel.append(rainSnowUnit);
                    combinedGraphLabel.append(")");
                }
                if (combinedGraphValues.contains(2)) {
                    combinedGraphLabel.append(", ");
                    combinedGraphLabel.append(getString(R.string.label_wind));
                    combinedGraphLabel.append(" (");
                    combinedGraphLabel.append(windUnit);
                    combinedGraphLabel.append(")");
                }
                if (combinedGraphValues.contains(3)) {
                    combinedGraphLabel.append(", ");
                    combinedGraphLabel.append(getString(R.string.label_pressure));
                    combinedGraphLabel.append(" (");
                    combinedGraphLabel.append(pressureUnit);
                    combinedGraphLabel.append(")");
                }
                combinedLabel.setText(combinedGraphLabel.toString());

                setCombinedChart(locationId, currentLocation.getLocale());
                setTemperatureChart(locationId, currentLocation.getLocale());
                setWindChart(locationId, windUnitFromPreferences, currentLocation.getLocale());
                setRainChart(locationId, currentLocation.getLocale());
                setRainBarChart(locationId, currentLocation.getLocale());
                setSnowChart(locationId, currentLocation.getLocale());
                setSnowBarChart(locationId, currentLocation.getLocale());
                setPressureChart(locationId, pressureUnitFromPreferences, currentLocation.getLocale());
                localityView.setText(cityAndCountry);
            }
        });
    }
}
