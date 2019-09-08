package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Weather;

import java.util.Calendar;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

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

    public static String getDewPointWithUnit(Context context, Weather weather, Locale locale) {
        if (weather == null) {
            return null;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        double humidityLogarithm = Math.log(weather.getHumidity() / 100) / Math.log(Math.E);
        double dewPointPart = humidityLogarithm + ((17.67 * weather.getTemperature())/(243.5 + weather.getTemperature()));
        double dewPoint = (243.5 * dewPointPart) / (17.67 - dewPointPart);
        appendLog(context, "TemperatureUtil", "humidityLogarithm=" + humidityLogarithm + ", dewPointPart=" + dewPointPart +
                ", dewPoint=" + dewPoint);
        if (unitsFromPreferences.contains("fahrenheit") ) {
            double fahrenheitValue = (dewPoint * 1.8f) + 32;
            return String.format(locale, "%d",
                    Math.round(fahrenheitValue)) + getTemperatureUnit(context);
        } else {
            return String.format(locale, "%d",
                    Math.round(dewPoint)) + getTemperatureUnit(context);
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
        double value = getTemperatureInCelsius(context, weather);
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return (value * 1.8d) + 32;
        } else {
            return value;
        }
    }

    public static double getTemperature(Context context, Weather weather) {
        if (weather == null) {
            return 0;
        }
        String unitsFromPreferences = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_TEMPERATURE_UNITS, "celsius");
        double value = getTemperatureInCelsius(context, weather);
        if (unitsFromPreferences.contains("fahrenheit") ) {
            return (value * 1.8d) + 32;
        } else {
            return value;
        }
    }

    public static double getTemperatureInCelsius(Context context, DetailedWeatherForecast weather) {
        if (weather == null) {
            return 0;
        }
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
        return value;
    }

    public static double getTemperatureInCelsius(Context context, Weather weather) {
        if (weather == null) {
            return 0;
        }
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
        return value;
    }

    public static int getTemperatureStatusIcon(Context context, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        if ((weatherRecord == null) || (weatherRecord.getWeather() == null)) {
            return R.drawable.zero0;
        }
        float temperature = (float) getTemperature(context, weatherRecord.getWeather());
        return getResourceForNumber(context, temperature);
    }

    private static int getResourceForNumber(Context context, float number) {
        String fileName;
        int roundedNumber = Math.round(number);
        if (roundedNumber < -50) {
            return R.drawable.minus50;
        } else if (roundedNumber > 50) {
            return R.drawable.plus50;
        }
        switch (roundedNumber) {
            case 1: return R.drawable.plus1;
            case 2: return R.drawable.plus2;
            case 3: return R.drawable.plus3;
            case 4: return R.drawable.plus4;
            case 5: return R.drawable.plus5;
            case 6: return R.drawable.plus6;
            case 7: return R.drawable.plus7;
            case 8: return R.drawable.plus8;
            case 9: return R.drawable.plus9;
            case 10: return R.drawable.plus10;
            case 11: return R.drawable.plus11;
            case 12: return R.drawable.plus12;
            case 13: return R.drawable.plus13;
            case 14: return R.drawable.plus14;
            case 15: return R.drawable.plus15;
            case 16: return R.drawable.plus16;
            case 17: return R.drawable.plus17;
            case 18: return R.drawable.plus18;
            case 19: return R.drawable.plus19;
            case 20: return R.drawable.plus20;
            case 21: return R.drawable.plus21;
            case 22: return R.drawable.plus22;
            case 23: return R.drawable.plus23;
            case 24: return R.drawable.plus24;
            case 25: return R.drawable.plus25;
            case 26: return R.drawable.plus26;
            case 27: return R.drawable.plus27;
            case 28: return R.drawable.plus28;
            case 29: return R.drawable.plus29;
            case 30: return R.drawable.plus30;
            case 31: return R.drawable.plus31;
            case 32: return R.drawable.plus32;
            case 33: return R.drawable.plus33;
            case 34: return R.drawable.plus34;
            case 35: return R.drawable.plus35;
            case 36: return R.drawable.plus36;
            case 37: return R.drawable.plus37;
            case 38: return R.drawable.plus38;
            case 39: return R.drawable.plus39;
            case 40: return R.drawable.plus40;
            case 41: return R.drawable.plus41;
            case 42: return R.drawable.plus42;
            case 43: return R.drawable.plus43;
            case 44: return R.drawable.plus44;
            case 45: return R.drawable.plus45;
            case 46: return R.drawable.plus46;
            case 47: return R.drawable.plus47;
            case 48: return R.drawable.plus48;
            case 49: return R.drawable.plus49;
            case 50: return R.drawable.plus50;
            case -1: return R.drawable.minus1;
            case -2: return R.drawable.minus2;
            case -3: return R.drawable.minus3;
            case -4: return R.drawable.minus4;
            case -5: return R.drawable.minus5;
            case -6: return R.drawable.minus6;
            case -7: return R.drawable.minus7;
            case -8: return R.drawable.minus8;
            case -9: return R.drawable.minus9;
            case -10: return R.drawable.minus10;
            case -11: return R.drawable.minus11;
            case -12: return R.drawable.minus12;
            case -13: return R.drawable.minus13;
            case -14: return R.drawable.minus14;
            case -15: return R.drawable.minus15;
            case -16: return R.drawable.minus16;
            case -17: return R.drawable.minus17;
            case -18: return R.drawable.minus18;
            case -19: return R.drawable.minus19;
            case -20: return R.drawable.minus20;
            case -21: return R.drawable.minus21;
            case -22: return R.drawable.minus22;
            case -23: return R.drawable.minus23;
            case -24: return R.drawable.minus24;
            case -25: return R.drawable.minus25;
            case -26: return R.drawable.minus26;
            case -27: return R.drawable.minus27;
            case -28: return R.drawable.minus28;
            case -29: return R.drawable.minus29;
            case -30: return R.drawable.minus30;
            case -31: return R.drawable.minus31;
            case -32: return R.drawable.minus32;
            case -33: return R.drawable.minus33;
            case -34: return R.drawable.minus34;
            case -35: return R.drawable.minus35;
            case -36: return R.drawable.minus36;
            case -37: return R.drawable.minus37;
            case -38: return R.drawable.minus38;
            case -39: return R.drawable.minus39;
            case -40: return R.drawable.minus40;
            case -41: return R.drawable.minus41;
            case -42: return R.drawable.minus42;
            case -43: return R.drawable.minus43;
            case -44: return R.drawable.minus44;
            case -45: return R.drawable.minus45;
            case -46: return R.drawable.minus46;
            case -47: return R.drawable.minus47;
            case -48: return R.drawable.minus48;
            case -49: return R.drawable.minus49;
            case -50: return R.drawable.minus50;
            case 0:
            default:
                return R.drawable.zero0;
        }
    }
}
