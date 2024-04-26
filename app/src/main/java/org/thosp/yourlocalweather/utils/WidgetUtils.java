package org.thosp.yourlocalweather.utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithGraphWidgetProvider;
import org.thosp.yourlocalweather.widget.LessWidgetProvider;
import org.thosp.yourlocalweather.widget.MoreWidgetProvider;
import org.thosp.yourlocalweather.widget.WeatherForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.WeatherGraphWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class WidgetUtils {

    private static final String TAG = "WidgetUtils";

    public static void setSunset(Context context, RemoteViews remoteViews, Calendar calendar, Locale locale,
                                 int widgetSunsetId, int widgetSunsetIconId, Set<Integer> enabledDetails,
                                 boolean showLabelsOnWidget, String timeStylePreference) {
        if (!isDetailVisible(6, remoteViews, widgetSunsetId, widgetSunsetIconId, enabledDetails)) {
            return;
        }
        String value = "";
        if (calendar != null) {
            value = AppPreference.getLocalizedTime(context, calendar.getTime(), timeStylePreference, locale);
        }
        if (showLabelsOnWidget) {
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

    public static void setSunrise(Context context, RemoteViews remoteViews, Calendar calendar, Locale locale,
                                  int widgetSunriseId, int widgetSunriseIconId, Set<Integer> enabledDetails,
                                  boolean showLabelsOnWidget, String timeStylePreference) {
        if (!isDetailVisible(5, remoteViews, widgetSunriseId, widgetSunriseIconId, enabledDetails)) {
            return;
        }
        String value = "";
        if (calendar != null) {
            value = AppPreference.getLocalizedTime(context, calendar.getTime(), timeStylePreference, locale);
        }
        if (showLabelsOnWidget) {
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

    public static void setDewPoint(Context context, RemoteViews remoteViews, Weather weather,
                                   Locale locale, int dewPointId, int dewPointIconId,
                                   String temperatureUnitFromPreferences,
                                   Set<Integer> enabledDetails,
                                   boolean showLabelsOnWidget) {
        if (!isDetailVisible(4, remoteViews, dewPointId, dewPointIconId, enabledDetails)) {
            return;
        }
        String value = "";
        if (weather != null) {
            value = TemperatureUtil.getDewPointWithUnit(context, weather, temperatureUnitFromPreferences, locale);
        }
        if (showLabelsOnWidget) {
            String dewPointValue = context.getString(
                    R.string.dew_point_label,
                    value);
            remoteViews.setTextViewText(dewPointId, dewPointValue);
            remoteViews.setViewVisibility(dewPointIconId, TextView.GONE);
        } else {
            String dewPointValue = ": " + value;
            remoteViews.setImageViewBitmap(dewPointIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_dew_point)));
            remoteViews.setViewVisibility(dewPointIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(dewPointId, dewPointValue);
        }
    }

    public static void setHumidity(Context context,
                                   RemoteViews remoteViews,
                                   int value,
                                   int humidityId,
                                   int humidityIconId,
                                   Set<Integer> enabledDetails,
                                   boolean showLabelsOnWidget) {
        if (!isDetailVisible(1, remoteViews, humidityId, humidityIconId, enabledDetails)) {
            return;
        }
        String percentSign = context.getString(R.string.percent_sign);
        if (showLabelsOnWidget) {
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

    public static void setWind(Context context,
                               RemoteViews remoteViews,
                               float value,
                               float direction,
                               Locale locale,
                               int widgetWindId,
                               int widgetWindIconId,
                               Set<Integer> enabledDetails,
                               boolean showLabelsOnWidget,
                               String windUnitFromPreferences) {
        if (!isDetailVisible(0, remoteViews, widgetWindId, widgetWindIconId, enabledDetails)) {
            return;
        }
        WindWithUnit windWithUnit = AppPreference.getWindWithUnit(context, value, direction, windUnitFromPreferences, locale);
        if (showLabelsOnWidget) {
            String wind = context.getString(R.string.wind_label,
                            windWithUnit.getWindSpeed(0),
                            windWithUnit.getWindUnit(),
                            windWithUnit.getWindDirection());
            remoteViews.setTextViewText(widgetWindId, wind);
            remoteViews.setViewVisibility(widgetWindIconId, TextView.GONE);
        } else {
            String wind = ": "
                        + windWithUnit.getWindSpeed(0)
                        + " "
                        + windWithUnit.getWindUnit()
                        + " "
                        + windWithUnit.getWindDirection();
            remoteViews.setImageViewBitmap(widgetWindIconId, Utils.createWeatherIcon(context, context.getString(R.string.icon_wind)));
            remoteViews.setViewVisibility(widgetWindIconId, TextView.VISIBLE);
            remoteViews.setTextViewText(widgetWindId, wind);
        }
    }

    public static void setPressure(Context context, RemoteViews remoteViews, float value, String pressureUnitFromPreferences, Locale locale,
                                   int widgetPressureId, int widgetPressureIconId, Set<Integer> enabledDetails,
                                   boolean showLabelsOnWidget) {
        if (!isDetailVisible(2, remoteViews, widgetPressureId, widgetPressureIconId, enabledDetails)) {
            return;
        }
        PressureWithUnit pressureWithUnit = AppPreference.getPressureWithUnit(context, value, pressureUnitFromPreferences, locale);
        if (showLabelsOnWidget) {
            String pressure =
                    context.getString(R.string.pressure_label,
                            pressureWithUnit.getPressure(AppPreference.getPressureDecimalPlaces(pressureUnitFromPreferences)),
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
                                 int widgetCloudsId, int widgetCloudsIconId, Set<Integer> enabledDetails,
                                 boolean showLabelsOnWidget) {
        if (!isDetailVisible(3, remoteViews, widgetCloudsId, widgetCloudsIconId, enabledDetails)) {
            return;
        }
        String percentSign = context.getString(R.string.percent_sign);
        if (showLabelsOnWidget) {
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

    private static boolean isDetailVisible(int detailId,
                                           RemoteViews remoteViews,
                                           int widgetDetailId,
                                           int widgetDetailIconId,
                                           Set<Integer> enabledDetails) {
        if ((enabledDetails == null) || enabledDetails.contains(detailId)) {
            remoteViews.setViewVisibility(widgetDetailId, View.VISIBLE);
            remoteViews.setViewVisibility(widgetDetailIconId, View.VISIBLE);
            return true;
        }
        remoteViews.setViewVisibility(widgetDetailId, View.GONE);
        remoteViews.setViewVisibility(widgetDetailIconId, View.GONE);
        return false;
    }

    public static void updateWeatherForecast(
            Context context,
            Location location,
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
            Integer widgetId,
            Long daysCountFromWidgetSettings,
            Boolean hoursForecastFromWidgetSettings,
            Boolean forecastDayAbbrev,
            boolean fontBasedIcons,
            Map<Long, String> localizedHourMap,
            Map<Long, String> temperaturesMap,
            String temperatureUnitFromPreferences,
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
            int forecast_5_widget_temperatures,
            Integer forecast_6_widget_day_layout,
            int forecast_6_widget_icon,
            int forecast_6_widget_day,
            int forecast_6_widget_temperatures) {
        updateWeatherForecast(
                context,
                location,
                weatherForecastRecord,
                AppPreference.getTextColor(context),
                widgetId,
                daysCountFromWidgetSettings,
                hoursForecastFromWidgetSettings,
                forecastDayAbbrev,
                fontBasedIcons,
                localizedHourMap,
                temperaturesMap,
                temperatureUnitFromPreferences,
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
                forecast_5_widget_temperatures,
                forecast_6_widget_day_layout,
                forecast_6_widget_icon,
                forecast_6_widget_day,
                forecast_6_widget_temperatures);
    }

    public static void updateWeatherForecast(
            Context context,
            Location location,
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
            int fontColorId,
            Integer widgetId,
            Long daysCountFromWidgetSettings,
            Boolean hoursForecastFromWidgetSettings,
            Boolean forecastDayAbbrev,
            boolean fontBasedIcons,
            Map<Long, String> localizedHourMap,
            Map<Long, String> temperaturesMap,
            String temperatureUnitFromPreferences,
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
            int forecast_5_widget_temperatures,
            Integer forecast_6_widget_day_layout,
            int forecast_6_widget_icon,
            int forecast_6_widget_day,
            int forecast_6_widget_temperatures) {
        if (location == null) {
            return;
        }
        SimpleDateFormat sdfDayOfWeek = getDaysFormatter(context, widgetId, forecastDayAbbrev, location.getLocale());

        if (weatherForecastRecord == null) {
            appendLog(context, TAG, "weatherForecastRecord is null");
            return;
        }

        if ((hoursForecastFromWidgetSettings != null) && hoursForecastFromWidgetSettings) {
            createForecastByHours(
                    context,
                    fontColorId,
                    weatherForecastRecord,
                    daysCountFromWidgetSettings,
                    fontBasedIcons,
                    localizedHourMap,
                    temperaturesMap,
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
                    forecast_5_widget_temperatures,
                    forecast_6_widget_day_layout,
                    forecast_6_widget_icon,
                    forecast_6_widget_day,
                    forecast_6_widget_temperatures
            );
        } else {
            createForecastByDays(
                    context,
                    fontColorId,
                    weatherForecastRecord,
                    sdfDayOfWeek,
                    daysCountFromWidgetSettings,
                    fontBasedIcons,
                    temperatureUnitFromPreferences,
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
                    forecast_5_widget_temperatures,
                    forecast_6_widget_day_layout,
                    forecast_6_widget_icon,
                    forecast_6_widget_day,
                    forecast_6_widget_temperatures);
        }
    }

    private static WeatherForecastDbHelper.WeatherForecastRecord createForecastByDays(
            Context context,
            int fontColorId,
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
            SimpleDateFormat sdfDayOfWeek,
            Long daysCount,
            boolean fontBasedIcons,
            String temperatureUnitFromPreferences,
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
            int forecast_5_widget_temperatures,
            Integer forecast_6_widget_day_layout,
            int forecast_6_widget_icon,
            int forecast_6_widget_day,
            int forecast_6_widget_temperatures
    ) {
        Set<ForecastUtil.WeatherForecastPerDay> countedForecast = ForecastUtil.calculateWeatherForDays(context, weatherForecastRecord);
        for (ForecastUtil.WeatherForecastPerDay countedForecastForDay: countedForecast) {
            switch (countedForecastForDay.dayIndex) {
                case 1:
                    setForecastDayInfo(
                            context,
                            countedForecastForDay,
                            fontColorId,
                            daysCount,
                            remoteViews,
                            forecast_1_widget_day_layout,
                            forecast_1_widget_icon,
                            forecast_1_widget_day,
                            forecast_1_widget_temperatures,
                            sdfDayOfWeek,
                            temperatureUnitFromPreferences,
                            fontBasedIcons);
                    break;
                case 2:
                    setForecastDayInfo(
                            context,
                            countedForecastForDay,
                            fontColorId,
                            daysCount,
                            remoteViews,
                            forecast_2_widget_day_layout,
                            forecast_2_widget_icon,
                            forecast_2_widget_day,
                            forecast_2_widget_temperatures,
                            sdfDayOfWeek,
                            temperatureUnitFromPreferences,
                            fontBasedIcons);
                    break;
                case 3:
                    setForecastDayInfo(
                            context,
                            countedForecastForDay,
                            fontColorId,
                            daysCount,
                            remoteViews,
                            forecast_3_widget_day_layout,
                            forecast_3_widget_icon,
                            forecast_3_widget_day,
                            forecast_3_widget_temperatures,
                            sdfDayOfWeek,
                            temperatureUnitFromPreferences,
                            fontBasedIcons);
                    break;
                case 4:
                    setForecastDayInfo(
                            context,
                            countedForecastForDay,
                            fontColorId,
                            daysCount,
                            remoteViews,
                            forecast_4_widget_day_layout,
                            forecast_4_widget_icon,
                            forecast_4_widget_day,
                            forecast_4_widget_temperatures,
                            sdfDayOfWeek,
                            temperatureUnitFromPreferences,
                            fontBasedIcons);
                    break;
                case 5:
                    setForecastDayInfo(
                            context,
                            countedForecastForDay,
                            fontColorId,
                            daysCount,
                            remoteViews,
                            forecast_5_widget_day_layout,
                            forecast_5_widget_icon,
                            forecast_5_widget_day,
                            forecast_5_widget_temperatures,
                            sdfDayOfWeek,
                            temperatureUnitFromPreferences,
                            fontBasedIcons);
                    break;
                case 6:
                    setForecastDayInfo(
                            context,
                            countedForecastForDay,
                            fontColorId,
                            daysCount,
                            remoteViews,
                            forecast_6_widget_day_layout,
                            forecast_6_widget_icon,
                            forecast_6_widget_day,
                            forecast_6_widget_temperatures,
                            sdfDayOfWeek,
                            temperatureUnitFromPreferences,
                            fontBasedIcons);
                    break;
            }
        }
        return weatherForecastRecord;
    }

    private static WeatherForecastDbHelper.WeatherForecastRecord createForecastByHours(
            Context context,
            int fontColorId,
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord,
            long hoursCount,
            boolean fontBasedIcons,
            Map<Long, String> localizedHourMap,
            Map<Long, String> temperaturesMap,
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
            int forecast_5_widget_temperatures,
            Integer forecast_6_widget_day_layout,
            int forecast_6_widget_icon,
            int forecast_6_widget_day,
            int forecast_6_widget_temperatures) {

        int hourCounter = 0;
        long now = System.currentTimeMillis();
        for (DetailedWeatherForecast detailedWeatherForecast: weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {

            if ((detailedWeatherForecast == null) || ((1000*detailedWeatherForecast.getDateTime()) < now)) {
                continue;
            }

            switch (++hourCounter) {
                case 1:
                    setForecastHourInfo(
                            context,
                            fontColorId,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_1_widget_day_layout,
                            forecast_1_widget_icon,
                            detailedWeatherForecast.getWeatherId(),
                            forecast_1_widget_day,
                            forecast_1_widget_temperatures,
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            localizedHourMap,
                            temperaturesMap,
                            fontBasedIcons);
                    break;
                case 2:
                    setForecastHourInfo(
                            context,
                            fontColorId,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_2_widget_day_layout,
                            forecast_2_widget_icon,
                            detailedWeatherForecast.getWeatherId(),
                            forecast_2_widget_day,
                            forecast_2_widget_temperatures,
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            localizedHourMap,
                            temperaturesMap,
                            fontBasedIcons);
                    break;
                case 3:
                    setForecastHourInfo(
                            context,
                            fontColorId,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_3_widget_day_layout,
                            forecast_3_widget_icon,
                            detailedWeatherForecast.getWeatherId(),
                            forecast_3_widget_day,
                            forecast_3_widget_temperatures,
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            localizedHourMap,
                            temperaturesMap,
                            fontBasedIcons);
                    break;
                case 4:
                    setForecastHourInfo(
                            context,
                            fontColorId,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_4_widget_day_layout,
                            forecast_4_widget_icon,
                            detailedWeatherForecast.getWeatherId(),
                            forecast_4_widget_day,
                            forecast_4_widget_temperatures,
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            localizedHourMap,
                            temperaturesMap,
                            fontBasedIcons);
                    break;
                case 5:
                    setForecastHourInfo(
                            context,
                            fontColorId,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_5_widget_day_layout,
                            forecast_5_widget_icon,
                            detailedWeatherForecast.getWeatherId(),
                            forecast_5_widget_day,
                            forecast_5_widget_temperatures,
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            localizedHourMap,
                            temperaturesMap,
                            fontBasedIcons);
                    break;
                case 6:
                    setForecastHourInfo(
                            context,
                            fontColorId,
                            hourCounter,
                            hoursCount,
                            remoteViews,
                            forecast_6_widget_day_layout,
                            forecast_6_widget_icon,
                            detailedWeatherForecast.getWeatherId(),
                            forecast_6_widget_day,
                            forecast_6_widget_temperatures,
                            detailedWeatherForecast.getDateTime(),
                            detailedWeatherForecast.getTemperatureMax(),
                            detailedWeatherForecast.getTemperatureMin(),
                            detailedWeatherForecast.getWindSpeed(),
                            localizedHourMap,
                            temperaturesMap,
                            fontBasedIcons);
                    break;
            }
        }
        return weatherForecastRecord;
    }

    private static void setForecastHourInfo(
            Context context,
            int fontColorId,
            int dayCounter,
            long daysCount,
            RemoteViews remoteViews,
            Integer dayViewId,
            int forecastIconId,
            int weatherIdForTheDay,
            int weatherIdForDayName,
            int weatherIdForTemperatures,
            long forecastTime,
            double maxTemp,
            double minTemp,
            double maxWind,
            Map<Long, String> localizedHourMap,
            Map<Long, String> temperaturesMap,
            boolean fontBasedIcons) {

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
                fontBasedIcons,
                weatherIdForTheDay,
                maxTemp,
                maxWind,
                fontColorId);
        remoteViews.setTextViewText(
                weatherIdForDayName,
                localizedHourMap.get(forecastTime));
        remoteViews.setTextColor(weatherIdForDayName, fontColorId);
        remoteViews.setTextViewText(
                weatherIdForTemperatures,
                temperaturesMap.get(forecastTime));
        remoteViews.setTextColor(weatherIdForTemperatures, fontColorId);
    }

    private static void setForecastDayInfo(
            Context context,
            ForecastUtil.WeatherForecastPerDay countedForecastForDay,
            int fontColorId,
            long daysCount,
            RemoteViews remoteViews,
            Integer dayViewId,
            int forecastIconId,
            int weatherIdForDayName,
            int weatherIdForTemperatures,
            SimpleDateFormat sdfDayOfWeek,
            String temperatureUnitFromPreferences,
            boolean fontBasedIcons) {

        Calendar forecastCalendar = Calendar.getInstance();

        if (dayViewId != null) {
            if (countedForecastForDay.dayIndex > daysCount) {
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
                fontBasedIcons,
                countedForecastForDay.weatherIds.mainWeatherId,
                countedForecastForDay.weatherMaxMinForDay.maxTemp,
                countedForecastForDay.weatherMaxMinForDay.maxWind,
                fontColorId);
        forecastCalendar.set(Calendar.DAY_OF_YEAR, countedForecastForDay.dayInYear);
        forecastCalendar.set(Calendar.YEAR, countedForecastForDay.year);
        String forecastDay = sdfDayOfWeek.format(forecastCalendar.getTime());
        appendLog(context, TAG, "set forecast info for day:", countedForecastForDay.dayIndex + ":" + forecastDay);
        remoteViews.setTextViewText(
                weatherIdForDayName,
                forecastDay);
        remoteViews.setTextColor(weatherIdForDayName, fontColorId);
        remoteViews.setTextViewText(
                weatherIdForTemperatures,
                Math.round(TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnitFromPreferences, countedForecastForDay.weatherMaxMinForDay.minTemp)) +
                        "/" +
                        Math.round(TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnitFromPreferences, countedForecastForDay.weatherMaxMinForDay.maxTemp)) +
                        TemperatureUtil.getTemperatureUnit(context, temperatureUnitFromPreferences));
        remoteViews.setTextColor(weatherIdForTemperatures, fontColorId);
    }

    public static Set<Integer> getCurrentWeatherDetailsFromSettings(
            String storedCurrentWeatherDetails) {
        Set<Integer> currentWeatherDetails = new HashSet<>();

        String[] values = storedCurrentWeatherDetails.split(",");
        for (String value: values) {
            int intValue;
            try {
                intValue = Integer.parseInt(value);
                currentWeatherDetails.add(intValue);
            } catch (Exception e) {
                //do nothing, just continue
            }
        }

        return currentWeatherDetails;
    }

    public static SimpleDateFormat getDaysFormatter(Context context, Integer widgetId, Boolean forecastDayAbbrev, Locale locale) {
        if (widgetId == null) {
            return new SimpleDateFormat("EEEE", locale);
        }
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
        ContextCompat.startForegroundService(context, intent);
    }

    public static boolean isInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }

    public static void updateCurrentWeatherDetails(
            Context context,
            RemoteViews remoteViews,
            CurrentWeatherDbHelper.WeatherRecord weatherRecord,
            Locale locale,
            String storedCurrentWeatherDetails,
            String pressureUnitFromPreferences,
            String temperatureUnitFromPreferences,
            int textColorId,
            boolean showLabelsOnWidget,
            String windUnitFromPreferences,
            String timeStylePreference) {

        remoteViews.setTextColor(R.id.widget_current_detail_wind, textColorId);
        remoteViews.setTextColor(R.id.widget_current_detail_humidity, textColorId);
        remoteViews.setTextColor(R.id.widget_current_detail_dew_point, textColorId);
        remoteViews.setTextColor(R.id.widget_current_detail_sunrise, textColorId);
        remoteViews.setTextColor(R.id.widget_current_detail_sunset, textColorId);
        remoteViews.setTextColor(R.id.widget_current_detail_pressure, textColorId);
        remoteViews.setTextColor(R.id.widget_current_detail_clouds, textColorId);

        Set<Integer> currentWeatherDetailValues = WidgetUtils.getCurrentWeatherDetailsFromSettings(storedCurrentWeatherDetails);

        if (weatherRecord == null) {
            WidgetUtils.setWind(context,
                    remoteViews,
                    0,
                    0,
                    locale,
                    R.id.widget_current_detail_wind,
                    R.id.widget_current_detail_wind_icon,
                    currentWeatherDetailValues,
                    showLabelsOnWidget,
                    windUnitFromPreferences);
            WidgetUtils.setHumidity(context, remoteViews, 0,
                    R.id.widget_current_detail_humidity,
                    R.id.widget_current_detail_humidity_icon,
                    currentWeatherDetailValues, showLabelsOnWidget);
            WidgetUtils.setDewPoint(context, remoteViews, null, locale,
                    R.id.widget_current_detail_dew_point,
                    R.id.widget_current_detail_dew_point_icon,
                    temperatureUnitFromPreferences,
                    currentWeatherDetailValues,
                    showLabelsOnWidget);

            WidgetUtils.setSunrise(context,
                    remoteViews,
                    null,
                    locale,
                    R.id.widget_current_detail_sunrise,
                    R.id.widget_current_detail_sunrise_icon,
                    currentWeatherDetailValues,
                    showLabelsOnWidget,
                    timeStylePreference);
            WidgetUtils.setSunset(context,
                    remoteViews,
                    null,
                    locale,
                    R.id.widget_current_detail_sunset,
                    R.id.widget_current_detail_sunset_icon,
                    currentWeatherDetailValues,
                    showLabelsOnWidget,
                    timeStylePreference);

            WidgetUtils.setPressure(context,
                    remoteViews,
                    0,
                    pressureUnitFromPreferences,
                    locale,
                    R.id.widget_current_detail_pressure,
                    R.id.widget_current_detail_pressure_icon,
                    currentWeatherDetailValues,
                    showLabelsOnWidget);
            WidgetUtils.setClouds(context, remoteViews, 0,
                    R.id.widget_current_detail_clouds,
                    R.id.widget_current_detail_clouds_icon,
                    currentWeatherDetailValues,
                    showLabelsOnWidget);
            return;
        }

        Weather weather = weatherRecord.getWeather();

        if (weather == null) {
            return;
        }

        WidgetUtils.setWind(context,
                remoteViews,
                weather.getWindSpeed(),
                weather.getWindDirection(),
                locale,
                R.id.widget_current_detail_wind,
                R.id.widget_current_detail_wind_icon,
                currentWeatherDetailValues,
                showLabelsOnWidget,
                windUnitFromPreferences);
        WidgetUtils.setHumidity(context, remoteViews, weather.getHumidity(),
                R.id.widget_current_detail_humidity,
                R.id.widget_current_detail_humidity_icon,
                currentWeatherDetailValues,
                showLabelsOnWidget);
        WidgetUtils.setDewPoint(context,
                remoteViews,
                weather,
                locale,
                R.id.widget_current_detail_dew_point,
                R.id.widget_current_detail_dew_point_icon,
                temperatureUnitFromPreferences,
                currentWeatherDetailValues,
                showLabelsOnWidget);

        WidgetUtils.setPressure(context,
                remoteViews,
                weather.getPressure(),
                pressureUnitFromPreferences,
                locale,
                R.id.widget_current_detail_pressure,
                R.id.widget_current_detail_pressure_icon,
                currentWeatherDetailValues,
                showLabelsOnWidget);
        WidgetUtils.setClouds(context, remoteViews, weather.getClouds(),
                R.id.widget_current_detail_clouds,
                R.id.widget_current_detail_clouds_icon,
                currentWeatherDetailValues,
                showLabelsOnWidget);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1000 * weather.getSunrise());
        WidgetUtils.setSunrise(context,
                remoteViews,
                calendar,
                locale,
                R.id.widget_current_detail_sunrise,
                R.id.widget_current_detail_sunrise_icon,
                currentWeatherDetailValues,
                showLabelsOnWidget,
                timeStylePreference);
        calendar.setTimeInMillis(1000 * weather.getSunset());
        WidgetUtils.setSunset(context,
                remoteViews,
                calendar,
                locale,
                R.id.widget_current_detail_sunset,
                R.id.widget_current_detail_sunset_icon,
                currentWeatherDetailValues,
                showLabelsOnWidget,
                timeStylePreference);
    }
}
