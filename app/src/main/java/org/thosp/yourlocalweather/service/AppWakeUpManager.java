package org.thosp.yourlocalweather.service;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogWakeupSources;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.NotificationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AppWakeUpManager extends Service {

    private static final String TAG = "AppWakeUpManager";

    public static final int WAKE_UP = 1;
    public static final int FALL_DOWN = 2;

    public static final int SOURCE_CURRENT_WEATHER = 1;
    public static final int SOURCE_WEATHER_FORECAST = 2;
    public static final int SOURCE_NOTIFICATION = 3;
    public static final int SOURCE_LOCATION_UPDATE = 4;

    private static final long WAKEUP_TIMEOUT_IN_MS = 30000L;

    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;
    final private static List<Integer> wakeUpSources = new ArrayList<>();
    final private Lock wakeUpSourcesLock = new ReentrantLock();

    Handler timerWakeUpHandler = new Handler();
    Runnable timerWakeUpRunnable = new Runnable() {

        @Override
        public void run() {
            wakeDown();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return ret;
        }
        Notification notification = NotificationUtils.getNotificationForActivity(getBaseContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationUtils.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID, notification);
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():", intent.getAction());
        switch (intent.getAction()) {
            case "org.thosp.yourlocalweather.action.WAKE_UP":
                startWakeUp(intent.getIntExtra("wakeupSource", 0));
                return ret;
            case "org.thosp.yourlocalweather.action.FALL_DOWN":
                stopWakeUp(intent.getIntExtra("wakeupSource", 0));
                return ret;
            default:
        }
        return ret;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    private void startWakeUp(Integer wakeUpSource) {
        wakeUpSourcesLock.lock();
        try {
            if (!wakeUpSources.contains(wakeUpSource)) {
                wakeUpSources.add(wakeUpSource);
            }
            appendLogWakeupSources(getBaseContext(), TAG, "startWakeUp:", wakeUpSources);
            if ((wakeLock != null) && wakeLock.isHeld()) {
                appendLog(getBaseContext(), TAG,"wakeUp started");
                return;
            }
            wakeUp();
            appendLog(getBaseContext(), TAG,"start wakeup");
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception starting wakeup", e);
        } finally {
            wakeUpSourcesLock.unlock();
        }
    }

    private void stopWakeUp(Integer wakeUpSource) {
        wakeUpSourcesLock.lock();
        try {
            wakeUpSources.remove(wakeUpSource);
            appendLogWakeupSources(getBaseContext(), TAG, "stopWakeUp:", wakeUpSources);
            if (!wakeUpSources.isEmpty()) {
                return;
            }
            wakeDown();
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception stoping wakeup", e);
        } finally {
            wakeUpSourcesLock.unlock();
        }
    }

    public void wakeDown() {
        timerWakeUpHandler.removeCallbacksAndMessages(null);
        appendLog(getBaseContext(), TAG, "wakeDown wakeLock:", wakeLock);
        if ((wakeLock != null) && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                appendLog(getBaseContext(), TAG, "wakeLock released");
            } catch (Throwable th) {
                // ignoring this exception, probably wakeLock was already released
            }
        }
        NotificationUtils.cancelUpdateNotification(getBaseContext());
        stopForeground(true);
    }

    public void wakeUp() {
        appendLog(getBaseContext(), TAG, "powerManager:", powerManager);
        if (powerManager.isInteractive() || ((wakeLock != null) && wakeLock.isHeld())) {
            appendLog(getBaseContext(), TAG, "lock is held");
            return;
        }

        timerWakeUpHandler.postDelayed(timerWakeUpRunnable, WAKEUP_TIMEOUT_IN_MS);

        String wakeUpStrategy = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getString(Constants.KEY_WAKE_UP_STRATEGY, "nowakeup");
        appendLog(getBaseContext(), TAG, "wakeLock:wakeUpStrategy:", wakeUpStrategy);
        if (wakeLock != null) {
            try {
                wakeLock.release();
            } catch (Throwable th) {
                // ignoring this exception, probably wakeLock was already released
            }
        }
        if ("nowakeup".equals(wakeUpStrategy)) {
            return;
        }
        int powerLockID;
        if ("wakeupfull".equals(wakeUpStrategy)) {
            powerLockID = PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP;
        } else {
            powerLockID = PowerManager.PARTIAL_WAKE_LOCK;
        }
        appendLog(getBaseContext(), TAG, "wakeLock:powerLockID:", powerLockID);
        wakeLock = powerManager.newWakeLock(powerLockID, "YourLocalWeather:PowerLock");
        appendLog(getBaseContext(), TAG, "wakeLock:", wakeLock, ":", wakeLock.isHeld());
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(10000L);
        }
        appendLog(getBaseContext(), TAG, "wakeLock acquired");
    }
}
