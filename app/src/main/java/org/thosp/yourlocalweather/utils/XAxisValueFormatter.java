package org.thosp.yourlocalweather.utils;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

public class XAxisValueFormatter implements IAxisValueFormatter {

    private String[] mValues;

    public XAxisValueFormatter(String[] dates) {
        mValues = dates;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int valuesIndex = (int) value;
        if (mValues.length <= valuesIndex) {
            return "";
        }
        return mValues[valuesIndex];
    }
}
