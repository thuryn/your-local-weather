package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class WeatherForecastWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WeatherForecastWidgetProvider";

    private static final String WIDGET_NAME = "WEATHER_FORECAST_WIDGET";

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(context);

        Long locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId");

        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if ((currentLocation != null) && !currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }

        if (currentLocation == null) {
            return;
        }

        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        final WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());

        WeatherForecastWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
        WeatherForecastWidgetProvider.setWidgetIntents(context, remoteViews, WeatherForecastWidgetProvider.class, appWidgetId);

        Long daysCount = widgetSettingsDbHelper.getParamLong(appWidgetId, "forecastDaysCount");
        Boolean hoursForecast = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "hoursForecast");

        boolean fontBasedIcons = "weather_icon_set_fontbased".equals(AppPreference.getIconSet(context));

        ContextCompat.getMainExecutor(context).execute(()  -> {
                    remoteViews.setTextViewText(R.id.widget_weather_forecast_1x3_widget_city, Utils.getCityAndCountry(context, currentLocation));
                });
        Boolean forecastDayAbbrev = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "forecast_day_abbrev");

        Map<Long, String> localizedHourMap = new HashMap<>();
        Map<Long, String> temperaturesMap = new HashMap<>();
        for (DetailedWeatherForecast detailedWeatherForecast: weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {

            long forecastTime = detailedWeatherForecast.getDateTime();
            Calendar forecastCalendar = Calendar.getInstance();
            forecastCalendar.setTimeInMillis(forecastTime * 1000);
            Date forecastCalendarTime = forecastCalendar.getTime();
            String localizedHour = AppPreference.getLocalizedHour(context, forecastCalendarTime, currentLocation.getLocale());
            localizedHourMap.put(forecastTime, localizedHour);

            temperaturesMap.put(forecastTime, Math.round(TemperatureUtil.getTemperatureInPreferredUnit(context, temperatureUnitFromPreferences, detailedWeatherForecast.getTemperatureMin())) +
                    "/" +
                    Math.round(TemperatureUtil.getTemperatureInPreferredUnit(context, temperatureUnitFromPreferences, detailedWeatherForecast.getTemperatureMax())) +
                    TemperatureUtil.getTemperatureUnit(context, temperatureUnitFromPreferences));
        }

        ContextCompat.getMainExecutor(context).execute(()  -> {
            Long dayCountForForecast = (daysCount != null) ? daysCount : 5L;
            try {
                WidgetUtils.updateWeatherForecast(
                        context,
                        currentLocation,
                        weatherForecastRecord,
                        appWidgetId,
                        dayCountForForecast,
                        hoursForecast,
                        forecastDayAbbrev,
                        fontBasedIcons,
                        localizedHourMap,
                        temperaturesMap,
                        temperatureUnitFromPreferences,
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
                        R.id.widget_weather_forecast_1x3_forecast_5_widget_temperatures,
                        R.id.widget_weather_forecast_1x3_forecast_day_6,
                        R.id.widget_weather_forecast_1x3_forecast_6_widget_icon,
                        R.id.widget_weather_forecast_1x3_forecast_6_widget_day,
                        R.id.widget_weather_forecast_1x3_forecast_6_widget_temperatures,
                        R.id.widget_weather_forecast_1x3_forecast_day_7,
                        R.id.widget_weather_forecast_1x3_forecast_7_widget_icon,
                        R.id.widget_weather_forecast_1x3_forecast_7_widget_day,
                        R.id.widget_weather_forecast_1x3_forecast_7_widget_temperatures,
                        R.id.widget_weather_forecast_1x3_forecast_day_8,
                        R.id.widget_weather_forecast_1x3_forecast_8_widget_icon,
                        R.id.widget_weather_forecast_1x3_forecast_8_widget_day,
                        R.id.widget_weather_forecast_1x3_forecast_8_widget_temperatures);
            } catch (Exception e) {
                appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
            }
        });
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews, int widgetId) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getWidgetTextColor(context);
        int backgroundColorId = AppPreference.getWidgetBackgroundColor(context);

        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        Boolean showLocation = widgetSettingsDbHelper.getParamBoolean(widgetId, "showLocation");

        ContextCompat.getMainExecutor(context).execute(()  -> {
            boolean showLocationParam = showLocation != null && showLocation;
            if (showLocationParam) {
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
            remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_6_widget_day, textColorId);
            remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_6_widget_temperatures, textColorId);
            remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_7_widget_day, textColorId);
            remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_7_widget_temperatures, textColorId);
            remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_8_widget_day, textColorId);
            remoteViews.setTextColor(R.id.widget_weather_forecast_1x3_forecast_8_widget_temperatures, textColorId);
        });

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
