package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.service.ForecastWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ExtLocationWithForecastGraphWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "ExtLocationWithForecastGraphWidgetProvider";

    private static final String WIDGET_NAME = "EXT_LOC_WITH_FORECAST_GRAPH_WIDGET";

    private static final String DEFAULT_CURRENT_WEATHER_DETAILS = "0,1";
    private static final int MAX_CURRENT_WEATHER_DETAILS = 2;

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
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

        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

        WidgetUtils.updateCurrentWeatherDetails(
                context,
                remoteViews,
                weatherRecord,
                currentLocation.getLocale(),
                appWidgetId,
                DEFAULT_CURRENT_WEATHER_DETAILS);

        appendLog(context, TAG, "Updating weather in widget, currentLocation.id=" + currentLocation.getId() + ", weatherRecord=" + weatherRecord);

        if (weatherRecord != null) {
            Weather weather = weatherRecord.getWeather();

            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_city, Utils.getCityAndCountry(context, currentLocation.getOrderId()));
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_temperature, TemperatureUtil.getTemperatureWithUnit(
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
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, secondTemperature);
            } else {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, View.GONE);
            }
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_description,
                                        Utils.getWeatherDescription(context,
                                                                    currentLocation.getLocaleAbbrev(),
                                                                    weather));

            Utils.setWeatherIcon(remoteViews, context, weatherRecord,
                    R.id.widget_ext_loc_forecast_graph_3x3_widget_icon);
        } else {
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_city, context.getString(R.string.location_not_found));
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_temperature, TemperatureUtil.getTemperatureWithUnit(
                    context,
                    null,
                    currentLocation.getLatitude(),
                    0,
                    currentLocation.getLocale()));
            String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                    context,
                    null,
                    currentLocation.getLatitude(),
                    0,
                    currentLocation.getLocale());
            if (secondTemperature != null) {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, secondTemperature);
            } else {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, View.GONE);
            }
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_description, "");

            Utils.setWeatherIcon(remoteViews, context, weatherRecord,
                    R.id.widget_ext_loc_forecast_graph_3x3_widget_icon);
        }
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = null;
        try {
            weatherForecastRecord = WidgetUtils.updateWeatherForecast(
                    context,
                    currentLocation.getId(),
                    appWidgetId,
                    remoteViews,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_day_1,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_1_widget_icon,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_1_widget_day,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_1_widget_temperatures,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_day_2,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_2_widget_icon,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_2_widget_day,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_2_widget_temperatures,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_day_3,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_3_widget_icon,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_3_widget_day,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_3_widget_temperatures,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_day_4,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_4_widget_icon,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_4_widget_day,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_4_widget_temperatures,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_day_5,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_5_widget_icon,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_5_widget_day,
                    R.id.widget_ext_loc_forecast_graph_3x3_forecast_5_widget_temperatures);

            if (weatherForecastRecord != null) {
                remoteViews.setImageViewBitmap(R.id.widget_ext_loc_forecast_graph_3x3_widget_combined_chart,
                        GraphUtils.getCombinedChart(context, appWidgetId,
                                0.4f, weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList(), currentLocation.getId(), currentLocation.getLocale()));
            }
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, currentLocation);
        remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_graph_3x3_widget_last_update, lastUpdate);
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews, int widgetId) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getWidgetBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        remoteViews.setInt(R.id.widget_ext_loc_forecast_graph_3x3_widget_root, "setBackgroundColor", backgroundColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_widget_temperature, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_widget_description, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_widget_description, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_widget_second_temperature, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_1_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_1_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_2_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_2_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_3_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_3_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_4_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_4_widget_temperatures, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_5_widget_day, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_forecast_graph_3x3_forecast_5_widget_temperatures, textColorId);
        remoteViews.setInt(R.id.widget_ext_loc_forecast_graph_3x3_header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);

        appendLog(context, TAG, "setWidgetTheme:end");
    }

    public static int getNumberOfCurrentWeatherDetails() {
        return MAX_CURRENT_WEATHER_DETAILS;
    }

    public static String getDefaultCurrentWeatherDetails() {
        return DEFAULT_CURRENT_WEATHER_DETAILS;
    }

    @Override
    protected void sendWeatherUpdate(Context context, int widgetId) {
        super.sendWeatherUpdate(context, widgetId);
        if (currentLocation == null) {
            appendLog(context,
                    TAG,
                    "currentLocation is null");
            return;
        }
        if (currentLocation.getOrderId() != 0) {
            Intent intentToCheckWeather = new Intent(context, ForecastWeatherService.class);
            intentToCheckWeather.putExtra("locationId", currentLocation.getId());
            intentToCheckWeather.putExtra("forceUpdate", true);
            startServiceWithCheck(context, intentToCheckWeather);
        }
    }

    @Override
    ArrayList<String> getEnabledActionPlaces() {
        ArrayList<String> enabledWidgetActions = new ArrayList();
        enabledWidgetActions.add("action_city");
        enabledWidgetActions.add("action_current_weather_icon");
        enabledWidgetActions.add("action_forecast");
        return enabledWidgetActions;
    }

    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_ext_loc_forecast_graph_3x3;
    }

    @Override
    protected Class<?> getWidgetClass() {
        return ExtLocationWithForecastGraphWidgetProvider.class;
    }

    @Override
    protected String getWidgetName() {
        return WIDGET_NAME;
    }
}
