package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.Utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NotificationService extends AbstractCommonService {

    private static final String TAG = "NotificationsService";

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        appendLog(getBaseContext(), TAG, "onStartCommand:", intent);

        if (intent == null) {
            return ret;
        }
        switch (intent.getAction()) {
            case "android.intent.action.START_WEATHER_NOTIFICATION_UPDATE": startWeatherCheck(); scheduleNextNotificationAlarm(); return ret;
            default: return ret;
        }
    }

    public void startWeatherCheck() {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        boolean updateBySensor = "0".equals(updateAutoPeriodStr);
        if (!isNotificationEnabled || updateBySensor) {
            return;
        }
        Location currentLocation = NotificationUtils.getLocationForNotification(this);
        if (currentLocation == null) {
            return;
        }
        sendMessageToCurrentWeatherService(currentLocation, "NOTIFICATION", AppWakeUpManager.SOURCE_NOTIFICATION, true, true);
    }

    private void scheduleNextNotificationAlarm() {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(getBaseContext());
        if (!isNotificationEnabled) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        String intervalPref = AppPreference.getInterval(getBaseContext());
        if ("regular_only".equals(intervalPref)) {
            return;
        }
        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);
        appendLog(this, TAG, "Build.VERSION.SDK_INT:", Build.VERSION.SDK_INT);
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
}
