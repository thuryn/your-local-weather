package org.thosp.yourlocalweather.utils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.NumberFormat;
import java.util.Locale;

public class YAxisValueFormatter extends ValueFormatter {

    private NumberFormat decimalFormat;
    private String unit;

    public YAxisValueFormatter(Locale locale, int fractionalDigits, String unit) {
        this.unit = unit;
        decimalFormat = NumberFormat.getNumberInstance(locale);
        decimalFormat.setMaximumFractionDigits(fractionalDigits);
        decimalFormat.setMinimumFractionDigits(fractionalDigits);
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        if (axis.mEntries[axis.mEntries.length -1] == value) {
            return unit;
        }
        return decimalFormat.format(value);
    }
}
