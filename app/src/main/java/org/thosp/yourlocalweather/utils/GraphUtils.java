package org.thosp.yourlocalweather.utils;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import org.thosp.charting.charts.CombinedChart;
import org.thosp.charting.components.Description;
import org.thosp.charting.components.Legend;
import org.thosp.charting.components.LimitLine;
import org.thosp.charting.components.XAxis;
import org.thosp.charting.components.YAxis;
import org.thosp.charting.data.BarData;
import org.thosp.charting.data.BarDataSet;
import org.thosp.charting.data.BarEntry;
import org.thosp.charting.data.CombinedData;
import org.thosp.charting.data.Entry;
import org.thosp.charting.data.LineData;
import org.thosp.charting.data.LineDataSet;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class GraphUtils {

    private static Map<Integer, Bitmap> combinedGraphs = new HashMap<>();

    public static Bitmap getCombinedChart(Context context,
                                          int widgetId,
                                          Float heightMultiplier,
                                          List<DetailedWeatherForecast> weatherForecastList,
                                          long locationId,
                                          Locale locale,
                                          Boolean showLegend,
                                          Set<Integer> combinedGraphValuesFromSettings,
                                          int widgetTextColor,
                                          int widgetBackgroundColor,
                                          AppPreference.GraphGridColors widgetGraphGridColor,
                                          String temperatureUnitFromPreferences,
                                          String pressureUnitFromPreferences,
                                          String rainSnowUnitFromPreferences,
                                          boolean widgetGraphNativeScaled,
                                          String windUnitFromPreferences) {

        if (combinedGraphs.get(widgetId) != null) {
            return combinedGraphs.get(widgetId);
        }

        if (showLegend == null) {
            showLegend = true;
        }

        int[] size = getWidgetSize(context, widgetGraphNativeScaled, widgetId);
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
                                                            combinedGraphValuesFromSettings,
                                                            weatherForecastList,
                                                            locale,
                                                            18f,
                                                            yAxisValues,
                                                            0,
                                                            widgetTextColor,
                                                            widgetBackgroundColor,
                                                            widgetGraphGridColor,
                                                            showLegend,
                                                            temperatureUnitFromPreferences,
                                                            pressureUnitFromPreferences,
                                                            rainSnowUnitFromPreferences,
                                                            windUnitFromPreferences);

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

    protected static int[] getWidgetSize(Context context, boolean widgetGraphNativeScaled, int appWidgetId) {
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
        /*if (!isPortrait(context)) {
            mWidgetWidthPerOrientation = mWidgetLandWidth;
            mWidgetHeightPerOrientation = mWidgetLandHeight;
        }*/
        int[] size = new int[2];
        if (widgetGraphNativeScaled) {
            size[0] = mWidgetWidthPerOrientation;
            size[1] = mWidgetHeightPerOrientation;
            return size;
        }
        size[0] = dipToPixels(context, mWidgetWidthPerOrientation);
        size[1] = dipToPixels(context, mWidgetHeightPerOrientation);
        return size;
    }

    public static Set<Integer> getCombinedGraphValuesFromSettings(Set<Integer> combinedGraphValuesFromPreferences, WidgetSettingsDbHelper widgetSettingsDbHelper, int widgetId) {
        Set<Integer> combinedGraphValues = new HashSet<>();

        String storedGraphValues = widgetSettingsDbHelper.getParamString(widgetId, "combinedGraphValues");
        if ((storedGraphValues == null) || !storedGraphValues.contains(",")) {
            combinedGraphValues = combinedGraphValuesFromPreferences;
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
        } else {
            return d.getWidth() < d.getHeight();
        }
    }

    public static void invalidateGraph() {
        combinedGraphs = new HashMap<>();
    }

    public static CombinedChart generateCombinedGraph(Context context,
                                                      CombinedChart combinedChartFromLayout,
                                                      Set<Integer> combinedGraphValues,
                                                      List<DetailedWeatherForecast> weatherForecastList,
                                                      Locale locale,
                                                      Float textSize,
                                                      Integer yAxisValues,
                                                      int yAxisFractionalDigits,
                                                      int textColorId,
                                                      int backgroundColorId,
                                                      AppPreference.GraphGridColors gridColorId,
                                                      boolean showLegend,
                                                      String temperatureUnitFromPreferences,
                                                      String pressureUnitFromPreferences,
                                                      String rainSnowUnitFromPreferences,
                                                      String windUnitFromPreferences) {

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
        combinedChart.setAxisCount(3);
        //combinedChart.setLogEnabled(true);
        combinedChart.init();
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
            double temperatureValue = TemperatureUtil.getTemperature(context, temperatureUnitFromPreferences, weatherForecastList.get(i));
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
            if (entryCounter > 0) {
                boolean fromFreezeToHot = (temperatures[entryCounter - 1] < 0) && (temperatureForEntry > 0);
                boolean fromHotToFreeze = (temperatures[entryCounter - 1] > 0) && (temperatureForEntry < 0);

                if (fromFreezeToHot || fromHotToFreeze) {

                    float deltaX = weatherForecastList.get(entryCounter).getDateTime() - weatherForecastList.get(entryCounter - 1).getDateTime();
                    double deltaY = temperatureForEntry - temperatures[entryCounter - 1];
                    double prepona = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                    double alpha = Math.acos(deltaX/prepona);

                    double newDeltaY = Math.abs(temperatures[entryCounter]);
                    long zeroTempTime = weatherForecastList.get(entryCounter - 1).getDateTime()  + (long) (newDeltaY / Math.sin(alpha));
                    temperatureEntries.add(new Entry(
                            zeroTempTime,
                            0f));
                }
            }
            temperatureEntries.add(new Entry(
                    weatherForecastList.get(entryCounter++).getDateTime(),
                    (float) temperatureForEntry));
        }

        LineDataSet set = new LineDataSet(temperatureEntries, context.getString(R.string.graph_temperature_day_label));
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setCubicIntensity(0.2f);
        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setValueTextSize(12f);
        set.setDrawValues(false);
        List<Integer> temperatureColors = new ArrayList<>();
        temperatureColors.add(Color.RED);
        temperatureColors.add(Color.BLUE);
        set.setColors(temperatureColors);
        set.setHighlightEnabled(false);
        set.setValueFormatter(mValueFormatter);
        set.setValueTextColor(textColorId);

        double multiplier;
        switch (pressureUnitFromPreferences) {
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
                    pressureUnitFromPreferences,
                    locale);
            double pressureValue = multiplier * pressureWithUnit.getPressure();
            pressures[i] = pressureValue;
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
                    weatherForecastList.get(entryCounter++).getDateTime(),
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
        pressureSet.setAxisIndex(3);

        List<BarEntry> rainEntries = new ArrayList<>();
        int rainSnowSize = weatherForecastList.size();
        float[] rains = new float[rainSnowSize];
        float[] snows = new float[rainSnowSize];
        double minRainSnowValue = Double.MAX_VALUE;
        double maxRainSnowValue = Double.MIN_VALUE;
        boolean isRain = false;
        boolean isSnow = false;
        for (int i = 0; i < weatherForecastList.size(); i++) {
            DetailedWeatherForecast detailedWeatherForecast = weatherForecastList.get(i);
            double rainValue = AppPreference.getRainOrSnow(
                    rainSnowUnitFromPreferences, detailedWeatherForecast.getRain());
            if (!isRain && (rainValue > 0)) {
                isRain = true;
            }
            double snowValue = AppPreference.getRainOrSnow(
                    rainSnowUnitFromPreferences, detailedWeatherForecast.getSnow());
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
            if ((leftYaxis == CombinedGraph.RAINSNOW) ||
                    (rightYaxis == CombinedGraph.RAINSNOW)) {
                rains[i] = (float) rainValue;
                snows[i] = (float) snowValue;
            } else {
                rains[i] = (float) (Math.log10(rainValue + 1));
                snows[i] = (float) (Math.log10(snowValue + 1));
            }

        }

        boolean isRainSnowVector = isRain && isSnow;
        for (int i = 0; i < weatherForecastList.size(); i++) {
            if (isRainSnowVector) {
                float[] rainsnowBarData = new float[2];
                rainsnowBarData[0] = rains[i];
                rainsnowBarData[1] = snows[i];
                rainEntries.add(new BarEntry(
                        weatherForecastList.get(i).getDateTime(),
                        rainsnowBarData));
            } else if (isRain){
                rainEntries.add(new BarEntry(
                        weatherForecastList.get(i).getDateTime(),
                        rains[i]));
            } else if (isSnow){
                rainEntries.add(new BarEntry(
                        weatherForecastList.get(i).getDateTime(),
                        snows[i]));
            } else {
                rainEntries.add(new BarEntry(
                        weatherForecastList.get(i).getDateTime(),
                        rains[i]));
            }
        }

        String[] rainSnowLabels = getRainSnowLabelForCombinedGraph(context, locale, isRain, isSnow);
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

        List<Entry> windEntries = new ArrayList<>();
        int windSize = weatherForecastList.size();
        double minWindValue = Double.MAX_VALUE;
        double maxWindValue = Double.MIN_VALUE;
        for (int i = 0; i < windSize; i++) {
            double windSpeed = AppPreference.getWind(windUnitFromPreferences, weatherForecastList.get(i).getWindSpeed());
            if (windSpeed < minWindValue) {
                minWindValue = windSpeed;
            }
            if (windSpeed > maxWindValue) {
                maxWindValue = windSpeed;
            }
            windEntries.add(new Entry(weatherForecastList.get(i).getDateTime(), (float) (windSpeed)));
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
            yLeft.setLabelCount(yAxisValues);
        }
        if (leftYaxis == CombinedGraph.TEMPERATURE) {
            double axisMaximum = Math.ceil(maxTemperatureValue);
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
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, TemperatureUtil.getTemperatureUnit(context, temperatureUnitFromPreferences)));
        } else if (leftYaxis == CombinedGraph.WIND) {
            double axisMaximum = Math.ceil(maxWindValue);
            yLeft.setAxisMaximum((float) (axisMaximum));
            yLeft.setAxisMinimum(0f);
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getWindUnit(context, windUnitFromPreferences)));
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
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getPressureUnit(context, pressureUnitFromPreferences)));
            pressureSet.setAxisIndex(0);
        } else if (leftYaxis == CombinedGraph.RAINSNOW) {
            double axisMaximum = Math.ceil(maxRainSnowValue);
            yLeft.setAxisMaximum((float) (axisMaximum));
            yLeft.setAxisMinimum(0f);
            yLeft.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, context.getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences))));
            rainSet.setAxisIndex(0);
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
            yRight.setLabelCount(yAxisValues);
        }
        if (rightYaxis == CombinedGraph.WIND) {
            double axisMaximum = Math.ceil(maxWindValue);
            yRight.setAxisMaximum((float) (axisMaximum));
            yRight.setAxisMinimum(0f);
            yRight.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getWindUnit(context, windUnitFromPreferences)));
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
            yRight.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, AppPreference.getPressureUnit(context, pressureUnitFromPreferences)));
            pressureSet.setAxisIndex(1);
        } else if (rightYaxis == CombinedGraph.RAINSNOW) {
            double axisMaximum = Math.ceil(maxRainSnowValue);
            yRight.setAxisMaximum((float) (axisMaximum));
            yRight.setAxisMinimum(0f);
            yRight.setValueFormatter(new YAxisValueFormatter(locale, yAxisFractionalDigits, context.getString(AppPreference.getRainOrSnowUnit(rainSnowUnitFromPreferences))));
            rainSet.setAxisIndex(1);
        }
        if (rightYaxis == null) {
            yRight.setEnabled(false);
        } else {
            yRight.setEnabled(true);
        }

        if (rainsnow && (leftYaxis != CombinedGraph.RAINSNOW) && (rightYaxis != CombinedGraph.RAINSNOW)) {
            YAxis rainAxis = combinedChart.getAxis(2);
            rainAxis.setEnabled(true);
            rainAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            rainAxis.setAxisMaximum((float) AppPreference.getRainOrSnow(
                    rainSnowUnitFromPreferences, 2.2));
            rainAxis.setAxisMinimum(0);
            rainSet.setAxisIndex(2);
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
            rainData.setBarWidth(8000f);
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
        x.removeAllLimitLines();

        if ((weatherForecastList == null) || weatherForecastList.isEmpty()) {
            return;
        }

        List<String> passedDays = new ArrayList<>();
        for (int i = 0; i < weatherForecastList.size(); i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(weatherForecastList.get(i).getDateTime() * 1000);

            Calendar cal24InDay = Calendar.getInstance();
            cal24InDay.setTimeInMillis(weatherForecastList.get(i).getDateTime() * 1000);
            cal24InDay.set(Calendar.HOUR_OF_DAY, 0);

            Calendar cal12InDay = Calendar.getInstance();
            cal12InDay.setTimeInMillis(weatherForecastList.get(i).getDateTime() * 1000);
            cal12InDay.set(Calendar.HOUR_OF_DAY, 12);

            if (!passedDays.contains(cal12InDay.get(Calendar.DAY_OF_YEAR) + "12") && cal12InDay.after(cal)) {
                LimitLine limitLine12 = new LimitLine(cal12InDay.getTimeInMillis() / 1000);
                limitLine12.setLineColor(gridColor.getSecondaryGridColor());
                limitLine12.setLineWidth(0.5f);
                x.addLimitLine(limitLine12);
                passedDays.add(cal12InDay.get(Calendar.DAY_OF_YEAR) + "12");
            }

            if (!passedDays.contains(cal24InDay.get(Calendar.DAY_OF_YEAR) + "24")) {
                LimitLine limitLine24 = new LimitLine(cal24InDay.getTimeInMillis() / 1000);
                limitLine24.setLineColor(gridColor.getMainGridColor());
                limitLine24.setLineWidth(0.5f);
                x.addLimitLine(limitLine24);
                passedDays.add(cal24InDay.get(Calendar.DAY_OF_YEAR) + "24");
            }
        }

        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setLabelCount(25);
        x.setTextColor(textColorId);
        x.setGridColor(gridColor.getMainGridColor());
        x.setValueFormatter(new XAxisValueFormatter(locale));
        x.setDrawLimitLinesBehindData(true);
        if (!weatherForecastList.isEmpty()) {
            x.setAxisMinimum(weatherForecastList.get(0).getDateTime());
        }

        if (textSize != null) {
            x.setTextSize(textSize);
        }
    }

    public static String[] getRainSnowLabelForCombinedGraph(Context context, Locale locale, boolean isRain, boolean isSnow) {
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

        /*if (multiplier != 1.0) {
            addInfoLabelBuilder.append(" (*");
            addInfoLabelBuilder.append(decimalFormat.format(1 / multiplier));
            addInfoLabelBuilder.append(" ");
            addInfoLabelBuilder.append(context.getString(AppPreference.getRainOrSnowUnit(context)));
            addInfoLabelBuilder.append(" on ");
            addInfoLabelBuilder.append(TemperatureUtil.getTemperatureUnit(context));
            addInfoLabelBuilder.append(")");
        }*/
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
