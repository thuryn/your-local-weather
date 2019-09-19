package org.thosp.yourlocalweather.utils;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.NumberFormat;
import java.util.Locale;

public class RainSnowYAxisValueFormatter extends ValueFormatter {

    private NumberFormat decimalFormat;

    public RainSnowYAxisValueFormatter(Context context, Locale locale) {
        decimalFormat = NumberFormat.getNumberInstance(locale);
        int numberOfDecimalPlaces = AppPreference.getGraphFormatterForRainOrSnow(context);
        decimalFormat.setMaximumFractionDigits(numberOfDecimalPlaces);
        decimalFormat.setMinimumFractionDigits(numberOfDecimalPlaces);
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return decimalFormat.format(value);
    }
}