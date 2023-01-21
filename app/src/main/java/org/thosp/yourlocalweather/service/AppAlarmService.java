package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

public class AppAlarmService extends AbstractCommonService {

    private static final String TAG = "AppAlarmService";

    public static final long START_SENSORS_CHECK_PERIOD = 3600000; //1 hour
    private volatile boolean alarmStarted;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return ret;
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():", intent.getAction());

        if ("org.thosp.yourlocalweather.action.START_ALARM_SERVICE".equals(intent.getAction())) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    setAlarm();
                }
            };
            thread.start();
        } else if ("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE".equals(intent.getAction())) {
            alarmStarted = false;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    setAlarm();
                }
            };
            thread.start();
        } else if ("org.thosp.yourlocalweather.action.RESTART_NOTIFICATION_ALARM_SERVICE".equals(intent.getAction())) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    scheduleNextNotificationAlarm();
                }
            };
            thread.start();
        } else if ("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM_AUTO".equals(intent.getAction())) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    startLocationWeatherAlarmAuto();
                }
            };
            thread.start();
        } else if ("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM_REGULAR".equals(intent.getAction())) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    startLocationWeatherAlarmRegular();
                }
            };
            thread.start();
        }
        return ret;
    }

    private void startLocationWeatherAlarmRegular() {
        Calendar nowInCalendar = Calendar.getInstance();
        boolean locationUpdateNight = AppPreference.getLocationUpdateNight(getBaseContext());

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            scheduleNextRegularAlarm(getBaseContext(), false, updatePeriodMills);
        }
        List<Location> locations = locationsDbHelper.getAllRows();
        for (Location location: locations) {
            if ((location.getOrderId() == 0) || ((nowInCalendar.get(Calendar.HOUR_OF_DAY) < 6) && locationUpdateNight)) {
                continue;
            } else {
                sendMessageToCurrentWeatherService(location, AppWakeUpManager.SOURCE_CURRENT_WEATHER, true);
                scheduleNextLocationWeatherForecastUpdate(location.getId());
            }
        }
    }

    private void startLocationWeatherAlarmAuto() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        if (!"0".equals(updateAutoPeriodStr) && !"OFF".equals(updateAutoPeriodStr)) {
            long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
            scheduleNextRegularAlarm(getBaseContext(), true, updateAutoPeriodMills);
        }

        Calendar nowInCalendar = Calendar.getInstance();
        if ((nowInCalendar.get(Calendar.HOUR_OF_DAY) < 6) && AppPreference.getLocationAutoUpdateNight(getBaseContext())) {
            return;
        }

        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
    }

    public void setAlarm() {
        /*if (alarmStarted) {
            return;
        }*/
        alarmStarted = true;
        cancelAlarm(true);
        cancelAlarm(false);
        startScreenOnOffUpdates();
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        appendLog(getBaseContext(), TAG, "setAlarm:", updatePeriodStr, ":", updateAutoPeriodStr);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        appendLog(getBaseContext(), TAG, "locationsDbHelper.getLocationByOrderId(0):", autoLocation);
        if ((autoLocation != null) && autoLocation.isEnabled()) {
            if ("0".equals(updateAutoPeriodStr)) {
                startSensorBasedUpdates();
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingSensorStartIntent(getBaseContext()));
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingScreenStartIntent(getBaseContext()));
            } else if (!"OFF".equals(updateAutoPeriodStr)) {
                sendSensorAndScreenStopIntent();
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                appendLog(getBaseContext(), TAG, "next alarm:", updateAutoPeriodMills);
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
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (!isNotificationEnabled) {
            alarmManager.cancel(getPendingIntentForNotifiation());
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            return;
        }

        String intervalPref = AppPreference.getInterval(getBaseContext());
        if ("regular_only".equals(intervalPref)) {
            return;
        }
        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);
        appendLog(this, TAG, "Build.VERSION.SDK_INT:", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    getPendingIntentForNotifiation());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
                PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent;
    }

    private static void scheduleNextRegularAlarm(Context context, boolean autoLocation, long updatePeriodMilis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        appendLog(context, TAG, "Build.VERSION.SDK_INT:", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + updatePeriodMilis,
                    getPendingIntent(context, autoLocation));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
        if (!ForecastUtil.shouldUpdateForecast(this, locationId, UpdateWeatherService.WEATHER_FORECAST_TYPE)) {
            return;
        }
        sendMessageToWeatherForecastService(locationId);
    }

    public void startSensorBasedUpdates() {
        sendIntent("android.intent.action.START_SENSOR_BASED_UPDATES");
    }

    public void startScreenOnOffUpdates() {
        sendIntent("android.intent.action.START_SCREEN_BASED_UPDATES");
    }

    public void stopSensorBasedUpdates() {
        sendIntent("android.intent.action.STOP_SCREEN_BASED_UPDATES");
        sendIntent("android.intent.action.STOP_SENSOR_BASED_UPDATES");
    }

    protected void sendIntent(String intent) {
        Intent sendIntent = new Intent(intent);
        sendIntent.setPackage("org.thosp.yourlocalweather");
        ContextCompat.startForegroundService(getBaseContext(), sendIntent);
    }

    private static PendingIntent getPendingSensorStartIntent(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                sendIntent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getPendingScreenStartIntent(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_SCREEN_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                sendIntent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    private void sendSensorAndScreenStopIntent() {
        appendLog(getBaseContext(), TAG, "sendSensorAndScreenStopIntent");
        stopSensorBasedUpdates();
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
                PendingIntent.FLAG_IMMUTABLE);
    }

    private void startBackgroundService(Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(),
                1,
                intent,
                PendingIntent.FLAG_IMMUTABLE);
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
                                                                 PendingIntent.FLAG_IMMUTABLE);
        return pendingIntent == null;
    }
}
