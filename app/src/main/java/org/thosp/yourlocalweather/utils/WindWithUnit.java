package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;

import java.io.Serializable;
import java.util.Locale;

public class WindWithUnit implements Serializable {
    private double windSpeed;
    private String windUnit;
    private double windDirection;
    private Context context;
    private String directionTypeFromPreferences;

    public WindWithUnit(Context context, double windDirection) {
        this.windDirection = windDirection;
        this.context = context;
        directionTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_DIRECTION, "abbreviation");
    }

    public WindWithUnit(Context context, double windSpeed, String windUnit, double windDirection) {
        this.windSpeed = windSpeed;
        this.windUnit = windUnit;
        this.windDirection = windDirection;
        this.context = context;
        directionTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_WIND_DIRECTION, "abbreviation");
    }

    public String getWindUnit() {
        return windUnit;
    }

    public String getWindDirection() {
        return getWindDirectionByAbbrev();
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getWindSpeed(int decimalPlaces) {
        return String.format(Locale.getDefault(),
                "%." + decimalPlaces + "f", windSpeed);
    }

    private String getWindDirectionByAbbrev() {
        if ("nothing".equals(directionTypeFromPreferences)) {
            return "";
        } else if ("deg".equals(directionTypeFromPreferences)) {
            return String.format(Locale.getDefault(),"%.0f°", windDirection);
        } else {
            if (((windDirection >= 0) && (windDirection <= 5)) || (windDirection >= 355)) {
                return context.getString(R.string.wind_direction_north);
            } else if ((windDirection > 5) && (windDirection < 85)) {
                return context.getString(R.string.wind_direction_north_east);
            } else if ((windDirection >= 85) && (windDirection <= 95)) {
                return context.getString(R.string.wind_direction_east);
            } else if ((windDirection > 95) && (windDirection < 175)) {
                return context.getString(R.string.wind_direction_south_east);
            } else if ((windDirection >= 175) && (windDirection <= 185)) {
                return context.getString(R.string.wind_direction_south);
            } else if ((windDirection > 185) && (windDirection < 265)) {
                return context.getString(R.string.wind_direction_south_west);
            } else if ((windDirection >= 265) && (windDirection <= 275)) {
                return context.getString(R.string.wind_direction_west);
            } else if ((windDirection > 275) && (windDirection < 355)) {
                return context.getString(R.string.wind_direction_north_west);
            }
        }
        return "";
    }
}
