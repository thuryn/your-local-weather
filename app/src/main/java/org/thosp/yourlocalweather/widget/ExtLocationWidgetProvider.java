package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.ArrayList;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class ExtLocationWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WidgetExtLocInfo";

    private static final String WIDGET_NAME = "EXT_LOC_WIDGET";

    private static final String DEFAULT_CURRENT_WEATHER_DETAILS = "0,1,5,6";
    private static final int MAX_CURRENT_WEATHER_DETAILS = 4;

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);
        String pressureUnitFromPreferences = AppPreference.getPressureUnitFromPreferences(context);
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
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(context);
        int widgetTextColor = AppPreference.getWidgetTextColor(context);
        boolean showLabelsOnWidget = AppPreference.showLabelsOnWidget(context);
        String windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(context);
        String timeStylePreference = AppPreference.getTimeStylePreference(context);

        ContextCompat.getMainExecutor(context).execute(()  -> {
            String storedCurrentWeatherDetailsForUpdate = (storedCurrentWeatherDetails != null) ? storedCurrentWeatherDetails : DEFAULT_CURRENT_WEATHER_DETAILS;
            WidgetUtils.updateCurrentWeatherDetails(
                    context,
                    remoteViews,
                    weatherRecord,
                    currentLocation.getLocale(),
                    storedCurrentWeatherDetailsForUpdate,
                    pressureUnitFromPreferences,
                    temperatureUnitFromPreferences,
                    widgetTextColor,
                    showLabelsOnWidget,
                    windUnitFromPreferences,
                    timeStylePreference);
        });

        boolean fontBasedIcons = "weather_icon_set_fontbased".equals(AppPreference.getIconSet(context));

        if (weatherRecord != null) {
            Weather weather = weatherRecord.getWeather();
            String temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(context);

            String temperatureWIthUnit = TemperatureUtil.getTemperatureWithUnit(
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
                    weather);
            IconHolder weatherIconHolder = new IconHolder();
            if (fontBasedIcons) {
                weatherIconHolder.bitmapIcon = Utils.createWeatherIcon(context, Utils.getStrIconFromWEatherRecord(context, weatherRecord));
            } else {
                weatherIconHolder.resourceIcon = Utils.getWeatherResourceIcon(weatherRecord);
            }
            String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, timeStylePreference, currentLocation);

            ContextCompat.getMainExecutor(context).execute(()  -> {
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_city, Utils.getCityAndCountry(context, currentLocation));
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_temperature, temperatureWIthUnit);

                if (secondTemperature != null) {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.VISIBLE);
                    remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_second_temperature, secondTemperature);
                } else {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.GONE);
                }
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_description, weatherDescription);

                if (weatherIconHolder.bitmapIcon != null) {
                    remoteViews.setImageViewBitmap(R.id.widget_ext_loc_3x3_widget_icon, weatherIconHolder.bitmapIcon);
                } else {
                    remoteViews.setImageViewResource(R.id.widget_ext_loc_3x3_widget_icon, weatherIconHolder.resourceIcon);
                }
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_last_update, lastUpdate);
            });
        } else {
            String temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(context);
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

            ContextCompat.getMainExecutor(context).execute(()  -> {
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_city, context.getString(R.string.location_not_found));
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_temperature, temperatureWithUnit);
                if (secondTemperature != null) {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.VISIBLE);
                    remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_second_temperature, secondTemperature);
                } else {
                    remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.GONE);
                }
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_description, "");

                if (weatherIconHolder.bitmapIcon != null) {
                    remoteViews.setImageViewBitmap(R.id.widget_ext_loc_3x3_widget_icon, weatherIconHolder.bitmapIcon);
                } else {
                    remoteViews.setImageViewResource(R.id.widget_ext_loc_3x3_widget_icon, weatherIconHolder.resourceIcon);
                }
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_last_update, "");
            });
        }
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getWidgetTextColor(context);
        int backgroundColorId = AppPreference.getWidgetBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        ContextCompat.getMainExecutor(context).execute(()  -> {
                    remoteViews.setInt(R.id.widget_ext_loc_3x3_widget_root, "setBackgroundColor", backgroundColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_temperature, textColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_description, textColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_description, textColorId);
                    remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_second_temperature, textColorId);
                    remoteViews.setInt(R.id.widget_ext_loc_3x3_header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);
                });
        appendLog(context, TAG, "setWidgetTheme:end");
    }

    @Override
    ArrayList<String> getEnabledActionPlaces() {
        ArrayList<String> enabledWidgetActions = new ArrayList();
        enabledWidgetActions.add("action_city");
        enabledWidgetActions.add("action_current_weather_icon");
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
        return R.layout.widget_ext_loc_3x3;
    }

    @Override
    protected Class<?> getWidgetClass() {
        return ExtLocationWidgetProvider.class;
    }

    @Override
    protected String getWidgetName() {
        return WIDGET_NAME;
    }
}
