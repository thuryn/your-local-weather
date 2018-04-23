package org.thosp.yourlocalweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.loopj.android.http.AsyncHttpClient;

import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastResultHandler;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.CustomValueFormatter;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.WeatherForecastUtil;
import org.thosp.yourlocalweather.utils.XAxisValueFormatter;
import org.thosp.yourlocalweather.utils.YAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;


public class GraphsActivity extends BaseActivity {

    private static final String TAG = "GraphsActivity";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private ConnectionDetector mConnectionDetector;
    private Map<Long, List<DetailedWeatherForecast>> weatherForecastList = new HashMap<>();
    private Map<Long, Long> locationWeatherForecastLastUpdate = new HashMap<>();
    private LineChart mTemperatureChart;
    private LineChart mWindChart;
    private LineChart mRainChart;
    private LineChart mSnowChart;
    private String[] mDatesArray;
    private Handler mHandler;
    private ProgressDialog mGetWeatherProgress;
    private CustomValueFormatter mValueFormatter;
    private YAxisValueFormatter mYAxisFormatter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((YourLocalWeather) getApplication()).applyTheme(this);
        setContentView(R.layout.activity_graphs);
        mConnectionDetector = new ConnectionDetector(this);
        mGetWeatherProgress = getProgressDialog();
        mValueFormatter = new CustomValueFormatter();
        mYAxisFormatter = new YAxisValueFormatter();
        mTemperatureChart = (LineChart) findViewById(R.id.temperature_chart);
        mWindChart = (LineChart) findViewById(R.id.wind_chart);
        mRainChart = (LineChart) findViewById(R.id.rain_chart);
        mSnowChart = (LineChart) findViewById(R.id.snow_chart);
        TextView temperatureLabel = (TextView) findViewById(R.id.graphs_temperature_label);
        temperatureLabel.setText(getString(R.string.label_temperature) +
                                         ", " +
                                        TemperatureUtil.getTemperatureUnit(this));
        TextView windLabel = (TextView) findViewById(R.id.graphs_wind_label);
        windLabel.setText(getString(R.string.label_wind) + ", " + AppPreference.getWindUnit(this));
        TextView rainLabel = (TextView) findViewById(R.id.graphs_rain_label);
        rainLabel.setText(getString(R.string.label_rain) + ", " + getString(R.string.millimetre_label));
        TextView snowLabel = (TextView) findViewById(R.id.graphs_snow_label);
        snowLabel.setText(getString(R.string.label_snow) + ", " + getString(R.string.millimetre_label));

        updateUI();
        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case Constants.TASK_RESULT_ERROR:
                        Toast.makeText(GraphsActivity.this,
                                       R.string.toast_parse_error,
                                       Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.PARSE_RESULT_ERROR:
                        Toast.makeText(GraphsActivity.this,
                                       R.string.toast_parse_error,
                                       Toast.LENGTH_SHORT).show();
                        setVisibleUpdating(false);
                        break;
                    case Constants.PARSE_RESULT_SUCCESS:
                        setVisibleUpdating(false);
                        updateUI();
                        break;
                }
            }
        };

        ScrollView mRecyclerView = (ScrollView) findViewById(R.id.graph_scroll_view);
        mRecyclerView.setOnTouchListener(new ActivityTransitionTouchListener(
                WeatherForecastActivity.class,
                null, this));
    }

    private void setTemperatureChart(long locationId) {
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

        formatDate(locationId);
        XAxis x = mTemperatureChart.getXAxis();
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setTextColor(PreferenceUtil.getTextColor(this));
        x.setValueFormatter(new XAxisValueFormatter(mDatesArray));

        YAxis yLeft = mTemperatureChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this));
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(mYAxisFormatter);

        mTemperatureChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            double temperatureDay = TemperatureUtil.getTemperature(this, weatherForecastList.get(locationId).get(i));
            entries.add(new Entry(i, (float) temperatureDay));
        }

        LineDataSet set;
        if (mTemperatureChart.getData() != null) {
            mTemperatureChart.getData().removeDataSet(mTemperatureChart.getData().getDataSetByIndex(
                    mTemperatureChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, "Day");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setDrawValues(false);
            set.setValueTextSize(12f);
            set.setColor(Color.parseColor("#E84E40"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);
            set.setValueTextColor(PreferenceUtil.getTextColor(this));

            LineData data = new LineData(set);
            mTemperatureChart.setData(data);
        } else {
            set = new LineDataSet(entries, "Day");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
    
    private void setWindChart(long locationId) {
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

        formatDate(locationId);
        XAxis x = mWindChart.getXAxis();
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(PreferenceUtil.getTextColor(this));
        x.setDrawGridLines(false);
        x.setValueFormatter(new XAxisValueFormatter(mDatesArray));

        YAxis yLeft = mWindChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this));
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(mYAxisFormatter);

        mWindChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            double wind = AppPreference.getWind(this, weatherForecastList.get(locationId).get(i).getWindSpeed());
            entries.add(new Entry(i, (float) wind));
        }

        LineDataSet set;
        if (mWindChart.getData() != null) {
            mWindChart.getData().removeDataSet(mWindChart.getData().getDataSetByIndex(
                    mWindChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, "Wind");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
            set = new LineDataSet(entries, "Wind");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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

    private void setRainChart(long locationId) {
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

        formatDate(locationId);
        XAxis x = mRainChart.getXAxis();
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(PreferenceUtil.getTextColor(this));
        x.setDrawGridLines(false);
        x.setValueFormatter(new XAxisValueFormatter(mDatesArray));

        YAxis yLeft = mRainChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this));
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(mYAxisFormatter);

        mRainChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            entries.add(new Entry(i, (float) weatherForecastList.get(locationId).get(i).getRain()));
        }

        LineDataSet set;
        if (mRainChart.getData() != null) {
            mRainChart.getData().removeDataSet(mRainChart.getData().getDataSetByIndex(
                    mRainChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, "Rain");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
            set = new LineDataSet(entries, "Rain");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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

    private void setSnowChart(long locationId) {
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

        formatDate(locationId);
        XAxis x = mSnowChart.getXAxis();
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(PreferenceUtil.getTextColor(this));
        x.setDrawGridLines(false);
        x.setValueFormatter(new XAxisValueFormatter(mDatesArray));

        YAxis yLeft = mSnowChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(PreferenceUtil.getTextColor(this));
        yLeft.setGridColor(PreferenceUtil.getGraphGridColor(this));
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(mYAxisFormatter);

        mSnowChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.get(locationId).size(); i++) {
            entries.add(new Entry(i, (float) weatherForecastList.get(locationId).get(i).getSnow()));
        }

        LineDataSet set;
        if (mSnowChart.getData() != null) {
            mSnowChart.getData().removeDataSet(mSnowChart.getData().getDataSetByIndex(
                    mSnowChart.getData().getDataSetCount() - 1));
            set = new LineDataSet(entries, "Snow");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
            set = new LineDataSet(entries, "Snow");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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

    private void formatDate(long locationId) {
        SimpleDateFormat format = new SimpleDateFormat("EEE", Locale.getDefault());
        if (weatherForecastList.get(locationId) != null) {
            int mSize = weatherForecastList.get(locationId).size();
            mDatesArray = new String[mSize];

            for (int i = 0; i < mSize; i++) {
                Date date = new Date(weatherForecastList.get(locationId).get(i).getDateTime() * 1000);
                String day = format.format(date);
                mDatesArray[i] = day;
            }
        }
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
                if (mConnectionDetector.isNetworkAvailableAndConnected()) {
                    WeatherForecastUtil.getWeather(GraphsActivity.this, new GraphsWeatherForecastResultHandler(this));
                    setVisibleUpdating(true);
                } else {
                    Toast.makeText(this,
                                   R.string.connection_not_found,
                                   Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_toggle_values:
                toggleValues();
                return true;
            case R.id.action_toggle_yaxis:
                toggleYAxis();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleValues() {
        for (IDataSet set : mTemperatureChart.getData().getDataSets()) {
            set.setDrawValues(!set.isDrawValuesEnabled());
        }
        for (IDataSet set : mWindChart.getData().getDataSets()) {
            set.setDrawValues(!set.isDrawValuesEnabled());
        }
        for (IDataSet set : mRainChart.getData().getDataSets()) {
            set.setDrawValues(!set.isDrawValuesEnabled());
        }
        for (IDataSet set : mSnowChart.getData().getDataSets()) {
            set.setDrawValues(!set.isDrawValuesEnabled());
        }
        mTemperatureChart.invalidate();
        mWindChart.invalidate();
        mRainChart.invalidate();
        mSnowChart.invalidate();
    }

    private void toggleYAxis() {
        mTemperatureChart.getAxisLeft().setEnabled(!mTemperatureChart.getAxisLeft().isEnabled());
        mWindChart.getAxisLeft().setEnabled(!mWindChart.getAxisLeft().isEnabled());
        mRainChart.getAxisLeft().setEnabled(!mRainChart.getAxisLeft().isEnabled());
        mSnowChart.getAxisLeft().setEnabled(!mSnowChart.getAxisLeft().isEnabled());
        mTemperatureChart.invalidate();
        mWindChart.invalidate();
        mRainChart.invalidate();
        mSnowChart.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void setVisibleUpdating(boolean visible) {
        if (visible) {
            mGetWeatherProgress.show();
        } else {
            mGetWeatherProgress.cancel();
        }
    }

    public class GraphsWeatherForecastResultHandler implements WeatherForecastResultHandler {

        private Context context;

        public GraphsWeatherForecastResultHandler(Context context) {
            this.context = context;
        }

        public void processResources(CompleteWeatherForecast completeWeatherForecast, long lastUpdate) {
            long locationId = AppPreference.getCurrentLocationId(context);
            weatherForecastList.put(locationId, completeWeatherForecast.getWeatherForecastList());
            locationWeatherForecastLastUpdate.put(locationId, lastUpdate);
            mHandler.sendEmptyMessage(Constants.PARSE_RESULT_SUCCESS);
        }

        public void processError(Exception e) {
            mHandler.sendEmptyMessage(Constants.TASK_RESULT_ERROR);
            appendLog(getBaseContext(), TAG, "JSONException:", e);
        }
    }

    private void updateUI() {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        long locationId = AppPreference.getCurrentLocationId(this);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId);
        if (weatherForecastRecord != null) {
            weatherForecastList.put(locationId, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList());
            locationWeatherForecastLastUpdate.put(locationId, weatherForecastRecord.getLastUpdatedTime());
        }
        if ((weatherForecastRecord == null) || (locationWeatherForecastLastUpdate.get(locationId) + WeatherForecastActivity.AUTO_FORECAST_UPDATE_TIME_MILIS) <  Calendar.getInstance().getTimeInMillis()) {
            WeatherForecastUtil.getWeather(GraphsActivity.this, new GraphsWeatherForecastResultHandler(this));
            return;
        }

        TextView temperatureLabel = (TextView) findViewById(R.id.graphs_temperature_label);
        temperatureLabel.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        temperatureLabel.setTextColor(PreferenceUtil.getTextColor(this));
        TextView windLabel = (TextView) findViewById(R.id.graphs_wind_label);
        windLabel.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        windLabel.setTextColor(PreferenceUtil.getTextColor(this));
        TextView rainLabel = (TextView) findViewById(R.id.graphs_rain_label);
        rainLabel.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        rainLabel.setTextColor(PreferenceUtil.getTextColor(this));
        TextView snowLabel = (TextView) findViewById(R.id.graphs_snow_label);
        snowLabel.setBackgroundColor(PreferenceUtil.getBackgroundColor(this));
        snowLabel.setTextColor(PreferenceUtil.getTextColor(this));

        setTemperatureChart(locationId);
        setWindChart(locationId);
        setRainChart(locationId);
        setSnowChart(locationId);
    }
}
