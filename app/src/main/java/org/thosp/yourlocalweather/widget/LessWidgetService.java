package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.RemoteViews;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
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
        Weather weather = AppPreference.getWeather(this);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this,
                                                          LessWidgetProvider.class);

        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        for (int appWidgetId : widgetIds) {
            RemoteViews remoteViews = new RemoteViews(this.getPackageName(),
                                                      R.layout.widget_less_3x1);
            String weatherIcon = Utils.getStrIcon(this, weather);
            String lastUpdate = Utils.setLastUpdateTime(this, AppPreference
                    .getLastUpdateTimeMillis(this));
            String temperatureScale = Utils.getTemperatureScale(this);
            String temperature = String.format(Locale.getDefault(), "%d",
                    Math.round(weather.temperature.getTemp()));

            remoteViews.setTextViewText(R.id.widget_temperature,
                                        temperature + temperatureScale);
            remoteViews.setTextViewText(R.id.widget_description, Utils.getWeatherDescription(this, weather));
            remoteViews.setTextViewText(R.id.widget_city, Utils.getCityAndCountry(this));
            remoteViews.setTextViewText(R.id.widget_last_update, lastUpdate);
            remoteViews.setImageViewBitmap(R.id.widget_icon,
                                           Utils.createWeatherIcon(this, weatherIcon));
            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(this, TAG, "updateWidgetend");
    }
}
