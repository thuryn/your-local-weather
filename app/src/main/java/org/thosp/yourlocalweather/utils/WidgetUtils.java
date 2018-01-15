package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.thosp.yourlocalweather.R;

import java.util.Locale;

public class WidgetUtils {

    public static void setSunset(Context context, RemoteViews remoteViews, String value) {
        if (AppPreference.showLabelsOnWidget(context)) {
            String sunset = context.getString(R.string.sunset_label, value);
            remoteViews.setTextViewText(R.id.widget_sunset, sunset);
            remoteViews.setViewVisibility(R.id.widget_sunset_icon, TextView.GONE);
        } else {
            String sunset = ": " + value;
            remoteViews.setImageViewBitmap(R.id.widget_sunset_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_sunset)));
            remoteViews.setViewVisibility(R.id.widget_sunset_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_sunset, sunset);
        }
    }

    public static void setSunrise(Context context, RemoteViews remoteViews, String value) {
        if (AppPreference.showLabelsOnWidget(context)) {
            String sunrise = context.getString(R.string.sunrise_label, value);
            remoteViews.setTextViewText(R.id.widget_sunrise, sunrise);
            remoteViews.setViewVisibility(R.id.widget_sunrise_icon, TextView.GONE);
        } else {
            String sunrise = ": " + value;
            remoteViews.setImageViewBitmap(R.id.widget_sunrise_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_sunrise)));
            remoteViews.setViewVisibility(R.id.widget_sunrise_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_sunrise, sunrise);
        }
    }

    public static void setHumidity(Context context, RemoteViews remoteViews, int value) {
        String percentSign = context.getString(R.string.percent_sign);
        if (AppPreference.showLabelsOnWidget(context)) {
            String humidity =
                    context.getString(R.string.humidity_label,
                            String.valueOf(value), percentSign);
            remoteViews.setTextViewText(R.id.widget_humidity, humidity);
            remoteViews.setViewVisibility(R.id.widget_humidity_icon, TextView.GONE);
        } else {
            String humidity = ": " + String.valueOf(value) + percentSign;
            remoteViews.setImageViewBitmap(R.id.widget_humidity_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_humidity)));
            remoteViews.setViewVisibility(R.id.widget_humidity_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_humidity, humidity);
        }
    }

    public static void setWind(Context context, RemoteViews remoteViews, float value) {
        String speedScale = Utils.getSpeedScale(context);
        if (AppPreference.showLabelsOnWidget(context)) {
            String wind = context.getString(R.string.wind_label,
                    String.format(Locale.getDefault(), "%.0f", value),
                    speedScale);
            remoteViews.setTextViewText(R.id.widget_wind, wind);
            remoteViews.setViewVisibility(R.id.widget_wind_icon, TextView.GONE);
        } else {
            String wind = ": " + String.format(Locale.getDefault(), "%.0f", value) + " " + speedScale;
            remoteViews.setImageViewBitmap(R.id.widget_wind_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_wind)));
            remoteViews.setViewVisibility(R.id.widget_wind_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_wind, wind);
        }
    }

    public static void setPressure(Context context, RemoteViews remoteViews, float value) {
        String pressureMeasurement = context.getString(R.string.pressure_measurement);
        if (AppPreference.showLabelsOnWidget(context)) {
            String pressure =
                    context.getString(R.string.pressure_label,
                            String.format(Locale.getDefault(),
                                    "%.1f",
                                    value),
                            pressureMeasurement);
            remoteViews.setTextViewText(R.id.widget_pressure, pressure);
            remoteViews.setViewVisibility(R.id.widget_pressure_icon, TextView.GONE);
        } else {
            String pressure = ": " + String.format(Locale.getDefault(), "%.0f", value) + " " + pressureMeasurement;
            remoteViews.setImageViewBitmap(R.id.widget_pressure_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_barometer)));
            remoteViews.setViewVisibility(R.id.widget_pressure_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_pressure, pressure);
        }
    }

    public static void setClouds(Context context, RemoteViews remoteViews, int value) {
        String percentSign = context.getString(R.string.percent_sign);
        if (AppPreference.showLabelsOnWidget(context)) {
            String cloudnes =
                    context.getString(R.string.cloudiness_label,
                            String.valueOf(value), percentSign);
            remoteViews.setTextViewText(R.id.widget_clouds, cloudnes);
            remoteViews.setViewVisibility(R.id.widget_clouds_icon, TextView.GONE);
        } else {
            String cloudnes = ": " + String.valueOf(value) + " " + percentSign;
            remoteViews.setImageViewBitmap(R.id.widget_clouds_icon, Utils.createWeatherIcon(context, context.getString(R.string.icon_cloudiness)));
            remoteViews.setViewVisibility(R.id.widget_clouds_icon, TextView.VISIBLE);
            remoteViews.setTextViewText(R.id.widget_clouds, cloudnes);
        }
    }
}
