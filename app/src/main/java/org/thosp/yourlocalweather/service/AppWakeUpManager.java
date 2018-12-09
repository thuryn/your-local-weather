package org.thosp.yourlocalweather.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetProvider;
import org.thosp.yourlocalweather.widget.ExtLocationWithForecastWidgetProvider;
import org.thosp.yourlocalweather.widget.LessWidgetProvider;
import org.thosp.yourlocalweather.widget.MoreWidgetProvider;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

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
    private static List<Integer> wakeUpSources = new ArrayList<>();
    private Lock wakeUpSourcesLock = new ReentrantLock();
    final Messenger messenger = new Messenger(new PowerUpMessageHandler());

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
        return messenger.getBinder();
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
            printWakeupSources("startWakeUp");
            if ((wakeLock != null) && wakeLock.isHeld()) {
                appendLog(getBaseContext(), TAG,"wakeUp started");
                return;
            }
            wakeUp();
            appendLog(getBaseContext(), TAG,"start wakeup");
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception starting wakeup:" + e.getMessage(), e);
        } finally {
            wakeUpSourcesLock.unlock();
        }
    }

    private void stopWakeUp(Integer wakeUpSource) {
        wakeUpSourcesLock.lock();
        try {
            if (wakeUpSources.contains(wakeUpSource)) {
                wakeUpSources.remove(wakeUpSource);
            }
            appendLog(getBaseContext(), TAG, "wakeUpSources.size:" + wakeUpSources.size());
            printWakeupSources("stopWakeUp");
            if (!wakeUpSources.isEmpty()) {
                return;
            }
            wakeDown();
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception stoping wakeup:" + e.getMessage(), e);
        } finally {
            wakeUpSourcesLock.unlock();
        }
    }

    public void wakeDown() {
        timerWakeUpHandler.removeCallbacksAndMessages(null);
        appendLog(getBaseContext(), TAG, "wakeDown wakeLock:" + wakeLock);
        if ((wakeLock != null) && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                appendLog(getBaseContext(), TAG, "wakeLock released");
            } catch (Throwable th) {
                // ignoring this exception, probably wakeLock was already released
            }
        }
    }

    public void wakeUp() {
        appendLog(getBaseContext(), TAG, "powerManager:" + powerManager);

        boolean isInUse;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isInUse = powerManager.isInteractive();
        } else {
            isInUse = powerManager.isScreenOn();
        }

        if (isInUse || ((wakeLock != null) && wakeLock.isHeld())) {
            appendLog(getBaseContext(), TAG, "lock is held");
            return;
        }

        timerWakeUpHandler.postDelayed(timerWakeUpRunnable, WAKEUP_TIMEOUT_IN_MS);

        String wakeUpStrategy = PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                .getString(Constants.KEY_WAKE_UP_STRATEGY, "nowakeup");
        appendLog(getBaseContext(), TAG, "wakeLock:wakeUpStrategy:" + wakeUpStrategy);
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
        appendLog(getBaseContext(), TAG, "wakeLock:powerLockID:" + powerLockID);
        wakeLock = powerManager.newWakeLock(powerLockID, "YourLocalWeather:PowerLock");
        appendLog(getBaseContext(), TAG, "wakeLock:" + wakeLock + ":" + wakeLock.isHeld());
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        appendLog(getBaseContext(), TAG, "wakeLock acquired");
    }

    private void printWakeupSources(String wakeupdown) {
        StringBuilder wakeupSourcesList = new StringBuilder();
        wakeupSourcesList.append(wakeupdown);
        wakeupSourcesList.append(", WakeUp source list: ");
        for (Integer wakeupSource: wakeUpSources) {
            wakeupSourcesList.append(wakeupSource);
            wakeupSourcesList.append(",");
        }
        appendLog(getBaseContext(), TAG, "wakeUpSources:" + wakeupSourcesList.toString());
    }

    private class PowerUpMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int wakeUpSource = msg.arg1;
            appendLog(getBaseContext(), TAG, "handleMessage:" + msg.what + ":" + wakeUpSource);
            switch (msg.what) {
                case WAKE_UP:
                    startWakeUp(wakeUpSource);
                    break;
                case FALL_DOWN:
                    stopWakeUp(wakeUpSource);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
