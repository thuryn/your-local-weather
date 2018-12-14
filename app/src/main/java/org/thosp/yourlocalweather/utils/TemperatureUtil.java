package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Weather;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TemperatureUtil {

    private static double SOLAR_CONSTANT = 1395; // solar constant (w/m2)
    private static double transmissionCoefficientClearDay = 0.81;
    private static double transmissionCoefficientCloudy = 0.62;

    public static float getApparentTemperature(double dryBulbTemperature,
                                               int humidity,
                                               double windSpeed,
                                               int cloudiness,
                                               double latitude,
                                               long timestamp) {
        return getApparentTemperatureWithSolarIrradiation(dryBulbTemperature, humidity, windSpeed, cloudiness, latitude, timestamp);
    }

    public static float getApparentTemperatureWithoutSolarIrradiation(double dryBulbTemperature, int humidity, double windSpeed) {
        double e = (humidity / 100f) * 6.105 * Math.exp((17.27*dryBulbTemperature) / (237.7 + dryBulbTemperature));
        double apparentTemperature = dryBulbTemperature + (0.33*e)-(0.70*windSpeed)-4.00;
        return (float)apparentTemperature;
    }

    public static float getApparentTemperatureWithSolarIrradiation(double dryBulbTemperature,
                                                                   int humidity,
                                                                   double windSpeed,
                                                                   int cloudiness,
                                                                   double latitude,
                                                                   long timestamp) {
        double e = (humidity / 100f) * 6.105 * Math.exp((17.27*dryBulbTemperature / (237.7 + dryBulbTemperature)));
        double cosOfZenithAngle = getCosOfZenithAngle(Math.toRadians(latitude), timestamp);
        double secOfZenithAngle = 1/cosOfZenithAngle;
        double transmissionCoefficient = transmissionCoefficientClearDay -
                (transmissionCoefficientClearDay - transmissionCoefficientCloudy) * (cloudiness/100f);
        double calculatedIrradiation = 0;
        if (cosOfZenithAngle > 0) {
            calculatedIrradiation = (SOLAR_CONSTANT * cosOfZenithAngle*Math.pow(transmissionCoefficient, secOfZenithAngle))/10;
        }
        double apparentTemperature = dryBulbTemperature + (0.348 * e) - (0.70 * windSpeed) + ((0.70 * calculatedIrradiation)/(windSpeed + 10)) - 4.25;
        return (float)apparentTemperature;
    }

    private static double getCosOfZenithAngle(double latitude, long timestamp) {
        Calendar measuredTime = Calendar.getInstance();
        measuredTime.setTimeInMillis(timestamp);
        double declination = Math.toRadians(-23.44 * Math.cos(Math.toRadians((360f/365f) * (9 + measuredTime.get(Calendar.DAY_OF_YEAR)))));
        double hourAngle = ((12 * 60) - (60 * measuredTime.get(Calendar.HOUR_OF_DAY) + measuredTime.get(Calendar.MINUTE))) * 0.25;
        return Math.sin(latitude)*Math.sin(declination) + (Math.cos(latitude) * Math.cos(declination) * Math.cos(Math.toRadians(hourAngle)));
    }

    public static float getCanadianStandardTemperature(double dryBulbTemperature, double windSpeed) {
        double windWithPow = Math.pow(windSpeed, 0.16);
        return (float) (13.12 + (0.6215 * dryBulbTemperature) - (13.37 * windWithPow) + (0.486 * dryBulbTemperature * windWithPow));
    }

    public static String getSecondTemperatureWithLabel(Context context, Weather weather, double latitude, long timestamp, Locale locale) {
        if (weather == null) {
            return null;
        }
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        if ("measured_only".equals(temperatureTypeFromPreferences) || "appearance_only".equals(temperatureTypeFromPreferences)) {
            return null;
        }
        int label = R.string.label_measured_temperature;
        if ("measured_appearance_primary_measured".equals(temperatureTypeFromPreferences)) {
            label = R.string.label_apparent_temperature;
        }
        return context.getString(label,
                getSecondTemperatureWithUnit(
                        context,
                        weather,
                        latitude,
                        timestamp,
                        locale));
    }

    public static String getSecondTemperatureWithUnit(Context context, Weather weather, double latitude, long timestamp, Locale locale) {
        if (weather == null) {
            return null;
        }
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        if ("measured_only".equals(temperatureTypeFromPreferences) || "appearance_only".equals(temperatureTypeFromPreferences)) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String apparentSign = "";
        double value = weather.getTemperature();
        if ("measured_appearance_primary_measured".equals(temperatureTypeFromPreferences)) {
            apparentSign = "~";
            value = TemperatureUtil.getApparentTemperature(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getClouds(),
                    latitude,
                    timestamp);
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(locale, "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(locale, "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getTemperatureWithUnit(Context context, Weather weather, double latitude, long timestamp, Locale locale) {
        if (weather == null) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        String apparentSign = "";
        double value = weather.getTemperature();
        if ("appearance_only".equals(temperatureTypeFromPreferences) ||
                ("measured_appearance_primary_appearance".equals(temperatureTypeFromPreferences))) {
            apparentSign = "~";
            //value = TemperatureUtil.getApparentTemperature(weather.getTemperature(), weather.getHumidity(), weather.getWindSpeed());
            value = TemperatureUtil.getApparentTemperature(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getClouds(),
                    latitude,
                    timestamp);
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(locale, "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(locale, "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getForecastedTemperatureWithUnit(Context context, DetailedWeatherForecast weather, Locale locale) {
        if (weather == null) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String apparentSign = "";
        double value = weather.getTemperature();
        if (value > 0) {
            apparentSign += "+";
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(locale, "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(locale, "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getForecastedApparentTemperatureWithUnit(
            Context context,
            double latitude,
            DetailedWeatherForecast weather,
            Locale locale) {

        if (weather == null) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String apparentSign = "";
        double value = TemperatureUtil.getApparentTemperatureWithSolarIrradiation(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed(),
                    weather.getCloudiness(),
                    latitude,
                    weather.getDateTime()
                );
        if (value > 0) {
            apparentSign += "+";
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (value * 1.8f) + 32;
            return apparentSign + String.format(locale, "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return apparentSign + String.format(locale, "%d",
                    Math.round(value)) + getTemperatureUnit(context);
        }
    }

    public static String getTemperatureUnit(Context context) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return context.getString(R.string.temperature_unit_fahrenheit);
        } else {
            return context.getString(R.string.temperature_unit_celsius);
        }
    }

    public static double getTemperatureInPreferredUnit(Context context, double inputValue) {
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return (inputValue * 1.8d) + 32;
        } else {
            return inputValue;
        }
    }

    public static double getTemperature(Context context, DetailedWeatherForecast weather) {
        if (weather == null) {
            return 0;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        String temperatureTypeFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_TYPE, "measured_only");
        double value = weather.getTemperature();
        if ("appearance_only".equals(temperatureTypeFromPreferences) ||
                ("measured_appearance_primary_appearance".equals(temperatureTypeFromPreferences))) {
            value = TemperatureUtil.getApparentTemperatureWithoutSolarIrradiation(
                    weather.getTemperature(),
                    weather.getHumidity(),
                    weather.getWindSpeed());
        }
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return (value * 1.8d) + 32;
        } else {
            return value;
        }
    }

    public static int getTemperatureStatusIcon(Context context, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if ((weatherRecord == null) || (weatherRecord.getWeather() == null)) {
            return R.drawable.zero0;
        }
        float temperature = weatherRecord.getWeather().getTemperature();
        return getResourceForNumber(context, temperature);
    }

    private static int getResourceForNumber(Context context, float number) {
        String fileName;
        int roundedNumber = Math.round(number);
        if (roundedNumber == 0) {
            fileName = "zero0";
        } else if (roundedNumber > 0) {
            fileName = "plus" + roundedNumber;
        } else {
            fileName = "minus" + roundedNumber;
        }
        return context.getResources().getIdentifier(fileName, "drawable", context.getPackageName());
    }
}
