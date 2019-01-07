package org.thosp.yourlocalweather.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

        WeatherForecastWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
        WeatherForecastWidgetProvider.setWidgetIntents(context, remoteViews, WeatherForecastWidgetProvider.class, appWidgetId);
        remoteViews.setTextViewText(R.id.widget_weather_forecast_1x3_widget_city, Utils.getCityAndCountry(context, currentLocation.getOrderId()));
        try {
            WidgetUtils.updateWeatherForecast(
                    context,
                    currentLocation.getId(),
                    appWidgetId,
                    remoteViews,
                    R.id.widget_weather_forecast_1x3_forecast_day_1,
                    R.id.widget_weather_forecast_1x3_forecast_1_widget_icon,
                    R.id.widget_weather_forecast_1x3_forecast_1_widget_day,
                    R.id.widget_weather_forecast_1x3_forecast_1_widget_temperatures,
                    R.id.widget_weather_forecast_1x3_forecast_day_2,
                    R.id.widget_weather_forecast_1x3_forecast_2_widget_icon,
                    R.id.widget_weather_forecast_1x3_forecast_2_widget_day,
                    R.id.widget_weather_forecast_1x3_forecast_2_widget_temperatures,
                    R.id.widget_weather_forecast_1x3_forecast_day_3,
                    R.id.widget_weather_forecast_1x3_forecast_3_widget_icon,
                    R.id.widget_weather_forecast_1x3_forecast_3_widget_day,
                    R.id.widget_weather_forecast_1x3_forecast_3_widget_temperatures,
                    R.id.widget_weather_forecast_1x3_forecast_day_4,
                    R.id.widget_weather_forecast_1x3_forecast_4_widget_icon,
                    R.id.widget_weather_forecast_1x3_forecast_4_widget_day,
                    R.id.widget_weather_forecast_1x3_forecast_4_widget_temperatures,
                    R.id.widget_weather_forecast_1x3_forecast_day_5,
                    R.id.widget_weather_forecast_1x3_forecast_5_widget_icon,
                    R.id.widget_weather_forecast_1x3_forecast_5_widget_day,
                    R.id.widget_weather_forecast_1x3_forecast_5_widget_temperatures);
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews, int widgetId) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        Boolean showLocation = widgetSettingsDbHelper.getParamBoolean(widgetId, "showLocation");
        if (showLocation == null) {
            showLocation = false;
        }
        if (showLocation) {
            remoteViews.setViewVisibility(R.id.widget_weather_forecast_1x3_widget_city, View.VISIBLE);
        } else {
            remoteViews.setViewVisibility(R.id.widget_weather_forecast_1x3_widget_city, View.GONE);
        }
        remoteViews.setInt(R.id.widget_weather_forecast_1x3_widget_root, "setBackgroundColor", backgroundColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_1_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_1_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_2_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_2_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_3_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_3_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_4_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_4_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_5_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_5_widget_temperatures, textColorId);

        appendLog(context, TAG, "setWidgetTheme:end");
    }

    @Override
    ArrayList<String> getEnabledActionPlaces() {
        ArrayList<String> enabledWidgetActions = new ArrayList();
        enabledWidgetActions.add("action_forecast");
        return enabledWidgetActions;
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
