package org.thosp.yourlocalweather.utils;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
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
import org.thosp.yourlocalweather.WidgetSettingsDialogue;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class GraphUtils {

    private static Map<Integer, Bitmap> combinedGraphs = new HashMap<>();

    public static Bitmap getCombinedChart(Context context,
                                          int widgetId,
                                          Float heightMultiplier,
                                          List<DetailedWeatherForecast> weatherForecastList,
                                          long locationId,
                                          Locale locale) {

        if (combinedGraphs.get(widgetId) != null) {
            return combinedGraphs.get(widgetId);
        }

        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        Boolean showLegend = widgetSettingsDbHelper.getParamBoolean(widgetId, "combinedGraphShowLegend");

        if (showLegend == null) {
            showLegend = true;
        }

        int[] size = getWidgetSize(context, widgetId);
        int width = size[0];
        int height;
        if (heightMultiplier == null) {
            height = size[1];
        } else {
            height = (int) (width * heightMultiplier);
        }

        int yAxisValues = 4;
        if (height > 800) {
            yAxisValues += 6;
        } else if (height > 700) {
            yAxisValues += 4;
        } else if (height > 500) {
            yAxisValues += 2;
        }

        CombinedChart combinedChart = generateCombinedGraph(context,
                                    null,
                                                            getCombinedGraphValuesFromSettings(context, widgetSettingsDbHelper, widgetId),
                                                            weatherForecastList,
                                                            locationId,
                                                            locale,
                                                            18f,
                                                            yAxisValues,
                                                            0,
                                                            AppPreference.getTextColor(context),
                                                            AppPreference.getWidgetBackgroundColor(context),
                                                            AppPreference.getWidgetGraphGridColor(context),
                                                            showLegend);

        combinedChart.setBackgroundColor(ContextCompat.getColor(context,
                R.color.widget_transparentTheme_colorBackground));

        int bitmapHeight = height;

        if (!showLegend) {
            bitmapHeight += 20;
        }

        Bitmap.Config bitmapConfig = Bitmap.Config.ARGB_8888;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bitmapConfig = Bitmap.Config.RGBA_F16;
        }
        Bitmap combinedChartBitmap = Bitmap.createBitmap(width, bitmapHeight, bitmapConfig);
        Canvas combinedChartCanvas = new Canvas(combinedChartBitmap);
        combinedChart.layout(0, 0, width, height);
        combinedChart.draw(combinedChartCanvas);
        combinedGraphs.put(widgetId, combinedChartBitmap);
        return combinedChartBitmap;
    }

    protected static int[] getWidgetSize(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
                context.getApplicationContext());

        AppWidgetProviderInfo providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);

        int mWidgetLandWidth = providerInfo.minWidth;
        int mWidgetPortHeight = providerInfo.minHeight;
        int mWidgetPortWidth = providerInfo.minWidth;
        int mWidgetLandHeight = providerInfo.minHeight;

        Bundle mAppWidgetOptions = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mAppWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        }

        if (mAppWidgetOptions != null
                && mAppWidgetOptions
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) > 0) {

            mWidgetPortWidth = mAppWidgetOptions
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            mWidgetLandWidth = mAppWidgetOptions
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            mWidgetLandHeight = mAppWidgetOptions
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            mWidgetPortHeight = mAppWidgetOptions
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        } else {
            mWidgetLandWidth = providerInfo.minWidth;
            mWidgetPortHeight = providerInfo.minHeight;
            mWidgetPortWidth = providerInfo.minWidth;
            mWidgetLandHeight = providerInfo.minHeight;
        }

        int mWidgetWidthPerOrientation = mWidgetPortWidth;
        int mWidgetHeightPerOrientation = mWidgetPortHeight;
        if (!isPortrait(context)) {
            mWidgetWidthPerOrientation = mWidgetLandWidth;
            mWidgetHeightPerOrientation = mWidgetLandHeight;
        }
        int[] size = new int[2];
        if (AppPreference.isWidgetGraphNativeScaled(context)) {
            size[0] = mWidgetWidthPerOrientation;
            size[1] = mWidgetHeightPerOrientation;
            return size;
        }
        size[0] = dipToPixels(context, mWidgetWidthPerOrientation);
        size[1] = dipToPixels(context, mWidgetHeightPerOrientation);
        return size;
    }

    public static Set<Integer> getCombinedGraphValuesFromSettings(Context context, WidgetSettingsDbHelper widgetSettingsDbHelper, int widgetId) {
        Set<Integer> combinedGraphValues = new HashSet<>();

        String storedGraphValues = widgetSettingsDbHelper.getParamString(widgetId, "combinedGraphValues");
        if ((storedGraphValues == null) || !storedGraphValues.contains(",")) {
            combinedGraphValues = AppPreference.getCombinedGraphValues(context);
            StringBuilder valuesToStore = new StringBuilder();
            for (int selectedValue: combinedGraphValues) {
                valuesToStore.append(selectedValue);
                valuesToStore.append(",");
            }
            widgetSettingsDbHelper.saveParamString(widgetId, "combinedGraphValues", valuesToStore.toString());
        } else {
            String[] values = storedGraphValues.split(",");
            for (String value: values) {
                int intValue;
                try {
                    intValue = Integer.parseInt(value);
                    combinedGraphValues.add(intValue);
                } catch (Exception e) {
                    //do nothing, just continue
                }
            }
        }

        return combinedGraphValues;
    }

    public static int dipToPixels(Context context, int dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    private static boolean isPortrait (Context cx) {
        Display d = ((WindowManager) cx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if (d.getWidth() == d.getHeight()) {
            return false;
        } else if (d.getWidth() < d.getHeight()) {
            return true;
        } else {
            return false;
        }
    }

    public static void invalidateGraph() {
        combinedGraphs = new HashMap<>();
    }

    public static CombinedChart generateCombinedGraph(Context context,
                                                      CombinedChart combinedChartFromLayout,
                                                      Set<Integer> combinedGraphValues,
                                                      List<DetailedWeatherForecast> weatherForecastList,
                                                      long locationId,
                                                      Locale locale,
                                                      Float textSize,
                                                      Integer yAxisValues,
                                                      int yAxisFractionalDigits,
                                                      int textColorId,
                                                      int backgroundColorId,
                                                      AppPreference.GraphGridColors gridColorId,
                                                      boolean showLegend) {

        CustomValueFormatter mValueFormatter = new CustomValueFormatter(locale);

        boolean pressure = false;
        boolean rainsnow = false;
        boolean wind = false;
        boolean temperature = false;
        CombinedGraph leftYaxis = null;
        CombinedGraph rightYaxis = null;

        if (combinedGraphValues.contains(0)) {
            temperature = true;
            leftYaxis = CombinedGraph.TEMPERATURE;
        }
        if (combinedGraphValues.contains(2)) {
            wind = true;
            if (leftYaxis == null) {
                leftYaxis = CombinedGraph.WIND;
            } else {
                rightYaxis = CombinedGraph.WIND;
            }
        }
        if (combinedGraphValues.contains(3)) {
            pressure = true;
            if (leftYaxis == null) {
                leftYaxis = CombinedGraph.PRESSURE;
            } else {
                rightYaxis = CombinedGraph.PRESSURE;
            }
        }
        if (combinedGraphValues.contains(1)) {
            rainsnow = true;
            if (leftYaxis == null) {
                leftYaxis = CombinedGraph.RAINSNOW;
            } else if (rightYaxis == null) {
                rightYaxis = CombinedGraph.RAINSNOW;
            }
        }

        CombinedChart combinedChart = (combinedChartFromLayout != null) ? combinedChartFromLayout : new CombinedChart(context);
        Description graphDescription = new Description();
        graphDescription.setText("");
        combinedChart.setDescription(graphDescription);
        combinedChart.setDrawGridBackground(false);
        combinedChart.setTouchEnabled(true);
        combinedChart.setDragEnabled(true);
        combinedChart.setMaxHighlightDistance(300);
        combinedChart.setPinchZoom(true);
        combinedChart.getLegend().setEnabled(showLegend);
        combinedChart.getLegend().setTextColor(textColorId);
        combinedChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        combinedChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        if (textSize != null) {
            combinedChart.getLegend().setTextSize(textSize);
        }
        combinedChart.setBackgroundColor(backgroundColorId);
        combinedChart.setGridBackgroundColor(textColorId);

        setupXAxis(combinedChart.getXAxis(), weatherForecastList, textColorId, textSize, gridColorId, locale);

        int temperatureListSize = weatherForecastList.size();
        double[] temperatures = new double[temperatureListSize];
        double minTemperatureValue = Double.MAX_VALUE;
        double maxTemperatureValue = Double.MIN_VALUE;
        for (int i = 0; i < temperatureListSize; i++) {
            double temperatureValue = TemperatureUtil.getTemperature(context, weatherForecastList.get(i));
            temperatures[i] = temperatureValue;
            if (temperatureValue < minTemperatureValue) {
                minTemperatureValue = temperatureValue;
            }
            if (temperatureValue > maxTemperatureValue) {
                maxTemperatureValue = temperatureValue;
            }
        }

        maxTemperatureValue += 1;
        minTemperatureValue -= 1;

        List<Entry> temperatureEntries = new ArrayList<>();
        int entryCounter = 0;
        for (double temperatureForEntry: temperatures) {
            temperatureEntries.add(new Entry(
                    entryCounter++,
                    (float) temperatureForEntry));
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
            double pressureValue = multiplier * pressureWithUnit.getPressure();
            pressures[i] = pressureValue + valueShifter;
            if (pressureValue < minPressureValue) {
                minPressureValue = pressureValue;
            }
            if (pressureValue > maxPressureValue) {
                maxPressureValue = pressureValue;
            }
        }

        List<Entry> pressureEntries = new ArrayList<>();
        entryCounter = 0;
        for (double pressureForEntry: pressures) {
            pressureEntries.add(new Entry(
                    entryCounter++,
                    (float) pressureForEntry));
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
        if (rightYaxis == CombinedGraph.PRESSURE) {
            pressureSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        }

        List<BarEntry> rainEntries = new ArrayList<>();
        int rainSnowSize = weatherForecastList.size();
        float[] rains = new float[rainSnowSize];
        float[] snows = new float[rainSnowSize];
        double minRainSnowValue = Double.MAX_VALUE;
        double maxRainSnowValue = Double.MIN_VALUE;
        boolean isRain = false;
        boolean isSnow = false;
        double rainSnowmultiplier = 1;
        for (int i = 0; i < weatherForecastList.size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(i);
            if ((leftYaxis != CombinedGraph.RAINSNOW) && (rightYaxis != CombinedGraph.RAINSNOW)) {
                rainSnowmultiplier = 10;
            }
            double rainValue = rainSnowmultiplier * AppPreference.getRainOrSnow(
                    context, detailedWeatherForecast.getRain());
            if (!isRain && (rainValue > 0)) {
                isRain = true;
            }
            double snowValue = rainSnowmultiplier * AppPreference.getRainOrSnow(
                    context, detailedWeatherForecast.getSnow());
            if (!isSnow && (snowValue > 0)) {
                isSnow = true;
            }
            double rainsnowValue = snowValue + rainValue;
            if (rainsnowValue < minRainSnowValue) {
                minRainSnowValue = rainsnowValue;
            }
            if (rainsnowValue > maxRainSnowValue) {
                maxRainSnowValue = rainsnowValue;
            }
            rains[i] = (float) (valueShifter + rainValue);
            snows[i] = (float) (valueShifter + snowValue);
        }

        boolean isRainSnowVector = isRain && isSnow;
        for (int i = 0; i < weatherForecastList.size(); i++) {
            if (isRainSnowVector) {
                float[] rainsnowBarData = new float[2];
                rainsnowBarData[0] = rains[i];
                rainsnowBarData[1] = snows[i];
                rainEntries.add(new BarEntry(
                        i,
                        rainsnowBarData));
            } else if (isRain){
                rainEntries.add(new BarEntry(
                        i,
                        rains[i]));
            } else if (isSnow){
                rainEntries.add(new BarEntry(
                        i,
                        snows[i]));
            } else {
                rainEntries.add(new BarEntry(
                        i,
                        rains[i]));
            }
        }

        String[] rainSnowLabels = getRainSnowLabelForCombinedGraph(context, locale, rainSnowmultiplier, isRain, isSnow);
        boolean twoBars = rainSnowLabels.length > 1;
        BarDataSet rainSet = new BarDataSet(rainEntries, (twoBars)? null : rainSnowLabels[0]);
        rainSet.setValueTextSize(12f);
        rainSet.setDrawValues(false);
        rainSet.setHighlightEnabled(false);
        rainSet.setValueFormatter(mValueFormatter);
        rainSet.setValueTextColor(textColorId);
        if (twoBars) {
            rainSet.setColors(Color.parseColor("#5677FC"), Color.parseColor("#aaaaff"));
        } else if (isSnow) {
            rainSet.setColor(Color.parseColor("#ccd6fe"));
        } else {
            rainSet.setColor(Color.parseColor("#5677FC"));
        }
        if (twoBars) {
            rainSet.setStackLabels(rainSnowLabels);
        }
        if (rightYaxis == CombinedGraph.RAINSNOW) {
            rainSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        }

        List<Entry> windEntries = new ArrayList<>();
        int windSize = weatherForecastList.size();
        double minWindValue = Double.MAX_VALUE;
        double maxWindValue = Double.MIN_VALUE;
        for (int i = 0; i < windSize; i++) {
            double windSpeed = AppPreference.getWind(context, weatherForecastList.get(i).getWindSpeed());
            if (windSpeed < minWindValue) {
                minWindValue = windSpeed;
            }
            if (windSpeed > maxWindValue) {
                maxWindValue = windSpeed;
            }
            windEntries.add(new Entry(i, (float) (windSpeed + valueShifter)));
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
        if (rightYaxis == CombinedGraph.WIND) {
            windSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        }

        double pressureScale = 0;
        if (pressure) {
            pressureScale = (maxPressureValue - minPressureValue);
        }
        if (rainsnow && (rightYaxis != CombinedGraph.RAINSNOW)) {
            pressureScale = (maxRainSnowValue - minRainSnowValue) + 2;
        }

        double maxValueOnGraph;

        if (maxTemperatureValue > pressureScale) {
            maxValueOnGraph = maxTemperatureValue;
        } else {
            maxValueOnGraph = pressureScale + 1;
        }


        YAxis yLeft = combinedChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setTextColor(textColorId);
        yLeft.setGridColor(gridColorId.getMainGridColor());
        yLeft.setZeroLineWidth(20f);
        if (textSize != null) {
            yLeft.setTextSize(textSize);
        }
        yLeft.setXOffset(15);
        if (yAxisValues != null) {
            yLeft.setLabelCount(yAxisValues, true);
        }
        if (leftYaxis == CombinedGraph.TEMPERATURE) {
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, TemperatureUtil.getTemperatureUnit(context)));
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
        } else if (leftYaxis == CombinedGraph.WIND) {
            double axisMaximum = Math.ceil((maxWindValue + 1));
            double axisMinimum = Math.floor(minWindValue);
            if (yAxisValues != null) {
                int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
                int halfOfTHeDifference = restForRange / 2;
                int restOfTheDifference = restForRange % 2;
                axisMaximum += halfOfTHeDifference + restOfTheDifference;
                axisMinimum -= halfOfTHeDifference;
            }
            yLeft.setAxisMaximum((float) (axisMaximum));
            yLeft.setAxisMinimum((float) (axisMinimum));
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getWindUnit(context)));
        } else if (leftYaxis == CombinedGraph.PRESSURE) {
            double axisMaximum = Math.ceil(((maxPressureValue + 2) / multiplier));
            double axisMinimum = Math.floor(minPressureValue / multiplier);
            if (yAxisValues != null) {
                int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
                int halfOfTHeDifference = restForRange / 2;
                int restOfTheDifference = restForRange % 2;
                axisMaximum += halfOfTHeDifference + restOfTheDifference;
                axisMinimum -= halfOfTHeDifference;
            }
            yLeft.setAxisMaximum((float) (axisMaximum));
            yLeft.setAxisMinimum((float) (axisMinimum));
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getPressureUnit(context)));
        } else if (leftYaxis == CombinedGraph.RAINSNOW) {
            double axisMaximum = Math.ceil((maxRainSnowValue + 1));
            double axisMinimum = Math.floor(minRainSnowValue);
            if (yAxisValues != null) {
                int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
                int halfOfTHeDifference = restForRange / 2;
                int restOfTheDifference = restForRange % 2;
                axisMaximum += halfOfTHeDifference + restOfTheDifference;
                axisMinimum -= halfOfTHeDifference;
            }
            yLeft.setAxisMaximum((float) (axisMaximum));
            yLeft.setAxisMinimum((float) (axisMinimum));
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, context.getString(AppPreference.getRainOrSnowUnit(context))));
        }
        LimitLine zerolimitLine = new LimitLine(0);
        zerolimitLine.setLineColor(gridColorId.getMainGridColor());
        zerolimitLine.setLineWidth(0.5f);
        yLeft.addLimitLine(zerolimitLine);

        YAxis yRight = combinedChart.getAxisRight();
        yRight.setEnabled(true);
        yRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yRight.setDrawAxisLine(false);
        yRight.setDrawGridLines(true);
        yRight.enableGridDashedLine(5f, 10f, 0f);
        yRight.setTextColor(textColorId);
        yRight.setGridColor(gridColorId.getMainGridColor());
        yRight.setZeroLineWidth(20f);
        if (textSize != null) {
            yRight.setTextSize(textSize);
        }
        yRight.setXOffset(15);
        if (yAxisValues != null) {
            yRight.setLabelCount(yAxisValues, true);
        }
        if (rightYaxis == CombinedGraph.WIND) {
            double axisMaximum = Math.ceil((maxWindValue + 1));
            double axisMinimum = Math.floor(minWindValue);
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
        } else if (rightYaxis == CombinedGraph.PRESSURE) {
            double axisMaximum = Math.ceil(((maxPressureValue + 1) / multiplier));
            double axisMinimum = Math.floor(minPressureValue / multiplier);
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
        } else if (rightYaxis == CombinedGraph.RAINSNOW) {
            double axisMaximum = Math.ceil((maxRainSnowValue + 1));
            double axisMinimum = Math.floor(minRainSnowValue);
            if (yAxisValues != null) {
                int restForRange = (yAxisValues - 1) - (((int) (axisMaximum - axisMinimum)) % (yAxisValues - 1));
                int halfOfTHeDifference = restForRange / 2;
                int restOfTheDifference = restForRange % 2;
                axisMaximum += halfOfTHeDifference + restOfTheDifference;
                axisMinimum -= halfOfTHeDifference;
            }
            yRight.setAxisMaximum((float) (axisMaximum));
            yRight.setAxisMinimum((float) (axisMinimum));
            yRight.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, context.getString(AppPreference.getRainOrSnowUnit(context))));
        }
        if (rightYaxis == null) {
            yRight.setEnabled(false);
        } else {
            yRight.setEnabled(true);
        }

        combinedChart.clear();
        LineData lineData = new LineData();
        if (temperature) {
            lineData.addDataSet(set);
        }
        if (pressure) {
            lineData.addDataSet(pressureSet);
        }
        if (wind) {
            lineData.addDataSet(windSet);
        }
        CombinedData data = new CombinedData();
        data.setData(lineData);
        if (rainsnow) {
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

    public static void setupXAxis(XAxis x,
                                  List<DetailedWeatherForecast> weatherForecastList,
                                  int textColorId,
                                  Float textSize,
                                  AppPreference.GraphGridColors gridColor,
                                  Locale locale) {
        Map<Integer, Long> hourIndexes = new HashMap<>();

        int lastDayOflimitLine = 0;
        for (int i = 0; i < weatherForecastList.size(); i++) {
            hourIndexes.put(i, weatherForecastList.get(i).getDateTime());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(weatherForecastList.get(i).getDateTime() * 1000);
            if (cal.get(Calendar.DAY_OF_YEAR) != lastDayOflimitLine) {
                Calendar calOfPreviousRecord = Calendar.getInstance();
                int previousRecordHour = 24;
                if (i > 0) {
                    calOfPreviousRecord.setTimeInMillis(weatherForecastList.get(i - 1).getDateTime() * 1000);
                    previousRecordHour = calOfPreviousRecord.get(Calendar.HOUR_OF_DAY);
                }
                int currentHour = cal.get(Calendar.HOUR_OF_DAY);
                float timeSpan = (24 - previousRecordHour) + currentHour;
                float dayLine = currentHour / timeSpan;
                float midnight = i - dayLine;
                float hour6 = midnight + (6 / timeSpan);
                float hour12 = midnight + (12 / timeSpan);
                float hour18 = midnight + (18 / timeSpan);
                LimitLine limitLine = new LimitLine(midnight);
                limitLine.setLineColor(gridColor.getMainGridColor());
                limitLine.setLineWidth(0.5f);
                x.addLimitLine(limitLine);
                /*LimitLine limitLine6 = new LimitLine(hour6, "");
                limitLine6.setLineColor(Color.LTGRAY);
                limitLine6.setLineWidth(0.5f);
                x.addLimitLine(limitLine6);*/
                LimitLine limitLine12 = new LimitLine(hour12);
                limitLine12.setLineColor(gridColor.getSecondaryGridColor());
                limitLine12.setLineWidth(0.5f);
                x.addLimitLine(limitLine12);
                /*LimitLine limitLine18 = new LimitLine(hour18, "");
                limitLine18.setLineColor(Color.LTGRAY);
                limitLine18.setLineWidth(0.5f);
                x.addLimitLine(limitLine18);*/
                lastDayOflimitLine = cal.get(Calendar.DAY_OF_YEAR);
            }
        }

        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setLabelCount(25, true);
        x.setTextColor(textColorId);
        x.setValueFormatter(new XAxisValueFormatter(hourIndexes, locale));
        x.setDrawLimitLinesBehindData(true);

        if (textSize != null) {
            x.setTextSize(textSize);
        }
    }

    public static String[] getRainSnowLabelForCombinedGraph(Context context, Locale locale, double multiplier, boolean isRain, boolean isSnow) {
        NumberFormat decimalFormat = NumberFormat.getNumberInstance(locale);
        decimalFormat.setMaximumFractionDigits(1);
        decimalFormat.setMinimumFractionDigits(1);

        int resultSize = 1;
        if (isRain && isSnow) {
            resultSize = 2;
        }
        String[] result = new String[resultSize];
        StringBuilder rainLabelBuilder = new StringBuilder();
        StringBuilder snowLabelBuilder = new StringBuilder();
        if (isRain) {
            rainLabelBuilder.append(context.getString(R.string.graph_rain_label));
        }
        if (isSnow) {
            snowLabelBuilder.append(context.getString(R.string.graph_snow_label));
        }
        if (!isRain && !isSnow) {
            rainLabelBuilder.append(context.getString(R.string.graph_rain_label));
        }

        StringBuilder addInfoLabelBuilder;
        if (isRain && isSnow) {
            addInfoLabelBuilder = snowLabelBuilder;
        } else if (isRain) {
            addInfoLabelBuilder = rainLabelBuilder;
        } else if (isSnow) {
            addInfoLabelBuilder = snowLabelBuilder;
        } else {
            addInfoLabelBuilder = rainLabelBuilder;
        }

        if (multiplier != 1.0) {
            addInfoLabelBuilder.append(" (*");
            addInfoLabelBuilder.append(decimalFormat.format(1 / multiplier));
            addInfoLabelBuilder.append(" ");
            addInfoLabelBuilder.append(context.getString(AppPreference.getRainOrSnowUnit(context)));
            addInfoLabelBuilder.append(" on ");
            addInfoLabelBuilder.append(TemperatureUtil.getTemperatureUnit(context));
            addInfoLabelBuilder.append(")");
        }
        if (isRain && isSnow) {
            result[0] = rainLabelBuilder.toString();
            result[1] = snowLabelBuilder.toString();
        } else if (isRain) {
            result[0] = rainLabelBuilder.toString();
        } else if (isSnow){
            result[0] = snowLabelBuilder.toString();
        } else {
            result[0] = rainLabelBuilder.toString();
        }
        return result;
    }
}
