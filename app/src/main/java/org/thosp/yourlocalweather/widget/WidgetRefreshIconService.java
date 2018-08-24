package org.thosp.yourlocalweather.widget;

import android.app.IntentService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WidgetRefreshIconService extends IntentService {

    private static final String TAG = "WidgetRefreshIconService";

    private static long ROTATE_UPDATE_ICON_MILIS = 100;

    private final int[] refreshIcons = new int[8];
    private volatile static Map<Integer, Integer> currentRotationIndexes = new HashMap<>();
    public volatile static boolean isRotationActive = false;
    private static List<Integer> rotationSources = new ArrayList<>();
    private Lock rotationSourcesLock = new ReentrantLock();

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
        widgetTypes.put(new ComponentName(this, ExtLocationWithForecastWidgetProvider.class), R.layout.widget_less_3x1);
        //widgetTypes.put(new ComponentName(this, WeatherForecastWidgetProvider.class), R.layout.widget_less_3x1);
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
        int rotationSource = 0;
        if (intent.hasExtra("rotationSource")) {
            rotationSource = intent.getIntExtra("rotationSource", 0);
        }
        switch (action) {
            case "android.intent.action.START_ROTATING_UPDATE":
                startRotatingUpdateIcon(rotationSource);
                break;
            case "android.intent.action.STOP_ROTATING_UPDATE":
                stopRotatingUpdateIcon(rotationSource);
        }
    }

    private void startRotatingUpdateIcon(int rotationSource) {
        appendLog(getBaseContext(), TAG, "startRotatingUpdateIcon");
        rotationSourcesLock.lock();
        try {
            if (!rotationSources.contains(rotationSource)) {
                rotationSources.add(rotationSource);
            }
            if (WidgetRefreshIconService.isRotationActive || isThereRotationSchedule()) {
                appendLog(getBaseContext(), TAG,
                        "startRotatingUpdateIcon:endOnCondition:isRotationActive=" +
                                WidgetRefreshIconService.isRotationActive + ":isThereRotationSchedule=" +
                                isThereRotationSchedule());
                return;
            }
            WidgetRefreshIconService.isRotationActive = true;
            rotateRefreshButtonOneStep();
            appendLog(getBaseContext(), TAG,
                    "startRotatingUpdateIcon:setIsRotationActive=" +
                            WidgetRefreshIconService.isRotationActive + ":postingNewSchedule");
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        } finally {
            rotationSourcesLock.unlock();
        }
    }

    private void stopRotatingUpdateIcon(int rotationSource) {
        appendLog(getBaseContext(), TAG, "stopRotatingUpdateIcon");
        rotationSourcesLock.lock();
        try {
            if (rotationSources.contains(rotationSource)) {
                rotationSources.remove(rotationSource);
            }
            if (!rotationSources.isEmpty()) {
                return;
            }
            WidgetRefreshIconService.isRotationActive = false;
            appendLog(getBaseContext(), TAG,
                    "stopRotatingUpdateIcon:setIsRotationActive=" +
                            WidgetRefreshIconService.isRotationActive + ":postingNewSchedule");
            timerRotateIconHandler.removeCallbacksAndMessages(null);
        } finally {
            rotationSourcesLock.unlock();
        }
    }

    public boolean isThereRotationSchedule() {
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
                Integer currentRotationIndex = WidgetRefreshIconService.currentRotationIndexes.get(appWidgetId);
                if ((currentRotationIndex == null) || (currentRotationIndex >= refreshIcons.length)) {
                    currentRotationIndex = 0;
                }
                rv.setImageViewResource(R.id.widget_button_refresh, refreshIcons[currentRotationIndex]);
                widgetManager.partiallyUpdateAppWidget(appWidgetId, rv);
                WidgetRefreshIconService.currentRotationIndexes.put(appWidgetId, ++currentRotationIndex);
            }
        }
    }

    private static Handler timerRotateIconHandler = new Handler();
    Runnable timerRotateIconRunnable = new Runnable() {

        @Override
        public void run() {
            if (!isScreenOn() || !WidgetRefreshIconService.isRotationActive || isThereRotationSchedule()) {
                return;
            }
            rotateRefreshButtonOneStep();
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!WidgetRefreshIconService.isRotationActive || isThereRotationSchedule()) {
                return;
            }
            rotateRefreshButtonOneStep();
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        }
    };
}
