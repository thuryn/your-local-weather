package org.thosp.yourlocalweather.utils;

import org.thosp.charting.components.AxisBase;
import org.thosp.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class XAxisValueFormatter implements IAxisValueFormatter {

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat format;
    private final Set<Long> labelTimestamps;

    public XAxisValueFormatter(Locale locale, Set<Long> labelTimestamps) {
        this.format = new SimpleDateFormat("EEE", locale);
        this.labelTimestamps = labelTimestamps;
    }

    public XAxisValueFormatter(Locale locale) {
        this(locale, null);
    }

    private boolean isCloseToLabelTimestamp(long dataTime) {
        if (labelTimestamps == null || labelTimestamps.isEmpty()) {
            return true;
        }
        for (Long target : labelTimestamps) {
            if (target != null && Math.abs(dataTime - target) <= 3600) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        long dataTime = (long) value;
        if (dataTime <= 0) {
            return "";
        }
        if (!isCloseToLabelTimestamp(dataTime)) {
            return "";
        }
        calendar.setTimeInMillis(dataTime * 1000);
        return format.format(calendar.getTime());
    }
}
