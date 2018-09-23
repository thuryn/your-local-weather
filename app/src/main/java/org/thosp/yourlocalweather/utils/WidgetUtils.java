package org.thosp.yourlocalweather.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherCondition;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.ReconciliationDbService;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetService;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetWithForecastService;
import org.thosp.yourlocalweather.widget.LessWidgetService;
import org.thosp.yourlocalweather.widget.MoreWidgetService;
import org.thosp.yourlocalweather.widget.WeatherForecastWidgetService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WidgetUtils {

    private static final String TAG = "WidgetUtils";

    private static final SimpleDateFormat sdfDayOfWeek = new SimpleDateFormat("EEEE");

    public static void setSunset(Context context, RemoteViews remoteViews, String value) {
        if (AppPreference.showLabelsOnWidget(context)) {
            String sunset = context.getString(R.string.sunset_label, value);
            remoteViews.setTextViewText(R.id.widget_sunset, sunset);
            remoteViews.setViewVisibility(R.id.widget_sunset_icon, TextView.GONE);
        } else {
            String sunset = ": " + value;
            remoteViews.setImageViewBitmap(R.id.widget_sunset_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_sunset)));
            remoteViews.setViewVisibility(R.id.widget_sunset_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_sunset, sunset);
        }
    }

    public static void setSunrise(Context context, RemoteViews remoteViews, String value) {
        if (AppPreference.showLabelsOnWidget(context)) {
            String sunrise = context.getString(R.string.sunrise_label, value);
            remoteViews.setTextViewText(R.id.widget_sunrise, sunrise);
            remoteViews.setViewVisibility(R.id.widget_sunrise_icon, TextView.GONE);
        } else {
            String sunrise = ": " + value;
            remoteViews.setImageViewBitmap(R.id.widget_sunrise_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_sunrise)));
            remoteViews.setViewVisibility(R.id.widget_sunrise_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_sunrise, sunrise);
        }
    }

    public static void setHumidity(Context context, RemoteViews remoteViews, int value) {
        String percentSign = context.getString(R.string.percent_sign);
        if (AppPreference.showLabelsOnWidget(context)) {
            String humidity =
                    context.getString(R.string.humidity_label,
                            String.valueOf(value), percentSign);
            remoteViews.setTextViewText(R.id.widget_humidity, humidity);
            remoteViews.setViewVisibility(R.id.widget_humidity_icon, TextView.GONE);
        } else {
            String humidity = ": " + String.valueOf(value) + percentSign;
            remoteViews.setImageViewBitmap(R.id.widget_humidity_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_humidity)));
            remoteViews.setViewVisibility(R.id.widget_humidity_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_humidity, humidity);
        }
    }

    public static void setWind(Context context, RemoteViews remoteViews, float value) {
        WindWithUnit windWithUnit = AppPreference.getWindWithUnit(context, value);
        if (AppPreference.showLabelsOnWidget(context)) {
            String wind = context.getString(R.string.wind_label,
                            windWithUnit.getWindSpeed(0),
                            windWithUnit.getWindUnit());
            remoteViews.setTextViewText(R.id.widget_wind, wind);
            remoteViews.setViewVisibility(R.id.widget_wind_icon, TextView.GONE);
        } else {
            String wind = ": " + windWithUnit.getWindSpeed(0) + " " + windWithUnit.getWindUnit();
            remoteViews.setImageViewBitmap(R.id.widget_wind_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_wind)));
            remoteViews.setViewVisibility(R.id.widget_wind_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_wind, wind);
        }
    }

    public static void setPressure(Context context, RemoteViews remoteViews, float value) {
        WindWithUnit windWithUnit = AppPreference.getPressureWithUnit(context, value);
        if (AppPreference.showLabelsOnWidget(context)) {
            String pressure =
                    context.getString(R.string.pressure_label,
                            windWithUnit.getWindSpeed(0),
                            windWithUnit.getWindUnit());
            remoteViews.setTextViewText(R.id.widget_pressure, pressure);
            remoteViews.setViewVisibility(R.id.widget_pressure_icon, TextView.GONE);
        } else {
            String pressure = ": " + windWithUnit.getWindSpeed(0) + " " + windWithUnit.getWindUnit();
            remoteViews.setImageViewBitmap(R.id.widget_pressure_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_barometer)));
            remoteViews.setViewVisibility(R.id.widget_pressure_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_pressure, pressure);
        }
    }

    public static void setClouds(Context context, RemoteViews remoteViews, int value) {
        String percentSign = context.getString(R.string.percent_sign);
        if (AppPreference.showLabelsOnWidget(context)) {
            String cloudnes =
                    context.getString(R.string.cloudiness_label,
                            String.valueOf(value), percentSign);
            remoteViews.setTextViewText(R.id.widget_clouds, cloudnes);
            remoteViews.setViewVisibility(R.id.widget_clouds_icon, TextView.GONE);
        } else {
            String cloudnes = ": " + String.valueOf(value) + " " + percentSign;
            remoteViews.setImageViewBitmap(R.id.widget_clouds_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_cloudiness)));
            remoteViews.setViewVisibility(R.id.widget_clouds_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_clouds, cloudnes);
        }
    }

    public static void updateWeatherForecast(Context context, long locationId, RemoteViews remoteViews) {
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId);
        //appendLog(context, TAG, "updateWeatherForecast:locationId=" + locationId + ", weatherForecastRecord=" + weatherForecastRecord);
        if (weatherForecastRecord == null) {
            return;
        }
        Integer firstDayOfYear = null;
        Map<Integer, List<DetailedWeatherForecast>> weatherList = new HashMap<>();
        Calendar forecastCalendar = Calendar.getInstance();
        for (DetailedWeatherForecast detailedWeatherForecast : weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {
            forecastCalendar.setTimeInMillis(detailedWeatherForecast.getDateTime() * 1000);
            int forecastDay = forecastCalendar.get(Calendar.DAY_OF_YEAR);
            if (firstDayOfYear == null) {
                firstDayOfYear = forecastDay;
            }
            if (!weatherList.keySet().contains(forecastDay)) {
                List<DetailedWeatherForecast> dayForecastList = new ArrayList<>();
                weatherList.put(forecastDay, dayForecastList);
            }
            //appendLog(context, TAG, "preLoadWeather:forecastDay=" + forecastDay + ":detailedWeatherForecast=" + detailedWeatherForecast);
            weatherList.get(forecastDay).add(detailedWeatherForecast);
        }
        int dayCounter = 0;
        int daysInList = firstDayOfYear + weatherList.keySet().size();
        for (int dayInYear = firstDayOfYear; dayInYear < daysInList; dayInYear++) {
            if ((weatherList.get(dayInYear) == null) || (weatherList.get(dayInYear).size() < 3)) {
                continue;
            }
            dayCounter++;
            Map<Integer, Integer> weatherIdsInDay = new HashMap<>();
            for (DetailedWeatherForecast weatherForecastForDay : weatherList.get(dayInYear)) {
                WeatherCondition weatherCondition = weatherForecastForDay.getFirstWeatherCondition();
                /*appendLog(context, TAG, "preLoadWeather:dayInYear=" + dayInYear + ":dayCounter=" + dayCounter +
                        ":weatherCondition.getWeatherId()=" + weatherCondition.getWeatherId() +
                        ":weatherIdsInDay.get(weatherCondition.getWeatherId())=" + weatherIdsInDay.get(weatherCondition.getWeatherId()));*/
                if (weatherIdsInDay.get(weatherCondition.getWeatherId()) == null) {
                    weatherIdsInDay.put(weatherCondition.getWeatherId(), 1);
                } else {
                    weatherIdsInDay.put(weatherCondition.getWeatherId(), 1 + weatherIdsInDay.get(weatherCondition.getWeatherId()));
                }
            }
            Integer weatherIdForTheDay = 0;
            int maxIconOccurrence = 0;
            for (Integer weatherId : weatherIdsInDay.keySet()) {
                int iconCount = weatherIdsInDay.get(weatherId);
                if (iconCount > maxIconOccurrence) {
                    weatherIdForTheDay = weatherId;
                    maxIconOccurrence = iconCount;
                }
            }
            String iconId = null;
            double maxTemp = Double.MIN_VALUE;
            double minTemp = Double.MAX_VALUE;
            double maxWind = 0;
            for (DetailedWeatherForecast weatherForecastForDay : weatherList.get(dayInYear)) {
                WeatherCondition weatherCondition = weatherForecastForDay.getFirstWeatherCondition();
                /*appendLog(context, TAG, "preLoadWeather:weatherIdForTheDay=" + weatherIdForTheDay +
                        ":weatherForecastForDay.getTemperature()=" + weatherForecastForDay.getTemperature());*/
                if (weatherCondition.getWeatherId().equals(weatherIdForTheDay)) {
                    iconId = weatherCondition.getIcon();
                }
                double currentTemp = weatherForecastForDay.getTemperature();
                if (maxTemp < currentTemp) {
                    maxTemp = currentTemp;
                }
                if (minTemp > currentTemp) {
                    minTemp = currentTemp;
                }
                if (maxWind < weatherForecastForDay.getWindSpeed()) {
                    maxWind = weatherForecastForDay.getWindSpeed();
                }
            }
            maxTemp = TemperatureUtil.getTemperatureInPreferredUnit(context, maxTemp);
            minTemp = TemperatureUtil.getTemperatureInPreferredUnit(context, minTemp);
            switch (dayCounter) {
                case 1:
                    Utils.setForecastIcon(
                            remoteViews,
                            context,
                            R.id.forecast_1_widget_icon,
                            weatherIdForTheDay,
                            iconId,
                            maxTemp,
                            maxWind);
                    forecastCalendar.set(Calendar.DAY_OF_YEAR, dayInYear);
                    remoteViews.setTextViewText(
                            R.id.forecast_1_widget_day,
                            sdfDayOfWeek.format(forecastCalendar.getTime()));
                    remoteViews.setTextViewText(
                            R.id.forecast_1_widget_temperatures,
                            Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
                    break;
                case 2:
                    Utils.setForecastIcon(
                            remoteViews,
                            context,
                            R.id.forecast_2_widget_icon,
                            weatherIdForTheDay,
                            iconId,
                            maxTemp,
                            maxWind);
                    forecastCalendar.set(Calendar.DAY_OF_YEAR, dayInYear);
                    remoteViews.setTextViewText(
                            R.id.forecast_2_widget_day,
                            sdfDayOfWeek.format(forecastCalendar.getTime()));
                    remoteViews.setTextViewText(
                            R.id.forecast_2_widget_temperatures,
                            Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
                    break;
                case 3:
                    Utils.setForecastIcon(
                            remoteViews,
                            context,
                            R.id.forecast_3_widget_icon,
                            weatherIdForTheDay,
                            iconId,
                            maxTemp,
                            maxWind);
                    forecastCalendar.set(Calendar.DAY_OF_YEAR, dayInYear);
                    remoteViews.setTextViewText(
                            R.id.forecast_3_widget_day,
                            sdfDayOfWeek.format(forecastCalendar.getTime()));
                    remoteViews.setTextViewText(
                            R.id.forecast_3_widget_temperatures,
                            Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
                    break;
                case 4:
                    Utils.setForecastIcon(
                            remoteViews,
                            context,
                            R.id.forecast_4_widget_icon,
                            weatherIdForTheDay,
                            iconId,
                            maxTemp,
                            maxWind);
                    forecastCalendar.set(Calendar.DAY_OF_YEAR, dayInYear);
                    remoteViews.setTextViewText(
                            R.id.forecast_4_widget_day,
                            sdfDayOfWeek.format(forecastCalendar.getTime()));
                    remoteViews.setTextViewText(
                            R.id.forecast_4_widget_temperatures,
                            Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
                    break;
                case 5:
                    Utils.setForecastIcon(
                            remoteViews,
                            context,
                            R.id.forecast_5_widget_icon,
                            weatherIdForTheDay,
                            iconId,
                            maxTemp,
                            maxWind);
                    forecastCalendar.set(Calendar.DAY_OF_YEAR, dayInYear);
                    remoteViews.setTextViewText(
                            R.id.forecast_5_widget_day,
                            sdfDayOfWeek.format(forecastCalendar.getTime()));
                    remoteViews.setTextViewText(
                            R.id.forecast_5_widget_temperatures,
                            Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
                    break;
            }
        }
    }

    public static void updateWidgets(Context context) {
        startBackgroundService(context, new Intent(context, LessWidgetService.class));
        startBackgroundService(context, new Intent(context, MoreWidgetService.class));
        startBackgroundService(context, new Intent(context, ExtLocationWidgetService.class));
        startBackgroundService(context, new Intent(context, ExtLocationWidgetWithForecastService.class));
        startBackgroundService(context, new Intent(context, WeatherForecastWidgetService.class));
        startBackgroundService(context, new Intent(context, ReconciliationDbService.class));
    }

    public static void updateCurrentWeatherWidgets(Context context) {
        startBackgroundService(context, new Intent(context, LessWidgetService.class));
        startBackgroundService(context, new Intent(context, MoreWidgetService.class));
        startBackgroundService(context, new Intent(context, ExtLocationWidgetService.class));
        startBackgroundService(context, new Intent(context, ExtLocationWidgetWithForecastService.class));
        startBackgroundService(context, new Intent(context, ReconciliationDbService.class));
    }

    public static void startBackgroundService(Context context,
                                              Intent intent) {
        try {
            if (isInteractive(context)) {
                context.startService(intent);
                return;
            }
        } catch (Exception ise) {
            //
        }
        startBackgroundService(context, intent, 10);
    }

    public static void startBackgroundService(Context context,
                                              Intent intent,
                                              long triggerInMillis ) {
        PendingIntent pendingIntent = PendingIntent.getService(context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + triggerInMillis,
                    pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + triggerInMillis,
                    pendingIntent);
        }
    }

    public static boolean isInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }
}
