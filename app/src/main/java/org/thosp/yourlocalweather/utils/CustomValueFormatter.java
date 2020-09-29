package org.thosp.yourlocalweather.utils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomValueFormatter extends ValueFormatter {

    private NumberFormat decimalFormat;

    public CustomValueFormatter(Locale locale) {
        decimalFormat = NumberFormat.getNumberInstance(locale);
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        return decimalFormat.format(value);
    }
}
