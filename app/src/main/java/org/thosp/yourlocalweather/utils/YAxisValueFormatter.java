package org.thosp.yourlocalweather.utils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.NumberFormat;
import java.util.Locale;

public class YAxisValueFormatter implements IAxisValueFormatter {

    private NumberFormat decimalFormat;
    private String unit;

    public YAxisValueFormatter(Locale locale, int fractionalDigits, String unit) {
        this.unit = unit;
        decimalFormat = NumberFormat.getNumberInstance(locale);
        decimalFormat.setMaximumFractionDigits(fractionalDigits);
        decimalFormat.setMinimumFractionDigits(fractionalDigits);
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        if ((axis.getLabelCount() <= axis.mEntries.length) &&
                axis.mEntries[axis.getLabelCount() -1] == value) {
            return unit;
        }
        return decimalFormat.format(value);
    }
}
