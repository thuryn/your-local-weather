package org.thosp.yourlocalweather.utils;

import org.thosp.charting.data.Entry;
import org.thosp.charting.formatter.IValueFormatter;
import org.thosp.charting.utils.ViewPortHandler;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomValueFormatter implements IValueFormatter {

    private final NumberFormat decimalFormat;

    public CustomValueFormatter(Locale locale) {
        decimalFormat = NumberFormat.getNumberInstance(locale);
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return decimalFormat.format(value);
    }
}
