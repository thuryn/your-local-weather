package org.thosp.yourlocalweather;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import org.thosp.charting.components.Description;
import org.thosp.charting.components.YAxis;
import org.thosp.charting.data.BarData;
import org.thosp.charting.data.BarDataSet;
import org.thosp.charting.data.BarEntry;
import org.thosp.charting.data.Entry;
import org.thosp.charting.data.LineData;
import org.thosp.charting.data.LineDataSet;
import org.thosp.charting.interfaces.datasets.IDataSet;
import org.thosp.yourlocalweather.databinding.ActivityGraphsBinding;
import org.thosp.yourlocalweather.databinding.ActivitySettingGraphBinding;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.settings.GraphValuesSwitchListener;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.CustomValueFormatter;
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

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class GraphsActivity extends ForecastingActivity {

    private static final String TAG = "GraphsActivity";

    private volatile boolean inited;

    // 1. Hlavní binding pro celou aktivitu
    private ActivityGraphsBinding binding;

    private CustomValueFormatter mValueFormatter;
    private RainSnowYAxisValueFormatter rainSnowYAxisValueFormatter;
    private Set<Integer> visibleGraphs = new HashSet<>();
    private Set<Integer> combinedGraphValues = new HashSet<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWeatherForecastReceiver(UpdateWeatherService.ACTION_GRAPHS_UPDATE_RESULT);

        // 2. Inicializace View Bindingu
        binding = ActivityGraphsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        localityView = binding.graphLocality;
        switchLocationButton = binding.graphSwitchLocation;

        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(), "fonts/Roboto-Light.ttf");
        localityView.setTypeface(robotoLight);

        final Context appContext = this.getApplicationContext();
        final java.lang.ref.WeakReference<GraphsActivity> activityRef = new java.lang.ref.WeakReference<>(this);

        YourLocalWeather.executor.submit(() -> {
            GraphsActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            ConnectionDetector detector = new ConnectionDetector(appContext);
            LocationsDbHelper dbHelper = LocationsDbHelper.getInstance(appContext);
            String pressUnit = AppPreference.getPressureUnitFromPreferences(appContext);
            String rainSnowUnit = AppPreference.getRainSnowUnitFromPreferences(appContext);
            String tempUnit = AppPreference.getTemperatureUnitFromPreferences(appContext);

            Set<Integer> visGraphs = AppPreference.getGraphsActivityVisibleGraphs(appContext);
            Set<Integer> combGraphVals = AppPreference.getCombinedGraphValues(appContext);

            String tempUnitLabel = TemperatureUtil.getTemperatureUnit(appContext, tempUnit);
            String windUnitLabel = AppPreference.getWindUnit(appContext, AppPreference.getWindUnitFromPreferences(appContext));
            int rainOrSnowUnitRes = AppPreference.getRainOrSnowUnit(rainSnowUnit);
            String pressureUnitLabel = AppPreference.getPressureUnit(appContext, pressUnit);

            activity.runOnUiThread(() -> {
                GraphsActivity act = activityRef.get();
                if (act != null && act.binding != null && !act.isFinishing() && !act.isDestroyed()) {
                    act.connectionDetector = detector;
                    act.locationsDbHelper = dbHelper;
                    act.pressureUnitFromPreferences = pressUnit;
                    act.rainSnowUnitFromPreferences = rainSnowUnit;
                    act.temperatureUnitFromPreferences = tempUnit;
                    act.visibleGraphs = visGraphs;
                    act.combinedGraphValues = combGraphVals;

                    // Bezpečné dosazení textů přímo přes vygenerovanou strukturu bindingu
                    act.binding.graphsTemperatureLabel.setText(act.getString(R.string.label_temperature) + ", " + tempUnitLabel);
                    act.binding.graphsWindLabel.setText(act.getString(R.string.label_wind) + ", " + windUnitLabel);
                    act.binding.graphsRainLabel.setText(act.getString(R.string.label_rain) + ", " + act.getString(rainOrSnowUnitRes));
                    act.binding.graphsSnowLabel.setText(act.getString(R.string.label_snow) + ", " + act.getString(rainOrSnowUnitRes));
                    act.binding.graphsBarRainLabel.setText(act.getString(R.string.label_rain) + ", " + act.getString(rainOrSnowUnitRes));
                    act.binding.graphsBarSnowLabel.setText(act.getString(R.string.label_snow) + ", " + act.getString(rainOrSnowUnitRes));
                    act.binding.graphsPressureLabel.setText(act.getString(R.string.label_pressure) + ", " + pressureUnitLabel);

                    act.updateUI();
                    act.inited = true;
                }
            });
        });

        binding.graphScrollView.setOnTouchListener(new ActivityTransitionTouchListener(
                WeatherForecastActivity.class,
                null, this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(this, mWeatherUpdateReceiver,
                new IntentFilter(UpdateWeatherService.ACTION_GRAPHS_UPDATE_RESULT),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        if (inited) {
            YourLocalWeather.executor.submit(() -> {
                updateUI();
            });
        }
    }

    private void setCombinedChart(long locationId, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(0)) {
            binding.combinedChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.combinedChartCard.setVisibility(View.VISIBLE);
        }
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
        String pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(this);
        String rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(this);
        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(this);
        GraphUtils.generateCombinedGraph(this,
                binding.combinedChart,
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
        if (binding == null) return;
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(this);
        if (!visibleGraphs.contains(1)) {
            binding.temperatureChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.temperatureChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.temperatureChart.setDescription(graphDescription);
        binding.temperatureChart.setDrawGridBackground(false);
        binding.temperatureChart.setTouchEnabled(true);
        binding.temperatureChart.setDragEnabled(true);
        binding.temperatureChart.setMaxHighlightDistance(300);
        binding.temperatureChart.setPinchZoom(true);
        binding.temperatureChart.getLegend().setEnabled(false);
        binding.temperatureChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.temperatureChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.temperatureChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.temperatureChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(new YAxisValueFormatter(locale, 2, TemperatureUtil.getTemperatureUnit(this, temperatureUnitFromPreferences)));

        binding.temperatureChart.getAxisRight().setEnabled(false);

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
        if (binding.temperatureChart.getData() != null) {
            binding.temperatureChart.getData().removeDataSet(binding.temperatureChart.getData().getDataSetByIndex(
                    binding.temperatureChart.getData().getDataSetCount() - 1));
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
            binding.temperatureChart.setData(data);
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
            binding.temperatureChart.setData(data);
        }
        binding.temperatureChart.invalidate();
    }

    private void setWindChart(long locationId, String windUnitFromPreferences, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(2)) {
            binding.windChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.windChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.windChart.setDescription(graphDescription);
        binding.windChart.setDrawGridBackground(false);
        binding.windChart.setTouchEnabled(true);
        binding.windChart.setDragEnabled(true);
        binding.windChart.setMaxHighlightDistance(300);
        binding.windChart.setPinchZoom(true);
        binding.windChart.getLegend().setEnabled(false);
        binding.windChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.windChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.windChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.windChart.getAxisLeft();
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

        binding.windChart.getAxisRight().setEnabled(false);

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
        if (binding.windChart.getData() != null) {
            binding.windChart.getData().removeDataSet(binding.windChart.getData().getDataSetByIndex(
                    binding.windChart.getData().getDataSetCount() - 1));
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
            binding.windChart.setData(data);
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
            binding.windChart.setData(data);
        }
        binding.windChart.invalidate();
    }

    private void setRainChart(long locationId, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(3)) {
            binding.rainChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.rainChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.rainChart.setDescription(graphDescription);
        binding.rainChart.setDrawGridBackground(false);
        binding.rainChart.setTouchEnabled(true);
        binding.rainChart.setDragEnabled(true);
        binding.rainChart.setMaxHighlightDistance(300);
        binding.rainChart.setPinchZoom(true);
        binding.rainChart.getLegend().setEnabled(false);
        binding.rainChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.rainChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.rainChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.rainChart.getAxisLeft();
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

        binding.rainChart.getAxisRight().setEnabled(false);

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
        if (binding.rainChart.getData() != null) {
            binding.rainChart.getData().removeDataSet(binding.rainChart.getData().getDataSetByIndex(
                    binding.rainChart.getData().getDataSetCount() - 1));
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
            binding.rainChart.setData(data);
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
            binding.rainChart.setData(data);
        }
        binding.rainChart.invalidate();
    }

    private void setRainBarChart(long locationId, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(4)) {
            binding.rainBarChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.rainBarChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.barRainChart.setDescription(graphDescription);
        binding.barRainChart.setDrawGridBackground(false);
        binding.barRainChart.setTouchEnabled(true);
        binding.barRainChart.setDragEnabled(true);
        binding.barRainChart.setMaxHighlightDistance(300);
        binding.barRainChart.setPinchZoom(true);
        binding.barRainChart.getLegend().setEnabled(false);
        binding.barRainChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.barRainChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.barRainChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.barRainChart.getAxisLeft();
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

        binding.barRainChart.getAxisRight().setEnabled(false);

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
        if (binding.barRainChart.getData() != null) {
            binding.barRainChart.getData().removeDataSet(binding.barRainChart.getData().getDataSetByIndex(
                    binding.barRainChart.getData().getDataSetCount() - 1));
            set = new BarDataSet(entries, getString(R.string.graph_rain_label));
            set.setValueTextSize(0f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#5677FC"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            BarData data = new BarData(set);
            data.setBarWidth(8000f);
            binding.barRainChart.setData(data);
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
            binding.barRainChart.setData(data);
        }
        binding.barRainChart.invalidate();
    }

    private void setSnowChart(long locationId, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(5)) {
            binding.snowChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.snowChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.snowChart.setDescription(graphDescription);
        binding.snowChart.setDrawGridBackground(false);
        binding.snowChart.setTouchEnabled(true);
        binding.snowChart.setDragEnabled(true);
        binding.snowChart.setMaxHighlightDistance(300);
        binding.snowChart.setPinchZoom(true);
        binding.snowChart.getLegend().setEnabled(false);
        binding.snowChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.snowChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.snowChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.snowChart.getAxisLeft();
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

        binding.snowChart.getAxisRight().setEnabled(false);

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
        if (binding.snowChart.getData() != null) {
            binding.snowChart.getData().removeDataSet(binding.snowChart.getData().getDataSetByIndex(
                    binding.snowChart.getData().getDataSetCount() - 1));
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
            binding.snowChart.setData(data);
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
            binding.snowChart.setData(data);
        }
        binding.snowChart.invalidate();
    }

    private void setSnowBarChart(long locationId, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(6)) {
            binding.snowBarChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.snowBarChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.barSnowChart.setDescription(graphDescription);
        binding.barSnowChart.setDrawGridBackground(false);
        binding.barSnowChart.setTouchEnabled(true);
        binding.barSnowChart.setDragEnabled(true);
        binding.barSnowChart.setMaxHighlightDistance(300);
        binding.barSnowChart.setPinchZoom(true);
        binding.barSnowChart.getLegend().setEnabled(false);
        binding.barSnowChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.barSnowChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.barSnowChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.barSnowChart.getAxisLeft();
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

        binding.barSnowChart.getAxisRight().setEnabled(false);

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
        if (binding.barSnowChart.getData() != null) {
            binding.barSnowChart.getData().removeDataSet(binding.barSnowChart.getData().getDataSetByIndex(
                    binding.barSnowChart.getData().getDataSetCount() - 1));
            set = new BarDataSet(entries, getString(R.string.graph_snow_label));
            set.setValueTextSize(0f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#009688"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            BarData data = new BarData(set);
            data.setBarWidth(8000f);
            binding.barSnowChart.setData(data);
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
            binding.barSnowChart.setData(data);
        }
        binding.barSnowChart.invalidate();
    }

    private void setPressureChart(long locationId, String pressureUnitFromPreferences, Locale locale) {
        if (binding == null) return;
        if (!visibleGraphs.contains(7)) {
            binding.pressureChartCard.setVisibility(View.GONE);
            return;
        } else {
            binding.pressureChartCard.setVisibility(View.VISIBLE);
        }
        Description graphDescription = new Description();
        graphDescription.setText("");
        binding.pressureChart.setDescription(graphDescription);
        binding.pressureChart.setDrawGridBackground(false);
        binding.pressureChart.setTouchEnabled(true);
        binding.pressureChart.setDragEnabled(true);
        binding.pressureChart.setMaxHighlightDistance(300);
        binding.pressureChart.setPinchZoom(true);
        binding.pressureChart.getLegend().setEnabled(false);
        binding.pressureChart.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        binding.pressureChart.setGridBackgroundColor(PreferenceUtil.getTextColor(this));

        GraphUtils.setupXAxis(
                binding.pressureChart.getXAxis(),
                weatherForecastList.get(locationId),
                PreferenceUtil.getTextColor(this),
                null,
                PreferenceUtil.getGraphGridColor(this),
                locale);

        YAxis yLeft = binding.pressureChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this).getMainGridColor());
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(new YAxisValueFormatter(locale, 2, AppPreference.getPressureUnit(this, pressureUnitFromPreferences)));

        binding.pressureChart.getAxisRight().setEnabled(false);

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
        if (binding.pressureChart.getData() != null) {
            binding.pressureChart.getData().removeDataSet(binding.pressureChart.getData().getDataSetByIndex(
                    binding.pressureChart.getData().getDataSetCount() - 1));
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
            binding.pressureChart.setData(data);
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
            binding.pressureChart.setData(data);
        }
        binding.pressureChart.invalidate();
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
            if (visibleColumn < 4) {
                checkedItems[visibleColumn] = true;
            }
        }

        // Použití specifického sub-bindingu pro dialog nastavení grafů
        ActivitySettingGraphBinding dialogBinding = ActivitySettingGraphBinding.inflate(getLayoutInflater());

        dialogBinding.widgetSettingGraphTemperatreSwitch.setChecked(checkedItems[0]);
        final GraphValuesSwitchListener temperatureSwitchListener = new GraphValuesSwitchListener(checkedItems[0]);
        dialogBinding.widgetSettingGraphTemperatreSwitch.setOnCheckedChangeListener(temperatureSwitchListener);

        dialogBinding.widgetSettingGraphRainSwitch.setChecked(checkedItems[1]);
        final GraphValuesSwitchListener rainsnowSwitchListener = new GraphValuesSwitchListener(checkedItems[1]);
        dialogBinding.widgetSettingGraphRainSwitch.setOnCheckedChangeListener(rainsnowSwitchListener);

        dialogBinding.widgetSettingGraphWindSwitch.setChecked(checkedItems[2]);
        final GraphValuesSwitchListener windSwitchListener = new GraphValuesSwitchListener(checkedItems[2], dialogBinding.widgetSettingGraphPressureSwitch);
        dialogBinding.widgetSettingGraphWindSwitch.setOnCheckedChangeListener(windSwitchListener);

        dialogBinding.widgetSettingGraphPressureSwitch.setChecked(checkedItems[3]);
        final GraphValuesSwitchListener pressureSwitchListener = new GraphValuesSwitchListener(checkedItems[3], dialogBinding.widgetSettingGraphWindSwitch);
        dialogBinding.widgetSettingGraphPressureSwitch.setOnCheckedChangeListener(pressureSwitchListener);

        if (dialogBinding.widgetSettingGraphWindSwitch.isChecked()) {
            dialogBinding.widgetSettingGraphPressureSwitch.setEnabled(false);
        } else if (dialogBinding.widgetSettingGraphPressureSwitch.isChecked()) {
            dialogBinding.widgetSettingGraphWindSwitch.setEnabled(false);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.forecast_settings_combined_values)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    combinedGraphValues = new HashSet<>();
                    if (temperatureSwitchListener.isChecked()) combinedGraphValues.add(0);
                    if (rainsnowSwitchListener.isChecked()) combinedGraphValues.add(1);
                    if (windSwitchListener.isChecked()) combinedGraphValues.add(2);
                    if (pressureSwitchListener.isChecked()) combinedGraphValues.add(3);

                    AppPreference.setCombinedGraphValues(GraphsActivity.this, combinedGraphValues);
                    YourLocalWeather.executor.submit(() -> updateUI());
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> finish())
                .show();
    }

    private void showSettings() {
        final Set<Integer> mSelectedItems = new HashSet<>();
        boolean[] checkedItems = new boolean[8];
        for (Integer visibleColumn: visibleGraphs) {
            if (visibleColumn < 8) {
                mSelectedItems.add(visibleColumn);
                checkedItems[visibleColumn] = true;
            }
        }
        final Context context = this;
        new AlertDialog.Builder(this)
                .setTitle(R.string.forecast_settings_visible_graphs)
                .setMultiChoiceItems(R.array.pref_graphs, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        mSelectedItems.add(which);
                    } else mSelectedItems.remove(which);
                })
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    visibleGraphs = new HashSet<>();
                    for (Integer selectedItem: mSelectedItems) {
                        visibleGraphs.add(selectedItem);
                    }
                    AppPreference.setGraphsActivityVisibleGraphs(context, visibleGraphs);
                    YourLocalWeather.executor.submit(() -> updateUI());
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {})
                .show();
    }

    private void toggleValues() {
        if (binding == null) return;
        toggleValuesForGraph(binding.temperatureChart.getData());
        toggleValuesForGraph(binding.windChart.getData());
        toggleValuesForGraph(binding.rainChart.getData());
        toggleValuesForGraph(binding.snowChart.getData());
        toggleValuesForGraph(binding.pressureChart.getData());
        binding.temperatureChart.invalidate();
        binding.windChart.invalidate();
        binding.rainChart.invalidate();
        binding.snowChart.invalidate();
        binding.pressureChart.invalidate();
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
        if (binding == null) return;
        binding.temperatureChart.getAxisLeft().setEnabled(!binding.temperatureChart.getAxisLeft().isEnabled());
        binding.windChart.getAxisLeft().setEnabled(!binding.windChart.getAxisLeft().isEnabled());
        binding.rainChart.getAxisLeft().setEnabled(!binding.rainChart.getAxisLeft().isEnabled());
        binding.snowChart.getAxisLeft().setEnabled(!binding.snowChart.getAxisLeft().isEnabled());
        binding.pressureChart.getAxisLeft().setEnabled(!binding.pressureChart.getAxisLeft().isEnabled());
        binding.temperatureChart.invalidate();
        binding.windChart.invalidate();
        binding.rainChart.invalidate();
        binding.barRainChart.invalidate();
        binding.snowChart.invalidate();
        binding.barSnowChart.invalidate();
        binding.pressureChart.invalidate();
    }

    @Override
    protected void updateUI() {
        if (binding == null) return;
        int maxOrderId = locationsDbHelper.getMaxOrderId();
        runOnUiThread(() -> {
            if (binding == null) return;
            binding.graphSwitchPanel.setVisibility(View.INVISIBLE);
            if ((maxOrderId > 1) || ((maxOrderId == 1) && (locationsDbHelper.getLocationByOrderId(0).isEnabled()))) {
                switchLocationButton.setVisibility(View.VISIBLE);
            } else {
                switchLocationButton.setVisibility(View.GONE);
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
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId, 1);
        if (weatherForecastRecord != null) {
            runOnUiThread(() -> {
                weatherForecastList.put(locationId, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList());
                locationWeatherForecastLastUpdate.put(locationId, weatherForecastRecord.getLastUpdatedTime());
            });
        } else {
            runOnUiThread(() -> sendMessageToCurrentWeatherService(currentLocation, "GRAPHS"));
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

        runOnUiThread(() -> {
            if (binding == null) return;
            binding.graphsTemperatureLabel.setBackgroundColor(backgroundColor);
            binding.graphsTemperatureLabel.setTextColor(textColor);
            binding.graphsWindLabel.setBackgroundColor(backgroundColor);
            binding.graphsWindLabel.setTextColor(textColor);
            binding.graphsRainLabel.setBackgroundColor(backgroundColor);
            binding.graphsRainLabel.setTextColor(textColor);
            binding.graphsSnowLabel.setBackgroundColor(backgroundColor);
            binding.graphsSnowLabel.setTextColor(textColor);
            binding.graphsBarRainLabel.setBackgroundColor(backgroundColor);
            binding.graphsBarRainLabel.setTextColor(textColor);
            binding.graphsBarSnowLabel.setBackgroundColor(backgroundColor);
            binding.graphsBarSnowLabel.setTextColor(textColor);

            StringBuilder combinedGraphLabel = new StringBuilder();
            if (combinedGraphValues.contains(0)) {
                combinedGraphLabel.append(getString(R.string.label_temperature)).append(" (").append(temperatureUnit).append(")");
            }
            if (combinedGraphValues.contains(1)) {
                if (combinedGraphLabel.length() > 0) combinedGraphLabel.append(", ");
                combinedGraphLabel.append(getString(R.string.graph_rain_label)).append("/").append(getString(R.string.graph_snow_label)).append(" (").append(rainSnowUnit).append(")");
            }
            if (combinedGraphValues.contains(2)) {
                if (combinedGraphLabel.length() > 0) combinedGraphLabel.append(", ");
                combinedGraphLabel.append(getString(R.string.label_wind)).append(" (").append(windUnit).append(")");
            }
            if (combinedGraphValues.contains(3)) {
                if (combinedGraphLabel.length() > 0) combinedGraphLabel.append(", ");
                combinedGraphLabel.append(getString(R.string.label_pressure)).append(" (").append(pressureUnit).append(")");
            }
            binding.graphsCombinedLabel.setText(combinedGraphLabel.toString());

            setCombinedChart(locationId, currentLocation.getLocale());
            setTemperatureChart(locationId, currentLocation.getLocale());
            setWindChart(locationId, windUnitFromPreferences, currentLocation.getLocale());
            setRainChart(locationId, currentLocation.getLocale());
            setRainBarChart(locationId, currentLocation.getLocale());
            setSnowChart(locationId, currentLocation.getLocale());
            setSnowBarChart(locationId, currentLocation.getLocale());
            setPressureChart(locationId, pressureUnitFromPreferences, currentLocation.getLocale());
            localityView.setText(cityAndCountry);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}