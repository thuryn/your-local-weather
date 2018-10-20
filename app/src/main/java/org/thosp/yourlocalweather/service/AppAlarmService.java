package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AppAlarmService extends AbstractCommonService {

    private static final String TAG = "AppAlarmService";

    private enum BindedServiceActions {
        START_SCREEN, STOP_SCREEN, START_SENSOR, STOP_SENSOR
    }

    private final IBinder binder = new AppAlarmServiceBinder();
    private static Queue<BindedServiceActions> screenOnOffUpdateServiceActions = new LinkedList<>();
    private static Queue<BindedServiceActions> sensorLocationUpdateServiceActions = new LinkedList<>();
    ScreenOnOffUpdateService screenOnOffUpdateService;
    SensorLocationUpdateService sensorLocationUpdateService;
    boolean bound = false;

    public static final long START_SENSORS_CHECK_PERIOD = 3600000; //1 hour
    private volatile boolean alarmStarted;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, ScreenOnOffUpdateService.class);
        bindService(intent, screenOnOffUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        intent = new Intent(this, SensorLocationUpdateService.class);
        bindService(intent, sensorLocationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unbindService(screenOnOffUpdateServiceConnection);
        unbindService(sensorLocationUpdateServiceConnection);
        bound = false;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return ret;
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():" + intent.getAction());
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        if ("org.thosp.yourlocalweather.action.START_ALARM_SERVICE".equals(intent.getAction())) {
            setAlarm();
        } else if ("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE".equals(intent.getAction())) {
            alarmStarted = false;
            setAlarm();
        } else if ("org.thosp.yourlocalweather.action.RESTART_NOTIFICATION_ALARM_SERVICE".equals(intent.getAction())) {
            scheduleNextNotificationAlarm();
        } else if ("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM_AUTO".equals(intent.getAction())) {
            String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
            if (!"0".equals(updateAutoPeriodStr) && !"OFF".equals(updateAutoPeriodStr)) {
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                scheduleNextRegularAlarm(getBaseContext(), true, updateAutoPeriodMills);
            }

            long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
            Intent intentToStartUpdate = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
            intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
            intentToStartUpdate.putExtra("locationId", locationId);
            startBackgroundService(intentToStartUpdate);
        } else if ("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM_REGULAR".equals(intent.getAction())) {
            String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
            long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
            if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
                scheduleNextRegularAlarm(getBaseContext(), false, updatePeriodMills);
            }
            List<Location> locations = locationsDbHelper.getAllRows();
            for (Location location: locations) {
                if (location.getOrderId() == 0) {
                    continue;
                } else {
                    sendMessageToCurrentWeatherService(location, AppWakeUpManager.SOURCE_CURRENT_WEATHER);
                    scheduleNextLocationWeatherForecastUpdate(location.getId());
                }
            }
        }
        return ret;
    }

    public void setAlarm() {
        /*if (alarmStarted) {
            return;
        }*/
        alarmStarted = true;
        cancelAlarm(true);
        cancelAlarm(false);
        sendScreenStartIntent();
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        appendLog(getBaseContext(), TAG, "setAlarm:" + updatePeriodStr + ":" + updateAutoPeriodStr);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        appendLog(getBaseContext(), TAG, "locationsDbHelper.getLocationByOrderId(0):"
                + locationsDbHelper.getLocationByOrderId(0));
        appendLog(getBaseContext(), TAG, "locationsDbHelper.getLocationByOrderId(0).isEnabled()"
                + locationsDbHelper.getLocationByOrderId(0).isEnabled());
        if (locationsDbHelper.getLocationByOrderId(0).isEnabled()) {
            if ("0".equals(updateAutoPeriodStr)) {
                sendSensorStartIntent();
                /*alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingSensorStartIntent(getBaseContext()));
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingScreenStartIntent(getBaseContext()));*/
            } else if (!"OFF".equals(updateAutoPeriodStr)) {
                sendSensorAndScreenStopIntent();
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                appendLog(getBaseContext(), TAG, "next alarm:" + updateAutoPeriodMills);
                scheduleNextRegularAlarm(getBaseContext(), true, updateAutoPeriodMills);
            } else {
                sendSensorAndScreenStopIntent();
            }
        }
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            scheduleNextRegularAlarm(getBaseContext(), false, updatePeriodMills);
        }
        scheduleNextNotificationAlarm();
    }

    public void cancelAlarm(boolean autoLocation) {
        appendLog(getBaseContext(), TAG, "cancelAlarm");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(getBaseContext(), autoLocation));
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

    private static void scheduleNextRegularAlarm(Context context, boolean autoLocation, long updatePeriodMilis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        appendLog(context, TAG, "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + updatePeriodMilis,
                    getPendingIntent(context, autoLocation));
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + updatePeriodMilis,
                    getPendingIntent(context, autoLocation));
        }
    }

    private void scheduleNextLocationWeatherForecastUpdate(long locationId) {
        if (!ForecastUtil.shouldUpdateForecast(this, locationId)) {
            return;
        }
        sendMessageToWeatherForecastService(locationId);
    }

    private void sendSensorStartIntent() {
        appendLog(getBaseContext(), TAG, "sensorLocationUpdateService.startSensorBasedUpdates");
        if (sensorLocationUpdateService == null) {
            sensorLocationUpdateServiceActions.add(BindedServiceActions.START_SENSOR);
            return;
        }
        sensorLocationUpdateService.startSensorBasedUpdates(0);
    }

    private static PendingIntent getPendingSensorStartIntent(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void sendScreenStartIntent() {
        appendLog(getBaseContext(), TAG, "screenOnOffUpdateService.startSensorBasedUpdates");
        if (screenOnOffUpdateService == null) {
            screenOnOffUpdateServiceActions.add(BindedServiceActions.START_SCREEN);
            return;
        }
        screenOnOffUpdateService.startSensorBasedUpdates(0);
    }

    private static PendingIntent getPendingScreenStartIntent(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_SCREEN_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void sendSensorAndScreenStopIntent() {
        appendLog(getBaseContext(), TAG, "sendSensorAndScreenStopIntent");
        if (screenOnOffUpdateService == null) {
            screenOnOffUpdateServiceActions.add(BindedServiceActions.STOP_SCREEN);
        } else {
            screenOnOffUpdateService.stopSensorBasedUpdates();
        }
        if (sensorLocationUpdateService == null) {
            sensorLocationUpdateServiceActions.add(BindedServiceActions.STOP_SENSOR);
        } else {
            sensorLocationUpdateService.stopSensorBasedUpdates();
        }
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingSensorStartIntent(getBaseContext()));
        alarmManager.cancel(getPendingScreenStartIntent(getBaseContext()));
    }

    private static PendingIntent getPendingIntent(Context context, boolean autoLocation) {
        Intent intent;
        if (autoLocation) {
            intent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM_AUTO");
        } else {
            intent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM_REGULAR");
        }
        intent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void startBackgroundService(Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(),
                1,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime()+10,
                pendingIntent);
    }

    public boolean isAlarmOff() {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                                                                 0,
                                                                 intent,
                                                                 PendingIntent.FLAG_NO_CREATE);
        return pendingIntent == null;
    }

    private ServiceConnection screenOnOffUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder binder =
                    (ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder) service;
            screenOnOffUpdateService = binder.getService();
            BindedServiceActions bindedServiceActions;
            while ((bindedServiceActions = screenOnOffUpdateServiceActions.poll()) != null) {
                switch (bindedServiceActions) {
                    case START_SCREEN:
                        screenOnOffUpdateService.startSensorBasedUpdates(0);
                        break;
                    case STOP_SCREEN:
                        screenOnOffUpdateService.stopSensorBasedUpdates();
                        break;
                }
            }
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    private ServiceConnection sensorLocationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SensorLocationUpdateService.SensorLocationUpdateServiceBinder binder =
                    (SensorLocationUpdateService.SensorLocationUpdateServiceBinder) service;
            sensorLocationUpdateService = binder.getService();
            BindedServiceActions bindedServiceActions;
            while ((bindedServiceActions = sensorLocationUpdateServiceActions.poll()) != null) {
                switch (bindedServiceActions) {
                    case START_SENSOR:
                        sensorLocationUpdateService.startSensorBasedUpdates(0);
                        break;
                    case STOP_SCREEN:
                        sensorLocationUpdateService.stopSensorBasedUpdates();
                        break;
                }
            }
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    public class AppAlarmServiceBinder extends Binder {
        public AppAlarmService getService() {
            return AppAlarmService.this;
        }
    }
}
