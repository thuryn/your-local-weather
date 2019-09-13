package org.thosp.yourlocalweather.utils;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    public static void weatherNotification(Context context, Long locationId) {
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(context);
        boolean updateBySensor = "0".equals(updateAutoPeriodStr);
        if (updateBySensor) {
            return;
        }
        Notification notification = getWeatherNotification(context, locationId);
        if (notification == null) {
            return;
        }
        showNotification(context, notification);
    }
    
    public static Notification getWeatherNotification(Context context, Long locationId) {
        if (locationId == null) {
            appendLog(context, TAG, "showNotification - locationId is null");
            return null;
        }
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location currentLocation = locationsDbHelper.getLocationById(locationId);
        if (currentLocation == null) {
            appendLog(context, TAG, "showNotification - current location is null");
            return null;
        }
        CurrentWeatherDbHelper.WeatherRecord weatherRecord =
                currentWeatherDbHelper.getWeather(currentLocation.getId());

        if (weatherRecord == null) {
            appendLog(context, TAG, "showNotification - current weather record is null");
            return null;
        }
        return getNotification(context, currentLocation, weatherRecord);
    }
    
    public static Notification getNotification(Context context, Location location, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {

        if (!AppPreference.isNotificationEnabled(context)) {
            return null;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        boolean isOutgoing = false;
        String notificationPresence = AppPreference.getNotificationPresence(context);
        if ("permanent".equals(notificationPresence) || "on_lock_screen".equals(notificationPresence)) {
            isOutgoing = true;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel("yourLocalWeather");
            boolean createNotification = notificationChannel == null;
            if (!createNotification &&
                    ((notificationChannel.getImportance() == NotificationManager.IMPORTANCE_LOW) ||
                            (AppPreference.isVibrateEnabled(context) && (notificationChannel.getVibrationPattern() == null)))) {
                notificationManager.deleteNotificationChannel("yourLocalWeather");
                createNotification = true;
            }
            if (createNotification) {
                NotificationChannel channel = new NotificationChannel("yourLocalWeather",
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(context.getString(R.string.notification_channel_description));
                channel.setVibrationPattern(isVibrateEnabled(context));
                channel.enableVibration(AppPreference.isVibrateEnabled(context));
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setSound(null, null);
                notificationManager.createNotificationChannel(channel);
            }
        }

        String notificationIconStyle = AppPreference.getNotificationStatusIconStyle(context);
        int notificationIcon;
        switch (notificationIconStyle) {
            case "icon_temperature": notificationIcon = TemperatureUtil.getTemperatureStatusIcon(context, weatherRecord); break;
            case "one_invisible_point": notificationIcon = R.drawable.one_transparent_point; break;
            case "icon_sun":
            default: notificationIcon = R.drawable.small_icon;
        }

        Notification notification;
        String notificationVisualStyle = AppPreference.getNotificationVisualStyle(context);
        switch (notificationVisualStyle) {
            case "custom_with_forecast": notification = customNotification(context, location, notificationIcon, isOutgoing, weatherRecord); break;
            case "build_in":
            default:
                notification = regularNotification(context, location, notificationIcon, isOutgoing, weatherRecord); break;
        }
        return notification;
    }

    public static void showNotification(Context context, Notification notification) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private static Notification regularNotification(Context context,
                                                    Location location,
                                                    int notificationIcon,
                                                    boolean isOutgoing,
                                                    CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        Weather weather = weatherRecord.getWeather();
        String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                context,
                weather,
                location.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                location.getLocale());

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
        String cityAndCountry = Utils.getCityAndCountry(context, location.getOrderId());
        return new NotificationCompat.Builder(context, "yourLocalWeather")
                .setContentIntent(launchIntent)
                .setSmallIcon(notificationIcon)
                .setTicker(temperatureWithUnit
                        + "  "
                        + cityAndCountry)
                .setContentTitle(temperatureWithUnit +
                        "  " +
                        Utils.getWeatherDescription(context, location.getLocaleAbbrev(), weatherRecord.getWeather()))
                .setContentText(cityAndCountry)
                .setVibrate(isVibrateEnabled(context))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), Utils.getWeatherResourceIcon(weatherRecord)))
                .setOngoing(isOutgoing)
                .build();
    }

    private static Notification customNotification(Context context,
                                                   Location location,
                                                   int notificationIcon,
                                                   boolean isOutgoing,
                                                   CurrentWeatherDbHelper.WeatherRecord weatherRecord) {

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_custom_weather);
        RemoteViews remoteViewsExpanded = new RemoteViews(context.getPackageName(), R.layout.notification_weather_forecast_expanded);

        Weather weather = weatherRecord.getWeather();

        String cityAndCountry = Utils.getCityAndCountry(context, location.getOrderId());
        String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                context,
                weather,
                location.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                location.getLocale());
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_city, cityAndCountry);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_city, cityAndCountry);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_temperature, temperatureWithUnit);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_temperature, temperatureWithUnit);
        String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                context,
                weather,
                location.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                location.getLocale());
        if (secondTemperature != null) {
            remoteViews.setViewVisibility(R.id.notification_custom_weather_widget_second_temperature, View.VISIBLE);
            remoteViews.setTextViewText(R.id.notification_custom_weather_widget_second_temperature, secondTemperature);
            remoteViewsExpanded.setViewVisibility(R.id.notification_weather_forecast_expanded_widget_second_temperature, View.VISIBLE);
            remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_second_temperature, secondTemperature);
        } else {
            remoteViews.setViewVisibility(R.id.notification_custom_weather_widget_second_temperature, View.GONE);
            remoteViewsExpanded.setViewVisibility(R.id.notification_weather_forecast_expanded_widget_second_temperature, View.GONE);
        }
        String weatherDescription = Utils.getWeatherDescription(context,
                location.getLocaleAbbrev(),
                weather);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_description, weatherDescription);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_description, weatherDescription);

        Utils.setWeatherIcon(remoteViews, context, weatherRecord, R.id.notification_custom_weather_widget_icon);
        Utils.setWeatherIcon(remoteViewsExpanded, context, weatherRecord, R.id.notification_weather_forecast_expanded_widget_icon);

        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = null;
        try {
            weatherForecastRecord = WidgetUtils.updateWeatherForecast(
                    context,
                    location.getId(),
                    null,
                    remoteViewsExpanded,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_1_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_1_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_1_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_2_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_2_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_2_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_3_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_3_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_3_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_4_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_4_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_4_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_5_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_5_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_5_widget_temperatures);
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, location);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_last_update, lastUpdate);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_last_update, lastUpdate);

        return new NotificationCompat.Builder(context, "yourLocalWeather")
                .setSmallIcon(notificationIcon)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViewsExpanded)
                .setVibrate(isVibrateEnabled(context))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isOutgoing)
                .setAutoCancel(true)
                .build();
    }

    private static long[] isVibrateEnabled(Context context) {
        if (!AppPreference.isVibrateEnabled(context)) {
            return null;
        }
        return new long[]{0, 500, 500};
    }

    public static boolean isScreenLocked(Context context) {
        android.app.KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    public static Location getLocationForNotification(Context context) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }
}
