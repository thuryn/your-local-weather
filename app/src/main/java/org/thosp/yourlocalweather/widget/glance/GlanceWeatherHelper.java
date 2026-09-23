package org.thosp.yourlocalweather.widget.glance;

import android.content.Context;
import android.graphics.Bitmap;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WindWithUnit;

import java.util.Locale;

public class GlanceWeatherHelper {

    public static String getTemperatureText(Context context, Location location, CurrentWeatherDbHelper.WeatherRecord weatherRecord, String temperatureUnit) {
        if (location == null || weatherRecord == null || weatherRecord.getWeather() == null) {
            return "--°";
        }
        Weather weather = weatherRecord.getWeather();
        String tempType = AppPreference.getTemeratureTypeFromPreferences(context);
        String temp = TemperatureUtil.getTemperatureWithUnit(
                context, weather, location.getLatitude(),
                weatherRecord.getLastUpdatedTime(), tempType,
                temperatureUnit, location.getLocale()
        );
        return (temp != null) ? temp : "--°";
    }

    public static String getSecondTemperatureText(Context context, Location location, CurrentWeatherDbHelper.WeatherRecord weatherRecord, String temperatureUnit) {
        if (location == null || weatherRecord == null || weatherRecord.getWeather() == null) {
            return null;
        }
        Weather weather = weatherRecord.getWeather();
        return TemperatureUtil.getSecondTemperatureWithUnit(
                context, weather, location.getLatitude(),
                weatherRecord.getLastUpdatedTime(), temperatureUnit, location.getLocale()
        );
    }

    public static String getWindText(Context context, Weather weather, String windUnit, Locale locale) {
        if (weather == null) {
            return null;
        }
        WindWithUnit windWithUnit = AppPreference.getWindWithUnit(context, weather.getWindSpeed(), weather.getWindDirection(), windUnit, locale);
        return "=: " + windWithUnit.getWindSpeed(0) + " " + windWithUnit.getWindUnit() + " " + windWithUnit.getWindDirection();
    }

    public static String getHumidityText(Context context, Weather weather) {
        if (weather == null) {
            return null;
        }
        return "%: " + weather.getHumidity() + "%";
    }

    public static String getWeatherDescription(Context context, Weather weather) {
        if (weather == null) {
            return "";
        }
        return Utils.getWeatherDescription(context, weather);
    }

    public static Bitmap getWeatherIconBitmap(Context context, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if (weatherRecord == null) {
            return null;
        }
        String strIcon = Utils.getStrIconFromWEatherRecord(context, weatherRecord);
        return Utils.createWeatherIcon(context, strIcon);
    }

    public static int getWeatherResourceIcon(CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        return Utils.getWeatherResourceIcon(weatherRecord);
    }

    public static Bitmap getForecastDayIconBitmap(Context context, Integer weatherId) {
        if (weatherId == null) {
            return null;
        }
        String strIcon = org.thosp.shared_resources.Utils.getStrIcon(context, weatherId, 0L, 0L);
        return Utils.createWeatherIcon(context, strIcon);
    }

    public static int getForecastDayIconResId(Integer weatherId, double maxTemp, double maxWind) {
        return Utils.getWeatherResourceIcon(weatherId, maxTemp, maxWind);
    }
}
