package org.thosp.yourlocalweather.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WidgetRefreshIconService extends Service {

    private static final String TAG = "WidgetRefreshIconService";

    public static final int START_ROTATING_UPDATE = 1;
    public static final int STOP_ROTATING_UPDATE = 2;

    private static long ROTATE_UPDATE_ICON_MILIS = 100;

    private final int[] refreshIcons = new int[8];
    private volatile static Map<Integer, Integer> currentRotationIndexes = new HashMap<>();
    public volatile static boolean isRotationActive = false;
    private static List<Integer> rotationSources = new ArrayList<>();
    private Lock rotationSourcesLock = new ReentrantLock();

    private final Map<ComponentName, Integer> widgetTypes = new HashMap<>();

    private PowerManager powerManager;
    final Messenger messenger = new Messenger(new RefreshIconMessageHandler());

    public WidgetRefreshIconService() {
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
        widgetTypes.put(new ComponentName(this, ExtLocationWithForecastGraphWidgetProvider.class), R.layout.widget_ext_loc_forecast_graph_3x3);
        //widgetTypes.put(new ComponentName(this, WeatherForecastWidgetProvider.class), R.layout.widget_less_3x1);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        IntentFilter filterScreenOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
        getApplication().registerReceiver(screenOnReceiver, filterScreenOn);
    }

    @Override
    public void onDestroy() {
        getApplication().unregisterReceiver(screenOnReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void startRotatingUpdateIcon(Integer rotationSource) {
        //appendLog(getBaseContext(), TAG, "startRotatingUpdateIcon:" + rotationSource);
        rotationSourcesLock.lock();
        try {
            if (!rotationSources.contains(rotationSource)) {
                rotationSources.add(rotationSource);
            }
            printRotationSources();
            if (WidgetRefreshIconService.isRotationActive) {
                appendLog(getBaseContext(), TAG,
                        "startRotatingUpdateIcon:endOnCondition:isRotationActive=",
                                WidgetRefreshIconService.isRotationActive, ":isThereRotationSchedule=");
                return;
            }
            WidgetRefreshIconService.isRotationActive = true;
            rotateRefreshButtonOneStep();
            appendLog(getBaseContext(), TAG,
                    "startRotatingUpdateIcon:setIsRotationActive=",
                            WidgetRefreshIconService.isRotationActive, ":postingNewSchedule");
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception storting rotation:", e);
        } finally {
            rotationSourcesLock.unlock();
        }
    }

    private void stopRotatingUpdateIcon(Integer rotationSource) {
        //appendLog(getBaseContext(), TAG, "stopRotatingUpdateIcon:" + rotationSource);
        rotationSourcesLock.lock();
        try {
            //appendLog(getBaseContext(), TAG, "stopRotatingUpdateIcon:rotationSources.contains(rotationSource):" + rotationSources.contains(rotationSource));
            if (rotationSources.contains(rotationSource)) {
                rotationSources.remove(rotationSource);
            }
            printRotationSources();
            if (!rotationSources.isEmpty()) {
                return;
            }
            WidgetRefreshIconService.isRotationActive = false;
            appendLog(getBaseContext(), TAG,
                    "stopRotatingUpdateIcon:setIsRotationActive=",
                            WidgetRefreshIconService.isRotationActive, ":postingNewSchedule");
            timerRotateIconHandler.removeCallbacksAndMessages(null);
            WidgetUtils.updateWidgets(getBaseContext());
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception stoping rotation:", e);
        } finally {
            rotationSourcesLock.unlock();
        }
    }

    private void printRotationSources() {
        for (int rotationSourceForLog: rotationSources) {
            appendLog(getBaseContext(), TAG, "RotationSource:", rotationSourceForLog);
        }
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
                rv.setImageViewResource(R.id.widget_ext_loc_3x3_widget_button_refresh, refreshIcons[currentRotationIndex]);
                rv.setImageViewResource(R.id.widget_ext_loc_forecast_3x3_widget_button_refresh, refreshIcons[currentRotationIndex]);
                rv.setImageViewResource(R.id.widget_ext_loc_graph_3x3_widget_button_refresh, refreshIcons[currentRotationIndex]);
                rv.setImageViewResource(R.id.widget_less_3x1_widget_button_refresh, refreshIcons[currentRotationIndex]);
                rv.setImageViewResource(R.id.widget_more_3x3_widget_button_refresh, refreshIcons[currentRotationIndex]);
                widgetManager.partiallyUpdateAppWidget(appWidgetId, rv);
                WidgetRefreshIconService.currentRotationIndexes.put(appWidgetId, ++currentRotationIndex);
            }
        }
    }

    private static Handler timerRotateIconHandler = new Handler();
    Runnable timerRotateIconRunnable = new Runnable() {

        @Override
        public void run() {
            if (!isScreenOn() || !WidgetRefreshIconService.isRotationActive) {
                return;
            }
            rotateRefreshButtonOneStep();
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!WidgetRefreshIconService.isRotationActive) {
                return;
            }
            rotateRefreshButtonOneStep();
            timerRotateIconHandler.postDelayed(timerRotateIconRunnable, ROTATE_UPDATE_ICON_MILIS);
        }
    };

    protected boolean isInteractive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }

    protected void startBackgroundService(Intent intent) {
        try {
            if (isInteractive()) {
                getBaseContext().startService(intent);
                return;
            }
        } catch (Exception ise) {
            //
        }
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10,
                    pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10,
                    pendingIntent);
        }
    }

    private class RefreshIconMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            appendLog(getBaseContext(), TAG, "handleMessage:", msg.what);
            int rotationSource = msg.arg1;
            switch (msg.what) {
                case START_ROTATING_UPDATE:
                    startRotatingUpdateIcon(rotationSource);
                    break;
                case STOP_ROTATING_UPDATE:
                    stopRotatingUpdateIcon(rotationSource);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
