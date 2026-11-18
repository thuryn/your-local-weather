package org.thosp.yourlocalweather.utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    public static final int NOTIFICATION_ID = 2109876543;
    public static final int FORECAST_ALERT_NOTIFICATION_ID = 2109876544;
    public static final String CHANNEL_DEFAULT = "yourLocalWeather";
    public static final String CHANNEL_FORECAST_ALERT = "yourLocalWeatherForecastAlert";

    public static Notification updateNotification;

    public static void weatherNotification(Context context, Long locationId) {
        if (!AppPreference.getInstance().isNotificationEnabled(context)) {
            cancelNotification(context, NOTIFICATION_ID);
            updateNotification = null;
            return;
        }

        String notificationPresence = AppPreference.getNotificationPresence(context);
        boolean forecastFilterActive = AppPreference.isNotificationForecastConditionEnabled(context)
                && !"permanent".equals(notificationPresence);
        String forecastAlertSummary = null;
        String channelId = CHANNEL_DEFAULT;

        if (forecastFilterActive) {
            long nowMs = System.currentTimeMillis();
            if (AppPreference.isNotificationForecastQuietHoursEnabled(context)) {
                int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int startHour = AppPreference.getNotificationForecastQuietHoursStart(context);
                int endHour = AppPreference.getNotificationForecastQuietHoursEnd(context);
                if (NotificationForecastEvaluator.isInQuietHours(currentHour, startHour, endHour)) {
                    appendLog(context, TAG, "Forecast alert suppressed due to quiet hours");
                    return;
                }
            }

            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord =
                    WeatherForecastDbHelper.getInstance(context).getWeatherForecast(locationId);
            List<DetailedWeatherForecast> forecastList =
                    (weatherForecastRecord != null && weatherForecastRecord.getCompleteWeatherForecast() != null)
                            ? weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()
                            : null;

            Set<NotificationForecastEvaluator.ConditionType> enabledConditions =
                    NotificationForecastEvaluator.ConditionType.fromKeys(
                            AppPreference.getNotificationForecastConditions(context));
            int lookaheadHours = AppPreference.getNotificationForecastLookaheadHours(context);
            double windThresholdMs = AppPreference.getNotificationForecastWindThresholdMs(context);

            NotificationForecastEvaluator.EvaluationResult evalResult =
                    NotificationForecastEvaluator.evaluateForecast(
                            forecastList, nowMs, lookaheadHours, enabledConditions, windThresholdMs);

            if (!evalResult.isMatched()) {
                appendLog(context, TAG, "Forecast condition not matched, skipping notification");
                cancelNotification(context, FORECAST_ALERT_NOTIFICATION_ID);
                return;
            }

            long lastAlertTimeMs = AppPreference.getNotificationForecastLastAlertTimeMs(context);
            int cooldownMinutes = AppPreference.getNotificationForecastCooldownMinutes(context);
            Set<NotificationForecastEvaluator.ConditionType> lastAlertConditions =
                    NotificationForecastEvaluator.ConditionType.deserialize(
                            AppPreference.getNotificationForecastLastAlertConditions(context));

            if (NotificationForecastEvaluator.isWithinCooldown(
                    nowMs, lastAlertTimeMs, cooldownMinutes, evalResult.getMatchedConditions(), lastAlertConditions)) {
                appendLog(context, TAG, "Forecast alert suppressed due to cooldown");
                return;
            }

            AppPreference.setNotificationForecastLastAlert(
                    context,
                    nowMs,
                    NotificationForecastEvaluator.ConditionType.serialize(evalResult.getMatchedConditions()));
            forecastAlertSummary = formatForecastAlertSummary(context, locationId, evalResult);
            channelId = CHANNEL_FORECAST_ALERT;
        }

        int targetNotificationId = forecastFilterActive ? FORECAST_ALERT_NOTIFICATION_ID : NOTIFICATION_ID;
        Notification notification = getWeatherNotification(context, locationId, channelId, forecastAlertSummary);
        if (notification == null) {
            cancelNotification(context, targetNotificationId);
            if (!forecastFilterActive) {
                updateNotification = null;
            }
            return;
        }
        showNotification(context, targetNotificationId, notification);
    }

    public static Notification getWeatherNotification(Context context, Location location) {
        if (location == null) {
            appendLog(context, TAG, "showNotification - location is null");
            return getNoWeatherNotification(context);
        } else {
            return getWeatherNotification(context, location.getId());
        }
    }

    public static Notification getWeatherNotification(Context context, Long locationId) {
        return getWeatherNotification(context, locationId, CHANNEL_DEFAULT, null);
    }

    public static Notification getWeatherNotification(
            Context context,
            Long locationId,
            String channelId,
            String forecastAlertSummary) {
        if (locationId == null) {
            appendLog(context, TAG, "showNotification - locationId is null");
            return getNoWeatherNotification(context);
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
        return getNotification(context, currentLocation, weatherRecord, channelId, forecastAlertSummary);
    }

    public static Notification getNoWeatherNotification(Context context) {
        updateNotification = new NotificationCompat.Builder(context, "yourLocalWeather")
                .setSmallIcon(R.drawable.ic_refresh_white_18dp_1)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(false)
                .build();
        return updateNotification;
    }

    public static Notification getNotificationForActivity(Context context) {
        checkAndCreateNotificationChannel(context);
        return getWeatherNotification(context, getLocationForNotification(context));
    }

    public static void checkAndCreateNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(CHANNEL_DEFAULT);
        if (notificationChannel == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_DEFAULT,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notification_channel_description));
            channel.setVibrationPattern(isVibrateEnabled(context));
            channel.enableVibration(AppPreference.getInstance().isVibrateEnabled(context));
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        } else if (notificationChannel.getImportance() == NotificationManager.IMPORTANCE_LOW) {
            notificationChannel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
        }

        NotificationChannel alertChannel = notificationManager.getNotificationChannel(CHANNEL_FORECAST_ALERT);
        if (alertChannel == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_FORECAST_ALERT,
                    context.getString(R.string.notification_alert_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.notification_alert_channel_description));
            channel.setVibrationPattern(isVibrateEnabled(context));
            channel.enableVibration(AppPreference.getInstance().isVibrateEnabled(context));
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }

        YourLocalWeather.executor.submit(() -> {
            if (AppPreference.getInstance().isVibrateEnabled(context) && (notificationChannel != null) && (notificationChannel.getVibrationPattern() == null)) {
                notificationChannel.setVibrationPattern(isVibrateEnabled(context));
                notificationChannel.enableVibration(AppPreference.getInstance().isVibrateEnabled(context));
            }
        });
    }

    public static Notification getNotification(Context context, Location location, CurrentWeatherDbHelper.WeatherRecord weatherRecord) {
        return getNotification(context, location, weatherRecord, CHANNEL_DEFAULT, null);
    }

    public static Notification getNotification(
            Context context,
            Location location,
            CurrentWeatherDbHelper.WeatherRecord weatherRecord,
            String channelId,
            String forecastAlertSummary) {

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
            case "custom_with_forecast": notification = customNotification(context, location, notificationIcon, isOutgoing, weatherRecord, channelId); break;
            case "build_in":
            default:
                notification = regularNotification(context, location, notificationIcon, isOutgoing, weatherRecord, channelId, forecastAlertSummary); break;
        }
        return notification;
    }

    public static void showNotification(Context context, Notification notification) {
        showNotification(context, NOTIFICATION_ID, notification);
    }

    public static void showNotification(Context context, int notificationId, Notification notification) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    public static void cancelUpdateNotification(Context context) {
        if (updateNotification != null) {
            cancelNotification(context, NOTIFICATION_ID);
            updateNotification = null;
        }
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
                                                    CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                                    String channelId,
                                                    String forecastAlertSummary) {
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
        String cityAndCountry = Utils.getCityAndCountry(context, location);
        String contentText = (forecastAlertSummary != null)
                ? cityAndCountry + " • " + forecastAlertSummary
                : cityAndCountry;
        return new NotificationCompat.Builder(context, channelId)
                .setContentIntent(launchIntent)
                .setSmallIcon(notificationIcon)
                .setTicker(temperatureWithUnit
                        + "  "
                        + cityAndCountry)
                .setContentTitle(temperatureWithUnit +
                        "  " +
                        Utils.getWeatherDescription(context, weatherRecord.getWeather()))
                .setContentText(contentText)
                .setVibrate(isVibrateEnabled(context))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setLargeIcon(Utils.decodeSampledBitmapFromResource(context.getResources(), Utils.getWeatherResourceIcon(weatherRecord), 192, 192))
                .setOngoing(isOutgoing)
                .build();
    }

    private static Notification customNotification(Context context,
                                                   Location location,
                                                   int notificationIcon,
                                                   boolean isOutgoing,
                                                   CurrentWeatherDbHelper.WeatherRecord weatherRecord,
                                                   String channelId) {

        int textColor = PreferenceUtil.getNotificationTextColor(context);
        String timeStylePreference = AppPreference.getTimeStylePreference(context);
        Weather weather = weatherRecord.getWeather();
        String cityAndCountry = Utils.getCityAndCountry(context, location);
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

        List<DetailedWeatherForecast> weatherForecastList = new ArrayList<>();

        if (weatherForecastRecord != null && weatherForecastRecord.getCompleteWeatherForecast() != null) {
            for (DetailedWeatherForecast detailedWeatherForecast : weatherForecastRecord.getCompleteWeatherForecast().getWeatherForecastList()) {
                if (detailedWeatherForecast != null) {
                    weatherForecastList.add(detailedWeatherForecast);
                }
            }
        }

        for (DetailedWeatherForecast detailedWeatherForecast: weatherForecastList) {

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
                    4L,
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
                    R.id.notification_weather_forecast_expanded_forecast_5_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_6_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_6_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_6_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_7_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_7_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_7_widget_temperatures,
                    null,
                    R.id.notification_weather_forecast_expanded_forecast_8_widget_icon,
                    R.id.notification_weather_forecast_expanded_forecast_8_widget_day,
                    R.id.notification_weather_forecast_expanded_forecast_8_widget_temperatures);
        } catch (Exception e) {
            appendLog(context, TAG, "preLoadWeather:error updating weather forecast", e);
        }
        String lastUpdate = Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, timeStylePreference, location);
        remoteViews.setTextViewText(R.id.notification_custom_weather_widget_last_update, lastUpdate);
        remoteViews.setTextColor(R.id.notification_custom_weather_widget_last_update, textColor);
        remoteViewsExpanded.setTextViewText(R.id.notification_weather_forecast_expanded_widget_last_update, lastUpdate);
        remoteViewsExpanded.setTextColor(R.id.notification_weather_forecast_expanded_widget_last_update, textColor);

        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(notificationIcon)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViewsExpanded)
                .setVibrate(isVibrateEnabled(context))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isOutgoing)
                .setAutoCancel(true)
                .build();
    }

    private static String formatForecastAlertSummary(
            Context context,
            Long locationId,
            NotificationForecastEvaluator.EvaluationResult evalResult) {
        Location location = LocationsDbHelper.getInstance(context).getLocationById(locationId);
        java.util.Locale locale = (location != null) ? location.getLocale() : java.util.Locale.getDefault();
        String timeStr = AppPreference.getLocalizedHour(
                context,
                new Date(evalResult.getFirstMatchTimeMs()),
                locale);

        List<String> labels = new ArrayList<>();
        Set<NotificationForecastEvaluator.ConditionType> matched = evalResult.getMatchedConditions();
        if (matched.contains(NotificationForecastEvaluator.ConditionType.THUNDERSTORM)) {
            labels.add(context.getString(R.string.notification_forecast_condition_thunderstorm));
        }
        if (matched.contains(NotificationForecastEvaluator.ConditionType.RAIN)) {
            labels.add(context.getString(R.string.notification_forecast_condition_rain));
        }
        if (matched.contains(NotificationForecastEvaluator.ConditionType.SNOW)) {
            labels.add(context.getString(R.string.notification_forecast_condition_snow));
        }
        if (matched.contains(NotificationForecastEvaluator.ConditionType.FOG)) {
            labels.add(context.getString(R.string.notification_forecast_condition_fog));
        }
        if (matched.contains(NotificationForecastEvaluator.ConditionType.WIND)) {
            labels.add(context.getString(R.string.notification_forecast_condition_wind));
        }
        String joinedLabels = android.text.TextUtils.join(", ", labels);
        return context.getString(R.string.notification_forecast_alert_summary, timeStr, joinedLabels);
    }

    private static long[] isVibrateEnabled(Context context) {
        if (!AppPreference.getInstance().isVibrateEnabled(context)) {
            return null;
        }
        return new long[]{0, 500, 500};
    }

    public static boolean isScreenLocked(Context context) {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManager.isKeyguardLocked();
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
