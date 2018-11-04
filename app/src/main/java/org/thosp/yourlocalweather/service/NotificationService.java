package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.Utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NotificationService extends AbstractCommonService {

    private static final String TAG = "NotificationsService";

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        appendLog(getBaseContext(), TAG, "onStartCommand:" + intent);

        if (intent == null) {
            return ret;
        }
        switch (intent.getAction()) {
            case "android.intent.action.START_WEATHER_NOTIFICATION_UPDATE": startWeatherCheck(); scheduleNextNotificationAlarm(); return ret;
            case "android.intent.action.SHOW_WEATHER_NOTIFICATION": weatherNotification(); return ret;
            default: return ret;
        }
    }

    public void startWeatherCheck() {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(getBaseContext());
        if (!isNotificationEnabled) {
            return;
        }
        Location currentLocation = getLocationForNotification();
        if (currentLocation == null) {
            return;
        }
        sendMessageToCurrentWeatherService(currentLocation, "NOTIFICATION", AppWakeUpManager.SOURCE_NOTIFICATION, true);
    }

    private void scheduleNextNotificationAlarm() {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(getBaseContext());
        if (!isNotificationEnabled) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        String intervalPref = AppPreference.getInterval(getBaseContext());
        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);
        appendLog(this, TAG, "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    getPendingIntentForNotifiation());
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    getPendingIntentForNotifiation());
        }
    }

    private PendingIntent getPendingIntentForNotifiation() {
        Intent sendIntent = new Intent("android.intent.action.START_WEATHER_NOTIFICATION_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(), 0, sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pendingIntent;
    }

    private void weatherNotification() {
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        Location currentLocation = getLocationForNotification();
        if (currentLocation == null) {
            appendLog(getBaseContext(), TAG, "showNotification - shutdown");
            sendMessageToWakeUpService(
                    AppWakeUpManager.FALL_DOWN,
                    AppWakeUpManager.SOURCE_NOTIFICATION
            );
            return;
        }
        CurrentWeatherDbHelper.WeatherRecord weatherRecord =
                currentWeatherDbHelper.getWeather(currentLocation.getId());

        if (weatherRecord == null) {
            appendLog(getBaseContext(), TAG, "showNotification - shutdown");
            sendMessageToWakeUpService(
                    AppWakeUpManager.FALL_DOWN,
                    AppWakeUpManager.SOURCE_NOTIFICATION
            );
            return;
        }
        Weather weather = weatherRecord.getWeather();
        String temperatureWithUnit = TemperatureUtil.getTemperatureWithUnit(
                this,
                weather,
                currentLocation.getLatitude(),
                weatherRecord.getLastUpdatedTime());

        showNotification(temperatureWithUnit, weather);
    }

    private Location getLocationForNotification() {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }

    private void showNotification(String temperatureWithUnit, Weather weather) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "Your local weather",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Your local weather notifications");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, "default")
                .setContentIntent(launchIntent)
                .setSmallIcon(R.drawable.small_icon)
                .setTicker(temperatureWithUnit
                        + "  "
                        + Utils.getCityAndCountry(this, 0))
                .setContentTitle(temperatureWithUnit +
                        "  " +
                        Utils.getWeatherDescription(this, weather))
                .setContentText(Utils.getCityAndCountry(this, 0))
                .setVibrate(isVibrateEnabled())
                .setAutoCancel(true)
                .build();
        notificationManager.notify(0, notification);
        appendLog(getBaseContext(), TAG, "showNotification - shutdown");
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_NOTIFICATION
        );
    }

    private long[] isVibrateEnabled() {
        if (!AppPreference.isVibrateEnabled(this)) {
            return null;
        }
        return new long[]{500, 500, 500};
    }
}
