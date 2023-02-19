package org.thosp.yourlocalweather.utils;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    public static final int NOTIFICATION_ID = 2109876543;

    public static void weatherNotification(Context context, Long locationId) {
        /*String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(context);
        boolean updateBySensor = "0".equals(updateAutoPeriodStr);
        if (updateBySensor) {
            return;
        }*/
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
            return getNoWeatherNotification(context);
        }
        CurrentWeatherDbHelper.WeatherRecord weatherRecord =
                currentWeatherDbHelper.getWeather(currentLocation.getId());

        if (weatherRecord == null) {
            appendLog(context, TAG, "showNotification - current weather record is null");
            return getNoWeatherNotification(context);
        }
        return getNotification(context, currentLocation, weatherRecord);
    }

    public static Notification getNoWeatherNotification(Context context) {
        return new NotificationCompat.Builder(context, "yourLocalWeather")
                .setSmallIcon(R.drawable.ic_refresh_white_18dp_1)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(false)
                .build();
    }

    public static Notification getNotificationForActivity(Context context) {
        checkAndCreateNotificationChannel(context);
        return getWeatherNotification(context, getLocationForNotification(context).getId());
    }

    public static void checkAndCreateNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel("yourLocalWeather");
        boolean createNotification = notificationChannel == null;
        if (!createNotification &&
                ((notificationChannel.getImportance() == NotificationManager.IMPORTANCE_LOW) ||
                        (AppPreference.getInstance().isVibrateEnabled(context) && (notificationChannel.getVibrationPattern() == null)))) {
            notificationManager.deleteNotificationChannel("yourLocalWeather");
            createNotification = true;
        }
        if (createNotification) {
            NotificationChannel channel = new NotificationChannel("yourLocalWeather",
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notification_channel_description));
            channel.setVibrationPattern(isVibrateEnabled(context));
            channel.enableVibration(AppPreference.getInstance().isVibrateEnabled(context));
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Notification getNotification(Context context, Location location, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {

        if (!AppPreference.getInstance().isNotificationEnabled(context)) {
            return getNoWeatherNotification(context);
        }
        boolean isOutgoing = false;
        String notificationPresence = AppPreference.getNotificationPresence(context);
        if ("permanent".equals(notificationPresence) || "on_lock_screen".equals(notificationPresence)) {
            isOutgoing = true;
        }
        checkAndCreateNotificationChannel(context);

        String notificationIconStyle = AppPreference.getNotificationStatusIconStyle(context);
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(context);
        int notificationIcon;
        switch (notificationIconStyle) {
            case "icon_temperature": notificationIcon = TemperatureUtil.getTemperatureStatusIcon(context, temperatureUnitFromPreferences, weatherRecord); break;
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
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    private static Notification regularNotification(Context context,
                                                    Location location,
                                                    int notificationIcon,
                                                    boolean isOutgoing,
                                                    CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        Weather weather = weatherRecord.getWeather();
        String temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(context);
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(context);

        String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                context,
                weather,
                location.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                temeratureTypeFromPreferences,
                temperatureUnitFromPreferences,
                location.getLocale());

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        boolean defaultApiKey = ApiKeys.isDefaultOpenweatherApiKey(context);
        String cityAndCountry = Utils.getCityAndCountry(context, defaultApiKey, location);
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

        int textColor = PreferenceUtil.getTextColor(context);
        String timeStylePreference = AppPreference.getTimeStylePreference(context);
        Weather weather = weatherRecord.getWeather();
        boolean defaultApiKey = ApiKeys.isDefaultOpenweatherApiKey(context);
        String cityAndCountry = Utils.getCityAndCountry(context, defaultApiKey, location);
        String temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(context);
        String temeratureTypeFromPreferences = AppPreference.getTemeratureTypeFromPreferences(context);
        String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                context,
                weather,
                location.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                temeratureTypeFromPreferences,
                temperatureUnitFromPreferences,
                location.getLocale());
        String secondTemperature = TemperatureUtil.getSecondTemperatureWithUnit(
                context,
                weather,
                location.getLatitude(),
                weatherRecord.getLastUpdatedTime(),
                temperatureUnitFromPreferences,
                location.getLocale());
        String weatherDescription = Utils.getWeatherDescription(context,
                location.getLocaleAbbrev(),
                weather);
        boolean fontBasedIcons = "weather_icon_set_fontbased".equals(AppPreference.getIconSet(context));
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        final WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(location.getId());

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_custom_weather);
        RemoteViews remoteViewsExpanded = new RemoteViews(context.getPackageName(), R.layout.notification_weather_forecast_expanded);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_city, cityAndCountry);
        remoteViews.setTextColor(R.id.notification_custom_weather_widget_city, textColor);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_city, cityAndCountry);
        remoteViewsExpanded.setTextColor(R.id.notification_weather_forecast_expanded_widget_city, textColor);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_temperature, temperatureWithUnit);
        remoteViews.setTextColor(R.id.notification_custom_weather_widget_temperature, textColor);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_temperature, temperatureWithUnit);
        remoteViewsExpanded.setTextColor(R.id.notification_weather_forecast_expanded_widget_temperature, textColor);

        if (secondTemperature != null) {
            remoteViews.setViewVisibility(R.id.notification_custom_weather_widget_second_temperature, View.VISIBLE);
            remoteViews.setTextViewText(R.id.notification_custom_weather_widget_second_temperature, secondTemperature);
            remoteViews.setTextColor(R.id.notification_custom_weather_widget_second_temperature, textColor);
            remoteViewsExpanded.setViewVisibility(R.id.notification_weather_forecast_expanded_widget_second_temperature, View.VISIBLE);
            remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_second_temperature, secondTemperature);
            remoteViewsExpanded.setTextColor(R.id.notification_weather_forecast_expanded_widget_second_temperature, textColor);
        } else {
            remoteViews.setViewVisibility(R.id.notification_custom_weather_widget_second_temperature, View.GONE);
            remoteViewsExpanded.setViewVisibility(R.id.notification_weather_forecast_expanded_widget_second_temperature, View.GONE);
        }

        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_description, weatherDescription);
        remoteViews.setTextColor(R.id.notification_custom_weather_widget_description, textColor);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_description, weatherDescription);
        remoteViewsExpanded.setTextColor(R.id.notification_weather_forecast_expanded_widget_description, textColor);

        //int iconColorBlack = ContextCompat.getColor(context, R.color.widget_lightTheme_textColorPrimary);
        Utils.setWeatherIconWithColor(remoteViews, context, weatherRecord, fontBasedIcons, R.id.notification_custom_weather_widget_icon, textColor);
        Utils.setWeatherIconWithColor(remoteViewsExpanded, context, weatherRecord, fontBasedIcons, R.id.notification_weather_forecast_expanded_widget_icon, textColor);

        Map<Long, String> localizedHourMap = new HashMap<>();
        Map<Long, String> temperaturesMap = new HashMap<>();
        for (DetailedWeatherForecast detailedWeatherForecast: weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {

            long forecastTime = detailedWeatherForecast.getDateTime();
            Calendar forecastCalendar = Calendar.getInstance();
            forecastCalendar.setTimeInMillis(forecastTime * 1000);
            Date forecastCalendarTime = forecastCalendar.getTime();
            String localizedHour = AppPreference.getLocalizedHour(context, forecastCalendarTime, location.getLocale());
            localizedHourMap.put(forecastTime, localizedHour);

            temperaturesMap.put(forecastTime, Math.round(TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnitFromPreferences, detailedWeatherForecast.getTemperatureMin())) +
                    "/" +
                    Math.round(TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnitFromPreferences, detailedWeatherForecast.getTemperatureMax())) +
                    TemperatureUtil.getTemperatureUnit(context, temperatureUnitFromPreferences));
        }

        try {
            WidgetUtils.updateWeatherForecast(
                    context,
                    location,
                    weatherForecastRecord,
                    textColor,
                    null,
                    5l,
                    false,
                    null,
                    fontBasedIcons,
                    localizedHourMap,
                    temperaturesMap,
                    temperatureUnitFromPreferences,
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
        String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, timeStylePreference, location);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_last_update, lastUpdate);
        remoteViews.setTextColor(R.id.notification_custom_weather_widget_last_update, textColor);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_last_update, lastUpdate);
        remoteViewsExpanded.setTextColor(R.id.notification_weather_forecast_expanded_widget_last_update, textColor);

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
        if (!AppPreference.getInstance().isVibrateEnabled(context)) {
            return null;
        }
        return new long[]{0, 500, 500};
    }

    public static boolean isScreenLocked(Context context) {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return mKeyguardManager.isKeyguardLocked();
        } else {
            return mKeyguardManager.inKeyguardRestrictedInputMode();
        }
    }

    public static Location getLocationForNotification(Context context) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if ((currentLocation == null) || !currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }
}
