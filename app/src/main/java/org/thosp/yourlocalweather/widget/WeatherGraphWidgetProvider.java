package org.thosp.yourlocalweather.widget;

import android.content.Context;
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

public class WeatherGraphWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WeatherGraphWidgetProvider";

    private static final String WIDGET_NAME = "WEATHER_GRAPH_WIDGET";

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

        WeatherGraphWidgetProvider.setWidgetTheme(context, remoteViews);
        WeatherGraphWidgetProvider.setWidgetIntents(context, remoteViews, WeatherGraphWidgetProvider.class, appWidgetId);

        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = null;
        try {
            final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
            Location location = locationsDbHelper.getLocationById(currentLocation.getId());
            if (location != null) {
                weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
                if (weatherForecastRecord != null) {
                    remoteViews.setImageViewBitmap(R.id.widget_weather_graph_1x3_widget_combined_chart, GraphUtils.getCombinedChart(context, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList(), currentLocation.getId(), currentLocation.getLocale()));
                }
            }
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        remoteViews.setInt(R.id.widget_weather_graph_1x3_widget_root, "setBackgroundColor", backgroundColorId);
        appendLog(context, TAG, "setWidgetTheme:end");
    }

    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_weather_graph_1x3;
    }

    @Override
    protected Class<?> getWidgetClass() {
        return WeatherGraphWidgetProvider.class;
    }

    @Override
    protected String getWidgetName() {
        return WIDGET_NAME;
    }
}
