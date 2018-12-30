package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherForecastWidgetService extends IntentService {

    private static final String TAG = "WeatherForecastWidgetService";

    public WeatherForecastWidgetService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog(this, TAG, "updateWidgetstart");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, WeatherForecastWidgetProvider.class);

        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        for (int appWidgetId : widgetIds) {
            Long locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId");

            Location currentLocation;
            if (locationId == null) {
                currentLocation = locationsDbHelper.getLocationByOrderId(0);
            } else {
                currentLocation = locationsDbHelper.getLocationById(locationId);
            }

            if (currentLocation == null) {
                continue;
            }

            RemoteViews remoteViews = new RemoteViews(this.getPackageName(),
                    R.layout.widget_weather_forecast_1x3);

            WeatherForecastWidgetProvider.setWidgetTheme(this, remoteViews, appWidgetId);
            WeatherForecastWidgetProvider.setWidgetIntents(this, remoteViews, WeatherForecastWidgetProvider.class, appWidgetId);

            try {
                WidgetUtils.updateWeatherForecast(
                        getBaseContext(),
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
                appendLog(getBaseContext(), TAG, "preLoadWeather:error updating weather forecast", e);
            }
            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(this, TAG, "updateWidgetend");
    }
}
