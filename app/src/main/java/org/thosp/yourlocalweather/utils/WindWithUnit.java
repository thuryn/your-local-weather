package org.thosp.yourlocalweather.utils;

import java.io.Serializable;
import java.util.Locale;

public class WindWithUnit implements Serializable {
    private float windSpeed;
    private String windUnit;

    public WindWithUnit(float windSpeed, String windUnit) {
        this.windSpeed = windSpeed;
        this.windUnit = windUnit;
    }

    public String getWindUnit() {
        return windUnit;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public String getWindSpeed(int decimalPlaces) {
        return String.format(Locale.getDefault(),
                "%." + decimalPlaces + "f", windSpeed);
    }
}
