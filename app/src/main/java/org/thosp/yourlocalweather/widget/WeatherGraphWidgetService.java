package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
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
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.Calendar;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherGraphWidgetService extends IntentService {

    private static final String TAG = "WeatherGraphWidgetService";

    public WeatherGraphWidgetService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog(this, TAG, "updateWidgetstart");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, WeatherGraphWidgetProvider.class);

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
                    R.layout.widget_weather_graph_1x3);

            WeatherGraphWidgetProvider.setWidgetTheme(this, remoteViews);
            WeatherGraphWidgetProvider.setWidgetIntents(this, remoteViews, WeatherGraphWidgetProvider.class, appWidgetId);

            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = null;
            try {
                final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
                Location location = locationsDbHelper.getLocationById(currentLocation.getId());
                if (location != null) {
                    weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
                    if (weatherForecastRecord != null) {
                        remoteViews.setImageViewBitmap(R.id.widget_weather_graph_1x3_widget_combined_chart,
                                GraphUtils.getCombinedChart(this, appWidgetId, null, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList(), currentLocation.getId(), currentLocation.getLocale()));
                    }
                }
            } catch (Exception e) {
                appendLog(this, TAG, "preLoadWeather:error updating weather forecast", e);
            }
            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(this, TAG, "updateWidgetend");
    }
}
