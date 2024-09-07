package org.thosp.yourlocalweather.utils;

import org.thosp.charting.components.AxisBase;
import org.thosp.charting.formatter.IAxisValueFormatter;

import java.text.NumberFormat;
import java.util.Locale;

public class YAxisValueFormatter implements IAxisValueFormatter {

    private final NumberFormat decimalFormat;
    private final String unit;

    public YAxisValueFormatter(Locale locale, int fractionalDigits, String unit) {
        this.unit = unit;
        decimalFormat = NumberFormat.getNumberInstance(locale);
        decimalFormat.setMaximumFractionDigits(fractionalDigits);
        decimalFormat.setMinimumFractionDigits(fractionalDigits);
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        if (axis.mEntries[axis.mEntries.length -1] == value) {
            return unit;
        }
        return decimalFormat.format(value);
    }
}
