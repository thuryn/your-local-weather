package org.thosp.yourlocalweather.utils;

import java.io.Serializable;
import java.util.Locale;

public class PressureWithUnit implements Serializable {
    private double pressure;
    private String pressureUnit;

    public PressureWithUnit(double pressure, String pressureUnit) {
        this.pressure = pressure;
        this.pressureUnit = pressureUnit;
    }

    public double getPressure() {
        return pressure;
    }

    public String getPressureUnit() {
        return pressureUnit;
    }

    public String getPressure(int decimalPlaces) {
        return String.format(Locale.getDefault(),
                "%." + decimalPlaces + "f", pressure);
    }
}
