package org.thosp.yourlocalweather.widget;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.WidgetSettingsDialogue;
import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.service.WeatherRequestDataHolder;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.PermissionUtil;

import java.util.ArrayList;
import static org.thosp.yourlocalweather.widget.WidgetSettingName.*;

public abstract class AbstractWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "AbstractWidgetProvider";

    protected Location currentLocation;
    volatile boolean servicesStarted = false;

    @Override
    public void onEnabled(Context context) {
        appendLog(context, TAG, "onEnabled:start");
        super.onEnabled(context);
        YourLocalWeather.executor.submit(() -> {
                    LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
                    WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
                    if (PermissionUtil.noPermissionGranted(context)) {
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
                    if (currentLocation == null) {
                        return;
                    }
                    if (!currentLocation.isEnabled()) {
                        currentLocation = locationsDbHelper.getLocationByOrderId(1);
                    }
                });
        appendLog(context, TAG, "onEnabled:end");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Bundle extras = intent.getExtras();
        YourLocalWeather.executor.submit(() -> {
            appendLog(context, TAG, "intent:", intent, ", widget:", getWidgetClass());
            if (extras != null) {
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                appendLog(context, TAG, "EXTRA_APPWIDGET_ID:" + appWidgetId);
            }

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
                    appendLog(context, TAG, "widgetIds are zero in length");
                    return;
                }
                for (int widgetIditer: widgetIds) {
                    performActionOnReceiveForWidget(context, intent, widgetIditer);
                }
                return;
            }
            performActionOnReceiveForWidget(context, intent, widgetId);
        });
    }

    private void performActionOnReceiveForWidget(Context context, Intent intent, int widgetId) {
        try {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
            updateCurrentLocation(context, widgetId);
            switch (intent.getAction()) {
                case "org.thosp.yourlocalweather.action.WEATHER_UPDATE_RESULT":
                case "android.appwidget.action.APPWIDGET_UPDATE":
                    if (!servicesStarted) {
                        onEnabled(context);
                        servicesStarted = true;
                    }
                    onUpdate(context, widgetManager, new int[]{widgetId});
                    break;
                case Intent.ACTION_LOCALE_CHANGED:
                case Constants.ACTION_APPWIDGET_THEME_CHANGED:
                case Constants.ACTION_APPWIDGET_SETTINGS_SHOW_CONTROLS:
                    refreshWidgetValues(context);
                    break;
                case Constants.ACTION_APPWIDGET_UPDATE_PERIOD_CHANGED:
                    onEnabled(context);
                    break;
                case Constants.ACTION_APPWIDGET_CHANGE_SETTINGS:
                    onUpdate(context, widgetManager, new int[]{widgetId});
                    break;
            }

            if (intent.getAction().startsWith(Constants.ACTION_APPWIDGET_SETTINGS_OPENED)) {
                widgetId = intent.getIntExtra("widgetId", 0);
                openWidgetSettings(context, widgetId, intent.getStringExtra("settingName"));
            } else if (intent.getAction().startsWith(Constants.ACTION_APPWIDGET_START_ACTIVITY)) {
                AppPreference.setCurrentLocationId(context, currentLocation);
                Long widgetActionId = intent.getLongExtra("widgetAction", 1);
                Class activityClass = WidgetActions.getById(widgetActionId, "action_current_weather_icon").getActivityClass();
                Intent activityIntent = new Intent(context, activityClass);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(activityIntent);
            } else if (intent.getAction().startsWith(Constants.ACTION_APPWIDGET_CHANGE_LOCATION)) {
                WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
                changeLocation(widgetId, locationsDbHelper, widgetSettingsDbHelper);
                GraphUtils.invalidateGraph();
                onUpdate(context, widgetManager, new int[]{widgetId});
            } else if (intent.getAction().startsWith(Constants.ACTION_FORCED_APPWIDGET_UPDATE)) {
                sendWeatherUpdate(context, widgetId);
            }
        } catch (Exception e) {
            appendLog(context, TAG, e);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        YourLocalWeather.executor.submit(() -> {
            try {
                appendLog(context, TAG, "onUpdate:start");
                ComponentName componentName = new ComponentName(context, getWidgetClass());
                int[] appWidgetIdsForWidget = appWidgetManager.getAppWidgetIds(componentName);

                for (int appWidgetId : appWidgetIds) {

                    updateCurrentLocation(context, appWidgetId);
                    boolean found = false;
                    for (int widgetIdToSearch : appWidgetIdsForWidget) {
                        if (widgetIdToSearch == appWidgetId) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }

                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                            getWidgetLayout());

                    setWidgetIntents(context, remoteViews, getWidgetClass(), appWidgetId);
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
                    } else if (ExtLocationWithForecastGraphWidgetProvider.class.equals(getWidgetClass())) {
                        ExtLocationWithForecastGraphWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
                    }
                    preLoadWeather(context, remoteViews, appWidgetId);
                    ContextCompat.getMainExecutor(context).execute(()  -> {
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    });
                }
            } catch (Exception e) {
                appendLog(context, TAG, e.getMessage(), e);
            }
            appendLog(context, TAG, "onUpdate:end");
        });
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

    protected void sendWeatherUpdate(Context context, int widgetId) {
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        Long currentLocationId = widgetSettingsDbHelper.getParamLong(widgetId, "locationId");
        if (currentLocationId == null) {
            appendLog(context,
                    TAG,
                    "currentLocation is null");
            return;
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        currentLocation = locationsDbHelper.getLocationById(currentLocationId);
        if (currentLocation == null) {
            appendLog(context,
                    TAG,
                    "currentLocation is null");
            return;
        }
        if ((currentLocation.getOrderId() == 0) && currentLocation.isEnabled()) {
            startLocationAndWeatherUpdate(context);
        } else if (currentLocation.getOrderId() != 0) {
            startWeatherUpdate(context, currentLocation);
        }
    }

    public static void setWidgetIntents(Context context, RemoteViews remoteViews, Class<?>  widgetClass, int widgetId) {
        appendLog(context, TAG, "setWidgetIntents:widgetid:", widgetId);
        boolean showControls = AppPreference.isShowControls(context);
        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        WidgetActions mainIconAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_current_weather_icon"), "action_current_weather_icon");
        WidgetActions graphAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_graph"), "action_graph");
        WidgetActions forecastAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_forecast"), "action_forecast");
        WidgetActions cityAction = WidgetActions.getById(widgetSettingsDbHelper.getParamLong(widgetId, "action_city"), "action_city");

        ContextCompat.getMainExecutor(context).execute(()  -> {
            if (showControls) {
                remoteViews.setViewVisibility(R.id.widget_weather_graph_1x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_3x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_weather_forecast_1x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_less_3x1_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_more_3x3_settings_layout, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_graph_3x3_settings_layout, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.widget_weather_graph_1x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_3x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_weather_forecast_1x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_less_3x1_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_more_3x3_settings_layout, View.GONE);
                remoteViews.setViewVisibility(R.id.widget_ext_loc_forecast_graph_3x3_settings_layout, View.GONE);
            }

            Intent intentRefreshService = new Intent(context, widgetClass);
            intentRefreshService.setAction(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
            intentRefreshService.setPackage("org.thosp.yourlocalweather");
            intentRefreshService.putExtra("widgetId", widgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId,
                    intentRefreshService, PendingIntent.FLAG_IMMUTABLE);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_3x3_widget_last_update, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_widget_last_update, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_widget_last_update, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_less_3x1_widget_last_update, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_more_3x3_widget_last_update, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_graph_3x3_widget_last_update, pendingIntent);

            PendingIntent pendingIntentMainIconAction = getActionIntent(context, mainIconAction, widgetClass, widgetId);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_3x3_widget_icon, pendingIntentMainIconAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_widget_icon, pendingIntentMainIconAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_widget_icon, pendingIntentMainIconAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_less_3x1_widget_icon, pendingIntentMainIconAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_more_3x3_widget_icon, pendingIntentMainIconAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_graph_3x3_widget_icon, pendingIntentMainIconAction);

            PendingIntent pendingIntentGraphAction = getActionIntent(context, graphAction, widgetClass, widgetId);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_graph_3x3_forecast_graph, pendingIntentGraphAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_weather_graph_1x3_forecast_graph, pendingIntentGraphAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_graph_3x3_forecast_graph, pendingIntentGraphAction);

            PendingIntent pendingIntentForecastAction = getActionIntent(context, forecastAction, widgetClass, widgetId);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_3x3_forecast_layout, pendingIntentForecastAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_weather_forecast_1x3_forecast_layout, pendingIntentForecastAction);
            remoteViews.setOnClickPendingIntent(R.id.widget_ext_loc_forecast_graph_3x3_forecast_layout, pendingIntentForecastAction);

            Integer cityViewId = getCityViewId(widgetClass);
            if (cityViewId != null) {
                PendingIntent pendingIntentCityAction = getActionIntent(context, cityAction, widgetClass, widgetId);
                remoteViews.setOnClickPendingIntent(getCityViewId(widgetClass), pendingIntentCityAction);
            }

            setSettingButtonAction(context, widgetId, FORECAST_SETTINGS, R.id.widget_ext_loc_forecast_3x3_button_days_setting, remoteViews, ExtLocationWithForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, FORECAST_SETTINGS, R.id.widget_weather_forecast_1x3_button_days_setting, remoteViews, WeatherForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, FORECAST_SETTINGS, R.id.widget_ext_loc_forecast_graph_3x3_button_days_setting, remoteViews, ExtLocationWithForecastGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, GRAPH_SETTING, R.id.widget_ext_loc_graph_3x3_button_graph_setting, remoteViews, ExtLocationWithGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, GRAPH_SETTING, R.id.widget_weather_graph_1x3_button_graph_setting, remoteViews, WeatherGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, GRAPH_SETTING, R.id.widget_ext_loc_forecast_graph_3x3_button_graph_setting, remoteViews, ExtLocationWithForecastGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, DETAILS_SETTING, R.id.widget_ext_loc_3x3_button_details_setting, remoteViews, ExtLocationWidgetProvider.class);
            setSettingButtonAction(context, widgetId, DETAILS_SETTING, R.id.widget_ext_loc_forecast_3x3_button_details_setting, remoteViews, ExtLocationWithForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, DETAILS_SETTING, R.id.widget_ext_loc_forecast_graph_3x3_button_details_setting, remoteViews, ExtLocationWithForecastGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, DETAILS_SETTING, R.id.widget_ext_loc_graph_3x3_button_details_setting, remoteViews, ExtLocationWithGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, DETAILS_SETTING, R.id.widget_more_3x3_button_details_setting, remoteViews, MoreWidgetProvider.class);

            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_ext_loc_forecast_3x3_button_location_setting, remoteViews, ExtLocationWithForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_weather_forecast_1x3_button_location_setting, remoteViews, WeatherForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_ext_loc_graph_3x3_button_location_setting, remoteViews, ExtLocationWithGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_weather_graph_1x3_button_location_setting, remoteViews, WeatherGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_weather_forecast_1x3_button_location_setting, remoteViews, WeatherForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_ext_loc_3x3_button_location_setting, remoteViews, ExtLocationWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_less_3x1_button_location_setting, remoteViews, LessWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_more_3x3_button_location_setting, remoteViews, MoreWidgetProvider.class);
            setSettingButtonAction(context, widgetId, LOCATION_SETTINGS, R.id.widget_ext_loc_forecast_graph_3x3_button_location_setting, remoteViews, ExtLocationWithForecastGraphWidgetProvider.class);

            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_ext_loc_forecast_3x3_button_action_setting, remoteViews, ExtLocationWithForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_weather_forecast_1x3_button_action_setting, remoteViews, WeatherForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_ext_loc_graph_3x3_button_action_setting, remoteViews, ExtLocationWithGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_weather_graph_1x3_button_action_setting, remoteViews, WeatherGraphWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_weather_forecast_1x3_button_action_setting, remoteViews, WeatherForecastWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_ext_loc_3x3_button_action_setting, remoteViews, ExtLocationWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_less_3x1_button_action_setting, remoteViews, LessWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_more_3x3_button_action_setting, remoteViews, MoreWidgetProvider.class);
            setSettingButtonAction(context, widgetId, WIDGET_ACTION_SETTINGS, R.id.widget_ext_loc_forecast_graph_3x3_button_action_setting, remoteViews, ExtLocationWithForecastGraphWidgetProvider.class);
        });
    }

    private static Integer getCityViewId(Class widgetClass) {
        if (ExtLocationWidgetProvider.class.equals(widgetClass)) {
            return R.id.widget_ext_loc_3x3_widget_city;
        } else if (MoreWidgetProvider.class.equals(widgetClass)) {
            return R.id.widget_more_3x3_widget_city;
        } else if (LessWidgetProvider.class.equals(widgetClass)) {
            return R.id.widget_less_3x1_widget_city;
        } else if (ExtLocationWithForecastWidgetProvider.class.equals(widgetClass)) {
            return R.id.widget_ext_loc_forecast_3x3_widget_city;
        } else if (ExtLocationWithGraphWidgetProvider.class.equals(widgetClass)) {
            return R.id.widget_ext_loc_graph_3x3_widget_city;
        } else if (ExtLocationWithForecastGraphWidgetProvider.class.equals(widgetClass)) {
            return R.id.widget_ext_loc_forecast_graph_3x3_widget_city;
        } else {
            return null;
        }
    }

    private static PendingIntent getActionIntent(Context context, WidgetActions widgetAction, Class widgetClass, int widgetId) {
        switch (widgetAction) {
            case LOCATION_SWITCH: return getSwitchLocationIntent(context, widgetClass, widgetId);
            case MAIN_SCREEN:
            case FORECAST_SCREEN:
            case GRAPHS_SCREEN:
            default:
                return getActivityIntent(context, widgetClass, widgetId, widgetAction);
        }
    }

    private static PendingIntent getSwitchLocationIntent(Context context, Class widgetClass, int widgetId) {
        Intent intentSwitchLocality = new Intent(context, widgetClass);
        intentSwitchLocality.setAction(Constants.ACTION_APPWIDGET_CHANGE_LOCATION);
        intentSwitchLocality.putExtra("widgetId", widgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId,
                intentSwitchLocality, PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    private static PendingIntent getActivityIntent(
            Context context,
            Class widgetClass,
            int widgetId,
            WidgetActions widgetAction) {
        Intent activityIntent = new Intent(context, widgetClass);
        Long widgetActionId = widgetAction.getId();
        activityIntent.setAction(Constants.ACTION_APPWIDGET_START_ACTIVITY);
        activityIntent.putExtra("widgetId", widgetId);
        activityIntent.putExtra("widgetAction", widgetActionId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId + widgetActionId.intValue(),
                activityIntent, PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    private static void setSettingButtonAction(Context context, int widgetId, WidgetSettingName settingName, int buttonId, RemoteViews remoteViews, Class widgetClass) {
        Intent intentWeatherForecastWidgetProvider = new Intent(context, widgetClass);
        intentWeatherForecastWidgetProvider.setAction(Constants.ACTION_APPWIDGET_SETTINGS_OPENED);
        intentWeatherForecastWidgetProvider.setPackage("org.thosp.yourlocalweather");
        intentWeatherForecastWidgetProvider.putExtra("settingName", settingName.getWidgetSettingName());
        intentWeatherForecastWidgetProvider.putExtra("widgetId", widgetId);
        PendingIntent pendingWeatherForecastWidgetProvider = PendingIntent.getBroadcast(context, widgetId + settingName.getSettingNameId(),
                intentWeatherForecastWidgetProvider, PendingIntent.FLAG_IMMUTABLE);
        remoteViews.setOnClickPendingIntent(buttonId, pendingWeatherForecastWidgetProvider);
    }

    protected abstract void preLoadWeather(Context context, RemoteViews remoteViews, int widgetId);

    protected abstract Class<?> getWidgetClass();

    protected abstract String getWidgetName();

    protected abstract int getWidgetLayout();

    protected void updateCurrentLocation(Context context, int appWidgetId) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);

        Long locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId");

        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if ((currentLocation != null) && !currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }
    }

    private void changeLocation(int widgetId,
                                LocationsDbHelper locationsDbHelper,
                                WidgetSettingsDbHelper widgetSettingsDbHelper) {
        if (currentLocation == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if ((currentLocation == null) || !currentLocation.isEnabled()) {
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
            if ((currentLocation == null) || !currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        }
        if (currentLocation != null) {
            widgetSettingsDbHelper.saveParamLong(widgetId, "locationId", currentLocation.getId());
        }
    }

    private void openWidgetSettings(Context context, int widgetId, String settingsName) {
        Intent popUpIntent = new Intent(context, WidgetSettingsDialogue.class);
        popUpIntent.putExtra("widgetId", widgetId);
        popUpIntent.putExtra("settings_option", settingsName);
        popUpIntent.putStringArrayListExtra("widget_action_places", getEnabledActionPlaces());
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(popUpIntent);
    }

    abstract ArrayList<String> getEnabledActionPlaces();

    private void startLocationAndWeatherUpdate(Context context) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        ContextCompat.startForegroundService(context, intentToStartUpdate);
    }

    private void startWeatherUpdate(Context context, Location location) {
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(),
                null,
                true,
                true,
                UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        ContextCompat.startForegroundService(context, intentToStartUpdate);
    }
}
