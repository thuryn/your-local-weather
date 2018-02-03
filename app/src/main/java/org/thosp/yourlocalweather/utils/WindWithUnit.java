package org.thosp.yourlocalweather.utils;

import java.io.Serializable;
import java.util.Locale;

public class WindWithUnit implements Serializable {
    private double windSpeed;
    private String windUnit;

    public WindWithUnit(double windSpeed, String windUnit) {
        this.windSpeed = windSpeed;
        this.windUnit = windUnit;
    }

    public String getWindUnit() {
        return windUnit;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getWindSpeed(int decimalPlaces) {
        return String.format(Locale.getDefault(),
                "%." + decimalPlaces + "f", windSpeed);
    }
}
