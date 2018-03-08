package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.SharedPreferences;
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
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ExtLocationWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WidgetExtLocInfo";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    private static final String WIDGET_NAME = "EXT_LOC_WIDGET";

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews, int appWidgetId) {
        appendLog(context, TAG, "preLoadWeather:start");
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(context);

        Long locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId");

        Location location;
        if (locationId == null) {
            location = locationsDbHelper.getLocationByOrderId(0);
        } else {
            location = locationsDbHelper.getLocationById(locationId);
        }

        if (location == null) {
            return;
        }

        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(location.getId());

        if (weatherRecord != null) {
            Weather weather = weatherRecord.getWeather();

            String lastUpdate = Utils.setLastUpdateTime(context, weatherRecord.getLastUpdatedTime(), location.getLocationSource());

            remoteViews.setTextViewText(R.id.widget_city, Utils.getCityAndCountry(context, location.getOrderId()));
            remoteViews.setTextViewText(R.id.widget_temperature, AppPreference.getTemperatureWithUnit(
                    context,
                    weather.getTemperature()));
            remoteViews.setTextViewText(R.id.widget_description, Utils.getWeatherDescription(context, weather));

            WidgetUtils.setWind(context, remoteViews, weather.getWindSpeed());
            WidgetUtils.setHumidity(context, remoteViews, weather.getHumidity());
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(1000 * weather.getSunrise());
            WidgetUtils.setSunrise(context, remoteViews, sdf.format(calendar.getTime()));
            calendar.setTimeInMillis(1000 * weather.getSunset());
            WidgetUtils.setSunset(context, remoteViews, sdf.format(calendar.getTime()));

            Utils.setWeatherIcon(remoteViews, context, weatherRecord);
            remoteViews.setTextViewText(R.id.widget_last_update, lastUpdate);
        }
        appendLog(context, TAG, "preLoadWeather:end");
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews) {
        appendLog(context, TAG, "setWidgetTheme:start");
        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);

        remoteViews.setInt(R.id.widget_root, "setBackgroundColor", backgroundColorId);
        remoteViews.setTextColor(R.id.widget_temperature, textColorId);
        remoteViews.setTextColor(R.id.widget_description, textColorId);
        remoteViews.setTextColor(R.id.widget_description, textColorId);
        remoteViews.setTextColor(R.id.widget_wind, textColorId);
        remoteViews.setTextColor(R.id.widget_humidity, textColorId);
        remoteViews.setTextColor(R.id.widget_sunrise, textColorId);
        remoteViews.setTextColor(R.id.widget_sunset, textColorId);
        remoteViews.setInt(R.id.header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);
        appendLog(context, TAG, "setWidgetTheme:end");
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
