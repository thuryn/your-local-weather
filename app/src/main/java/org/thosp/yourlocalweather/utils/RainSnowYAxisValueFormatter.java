package org.thosp.yourlocalweather.utils;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;

public class RainSnowYAxisValueFormatter implements IAxisValueFormatter {

    private DecimalFormat format;

    public RainSnowYAxisValueFormatter(Context context) {
        format = new DecimalFormat(AppPreference.getGraphFormatterForRainOrSnow(context));
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return format.format(value);
    }
}