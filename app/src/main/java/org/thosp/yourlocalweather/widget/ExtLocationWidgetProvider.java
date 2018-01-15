package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ExtLocationWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WidgetExtLocInfo";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    private static final String WIDGET_NAME = "EXT_LOC_WIDGET";

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews) {
        SharedPreferences weatherPref = context.getSharedPreferences(Constants.PREF_WEATHER_NAME,
                Context.MODE_PRIVATE);
        String temperatureScale = Utils.getTemperatureScale(context);
        String temperature = String.format(Locale.getDefault(), "%d", Math.round(weatherPref.getFloat(Constants.WEATHER_DATA_TEMPERATURE, 0)));

        String lastUpdate = Utils.setLastUpdateTime(context,
                AppPreference.getLastUpdateTimeMillis(context));

        remoteViews.setTextViewText(R.id.widget_city, Utils.getCityAndCountry(context));
        remoteViews.setTextViewText(R.id.widget_temperature, temperature + temperatureScale);
        remoteViews.setTextViewText(R.id.widget_description, Utils.getWeatherDescription(context));

        WidgetUtils.setWind(context, remoteViews, weatherPref
                .getFloat(Constants.WEATHER_DATA_WIND_SPEED, 0));
        WidgetUtils.setHumidity(context, remoteViews, weatherPref.getInt(Constants.WEATHER_DATA_HUMIDITY, 0));
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1000 * weatherPref.getLong(Constants.WEATHER_DATA_SUNRISE, 0));
        WidgetUtils.setSunrise(context, remoteViews, sdf.format(calendar.getTime()));
        calendar.setTimeInMillis(1000 * weatherPref.getLong(Constants.WEATHER_DATA_SUNSET, 0));
        WidgetUtils.setSunset(context, remoteViews, sdf.format(calendar.getTime()));

        Utils.setWeatherIcon(remoteViews, context);
        remoteViews.setTextViewText(R.id.widget_last_update, lastUpdate);
    }

    @Override
    protected void setWidgetTheme(Context context, RemoteViews remoteViews) {
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
