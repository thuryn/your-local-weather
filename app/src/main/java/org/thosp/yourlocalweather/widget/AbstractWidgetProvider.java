package org.thosp.yourlocalweather.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.service.CurrentWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.AppWidgetProviderAlarm;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PermissionUtil;

import java.util.Calendar;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public abstract class AbstractWidgetProvider extends AppWidgetProvider {

    private static String TAG = "AbstractWidgetProvider";

    volatile long lastUpdatedWeather = 0;

    volatile boolean servicesStarted = false;

    @Override
    public void onEnabled(Context context) {
        appendLog(context, TAG, "onEnabled:start");
        super.onEnabled(context);
        if (PermissionUtil.areAllPermissionsGranted(context)) {
            Toast.makeText(context,
                    R.string.permissions_not_granted,
                    Toast.LENGTH_LONG).show();
        }
        AppWidgetProviderAlarm appWidgetProviderAlarm =
                new AppWidgetProviderAlarm(context, getWidgetClass());
        appWidgetProviderAlarm.cancelAlarm();
        appWidgetProviderAlarm.setAlarm();
        appendLog(context, TAG, "onEnabled:end");
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        appendLog(context, TAG, "intent:" + intent + ", widget:" + getWidgetClass());
        switch (intent.getAction()) {
            case "org.thosp.yourlocalweather.action.WEATHER_UPDATE_RESULT":
            case "android.appwidget.action.APPWIDGET_UPDATE":
                super.onReceive(context, intent);
                if (!servicesStarted) {
                    onEnabled(context);
                    servicesStarted = true;
                }

                AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
                ComponentName widgetComponent = new ComponentName(context, getWidgetClass());

                int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
                onUpdate(context, widgetManager, widgetIds);
                break;
            case Constants.ACTION_FORCED_APPWIDGET_UPDATE:
                if (!WidgetRefreshIconService.isRotationActive) {
                    if (AppPreference.isUpdateLocationEnabled(context)) {
                        Intent startLocationUpdateIntent = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
                        startLocationUpdateIntent.setPackage("org.thosp.yourlocalweather");
                        startLocationUpdateIntent.putExtra("updateSource", getWidgetName());
                        context.startService(startLocationUpdateIntent);
                        appendLog(context, TAG, "send intent START_LOCATION_UPDATE:" + startLocationUpdateIntent);
                    } else {
                        Intent intentToCheckWeather = new Intent(context, CurrentWeatherService.class);
                        intentToCheckWeather.putExtra("updateSource", getWidgetName());
                        context.startService(intentToCheckWeather);
                    }
                }
                break;
            case Intent.ACTION_SCREEN_ON:
                updateWather(context);
            case Intent.ACTION_LOCALE_CHANGED:
            case Constants.ACTION_APPWIDGET_THEME_CHANGED:
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName componentName = new ComponentName(context, getWidgetClass());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
                onUpdate(context, appWidgetManager, appWidgetIds);
                break;
            case Constants.ACTION_APPWIDGET_UPDATE_PERIOD_CHANGED:
                onEnabled(context);
                break;
            default:
                super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        appendLog(context, TAG, "onUpdate:start");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    getWidgetLayout());

            if (ExtLocationWidgetProvider.class.equals(getWidgetClass())) {
                ExtLocationWidgetProvider.setWidgetTheme(context, remoteViews);
            } else if (MoreWidgetProvider.class.equals(getWidgetClass())) {
                MoreWidgetProvider.setWidgetTheme(context, remoteViews);
            } else if (LessWidgetProvider.class.equals(getWidgetClass())) {
                LessWidgetProvider.setWidgetTheme(context, remoteViews);
            }
            setWidgetIntents(context, remoteViews, getWidgetClass());
            preLoadWeather(context, remoteViews);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        appendLog(context, TAG, "onUpdate:end");
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        AppWidgetProviderAlarm appWidgetProviderAlarm =
                new AppWidgetProviderAlarm(context, getWidgetClass());
        appWidgetProviderAlarm.cancelAlarm();
    }

    private void updateWather(Context context) {
        long now = Calendar.getInstance().getTimeInMillis();
        appendLog(context, TAG, "SCREEN_ON called, lastUpdate=" + lastUpdatedWeather + ", now=" + now);
        if (now < (lastUpdatedWeather + 900000)) {
            return;
        }
        lastUpdatedWeather = now;
        if(AppPreference.isUpdateLocationEnabled(context)) {
            Intent startLocationUpdateIntent = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
            startLocationUpdateIntent.setPackage("org.thosp.yourlocalweather");
            startLocationUpdateIntent.putExtra("updateSource", getWidgetName());
            context.startService(startLocationUpdateIntent);
        } else {
            context.startService(new Intent(context, getWidgetClass()));
        }
    }

    public static void setWidgetIntents(Context context, RemoteViews remoteViews, Class<?>  widgetClass) {
        Intent intentRefreshService = new Intent(context, widgetClass);
        intentRefreshService.setAction(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                intentRefreshService, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_button_refresh, pendingIntent);

        Intent intentStartActivity = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0,
                intentStartActivity, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_root, pendingIntent2);
    }

    protected abstract void preLoadWeather(Context context, RemoteViews remoteViews);

    protected abstract Class<?> getWidgetClass();

    protected abstract String getWidgetName();

    protected abstract int getWidgetLayout();
}
