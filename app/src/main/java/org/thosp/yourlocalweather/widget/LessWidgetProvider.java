package org.thosp.yourlocalweather.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.Locale;


public class LessWidgetProvider extends AbstractWidgetProvider {

    private static final String TAG = "WidgetLessInfo";

    private static final String WIDGET_NAME = "MORE_WIDGET";

    @Override
    protected void preLoadWeather(Context context, RemoteViews remoteViews) {
        SharedPreferences weatherPref = context.getSharedPreferences(Constants.PREF_WEATHER_NAME,
                                                                     Context.MODE_PRIVATE);
        String temperatureScale = Utils.getTemperatureScale(context);

        String temperature = String.format(Locale.getDefault(), "%d", Math.round(weatherPref.getFloat(Constants.WEATHER_DATA_TEMPERATURE, 0)));
        String weatherIcon = Utils.getStrIcon(context);
        String lastUpdate = Utils.setLastUpdateTime(context,
                                                    AppPreference.getLastUpdateTimeMillis(context));
        remoteViews.setTextViewText(R.id.widget_city, Utils.getCityAndCountry(context));
        remoteViews.setTextViewText(R.id.widget_temperature,
                                    temperature + temperatureScale);
        remoteViews.setTextViewText(R.id.widget_description, Utils.getWeatherDescription(context));
        Utils.setWeatherIcon(remoteViews, context);
        remoteViews.setTextViewText(R.id.widget_last_update, lastUpdate);
    }

    public static void setWidgetTheme(Context context, RemoteViews remoteViews) {

        int textColorId = AppPreference.getTextColor(context);
        int backgroundColorId = AppPreference.getBackgroundColor(context);
        int windowHeaderBackgroundColorId = AppPreference.getWindowHeaderBackgroundColorId(context);
        
        remoteViews.setInt(R.id.widget_root, "setBackgroundColor", backgroundColorId);
        remoteViews.setTextColor(R.id.widget_temperature, textColorId);
        remoteViews.setTextColor(R.id.widget_description, textColorId);
        remoteViews.setInt(R.id.header_layout, "setBackgroundColor", windowHeaderBackgroundColorId);
    }

    @Override
    protected int getWidgetLayout() {
        return R.layout.widget_less_3x1;
    }

    @Override
    protected Class<?> getWidgetClass() {
        return LessWidgetProvider.class;
    }

    @Override
    protected String getWidgetName() {
        return WIDGET_NAME;
    }
}
