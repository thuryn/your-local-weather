package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AppWakeUpManager {

    private static final String TAG = "AppWakeUpManager";

    private static final long WAKEUP_TIMEOUT_IN_MS = 30000L;

    private PowerManager.WakeLock wakeLock;
    private PowerManager powerManager;
    private Context context;
    private static AppWakeUpManager instance;

    Handler timerWakeUpHandler = new Handler();
    Runnable timerWakeUpRunnable = new Runnable() {

        @Override
        public void run() {
            wakeDown();
        }
    };

    public static AppWakeUpManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new AppWakeUpManager(ctx.getApplicationContext());
            instance.powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        }
        return instance;
    }

    private AppWakeUpManager(Context context) {
        this.context = context;
    }

    public void wakeDown() {
        timerWakeUpHandler.removeCallbacksAndMessages(null);
        if (wakeLock != null) {
            try {
                wakeLock.release();
                appendLog(context, TAG, "wakeLock released");
            } catch (Throwable th) {
                // ignoring this exception, probably wakeLock was already released
            }
        }
    }

    public void wakeUp() {
        appendLog(context, TAG, "powerManager:" + powerManager);

        boolean isInUse;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            isInUse = powerManager.isInteractive();
        } else {
            isInUse = powerManager.isScreenOn();
        }

        if (isInUse || ((wakeLock != null) && wakeLock.isHeld())) {
            return;
        }

        timerWakeUpHandler.postDelayed(timerWakeUpRunnable, WAKEUP_TIMEOUT_IN_MS);

        String wakeUpStrategy = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.KEY_WAKE_UP_STRATEGY, "nowakeup");

        appendLog(context, TAG, "wakeLock:wakeUpStrategy:" + wakeUpStrategy);

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

        appendLog(context, TAG, "wakeLock:powerLockID:" + powerLockID);

        wakeLock = powerManager.newWakeLock(powerLockID, TAG);
        appendLog(context, TAG, "wakeLock:" + wakeLock + ":" + wakeLock.isHeld());
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        appendLog(context, TAG, "wakeLock acquired");
    }
}
