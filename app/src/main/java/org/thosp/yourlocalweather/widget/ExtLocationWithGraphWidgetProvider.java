package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.service.UpdateWeatherService;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.ArrayList;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class ExtLocationWithGraphWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "ExtLocationWithGraphWidgetProvider";

    private static final String WIDGET_NAME = "EXT_LOC_WITH_GRAPH_WIDGET";

    private static final String DEFAULT_CURRENT_WEATHER_DETAILS = "0,1,5,6";
    private static final int MAX_CURRENT_WEATHER_DETAILS = 4;

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
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        String pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(context);
        Set<Integer> combinedGraphValuesFromPreferences = AppPreference.getCombinedGraphValues(context);
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

        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

        String storedCurrentWeatherDetails = widgetSettingsDbHelper.getParamString(appWidgetId, "currentWeatherDetails");
        int widgetTextColor = AppPreference.getWidgetTextColor(context);
        boolean showLabelsOnWidget = AppPreference.showLabelsOnWidget(context);
        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(context);
        String timeStylePreference = AppPreference.getTimeStylePreference(context);

        ContextCompat.getMainExecutor(context).execute(()  -> {
            String storedCurrentWeatherDetailsParam = (storedCurrentWeatherDetails == null) ? DEFAULT_CURRENT_WEATHER_DETAILS : storedCurrentWeatherDetails;
            WidgetUtils.updateCurrentWeatherDetails(
                    context,
                    remoteViews,
                    weatherRecord,
                    currentLocation.getLocale(),
                    storedCurrentWeatherDetailsParam,
                    pressureUnitFromPreferences,
                    temperatureUnitFromPreferences,
                    widgetTextColor,
                    showLabelsOnWidget,
                    windUnitFromPreferences,
                    timeStylePreference);
        });

        boolean fontBasedIcons = "weather_icon_set_fontbased".equals(AppPreference.getIconSet(context));
        Boolean showLegend = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "combinedGraphShowLegend");
        Set<Integer> combinedGraphValuesFromSettings = GraphUtils.getCombinedGraphValuesFromSettings(combinedGraphValuesFromPreferences, widgetSettingsDbHelper, appWidgetId);
        int widgetBackgroundColor = AppPreference.getWidgetBackgroundColor(context);
        AppPreference.GraphGridColors widgetGraphGridColor = AppPreference.getWidgetGraphGridColor(context);
        String rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(context);
        boolean widgetGraphNativeScaled = AppPreference.isWidgetGraphNativeScaled(context);
        String temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(context);

        if (weatherRecord != null) {
            Weather weather = weatherRecord.getWeather();

            String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                    context,
                    weather,
                    currentLocation.getLatitude(),
                    weatherRecord.getLastUpdatedTime(),
                    temeratureTypeFromPreferences,
                    temperatureUnitFromPreferences,
                    currentLocation.getLocale());
            String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                    context,
                    weather,
                    currentLocation.getLatitude(),
                    weatherRecord.getLastUpdatedTime(),
                    temperatureUnitFromPreferences,
                    currentLocation.getLocale());
            String weatherDescription = Utils.getWeatherDescription(context,
                    currentLocation.getLocaleAbbrev(),
                    weather);
            IconHolder weatherIconHolder = new IconHolder();
            if (fontBasedIcons) {
                weatherIconHolder.bitmapIcon = Utils.createWeatherIcon(context, Utils.getStrIconFromWEatherRecord(context, weatherRecord));
            } else {
                weatherIconHolder.resourceIcon = Utils.getWeatherResourceIcon(weatherRecord);
            }

            ContextCompat.getMainExecutor(context).execute(()  -> {
                remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_city, Utils.getCityAndCountry(context, currentLocation));
                remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_temperature, temperatureWithUnit);
                if (secondTemperature != null) {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, View.VISIBLE);
                    remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, secondTemperature);
                } else {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, View.GONE);
                }
                remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_description, weatherDescription);
                if (weatherIconHolder.bitmapIcon != null) {
                    remoteViews.setImageViewBitmap(R.id.widget_ext_loc_graph_3x3_widget_icon, weatherIconHolder.bitmapIcon);
                } else {
                    remoteViews.setImageViewResource(R.id.widget_ext_loc_graph_3x3_widget_icon, weatherIconHolder.resourceIcon);
                }
            });
        } else {
            String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                    context,
                    null,
                    currentLocation.getLatitude(),
                    0,
                    temeratureTypeFromPreferences,
                    temperatureUnitFromPreferences,
                    currentLocation.getLocale());
            String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                    context,
                    null,
                    currentLocation.getLatitude(),
                    0,
                    temperatureUnitFromPreferences,
                    currentLocation.getLocale());
            IconHolder weatherIconHolder = new IconHolder();
            if (fontBasedIcons) {
                weatherIconHolder.bitmapIcon = Utils.createWeatherIcon(context, Utils.getStrIconFromWEatherRecord(context, null));
            } else {
                weatherIconHolder.resourceIcon = Utils.getWeatherResourceIcon(null);
            }

            ContextCompat.getMainExecutor(context).execute(() -> {
                remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_city, context.getString(R.string.location_not_found));
                remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_temperature, temperatureWithUnit);

                if (secondTemperature != null) {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, View.VISIBLE);
                    remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, secondTemperature);
                } else {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, View.GONE);
                }
                remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_description, "");

                if (weatherIconHolder.bitmapIcon != null) {
                    remoteViews.setImageViewBitmap(R.id.widget_ext_loc_graph_3x3_widget_icon, weatherIconHolder.bitmapIcon);
                } else {
                    remoteViews.setImageViewResource(R.id.widget_ext_loc_graph_3x3_widget_icon, weatherIconHolder.resourceIcon);
                }
            });
        }
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
        try {
                ContextCompat.getMainExecutor(context).execute(() -> {
                    if (weatherForecastRecord != null) {
                        remoteViews.setImageViewBitmap(R.id.widget_ext_loc_graph_3x3_widget_combined_chart,
                                GraphUtils.getCombinedChart(
                                        context,
                                        appWidgetId,
                                        0.2f,
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
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, timeStylePreference, currentLocation);
        ContextCompat.getMainExecutor(context).execute(() -> {
                    remoteViews.setTextViewText(R.id.widget_ext_loc_graph_3x3_widget_last_update, lastUpdate);
                });
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews, int widgetId) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getWidgetTextColor(context);
        int backgroundColorId = AppPreference.getWidgetBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        ContextCompat.getMainExecutor(context).execute(()  -> {
                    remoteViews.setInt(R.id.widget_ext_loc_graph_3x3_widget_root, "setBackgroundColor", backgroundColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_graph_3x3_widget_temperature, textColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_graph_3x3_widget_description, textColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_graph_3x3_widget_description, textColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_graph_3x3_widget_second_temperature, textColorId);
                    remoteViews.setInt(R.id.widget_ext_loc_graph_3x3_header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);
                });

        appendLog(context, TAG, "setWidgetTheme:end");
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
            Intent intentToCheckWeather =new Intent(context, UpdateWeatherService.class);
            intentToCheckWeather.putExtra("updateType", UpdateWeatherService.START_WEATHER_FORECAST_UPDATE);
            intentToCheckWeather.putExtra("locationId", currentLocation.getId());
            intentToCheckWeather.putExtra("forceUpdate", true);
            ContextCompat.startForegroundService(context, intentToCheckWeather);
        }
    }

    @Override
    ArrayList<String> getEnabledActionPlaces() {
        ArrayList<String> enabledWidgetActions = new ArrayList();
        enabledWidgetActions.add("action_city");
        enabledWidgetActions.add("action_current_weather_icon");
        enabledWidgetActions.add("action_graph");
        return enabledWidgetActions;
    }

    public static int getNumberOfCurrentWeatherDetails() {
        return MAX_CURRENT_WEATHER_DETAILS;
    }

    public static String getDefaultCurrentWeatherDetails() {
        return DEFAULT_CURRENT_WEATHER_DETAILS;
    }

    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_ext_loc_graph_3x3;
    }

    @Override
    protected Class<?> getWidgetClass() {
        return ExtLocationWithGraphWidgetProvider.class;
    }

    @Override
    protected String getWidgetName() {
        return WIDGET_NAME;
    }
}
