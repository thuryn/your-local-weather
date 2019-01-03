package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ExtLocationWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WidgetExtLocInfo";

    private static final String WIDGET_NAME = "EXT_LOC_WIDGET";

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);

        Long locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId");

        if (locationId == null) {
            currentLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!currentLocation.isEnabled()) {
                currentLocation = locationsDbHelper.getLocationByOrderId(1);
            }
        } else {
            currentLocation = locationsDbHelper.getLocationById(locationId);
        }

        if (currentLocation == null) {
            return;
        }

        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

        if (weatherRecord != null) {
            Weather weather = weatherRecord.getWeather();

            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_city, Utils.getCityAndCountry(context, currentLocation.getOrderId()));
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_temperature, TemperatureUtil.getTemperatureWithUnit(
                    context,
                    weather,
                    currentLocation.getLatitude(),
                    weatherRecord.getLastUpdatedTime(),
                    currentLocation.getLocale()));
            String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                    context,
                    weather,
                    currentLocation.getLatitude(),
                    weatherRecord.getLastUpdatedTime(),
                    currentLocation.getLocale());
            if (secondTemperature != null) {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_second_temperature, secondTemperature);
            } else {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.GONE);
            }
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_description,
                                        Utils.getWeatherDescription(context,
                                                                    currentLocation.getLocaleAbbrev(),
                                                                    weather));

            WidgetUtils.setWind(context,
                                remoteViews,
                                weather.getWindSpeed(),
                                weather.getWindDirection(),
                                currentLocation.getLocale(),
                    R.id.widget_ext_loc_3x3_widget_wind,
                    R.id.widget_ext_loc_3x3_widget_wind_icon);
            WidgetUtils.setHumidity(context, remoteViews, weather.getHumidity(),
                    R.id.widget_ext_loc_3x3_widget_humidity,
                    R.id.widget_ext_loc_3x3_widget_humidity_icon);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(1000 * weather.getSunrise());
            WidgetUtils.setSunrise(context,
                    remoteViews, AppPreference.getLocalizedTime(context, calendar.getTime(), currentLocation.getLocale()),
                    R.id.widget_ext_loc_3x3_widget_sunrise,
                    R.id.widget_ext_loc_3x3_widget_sunrise_icon);
            calendar.setTimeInMillis(1000 * weather.getSunset());
            WidgetUtils.setSunset(context, remoteViews, AppPreference.getLocalizedTime(context, calendar.getTime(), currentLocation.getLocale()),
                    R.id.widget_ext_loc_3x3_widget_sunset,
                    R.id.widget_ext_loc_3x3_widget_sunset_icon);

            Utils.setWeatherIcon(remoteViews, context, weatherRecord,
                    R.id.widget_ext_loc_3x3_widget_icon);
            String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, currentLocation);
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_last_update, lastUpdate);
        } else {
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_city, context.getString(R.string.location_not_found));
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_temperature, TemperatureUtil.getTemperatureWithUnit(
                    context,
                    null,
                    currentLocation.getLatitude(),
                    0,
                    currentLocation.getLocale()));
            String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                    context,
                    null,
                    currentLocation.getLatitude(),
                    0,
                    currentLocation.getLocale());
            if (secondTemperature != null) {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_second_temperature, secondTemperature);
            } else {
                remoteViews.setViewVisibility(R.id.widget_ext_loc_3x3_widget_second_temperature, View.GONE);
            }
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_description, "");

            WidgetUtils.setWind(context,
                                remoteViews,
                            0,
                        0,
                                currentLocation.getLocale(),
                    R.id.widget_ext_loc_3x3_widget_wind,
                    R.id.widget_ext_loc_3x3_widget_wind_icon);
            WidgetUtils.setHumidity(context, remoteViews, 0,
                    R.id.widget_ext_loc_3x3_widget_humidity,
                    R.id.widget_ext_loc_3x3_widget_humidity_icon);
            WidgetUtils.setSunrise(context, remoteViews, "",
                    R.id.widget_ext_loc_3x3_widget_sunrise,
                    R.id.widget_ext_loc_3x3_widget_sunrise_icon);
            WidgetUtils.setSunset(context, remoteViews, "",
                    R.id.widget_ext_loc_3x3_widget_sunset,
                    R.id.widget_ext_loc_3x3_widget_sunset_icon);

            Utils.setWeatherIcon(remoteViews, context, weatherRecord,
                    R.id.widget_ext_loc_3x3_widget_icon);
            remoteViews.setTextViewText(R.id.widget_ext_loc_3x3_widget_last_update, "");
        }
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        remoteViews.setInt(R.id.widget_ext_loc_3x3_widget_root, "setBackgroundColor", backgroundColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_temperature, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_description, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_description, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_wind, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_humidity, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_sunrise, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_sunset, textColorId);
        remoteViews.setTextColor(R.id.widget_ext_loc_3x3_widget_second_temperature, textColorId);
        remoteViews.setInt(R.id.widget_ext_loc_3x3_header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);
        appendLog(context, TAG, "setWidgetTheme:end");
    }

    @Override
    ArrayList<String> getEnabledActionPlaces() {
        ArrayList<String> enabledWidgetActions = new ArrayList();
        enabledWidgetActions.add("action_city");
        enabledWidgetActions.add("action_current_weather_icon");
        return enabledWidgetActions;
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
