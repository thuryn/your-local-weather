package org.thosp.yourlocalweather.utils;

import android.content.Context;

import org.thosp.yourlocalweather.GraphsActivity;
import org.thosp.yourlocalweather.WeatherForecastActivity;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.util.Calendar;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ForecastUtil {

    private static final String TAG = "ForecastUtil";

    public static long AUTO_FORECAST_UPDATE_TIME_MILIS = 3600000; // 1h

    public static boolean shouldUpdateForecast(Context context, long locationId) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord =
                weatherForecastDbHelper.getWeatherForecast(locationId);
        long now = Calendar.getInstance().getTimeInMillis();
        if ((weatherForecastRecord == null) ||
                (weatherForecastRecord.getLastUpdatedTime() +
                        AUTO_FORECAST_UPDATE_TIME_MILIS) <  now) {
            return true;
        }
        appendLog(context,
                TAG,
                "weatherForecastRecord.getLastUpdatedTime():" +
                        ((weatherForecastRecord != null)?weatherForecastRecord.getLastUpdatedTime():"null") +
                ", now:" + now);
        return false;
    }
}
