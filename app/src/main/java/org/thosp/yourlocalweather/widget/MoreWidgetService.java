package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.Locale;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class MoreWidgetService extends IntentService {

    private static final String TAG = "UpdateMoreWidgetService";
    
    public MoreWidgetService() {
        super(TAG);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        appendLog(this, TAG, "updateWidgetstart");
        Weather weather = AppPreference.getWeather(this);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, MoreWidgetProvider.class);

        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        for (int appWidgetId : widgetIds) {
            String temperatureScale = Utils.getTemperatureScale(this);

            String temperature = String.format(Locale.getDefault(), "%d",
                    Math.round(weather.temperature.getTemp()));

            String lastUpdate = Utils.setLastUpdateTime(this, AppPreference
                    .getLastUpdateTimeMillis(this));

            RemoteViews remoteViews = new RemoteViews(this.getPackageName(),
                                                      R.layout.widget_more_3x3);
            remoteViews.setTextViewText(R.id.widget_city, Utils.getCityAndCountry(this));
            remoteViews.setTextViewText(R.id.widget_temperature, temperature + temperatureScale);
            if(!AppPreference.hideDescription(this))
                remoteViews.setTextViewText(R.id.widget_description, Utils.getWeatherDescription(this, weather));
            else remoteViews.setTextViewText(R.id.widget_description, " ");
            WidgetUtils.setWind(getBaseContext(), remoteViews, weather.wind.getSpeed());
            WidgetUtils.setHumidity(getBaseContext(), remoteViews, weather.currentCondition.getHumidity());
            WidgetUtils.setPressure(getBaseContext(), remoteViews, weather.currentCondition.getPressure());
            WidgetUtils.setClouds(getBaseContext(), remoteViews, weather.cloud.getClouds());
            Utils.setWeatherIcon(remoteViews, this);
            remoteViews.setTextViewText(R.id.widget_last_update, lastUpdate);

            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(getBaseContext(), TAG, "updateWidgetend");
    }
}
