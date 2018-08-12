package org.thosp.yourlocalweather.utils;

import android.content.Context;

import org.thosp.yourlocalweather.GraphsActivity;
import org.thosp.yourlocalweather.WeatherForecastActivity;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.util.Calendar;

public class ForecastUtil {

    public static long AUTO_FORECAST_UPDATE_TIME_MILIS = 3600000; // 1h

    public static boolean shouldUpdateForecast(Context context, long locationId) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord =
                weatherForecastDbHelper.getWeatherForecast(locationId);
        if ((weatherForecastRecord == null) ||
                (weatherForecastRecord.getLastUpdatedTime() +
                        AUTO_FORECAST_UPDATE_TIME_MILIS) <  Calendar.getInstance().getTimeInMillis()) {
            return true;
        }
        return false;
    }
}
