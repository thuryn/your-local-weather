package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherCondition;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WeatherForecastWidgetProvider";

    private static final String WIDGET_NAME = "WEATHER_FORECAST_WIDGET";

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);

        Long locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId");

        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }

        if (currentLocation == null) {
            return;
        }
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

        if (weatherRecord == null) {
            return;
        }

        Weather weather = weatherRecord.getWeather();

        WeatherForecastWidgetProvider.setWidgetTheme(context, remoteViews);
        WeatherForecastWidgetProvider.setWidgetIntents(context, remoteViews, WeatherForecastWidgetProvider.class, appWidgetId);

        remoteViews.setTextViewText(R.id.widget_city, Utils.getCityAndCountry(context, currentLocation.getOrderId()));
        remoteViews.setTextViewText(R.id.widget_temperature, TemperatureUtil.getTemperatureWithUnit(
                context,
                weather,
                currentLocation.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                currentLocation.getLocale()));
        String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                context,
                weather,
                currentLocation.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                currentLocation.getLocale());
        if (secondTemperature != null) {
            remoteViews.setViewVisibility(R.id.widget_second_temperature, View.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_second_temperature, secondTemperature);
        } else {
            remoteViews.setViewVisibility(R.id.widget_second_temperature, View.GONE);
        }
        remoteViews.setTextViewText(R.id.widget_description,
                                    Utils.getWeatherDescription(context,
                                                                currentLocation.getLocaleAbbrev(),
                                                                weather));
        WidgetUtils.setWind(context,
                            remoteViews,
                            weather.getWindSpeed(),
                            weather.getWindDirection(),
                            currentLocation.getLocale());
        WidgetUtils.setHumidity(context, remoteViews, weather.getHumidity());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1000 * weather.getSunrise());
        WidgetUtils.setSunrise(context, remoteViews, AppPreference.getLocalizedTime(context, calendar.getTime(), currentLocation.getLocale()));
        calendar.setTimeInMillis(1000 * weather.getSunset());
        WidgetUtils.setSunset(context, remoteViews, AppPreference.getLocalizedTime(context, calendar.getTime(), currentLocation.getLocale()));
        Utils.setWeatherIcon(remoteViews, context, weatherRecord);

        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = null;
        try {
            weatherForecastRecord = WidgetUtils.updateWeatherForecast(context, currentLocation.getId(), remoteViews);
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, currentLocation);
        remoteViews.setTextViewText(R.id.widget_last_update, lastUpdate);
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        remoteViews.setInt(R.id.widget_root, "setBackgroundColor", backgroundColorId);
        remoteViews.setTextColor(R.id.forecast_1_widget_day, textColorId);
        remoteViews.setTextColor(R.id.forecast_1_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.forecast_2_widget_day, textColorId);
        remoteViews.setTextColor(R.id.forecast_2_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.forecast_3_widget_day, textColorId);
        remoteViews.setTextColor(R.id.forecast_3_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.forecast_4_widget_day, textColorId);
        remoteViews.setTextColor(R.id.forecast_4_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.forecast_5_widget_day, textColorId);
        remoteViews.setTextColor(R.id.forecast_5_widget_temperatures, textColorId);
        remoteViews.setInt(R.id.header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);
        appendLog(context, TAG, "setWidgetTheme:end");
    }

    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_weather_forecast_1x3;
    }

    @Override
    protected Class<?> getWidgetClass() {
        return WeatherForecastWidgetProvider.class;
    }

    @Override
    protected String getWidgetName() {
        return WIDGET_NAME;
    }
}
