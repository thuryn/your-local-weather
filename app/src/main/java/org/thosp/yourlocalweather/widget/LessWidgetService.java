package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LessWidgetService extends IntentService {

    private static final String TAG = "UpdateLessWidgetService:";
    
    public LessWidgetService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog(this, TAG, "updateWidgetstart");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(this);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this,
                                                          LessWidgetProvider.class);

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
                return;
            }

            Weather weather = weatherRecord.getWeather();

            RemoteViews remoteViews = new RemoteViews(this.getPackageName(),
                                                      R.layout.widget_less_3x1);

            LessWidgetProvider.setWidgetTheme(this, remoteViews);
            LessWidgetProvider.setWidgetIntents(this, remoteViews, LessWidgetProvider.class, appWidgetId);

            remoteViews.setTextViewText(R.id.widget_less_3x1_widget_temperature, TemperatureUtil.getTemperatureWithUnit(
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
                remoteViews.setViewVisibility(R.id.widget_less_3x1_widget_second_temperature, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_less_3x1_widget_second_temperature, secondTemperature);
            } else {
                remoteViews.setViewVisibility(R.id.widget_less_3x1_widget_second_temperature, View.GONE);
            }
            remoteViews.setTextViewText(R.id.widget_less_3x1_widget_description,
                                        Utils.getWeatherDescription(this,
                                                                    currentLocation.getLocaleAbbrev(),
                                                                    weather));
            remoteViews.setTextViewText(R.id.widget_less_3x1_widget_city, Utils.getCityAndCountry(this, currentLocation.getOrderId()));
            String lastUpdate = Utils.getLastUpdateTime(this, weatherRecord, currentLocation);
            remoteViews.setTextViewText(R.id.widget_less_3x1_widget_last_update, lastUpdate);
            Utils.setWeatherIcon(remoteViews, this, weatherRecord, R.id.widget_less_3x1_widget_icon);
            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(this, TAG, "updateWidgetend");
    }
}
