package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
import android.app.ProgressDialog;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WidgetRefreshIconService extends IntentService {

    private static final String TAG = "WidgetRefreshIconService";

    private static long ROTATE_UPDATE_ICON_MILIS = 100;

    private volatile static Lock rotationLock = new ReentrantLock();
    private final int[] refreshIcons = new int[8];
    private volatile int currentRotationIndex;
    public volatile static boolean isRotationActive = false;

    private final Map<ComponentName, Integer> widgetTypes = new HashMap<>();

    private PowerManager powerManager;

    public WidgetRefreshIconService() {
        super(TAG);
        refreshIcons[0] = R.drawable.ic_refresh_white_18dp;
        refreshIcons[1] = R.drawable.ic_refresh_white_18dp_1;
        refreshIcons[2] = R.drawable.ic_refresh_white_18dp_2;
        refreshIcons[3] = R.drawable.ic_refresh_white_18dp_3;
        refreshIcons[4] = R.drawable.ic_refresh_white_18dp_4;
        refreshIcons[5] = R.drawable.ic_refresh_white_18dp_5;
        refreshIcons[6] = R.drawable.ic_refresh_white_18dp_6;
        refreshIcons[7] = R.drawable.ic_refresh_white_18dp_7;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        widgetTypes.put(new ComponentName(this, ExtLocationWidgetProvider.class), R.layout.widget_ext_loc_3x3);
        widgetTypes.put(new ComponentName(this, MoreWidgetProvider.class), R.layout.widget_more_3x3);
        widgetTypes.put(new ComponentName(this, LessWidgetProvider.class), R.layout.widget_less_3x1);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        IntentFilter filterScreenOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
        getApplication().registerReceiver(screenOnReceiver, filterScreenOn);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        switch (action) {
            case "android.intent.action.START_ROTATING_UPDATE":
                startRotatingUpdateIcon();
                break;
            case "android.intent.action.STOP_ROTATING_UPDATE":
                stopRotatingUpdateIcon();
        }
    }

    private void startRotatingUpdateIcon() {
        appendLog(getBaseContext(), TAG, "startRotatingUpdateIcon");
        rotationLock.lock();
        appendLog(getBaseContext(), TAG, "startRotatingUpdateIcon:lockAcquired");
        try {
            if (isRotationActive || isThereRotationSchedule()) {
                appendLog(getBaseContext(), TAG,
                        "startRotatingUpdateIcon:endOnCondition:isRotationActive=" +
                        isRotationActive + ":isThereRotationSchedule=" +
                        isThereRotationSchedule());
                return;
            }
            isRotationActive = true;
            currentRotationIndex = 0;
            rotateRefreshButtonOneStep();
            appendLog(getBaseContext(), TAG,
                    "startRotatingUpdateIcon:setIsRotationActive=" +
                            isRotationActive + ":postingNewSchedule");
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        } finally {
            rotationLock.unlock();
            appendLog(getBaseContext(), TAG,
                    "startRotatingUpdateIcon:lockReleased");
        }
    }

    private void stopRotatingUpdateIcon() {
        appendLog(getBaseContext(), TAG, "stopRotatingUpdateIcon");
        rotationLock.lock();
        appendLog(getBaseContext(), TAG, "stopRotatingUpdateIcon:lockAcquired");
        try {
            isRotationActive = false;
            appendLog(getBaseContext(), TAG,
                    "stopRotatingUpdateIcon:setIsRotationActive=" +
                            isRotationActive + ":postingNewSchedule");
            timerRotateIconHandler.removeCallbacksAndMessages(null);
        } finally {
            rotationLock.unlock();
            appendLog(getBaseContext(), TAG,
                    "stopRotatingUpdateIcon:lockReleased");
        }
    }

    private static boolean isRotationActive() {
        return isRotationActive;
    }

    public static boolean isThereRotationSchedule() {
        return timerRotateIconHandler.hasMessages(0);
    }

    private boolean isScreenOn() {
        return powerManager.isScreenOn();
    }

    private void rotateRefreshButtonOneStep() {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        for (ComponentName componentName: widgetTypes.keySet()) {
            RemoteViews rv = new RemoteViews(this.getPackageName(), widgetTypes.get(componentName));

            int[] widgetIds = widgetManager.getAppWidgetIds(componentName);
            for (int appWidgetId : widgetIds) {
                rv.setImageViewResource(R.id.widget_button_refresh, refreshIcons[currentRotationIndex]);
                widgetManager.partiallyUpdateAppWidget(appWidgetId, rv);
            }
        }
        currentRotationIndex++;
        if (currentRotationIndex >= refreshIcons.length) {
            currentRotationIndex = 0;
        }
    }

    private static Handler timerRotateIconHandler = new Handler();
    Runnable timerRotateIconRunnable = new Runnable() {

        @Override
        public void run() {
            rotationLock.lock();
            try {
                if (!isScreenOn() || !isRotationActive() || isThereRotationSchedule()) {
                    return;
                }
                rotateRefreshButtonOneStep();
                timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
            } finally {
                rotationLock.unlock();
            }
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            rotationLock.lock();
            try {
                if (!isRotationActive() || isThereRotationSchedule()) {
                    return;
                }
                rotateRefreshButtonOneStep();
                timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
            } finally {
                rotationLock.unlock();
            }
        }
    };
}
