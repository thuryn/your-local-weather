package org.thosp.yourlocalweather.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherCondition;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.service.ReconciliationDbService;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.LessWidgetProvider;
import org.thosp.yourlocalweather.widget.MoreWidgetProvider;
import org.thosp.yourlocalweather.widget.WeatherForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.WeatherGraphWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WidgetUtils {

    private static final String TAG = "WidgetUtils";

    public static void setSunset(Context context, RemoteViews remoteViews, String value,
                                 int widgetSunsetId, int widgetSunsetIconId) {
        if (AppPreference.showLabelsOnWidget(context)) {
            String sunset = context.getString(R.string.sunset_label, value);
            remoteViews.setTextViewText(widgetSunsetId, sunset);
            remoteViews.setViewVisibility(widgetSunsetIconId, TextView.GONE);
        } else {
            String sunset = ": " + value;
            remoteViews.setImageViewBitmap(widgetSunsetIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_sunset)));
            remoteViews.setViewVisibility(widgetSunsetIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(widgetSunsetId, sunset);
        }
    }

    public static void setSunrise(Context context, RemoteViews remoteViews, String value,
                                  int widgetSunriseId, int widgetSunriseIconId) {
        if (AppPreference.showLabelsOnWidget(context)) {
            String sunrise = context.getString(R.string.sunrise_label, value);
            remoteViews.setTextViewText(widgetSunriseId, sunrise);
            remoteViews.setViewVisibility(widgetSunriseIconId, TextView.GONE);
        } else {
            String sunrise = ": " + value;
            remoteViews.setImageViewBitmap(widgetSunriseIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_sunrise)));
            remoteViews.setViewVisibility(widgetSunriseIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(widgetSunriseId, sunrise);
        }
    }

    public static void setHumidity(Context context, RemoteViews remoteViews, int value,
                                   int humidityId, int humidityIconId) {
        String percentSign = context.getString(R.string.percent_sign);
        if (AppPreference.showLabelsOnWidget(context)) {
            String humidity =
                    context.getString(R.string.humidity_label,
                            String.valueOf(value), percentSign);
            remoteViews.setTextViewText(humidityId, humidity);
            remoteViews.setViewVisibility(humidityIconId, TextView.GONE);
        } else {
            String humidity = ": " + String.valueOf(value) + percentSign;
            remoteViews.setImageViewBitmap(humidityIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_humidity)));
            remoteViews.setViewVisibility(humidityIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(humidityId, humidity);
        }
    }

    public static void setWind(Context context, RemoteViews remoteViews, float value, float direction, Locale locale,
                               int widgetWindId, int widgetWindIconId) {
        WindWithUnit windWithUnit = AppPreference.getWindWithUnit(context, value, direction, locale);
        if (AppPreference.showLabelsOnWidget(context)) {
            String wind = context.getString(R.string.wind_label,
                            windWithUnit.getWindSpeed(0),
                            windWithUnit.getWindUnit(),
                            windWithUnit.getWindDirection());
            remoteViews.setTextViewText(widgetWindId, wind);
            remoteViews.setViewVisibility(widgetWindIconId, TextView.GONE);
        } else {
            String wind = ": " + windWithUnit.getWindSpeed(0) + " " + windWithUnit.getWindUnit();
            remoteViews.setImageViewBitmap(widgetWindIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_wind)));
            remoteViews.setViewVisibility(widgetWindIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(widgetWindId, wind);
        }
    }

    public static void setPressure(Context context, RemoteViews remoteViews, float value, Locale locale,
                                   int widgetPressureId, int widgetPressureIconId) {
        PressureWithUnit pressureWithUnit = AppPreference.getPressureWithUnit(context, value, locale);
        if (AppPreference.showLabelsOnWidget(context)) {
            String pressure =
                    context.getString(R.string.pressure_label,
                            pressureWithUnit.getPressure(AppPreference.getPressureDecimalPlaces(context)),
                            pressureWithUnit.getPressureUnit());
            remoteViews.setTextViewText(widgetPressureId, pressure);
            remoteViews.setViewVisibility(widgetPressureIconId, TextView.GONE);
        } else {
            String pressure = ": " + pressureWithUnit.getPressure(0) + " " + pressureWithUnit.getPressureUnit();
            remoteViews.setImageViewBitmap(widgetPressureIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_barometer)));
            remoteViews.setViewVisibility(widgetPressureIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(widgetPressureId, pressure);
        }
    }

    public static void setClouds(Context context, RemoteViews remoteViews, int value,
                                 int widgetCloudsId, int widgetCloudsIconId) {
        String percentSign = context.getString(R.string.percent_sign);
        if (AppPreference.showLabelsOnWidget(context)) {
            String cloudnes =
                    context.getString(R.string.cloudiness_label,
                            String.valueOf(value), percentSign);
            remoteViews.setTextViewText(widgetCloudsId, cloudnes);
            remoteViews.setViewVisibility(widgetCloudsIconId, TextView.GONE);
        } else {
            String cloudnes = ": " + String.valueOf(value) + " " + percentSign;
            remoteViews.setImageViewBitmap(widgetCloudsIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_cloudiness)));
            remoteViews.setViewVisibility(widgetCloudsIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(widgetCloudsId, cloudnes);
        }
    }

    public static WeatherForecastDbHelper.WeatherForecastRecord updateWeatherForecast(
            Context context,
            long locationId,
            Integer widgetId,
            RemoteViews remoteViews,
            Integer forecast_1_widget_day_layout,
            int forecast_1_widget_icon,
            int forecast_1_widget_day,
            int forecast_1_widget_temperatures,
            Integer forecast_2_widget_day_layout,
            int forecast_2_widget_icon,
            int forecast_2_widget_day,
            int forecast_2_widget_temperatures,
            Integer forecast_3_widget_day_layout,
            int forecast_3_widget_icon,
            int forecast_3_widget_day,
            int forecast_3_widget_temperatures,
            Integer forecast_4_widget_day_layout,
            int forecast_4_widget_icon,
            int forecast_4_widget_day,
            int forecast_4_widget_temperatures,
            Integer forecast_5_widget_day_layout,
            int forecast_5_widget_icon,
            int forecast_5_widget_day,
            int forecast_5_widget_temperatures) {
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location location = locationsDbHelper.getLocationById(locationId);
        if (location == null) {
            return null;
        }
        SimpleDateFormat sdfDayOfWeek = getDaysFormatter(context, widgetId, location.getLocale());

        Long daysCount = 5l;
        Boolean hoursForecast = null;
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        if (widgetId != null) {
            daysCount = widgetSettingsDbHelper.getParamLong(widgetId, "forecastDaysCount");
            hoursForecast = widgetSettingsDbHelper.getParamBoolean(widgetId, "hoursForecast");
            if (daysCount == null) {
                daysCount = 5l;
            }
        }

        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(locationId);
        //appendLog(context, TAG, "updateWeatherForecast:locationId=" + locationId + ", weatherForecastRecord=" + weatherForecastRecord);
        if (weatherForecastRecord == null) {
            return null;
        }
        if ((hoursForecast != null) && hoursForecast) {
            return createForecastByHours(
                    context,
                    location,
                    weatherForecastRecord,
                    daysCount,
                    remoteViews,
                    forecast_1_widget_day_layout,
                    forecast_1_widget_icon,
                    forecast_1_widget_day,
                    forecast_1_widget_temperatures,
                    forecast_2_widget_day_layout,
                    forecast_2_widget_icon,
                    forecast_2_widget_day,
                    forecast_2_widget_temperatures,
                    forecast_3_widget_day_layout,
                    forecast_3_widget_icon,
                    forecast_3_widget_day,
                    forecast_3_widget_temperatures,
                    forecast_4_widget_day_layout,
                    forecast_4_widget_icon,
                    forecast_4_widget_day,
                    forecast_4_widget_temperatures,
                    forecast_5_widget_day_layout,
                    forecast_5_widget_icon,
                    forecast_5_widget_day,
                    forecast_5_widget_temperatures
            );
        } else {
            return createForecastByDays(
                    context,
                    weatherForecastRecord,
                    sdfDayOfWeek,
                    daysCount,
                    remoteViews,
                    forecast_1_widget_day_layout,
                    forecast_1_widget_icon,
                    forecast_1_widget_day,
                    forecast_1_widget_temperatures,
                    forecast_2_widget_day_layout,
                    forecast_2_widget_icon,
                    forecast_2_widget_day,
                    forecast_2_widget_temperatures,
                    forecast_3_widget_day_layout,
                    forecast_3_widget_icon,
                    forecast_3_widget_day,
                    forecast_3_widget_temperatures,
                    forecast_4_widget_day_layout,
                    forecast_4_widget_icon,
                    forecast_4_widget_day,
                    forecast_4_widget_temperatures,
                    forecast_5_widget_day_layout,
                    forecast_5_widget_icon,
                    forecast_5_widget_day,
                    forecast_5_widget_temperatures);
        }
    }

    private static WeatherForecastDbHelper.WeatherForecastRecord createForecastByDays(
            Context context,
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
            SimpleDateFormat sdfDayOfWeek,
            Long daysCount,
            RemoteViews remoteViews,
            Integer forecast_1_widget_day_layout,
            int forecast_1_widget_icon,
            int forecast_1_widget_day,
            int forecast_1_widget_temperatures,
            Integer forecast_2_widget_day_layout,
            int forecast_2_widget_icon,
            int forecast_2_widget_day,
            int forecast_2_widget_temperatures,
            Integer forecast_3_widget_day_layout,
            int forecast_3_widget_icon,
            int forecast_3_widget_day,
            int forecast_3_widget_temperatures,
            Integer forecast_4_widget_day_layout,
            int forecast_4_widget_icon,
            int forecast_4_widget_day,
            int forecast_4_widget_temperatures,
            Integer forecast_5_widget_day_layout,
            int forecast_5_widget_icon,
            int forecast_5_widget_day,
            int forecast_5_widget_temperatures
    ) {
        Integer firstDayOfYear = null;
        Map<Integer, List<DetailedWeatherForecast>> weatherList = new HashMap<>();
        Calendar forecastCalendar = Calendar.getInstance();
        int initialYearForTheList = forecastCalendar.get(Calendar.YEAR);
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
            int dayInYearForList;
            int yearForList;
            if (dayInYear > 365) {
                dayInYearForList = dayInYear - 365;
                yearForList = initialYearForTheList + 1;
            } else {
                dayInYearForList = dayInYear;
                yearForList = initialYearForTheList;
            }
            if ((weatherList.get(dayInYearForList) == null) || (weatherList.get(dayInYearForList).size() < 3)) {
                continue;
            }
            dayCounter++;
            Map<Integer, Integer> weatherIdsInDay = new HashMap<>();
            for (DetailedWeatherForecast weatherForecastForDay : weatherList.get(dayInYearForList)) {
                WeatherCondition weatherCondition = weatherForecastForDay.getFirstWeatherCondition();
                /*appendLog(context, TAG, "preLoadWeather:dayInYear=" + dayInYearForList + ":dayCounter=" + dayCounter +
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
            for (DetailedWeatherForecast weatherForecastForDay : weatherList.get(dayInYearForList)) {
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
                    setForecastDayInfo(
                            context,
                            dayCounter,
                            daysCount,
                            remoteViews,
                            forecast_1_widget_day_layout,
                            forecast_1_widget_icon,
                            weatherIdForTheDay,
                            forecast_1_widget_day,
                            forecast_1_widget_temperatures,
                            iconId,
                            dayInYearForList,
                            yearForList,
                            maxTemp,
                            minTemp,
                            maxWind,
                            sdfDayOfWeek);
                    break;
                case 2:
                    setForecastDayInfo(
                            context,
                            dayCounter,
                            daysCount,
                            remoteViews,
                            forecast_2_widget_day_layout,
                            forecast_2_widget_icon,
                            weatherIdForTheDay,
                            forecast_2_widget_day,
                            forecast_2_widget_temperatures,
                            iconId,
                            dayInYearForList,
                            yearForList,
                            maxTemp,
                            minTemp,
                            maxWind,
                            sdfDayOfWeek);
                    break;
                case 3:
                    setForecastDayInfo(
                            context,
                            dayCounter,
                            daysCount,
                            remoteViews,
                            forecast_3_widget_day_layout,
                            forecast_3_widget_icon,
                            weatherIdForTheDay,
                            forecast_3_widget_day,
                            forecast_3_widget_temperatures,
                            iconId,
                            dayInYearForList,
                            yearForList,
                            maxTemp,
                            minTemp,
                            maxWind,
                            sdfDayOfWeek);
                    break;
                case 4:
                    setForecastDayInfo(
                            context,
                            dayCounter,
                            daysCount,
                            remoteViews,
                            forecast_4_widget_day_layout,
                            forecast_4_widget_icon,
                            weatherIdForTheDay,
                            forecast_4_widget_day,
                            forecast_4_widget_temperatures,
                            iconId,
                            dayInYearForList,
                            yearForList,
                            maxTemp,
                            minTemp,
                            maxWind,
                            sdfDayOfWeek);
                    break;
                case 5:
                    setForecastDayInfo(
                            context,
                            dayCounter,
                            daysCount,
                            remoteViews,
                            forecast_5_widget_day_layout,
                            forecast_5_widget_icon,
                            weatherIdForTheDay,
                            forecast_5_widget_day,
                            forecast_5_widget_temperatures,
                            iconId,
                            dayInYearForList,
                            yearForList,
                            maxTemp,
                            minTemp,
                            maxWind,
                            sdfDayOfWeek);
                    break;
            }
        }
        return weatherForecastRecord;
    }

    private static WeatherForecastDbHelper.WeatherForecastRecord createForecastByHours(
            Context context,
            Location location,
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
            long hoursCount,
            RemoteViews remoteViews,
            Integer forecast_1_widget_day_layout,
            int forecast_1_widget_icon,
            int forecast_1_widget_day,
            int forecast_1_widget_temperatures,
            Integer forecast_2_widget_day_layout,
            int forecast_2_widget_icon,
            int forecast_2_widget_day,
            int forecast_2_widget_temperatures,
            Integer forecast_3_widget_day_layout,
            int forecast_3_widget_icon,
            int forecast_3_widget_day,
            int forecast_3_widget_temperatures,
            Integer forecast_4_widget_day_layout,
            int forecast_4_widget_icon,
            int forecast_4_widget_day,
            int forecast_4_widget_temperatures,
            Integer forecast_5_widget_day_layout,
            int forecast_5_widget_icon,
            int forecast_5_widget_day,
            int forecast_5_widget_temperatures) {

        int hourCounter = 0;
        for (DetailedWeatherForecast detailedWeatherForecast: weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {
            switch (++hourCounter) {
                case 1:
                    setForecastHourInfo(
                            context,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_1_widget_day_layout,
                            forecast_1_widget_icon,
                            detailedWeatherForecast.getFirstWeatherCondition().getWeatherId(),
                            forecast_1_widget_day,
                            forecast_1_widget_temperatures,
                            detailedWeatherForecast.getFirstWeatherCondition().getIcon(),
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            location);
                    break;
                case 2:
                    setForecastHourInfo(
                            context,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_2_widget_day_layout,
                            forecast_2_widget_icon,
                            detailedWeatherForecast.getFirstWeatherCondition().getWeatherId(),
                            forecast_2_widget_day,
                            forecast_2_widget_temperatures,
                            detailedWeatherForecast.getFirstWeatherCondition().getIcon(),
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            location);
                    break;
                case 3:
                    setForecastHourInfo(
                            context,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_3_widget_day_layout,
                            forecast_3_widget_icon,
                            detailedWeatherForecast.getFirstWeatherCondition().getWeatherId(),
                            forecast_3_widget_day,
                            forecast_3_widget_temperatures,
                            detailedWeatherForecast.getFirstWeatherCondition().getIcon(),
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            location);
                    break;
                case 4:
                    setForecastHourInfo(
                            context,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_4_widget_day_layout,
                            forecast_4_widget_icon,
                            detailedWeatherForecast.getFirstWeatherCondition().getWeatherId(),
                            forecast_4_widget_day,
                            forecast_4_widget_temperatures,
                            detailedWeatherForecast.getFirstWeatherCondition().getIcon(),
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            location);
                    break;
                case 5:
                    setForecastHourInfo(
                            context,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_5_widget_day_layout,
                            forecast_5_widget_icon,
                            detailedWeatherForecast.getFirstWeatherCondition().getWeatherId(),
                            forecast_5_widget_day,
                            forecast_5_widget_temperatures,
                            detailedWeatherForecast.getFirstWeatherCondition().getIcon(),
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            location);
                    break;
            }
        }
        return weatherForecastRecord;
    }

    private static void setForecastHourInfo(
            Context context,
            int dayCounter,
            long daysCount,
            RemoteViews remoteViews,
            Integer dayViewId,
            int forecastIconId,
            int weatherIdForTheDay,
            int weatherIdForDayName,
            int weatherIdForTemperatures,
            String iconId,
            long forecastTime,
            double maxTemp,
            double minTemp,
            double maxWind,
            Location location) {

        if (dayViewId != null) {
            if (dayCounter > daysCount) {
                remoteViews.setViewVisibility(dayViewId, View.GONE);
                return;
            } else {
                remoteViews.setViewVisibility(dayViewId, View.VISIBLE);
            }
        }

        Calendar forecastCalendar = Calendar.getInstance();
        forecastCalendar.setTimeInMillis(forecastTime * 1000);
        Utils.setForecastIcon(
                remoteViews,
                context,
                forecastIconId,
                weatherIdForTheDay,
                iconId,
                maxTemp,
                maxWind);
        remoteViews.setTextViewText(
                weatherIdForDayName,
                AppPreference.getLocalizedHour(context, forecastCalendar.getTime(), location.getLocale()));
        remoteViews.setTextViewText(
                weatherIdForTemperatures,
                Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
    }

    private static void setForecastDayInfo(
            Context context,
            int dayCounter,
            long daysCount,
            RemoteViews remoteViews,
            Integer dayViewId,
            int forecastIconId,
            int weatherIdForTheDay,
            int weatherIdForDayName,
            int weatherIdForTemperatures,
            String iconId,
            int dayInYearForList,
            int yearForList,
            double maxTemp,
            double minTemp,
            double maxWind,
            SimpleDateFormat sdfDayOfWeek) {

        Calendar forecastCalendar = Calendar.getInstance();

        if (dayViewId != null) {
            if (dayCounter > daysCount) {
                remoteViews.setViewVisibility(dayViewId, View.GONE);
                return;
            } else {
                remoteViews.setViewVisibility(dayViewId, View.VISIBLE);
            }
        }

        Utils.setForecastIcon(
                remoteViews,
                context,
                forecastIconId,
                weatherIdForTheDay,
                iconId,
                maxTemp,
                maxWind);
        forecastCalendar.set(Calendar.DAY_OF_YEAR, dayInYearForList);
        forecastCalendar.set(Calendar.YEAR, yearForList);
        remoteViews.setTextViewText(
                weatherIdForDayName,
                sdfDayOfWeek.format(forecastCalendar.getTime()));
        remoteViews.setTextViewText(
                weatherIdForTemperatures,
                Math.round(minTemp) + "/" + Math.round(maxTemp) + TemperatureUtil.getTemperatureUnit(context));
    }

    public static SimpleDateFormat getDaysFormatter(Context context, Integer widgetId, Locale locale) {
        if (widgetId == null) {
            return new SimpleDateFormat("EEEE", locale);
        }
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        Boolean forecastDayAbbrev = widgetSettingsDbHelper.getParamBoolean(widgetId, "forecast_day_abbrev");
        if ((forecastDayAbbrev != null) && forecastDayAbbrev) {
            return new SimpleDateFormat("EEE", locale);
        } else {
            return new SimpleDateFormat("EEEE", locale);
        }
    }

    private static void updateWidgetForType(Context context, Class<?> widgetProvider) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        Intent intentToUpdate = new Intent(context, widgetProvider);
        ComponentName widgetComponent = new ComponentName(context, widgetProvider);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        intentToUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intentToUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(intentToUpdate);
    }

    public static void updateWidgets(Context context) {
        updateWidgetForType(context, LessWidgetProvider.class);
        updateWidgetForType(context, MoreWidgetProvider.class);
        updateWidgetForType(context, ExtLocationWidgetProvider.class);
        updateWidgetForType(context, ExtLocationWithForecastWidgetProvider.class);
        updateWidgetForType(context, WeatherForecastWidgetProvider.class);
        updateWidgetForType(context, ExtLocationWithGraphWidgetProvider.class);
        updateWidgetForType(context, WeatherGraphWidgetProvider.class);
        updateWidgetForType(context, ExtLocationWithForecastGraphWidgetProvider.class);
    }

    public static void startBackgroundService(Context context, Intent intent) {
        startBackgroundService(context, intent, 10);
    }

    public static void startBackgroundService(Context context,
                                              Intent intent,
                                              long triggerInMillis ) {
        intent.setPackage("org.thosp.yourlocalweather");
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
