package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WindWithUnit;

import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NotificationService extends IntentService {

    private static final String TAG = "NotificationsService";

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        weatherNotification(AppPreference.getWeather(this));
    }

    public static void setNotificationServiceAlarm(Context context,
                                                   boolean isNotificationEnable) {
        Intent intentToCheckWeather = new Intent(context, CurrentWeatherService.class);
        intentToCheckWeather.putExtra("updateSource", "NOTIFICATION");
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intentToCheckWeather,
                                                               PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        String intervalPref = AppPreference.getInterval(context);
        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);
        if (isNotificationEnable) {

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                             SystemClock.elapsedRealtime() + intervalMillis,
                                             intervalMillis,
                                             pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void weatherNotification(Weather weather) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String temperatureWithUnit = AppPreference.getTemperatureWithUnit(this, weather.temperature.getTemp());

        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(launchIntent)
                .setSmallIcon(R.drawable.small_icon)
                .setTicker(temperatureWithUnit
                                   + "  "
                                   + Utils.getCityAndCountry(this))
                .setContentTitle(temperatureWithUnit +
                                 "  " +
                                 Utils.getWeatherDescription(this, weather))
                .setContentText(Utils.getCityAndCountry(this))
                .setVibrate(isVibrateEnabled())
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, notification);
    }

    private long[] isVibrateEnabled() {
        if (!AppPreference.isVibrateEnabled(this)) {
            return null;
        }
        return new long[]{500, 500, 500};
    }
}
