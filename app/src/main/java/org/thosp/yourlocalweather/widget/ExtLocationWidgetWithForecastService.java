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

public class ExtLocationWidgetWithForecastService extends IntentService {

    private static final String TAG = "ExtLocationWidgetWithForecastService";

    public ExtLocationWidgetWithForecastService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog(this, TAG, "updateWidgetstart");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, ExtLocationWithForecastWidgetProvider.class);

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

            CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

            if (weatherRecord == null) {
                continue;
            }

            Weather weather = weatherRecord.getWeather();

            RemoteViews remoteViews = new RemoteViews(this.getPackageName(),
                    R.layout.widget_ext_loc_forecast_3x3);

            ExtLocationWithForecastWidgetProvider.setWidgetTheme(this, remoteViews, appWidgetId);
            ExtLocationWithForecastWidgetProvider.setWidgetIntents(this, remoteViews, ExtLocationWithForecastWidgetProvider.class, appWidgetId);

            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_3x3_widget_city, Utils.getCityAndCountry(this, currentLocation.getOrderId()));
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_3x3_widget_temperature, TemperatureUtil.getTemperatureWithUnit(
                    this,
                    weather,
                    currentLocation.getLatitude(),
                    weatherRecord.getLastUpdatedTime(),
                    currentLocation.getLocale()));
            String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                    this,
                    weather,
                    currentLocation.getLatitude(),
                    weatherRecord.getLastUpdatedTime(),
                    currentLocation.getLocale());
            if (secondTemperature != null) {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_3x3_widget_second_temperature, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_3x3_widget_second_temperature, secondTemperature);
            } else {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_3x3_widget_second_temperature, View.GONE);
            }
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_3x3_widget_description,
                                        Utils.getWeatherDescription(this,
                                                                    currentLocation.getLocaleAbbrev(),
                                                                    weather));
            WidgetUtils.setWind(getBaseContext(),
                                remoteViews,
                                weather.getWindSpeed(),
                                weather.getWindDirection(),
                                currentLocation.getLocale(),
                    R.id.widget_ext_loc_forecast_3x3_widget_wind,
                    R.id.widget_ext_loc_forecast_3x3_widget_wind_icon);
            WidgetUtils.setHumidity(getBaseContext(), remoteViews, weather.getHumidity(),
                    R.id.widget_ext_loc_forecast_3x3_widget_humidity, R.id.widget_ext_loc_forecast_3x3_widget_humidity_icon);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(1000 * weather.getSunrise());
            WidgetUtils.setSunrise(
                    getBaseContext(),
                    remoteViews,
                    AppPreference.getLocalizedTime(this, calendar.getTime(), currentLocation.getLocale()),
                    R.id.widget_ext_loc_forecast_3x3_widget_sunrise,
                    R.id.widget_ext_loc_forecast_3x3_widget_sunrise_icon);
            calendar.setTimeInMillis(1000 * weather.getSunset());
            WidgetUtils.setSunset(getBaseContext(),
                                    remoteViews,
                                    AppPreference.getLocalizedTime(this, calendar.getTime(), currentLocation.getLocale()),
                                    R.id.widget_ext_loc_forecast_3x3_widget_sunset,
                                    R.id.widget_ext_loc_forecast_3x3_widget_sunset_icon);
            Utils.setWeatherIcon(remoteViews, this, weatherRecord,
                    R.id.widget_ext_loc_forecast_3x3_widget_icon);

            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = null;
            try {
                weatherForecastRecord = WidgetUtils.updateWeatherForecast(
                        getBaseContext(),
                        currentLocation.getId(),
                        appWidgetId,
                        remoteViews,
                        R.id.widget_ext_loc_forecast_3x3_forecast_day_1,
                        R.id.widget_ext_loc_forecast_3x3_forecast_1_widget_icon,
                        R.id.widget_ext_loc_forecast_3x3_forecast_1_widget_day,
                        R.id.widget_ext_loc_forecast_3x3_forecast_1_widget_temperatures,
                        R.id.widget_ext_loc_forecast_3x3_forecast_day_2,
                        R.id.widget_ext_loc_forecast_3x3_forecast_2_widget_icon,
                        R.id.widget_ext_loc_forecast_3x3_forecast_2_widget_day,
                        R.id.widget_ext_loc_forecast_3x3_forecast_2_widget_temperatures,
                        R.id.widget_ext_loc_forecast_3x3_forecast_day_3,
                        R.id.widget_ext_loc_forecast_3x3_forecast_3_widget_icon,
                        R.id.widget_ext_loc_forecast_3x3_forecast_3_widget_day,
                        R.id.widget_ext_loc_forecast_3x3_forecast_3_widget_temperatures,
                        R.id.widget_ext_loc_forecast_3x3_forecast_day_4,
                        R.id.widget_ext_loc_forecast_3x3_forecast_4_widget_icon,
                        R.id.widget_ext_loc_forecast_3x3_forecast_4_widget_day,
                        R.id.widget_ext_loc_forecast_3x3_forecast_4_widget_temperatures,
                        R.id.widget_ext_loc_forecast_3x3_forecast_day_5,
                        R.id.widget_ext_loc_forecast_3x3_forecast_5_widget_icon,
                        R.id.widget_ext_loc_forecast_3x3_forecast_5_widget_day,
                        R.id.widget_ext_loc_forecast_3x3_forecast_5_widget_temperatures
                        );
            } catch (Exception e) {
                appendLog(getBaseContext(), TAG, "preLoadWeather:error updating weather forecast", e);
            }
            String lastUpdate = Utils.getLastUpdateTime(this, weatherRecord, weatherForecastRecord, currentLocation);
            remoteViews.setTextViewText(R.id.widget_ext_loc_forecast_3x3_widget_last_update, lastUpdate);
            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(this, TAG, "updateWidgetend");
    }
}
