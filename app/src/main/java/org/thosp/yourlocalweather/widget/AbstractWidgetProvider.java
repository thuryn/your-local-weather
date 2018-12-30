package org.thosp.yourlocalweather.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.thosp.yourlocalweather.WidgetSettingsDialogue;
import org.thosp.yourlocalweather.GraphsActivity;
import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.WeatherForecastActivity;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.service.CurrentWeatherService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PermissionUtil;

import java.util.HashSet;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public abstract class AbstractWidgetProvider extends AppWidgetProvider {

    private static String TAG = "AbstractWidgetProvider";

    protected Location currentLocation;
    volatile boolean servicesStarted = false;

    @Override
    public void onEnabled(Context context) {
        appendLog(context, TAG, "onEnabled:start");
        super.onEnabled(context);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        if (PermissionUtil.areAllPermissionsGranted(context)) {
            Toast.makeText(context,
                    R.string.permissions_not_granted,
                    Toast.LENGTH_LONG).show();
        }
        ComponentName widgetComponent = new ComponentName(context, getWidgetClass());

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        if (widgetIds.length == 0) {
            return;
        }
        int currentWidget = widgetIds[0];
        Long locationId = widgetSettingsDbHelper.getParamLong(currentWidget, "locationId");
        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }
        if (!currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        appendLog(context, TAG, "onEnabled:end");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        appendLog(context, TAG, "intent:", intent, ", widget:", getWidgetClass());
        super.onReceive(context, intent);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);

        Integer widgetId = null;
        ComponentName widgetComponent = new ComponentName(context, getWidgetClass());

        if (intent.hasExtra("widgetId")) {
            widgetId = intent.getIntExtra("widgetId", 0);
            if (widgetId == 0) {
                widgetId = null;
            }
        }
        if (widgetId == null) {
            int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
            if (widgetIds.length == 0) {
                return;
            }
            widgetId = widgetIds[0];
        }
        Long locationId = widgetSettingsDbHelper.getParamLong(widgetId, "locationId");
        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }
        switch (intent.getAction()) {
            case "org.thosp.yourlocalweather.action.WEATHER_UPDATE_RESULT":
            case "android.appwidget.action.APPWIDGET_UPDATE":
                if (!servicesStarted) {
                    onEnabled(context);
                    servicesStarted = true;
                }
                onUpdate(context, widgetManager, new int[] {widgetId});
                break;
            case Constants.ACTION_FORCED_APPWIDGET_UPDATE:
                if (!WidgetRefreshIconService.isRotationActive) {
                    sendWeatherUpdate(context);
                }
                onUpdate(context, widgetManager, new int[]{ widgetId});
                break;
            case Intent.ACTION_LOCALE_CHANGED:
            case Constants.ACTION_APPWIDGET_THEME_CHANGED:
            case Constants.ACTION_APPWIDGET_SETTINGS_SHOW_CONTROLS:
                refreshWidgetValues(context);
                break;
            case Constants.ACTION_APPWIDGET_SETTINGS_OPENED:
                openWidgetSettings(context, widgetId, intent.getStringExtra("settings_option"));
                break;
            case Constants.ACTION_APPWIDGET_UPDATE_PERIOD_CHANGED:
                onEnabled(context);
                break;
            case Constants.ACTION_APPWIDGET_CHANGE_LOCATION:
                changeLocation(widgetId, locationsDbHelper, widgetSettingsDbHelper);
                onUpdate(context, widgetManager, new int[]{ widgetId});
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        appendLog(context, TAG, "onUpdate:start");
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    getWidgetLayout());

            if (AppPreference.isShowControls(context)) {
                remoteViews.setViewVisibility(R.id.widget_weather_graph_1x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_3x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_weather_forecast_1x3_settings_layout, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.widget_weather_graph_1x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_3x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_weather_forecast_1x3_settings_layout, View.GONE);
            }

            if (ExtLocationWidgetProvider.class.equals(getWidgetClass())) {
                ExtLocationWidgetProvider.setWidgetTheme(context, remoteViews);
            } else if (MoreWidgetProvider.class.equals(getWidgetClass())) {
                MoreWidgetProvider.setWidgetTheme(context, remoteViews);
            } else if (LessWidgetProvider.class.equals(getWidgetClass())) {
                LessWidgetProvider.setWidgetTheme(context, remoteViews);
            } else if (ExtLocationWithForecastWidgetProvider.class.equals(getWidgetClass())) {
                ExtLocationWithForecastWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
            } else if (WeatherForecastWidgetProvider.class.equals(getWidgetClass())) {
                WeatherForecastWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
            } else if (ExtLocationWithGraphWidgetProvider.class.equals(getWidgetClass())) {
                ExtLocationWithGraphWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
            } else if (WeatherGraphWidgetProvider.class.equals(getWidgetClass())) {
                WeatherGraphWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
            }
            setWidgetIntents(context, remoteViews, getWidgetClass(), appWidgetId);
            preLoadWeather(context, remoteViews, appWidgetId);

            appWidgetManager.updateAppWidget(new ComponentName(context, getWidgetClass()), remoteViews);
        }
        appendLog(context, TAG, "onUpdate:end");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        for (int widgetId: appWidgetIds) {
            widgetSettingsDbHelper.deleteRecordFromTable(widgetId);
        }
    }

    protected void refreshWidgetValues(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, getWidgetClass());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    protected void sendWeatherUpdate(Context context) {
        if (currentLocation == null) {
            appendLog(context,
                    TAG,
                    "currentLocation is null");
            return;
        }
        if ((currentLocation.getOrderId() == 0) && currentLocation.isEnabled()) {
            Intent startLocationUpdateIntent = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
            startLocationUpdateIntent.setPackage("org.thosp.yourlocalweather");
            startLocationUpdateIntent.putExtra("locationId", currentLocation.getId());
            startLocationUpdateIntent.putExtra("forceUpdate", true);
            startServiceWithCheck(context, startLocationUpdateIntent);
            appendLog(context, TAG, "send intent START_LOCATION_UPDATE:", startLocationUpdateIntent);
        } else if (currentLocation.getOrderId() != 0) {
            Intent intentToCheckWeather = new Intent(context, CurrentWeatherService.class);
            intentToCheckWeather.putExtra("locationId", currentLocation.getId());
            intentToCheckWeather.putExtra("forceUpdate", true);
            intentToCheckWeather.putExtra("updateWeatherOnly", true);
            startServiceWithCheck(context, intentToCheckWeather);
        }
    }

    public static void setWidgetIntents(Context context, RemoteViews remoteViews, Class<?>  widgetClass, int widgetId) {
        appendLog(context, TAG, "setWidgetIntents:widgetid:", widgetId);
        Intent intentRefreshService = new Intent(context, widgetClass);
        intentRefreshService.setAction(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
        intentRefreshService.setPackage("org.thosp.yourlocalweather");
        intentRefreshService.putExtra("widgetId", widgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                intentRefreshService, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_3x3_widget_button_refresh, pendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_widget_button_refresh, pendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_widget_button_refresh, pendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_less_3x1_widget_button_refresh, pendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_more_3x3_widget_button_refresh, pendingIntent);

        Intent intentStartActivity = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0,
                intentStartActivity, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_3x3_widget_icon, pendingIntent2);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_widget_icon, pendingIntent2);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_widget_icon, pendingIntent2);
        remoteViews.setOnClickPendingIntent(R.id.widget_less_3x1_widget_icon, pendingIntent2);
        remoteViews.setOnClickPendingIntent(R.id.widget_more_3x3_widget_icon, pendingIntent2);

        Intent intentStartGraphsActivity = new Intent(context, GraphsActivity.class);
        PendingIntent pendingIntentGraphsActivity = PendingIntent.getActivity(context, 0,
                intentStartGraphsActivity, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_forecast_graph, pendingIntentGraphsActivity);
        remoteViews.setOnClickPendingIntent(R.id.widget_weather_graph_1x3_forecast_graph, pendingIntentGraphsActivity);

        Intent intentWeatherForecastActivity = new Intent(context, WeatherForecastActivity.class);
        PendingIntent pendingIntent3 = PendingIntent.getActivity(context, 0,
                intentWeatherForecastActivity, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_forecast_layout, pendingIntent3);
        remoteViews.setOnClickPendingIntent(R.id.widget_weather_forecast_1x3_forecast_layout, pendingIntent3);

        Intent intentSwitchLocality = new Intent(context, widgetClass);
        intentSwitchLocality.setAction(Constants.ACTION_APPWIDGET_CHANGE_LOCATION);
        intentSwitchLocality.putExtra("widgetId", widgetId);
        intentSwitchLocality.setPackage("org.thosp.yourlocalweather");
        PendingIntent pendingSwitchLocalityIntent = PendingIntent.getBroadcast(context, 0,
                intentSwitchLocality, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_3x3_widget_city, pendingSwitchLocalityIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_widget_city, pendingSwitchLocalityIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_widget_city, pendingSwitchLocalityIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_less_3x1_widget_city, pendingSwitchLocalityIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_more_3x3_widget_city, pendingSwitchLocalityIntent);
    }

    protected abstract void preLoadWeather(Context context, RemoteViews remoteViews, int widgetId);

    protected abstract Class<?> getWidgetClass();

    protected abstract String getWidgetName();

    protected abstract int getWidgetLayout();

    private void startBackgroundService(Context context, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 10,
                pendingIntent);
    }

    private void changeLocation(int widgetId,
                                LocationsDbHelper locationsDbHelper,
                                WidgetSettingsDbHelper widgetSettingsDbHelper) {
        if (currentLocation == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
            if (currentLocation == null) {
                return;
            }
        }
        int newOrderId = 1 + currentLocation.getOrderId();
        currentLocation = locationsDbHelper.getLocationByOrderId(newOrderId);
        if (currentLocation == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        }
        if (currentLocation != null) {
            widgetSettingsDbHelper.saveParamLong(widgetId, "locationId", currentLocation.getId());
        }
    }

    protected void startServiceWithCheck(Context context, Intent intent) {
        try {
            context.startService(intent);
        } catch (IllegalStateException ise) {
            intent.putExtra("isInteractive", false);
            PendingIntent pendingIntent = PendingIntent.getService(context,
                    0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10,
                    pendingIntent);
        }
    }

    private void openWidgetSettings(Context context, int widgetId, String settingsName) {
        Intent popUpIntent = new Intent(context, WidgetSettingsDialogue.class);
        popUpIntent.putExtra("widgetId", widgetId);
        popUpIntent.putExtra("settings_option", settingsName);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(popUpIntent);
    }
}
