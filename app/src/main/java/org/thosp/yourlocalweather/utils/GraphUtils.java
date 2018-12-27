package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GraphUtils {

    private static Map<Long, Bitmap> combinedGraphs = new HashMap<>();

    public static Bitmap getCombinedChart(Context context,
                                          List<DetailedWeatherForecast> weatherForecastList,
                                          long locationId,
                                          Locale locale) {

        if (combinedGraphs.get(locationId) != null) {
            return combinedGraphs.get(locationId);
        }

        CombinedChart combinedChart = generateCombinedGraph(context,
                                    null,
                                                            weatherForecastList,
                                                            locationId,
                                                            locale,
                                                            18f,
                                                            4,
                                                            0,
                                                            AppPreference.getTextColor(context),
                                                            AppPreference.getBackgroundColor(context),
                                                            AppPreference.getGraphGridColor(context));

        combinedChart.setBackgroundColor(ContextCompat.getColor(context,
                R.color.widget_transparentTheme_colorBackground));
        Bitmap combinedChartBitmap = Bitmap.createBitmap( 1236, 548, Bitmap.Config.ARGB_8888);
        Canvas combinedChartCanvas = new Canvas(combinedChartBitmap);
        combinedChart.layout(0, 0, 1236, 548);
        combinedChart.draw(combinedChartCanvas);
        combinedGraphs.put(locationId, combinedChartBitmap);
        return combinedChartBitmap;
    }

    public static void invalidateGraph(long locationId) {
        combinedGraphs.remove(locationId);
    }

    public static CombinedChart generateCombinedGraph(Context context,
                                                      CombinedChart combinedChartFromLayout,
                                                      List<DetailedWeatherForecast> weatherForecastList,
                                                      long locationId,
                                                      Locale locale,
                                                      Float textSize,
                                                      Integer yAxisValues,
                                                      int yAxisFractionalDigits,
                                                      int textColorId,
                                                      int backgroundColorId,
                                                      int gridColorId) {
        String[] mDatesArray;
        int daysCount;
        Set<Integer> combinedGraphValues = AppPreference.getCombinedGraphValues(context);
        CustomValueFormatter mValueFormatter = new CustomValueFormatter(locale);

        CombinedChart combinedChart = (combinedChartFromLayout != null) ? combinedChartFromLayout : new CombinedChart(context);
        Description graphDescription = new Description();
        graphDescription.setText("");
        combinedChart.setDescription(graphDescription);
        combinedChart.setDrawGridBackground(false);
        combinedChart.setTouchEnabled(true);
        combinedChart.setDragEnabled(true);
        combinedChart.setMaxHighlightDistance(300);
        combinedChart.setPinchZoom(true);
        combinedChart.getLegend().setEnabled(true);
        combinedChart.getLegend().setTextColor(textColorId);
        combinedChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        combinedChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        if (textSize != null) {
            combinedChart.getLegend().setTextSize(textSize);
        }
        combinedChart.setBackgroundColor(backgroundColorId);
        combinedChart.setGridBackgroundColor(textColorId);

        FormattedDate formatedDate = formatDate(weatherForecastList, locationId, locale);
        mDatesArray = formatedDate.getDatesArray();
        daysCount = formatedDate.getDaysCount();
        XAxis x = combinedChart.getXAxis();
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(true);
        x.setLabelCount(daysCount,true);
        x.setTextColor(textColorId);
        x.setValueFormatter(new XAxisValueFormatter(mDatesArray));
        if (textSize != null) {
            x.setTextSize(textSize);
        }

        int temperatureListSize = weatherForecastList.size();
        double[] temperatures = new double[temperatureListSize];
        double minTemperatureValue = Double.MAX_VALUE;
        double maxTemperatureValue = Double.MIN_VALUE;
        for (int i = 0; i < temperatureListSize; i++) {
            double temperature = TemperatureUtil.getTemperature(context, weatherForecastList.get(i));
            temperatures[i] = temperature;
            if (temperature < minTemperatureValue) {
                minTemperatureValue = temperature;
            }
            if (temperature > maxTemperatureValue) {
                maxTemperatureValue = temperature;
            }
        }

        maxTemperatureValue += 2;
        minTemperatureValue -= 2;

        List<Entry> temperatureEntries = new ArrayList<>();
        int entryCounter = 0;
        for (double temperature: temperatures) {
            temperatureEntries.add(new Entry(
                    entryCounter++,
                    (float) temperature));
        }

        LineDataSet set = new LineDataSet(temperatureEntries, context.getString(R.string.graph_temperature_day_label));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setValueTextSize(12f);
        set.setDrawValues(false);
        set.setColor(Color.parseColor("#E84E40"));
        set.setHighlightEnabled(false);
        set.setValueFormatter(mValueFormatter);
        set.setValueTextColor(textColorId);


        double valueShifter = 0;
        String temperatureUnitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (temperatureUnitsFromPreferences.contains("fahrenheit")) {
            if (combinedGraphValues.contains(1)) {
                valueShifter = 0;
                minTemperatureValue = 0;
            } else {
                valueShifter = 32;
            }
        }

        double multiplier;
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_PRESSURE_UNITS, "hpa");
        switch (unitsFromPreferences) {
            case "inhg": multiplier = 50; break;
            default: multiplier = 1;
        }

        int pressuresSize = weatherForecastList.size();
        double[] pressures = new double[pressuresSize];
        double minPressureValue = Double.MAX_VALUE;
        double maxPressureValue = Double.MIN_VALUE;
        for (int i = 0; i < pressuresSize; i++) {
            PressureWithUnit pressureWithUnit = AppPreference.getPressureWithUnit(
                    context,
                    weatherForecastList.get(i).getPressure(),
                    locale);
            double pressure = multiplier * pressureWithUnit.getPressure();
            pressures[i] = pressure + valueShifter;
            if (pressure < minPressureValue) {
                minPressureValue = pressure;
            }
            if (pressure > maxPressureValue) {
                maxPressureValue = pressure;
            }
        }

        List<Entry> pressureEntries = new ArrayList<>();
        entryCounter = 0;
        for (double pressure: pressures) {
            pressureEntries.add(new Entry(
                    entryCounter++,
                    (float) (pressure - minPressureValue)));
        }

        double pressureScale = (maxPressureValue - minPressureValue);

        double maxValueOnGraph;

        if (maxTemperatureValue > pressureScale) {
            maxValueOnGraph = maxTemperatureValue;
        } else {
            maxValueOnGraph = pressureScale + 2;
        }

        double negativeValue = 0 - minTemperatureValue;
        if (negativeValue > 0) {
            minPressureValue = minPressureValue - negativeValue;
        }

        LineDataSet pressureSet = new LineDataSet(pressureEntries, context.getString(R.string.graph_pressure_label));
        pressureSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        pressureSet.setCubicIntensity(0.2f);
        pressureSet.setDrawCircles(false);
        pressureSet.setLineWidth(2f);
        pressureSet.setValueTextSize(12f);
        pressureSet.setDrawValues(false);
        pressureSet.setColor(Color.parseColor("#20cb02"));
        pressureSet.setHighlightEnabled(false);
        pressureSet.setValueFormatter(mValueFormatter);
        pressureSet.setValueTextColor(textColorId);

        List<BarEntry> rainEntries = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(i);
            rainEntries.add(new BarEntry(
                    i,
                    (float) (valueShifter + AppPreference.getRainOrSnow(
                            context, (10 * (detailedWeatherForecast.getRain() + detailedWeatherForecast.getSnow())))))
            );
        }

        BarDataSet rainSet = new BarDataSet(rainEntries, context.getString(R.string.graph_rain_label));
        rainSet.setValueTextSize(12f);
        rainSet.setDrawValues(false);
        rainSet.setColor(Color.parseColor("#5677FC"));
        rainSet.setHighlightEnabled(false);
        rainSet.setValueFormatter(mValueFormatter);
        rainSet.setValueTextColor(textColorId);

        List<Entry> windEntries = new ArrayList<>();
        int windSize = weatherForecastList.size();
        double minWindValue = Double.MAX_VALUE;
        double maxWindValue = Double.MIN_VALUE;
        for (int i = 0; i < windSize; i++) {
            double wind = AppPreference.getWind(context, weatherForecastList.get(i).getWindSpeed());
            if (wind < minWindValue) {
                minWindValue = wind;
            }
            if (wind > maxWindValue) {
                maxWindValue = wind;
            }
            windEntries.add(new Entry(i, (float) (wind + valueShifter)));
        }

        LineDataSet windSet = new LineDataSet(windEntries, context.getString(R.string.graph_wind_label));
        windSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        windSet.setCubicIntensity(0.2f);
        windSet.setDrawCircles(false);
        windSet.setLineWidth(2f);
        windSet.setValueTextSize(12f);
        windSet.setDrawValues(false);
        windSet.setColor(Color.parseColor("#00BCD4"));
        windSet.setHighlightEnabled(false);
        windSet.setValueFormatter(mValueFormatter);
        windSet.setValueTextColor(textColorId);

        YAxis yLeft = combinedChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(textColorId);
        yLeft.setGridColor(gridColorId);
        yLeft.setZeroLineWidth(20f);
        if (textSize != null) {
            yLeft.setTextSize(textSize);
        }
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, TemperatureUtil.getTemperatureUnit(context)));
        if (yAxisValues != null) {
            yLeft.setLabelCount(yAxisValues, true);
        }
        double axisMaximum = Math.ceil(maxValueOnGraph);
        double axisMinimum = Math.floor(minTemperatureValue);
        if (yAxisValues != null) {
            int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
            int halfOfTHeDifference = restForRange / 2;
            int restOfTheDifference = restForRange % 2;
            axisMaximum += halfOfTHeDifference + restOfTheDifference;
            axisMinimum -= halfOfTHeDifference;
        }
        yLeft.setAxisMaximum((float) (axisMaximum));
        yLeft.setAxisMinimum((float) (axisMinimum));

        YAxis yRight = combinedChart.getAxisRight();
        yRight.setEnabled(true);
        yRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yRight.setDrawAxisLine(false);
        yRight.setDrawGridLines(true);
        yRight.enableGridDashedLine(5f, 10f, 0f);
        yRight.setTextColor(textColorId);
        yRight.setGridColor(gridColorId);
        yRight.setZeroLineWidth(20f);
        if (textSize != null) {
            yRight.setTextSize(textSize);
        }
        yRight.setXOffset(15);
        if (yAxisValues != null) {
            yRight.setLabelCount(yAxisValues, true);
        }
        if (combinedGraphValues.contains(2)) {
            axisMaximum = Math.ceil((maxWindValue + 2));
            axisMinimum = Math.floor(minWindValue);
            if (yAxisValues != null) {
                int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
                int halfOfTHeDifference = restForRange / 2;
                int restOfTheDifference = restForRange % 2;
                axisMaximum += halfOfTHeDifference + restOfTheDifference;
                axisMinimum -= halfOfTHeDifference;
            }
            yRight.setAxisMaximum((float) (axisMaximum));
            yRight.setAxisMinimum((float) (axisMinimum));
            yRight.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getWindUnit(context)));
        } else if (combinedGraphValues.contains(3)) {
            axisMaximum = Math.ceil(((maxPressureValue + 2) / multiplier));
            axisMinimum = Math.floor(minPressureValue / multiplier);
            if (yAxisValues != null) {
                int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
                int halfOfTHeDifference = restForRange / 2;
                int restOfTheDifference = restForRange % 2;
                axisMaximum += halfOfTHeDifference + restOfTheDifference;
                axisMinimum -= halfOfTHeDifference;
            }
            yRight.setAxisMaximum((float) (axisMaximum));
            yRight.setAxisMinimum((float) (axisMinimum));
            yRight.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getPressureUnit(context)));
        } else {
            yRight.setEnabled(false);
        }

        combinedChart.clear();
        LineData lineData = new LineData();
        if (combinedGraphValues.contains(0)) {
            lineData.addDataSet(set);
        }
        if (combinedGraphValues.contains(3)) {
            lineData.addDataSet(pressureSet);
        }
        if (combinedGraphValues.contains(2)) {
            lineData.addDataSet(windSet);
        }
        CombinedData data = new CombinedData();
        data.setData(lineData);
        if (combinedGraphValues.contains(1)) {
            BarData rainData = new BarData();
            rainData.addDataSet(rainSet);
            data.setData(rainData);
        } else {
            BarData rainData = new BarData();
            data.setData(rainData);
        }
        combinedChart.setData(data);
        combinedChart.invalidate();
        return combinedChart;
    }

    public static FormattedDate formatDate(List<DetailedWeatherForecast> weatherForecastList, long locationId, Locale locale) {
        String[] mDatesArray = new String[0];
        int daysCount;
        SimpleDateFormat format = new SimpleDateFormat("EEE", locale);
        if (weatherForecastList != null) {
            int mSize = weatherForecastList.size();
            List<String> uniqueDate = new ArrayList<>();
            mDatesArray = new String[mSize];

            for (int i = 0; i < mSize; i++) {
                Date date = new Date(weatherForecastList.get(i).getDateTime() * 1000);
                String day = format.format(date);
                if (!uniqueDate.contains(day)) {
                    uniqueDate.add(day);
                }
                mDatesArray[i] = day;
            }
            daysCount = uniqueDate.size();
        } else {
            daysCount = 0;
        }

        return new FormattedDate(mDatesArray, daysCount);
    }

    public static class FormattedDate {
        private String[] mDatesArray;
        private int daysCount;

        public FormattedDate(String[] mDatesArray, int daysCount) {
            this.daysCount = daysCount;
            this.mDatesArray = mDatesArray;
        }

        public String[] getDatesArray() {
            return mDatesArray;
        }

        public int getDaysCount() {
            return daysCount;
        }
    }
}
