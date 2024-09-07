package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.ArrayList;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class WeatherGraphWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WeatherGraphWidgetProvider";

    private static final String WIDGET_NAME = "WEATHER_GRAPH_WIDGET";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS") ||
                intent.getAction().equals(Constants.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE)) {
            GraphUtils.invalidateGraph();
            refreshWidgetValues(context);
        }
    }

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");

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

        if (currentLocation == null) {
            return;
        }

        Boolean showLegend = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "combinedGraphShowLegend");
        Set<Integer> combinedGraphValuesFromPreferences = AppPreference.getCombinedGraphValues(context);
        Set<Integer> combinedGraphValuesFromSettings = GraphUtils.getCombinedGraphValuesFromSettings(combinedGraphValuesFromPreferences, widgetSettingsDbHelper, appWidgetId);
        int widgetTextColor = AppPreference.getWidgetTextColor(context);
        int widgetBackgroundColor = AppPreference.getWidgetBackgroundColor(context);
        AppPreference.GraphGridColors widgetGraphGridColor = AppPreference.getWidgetGraphGridColor(context);
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(context);
        String pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(context);
        String rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(context);
        boolean widgetGraphNativeScaled = AppPreference.isWidgetGraphNativeScaled(context);
        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(context);

        WeatherGraphWidgetProvider.setWidgetTheme(context, remoteViews, appWidgetId);
        WeatherGraphWidgetProvider.setWidgetIntents(context, remoteViews, WeatherGraphWidgetProvider.class, appWidgetId);

        ContextCompat.getMainExecutor(context).execute(()  -> {
                    remoteViews.setTextViewText(R.id.widget_weather_graph_1x3_widget_city, Utils.getCityAndCountry(context, currentLocation));
                });

        try {
            final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
            Location location = locationsDbHelper.getLocationById(currentLocation.getId());
            if (location != null) {
                WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
                    ContextCompat.getMainExecutor(context).execute(()  -> {
                        if (weatherForecastRecord != null) {
                            remoteViews.setImageViewBitmap(R.id.widget_weather_graph_1x3_widget_combined_chart,
                                    GraphUtils.getCombinedChart(
                                            context,
                                            appWidgetId,
                                            null,
                                            weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList(),
                                            currentLocation.getId(),
                                            currentLocation.getLocale(),
                                            showLegend,
                                            combinedGraphValuesFromSettings,
                                            widgetTextColor,
                                            widgetBackgroundColor,
                                            widgetGraphGridColor,
                                            temperatureUnitFromPreferences,
                                            pressureUnitFromPreferences,
                                            rainSnowUnitFromPreferences,
                                            widgetGraphNativeScaled,
                                            windUnitFromPreferences));
                        }
                    });
                }
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews, int widgetId) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int backgroundColorId = AppPreference.getWidgetBackgroundColor(context);

        final WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        Boolean showLocation = widgetSettingsDbHelper.getParamBoolean(widgetId, "showLocation");

        ContextCompat.getMainExecutor(context).execute(()  -> {
            boolean showLocationParam = showLocation != null && showLocation;
            if (showLocationParam) {
                remoteViews.setViewVisibility(R.id.widget_weather_graph_1x3_widget_city, View.VISIBLE);
            } else {
                remoteViews.setViewVisibility(R.id.widget_weather_graph_1x3_widget_city, View.GONE);
            }
            remoteViews.setInt(R.id.widget_weather_graph_1x3_widget_root, "setBackgroundColor", backgroundColorId);
        });
        appendLog(context, TAG, "setWidgetTheme:end");
    }

    @Override
    ArrayList<String> getEnabledActionPlaces() {
        ArrayList<String> enabledWidgetActions = new ArrayList();
        enabledWidgetActions.add("action_graph");
        return enabledWidgetActions;
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
