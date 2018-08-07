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
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AppAlarmService extends Service {

    private static final String TAG = "AppAlarmService";

    public static final long START_SENSORS_CHECK_PERIOD = 3600000; //1 hour
    private volatile boolean alarmStarted;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return ret;
        }

        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():" + intent.getAction());
        if ("org.thosp.yourlocalweather.action.START_ALARM_SERVICE".equals(intent.getAction())) {
            setAlarm();
        } else if ("org.thosp.yourlocalweather.action.RESTART_ALARM_SERVICE".equals(intent.getAction())) {
            alarmStarted = false;
            setAlarm();
        } else if ("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM".equals(intent.getAction())) {
            boolean autoLocation = intent.getBooleanExtra("autoLocation", false);
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

            String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
            String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
            long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);

            if (autoLocation && !"0".equals(updateAutoPeriodStr) && !"OFF".equals(updateAutoPeriodStr)) {
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                scheduleNextRegularAlarm(getBaseContext(), true, updateAutoPeriodMills);
            } else if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
                scheduleNextRegularAlarm(getBaseContext(), false, updatePeriodMills);
            }

            if (autoLocation) {
                Intent intentToStartUpdate = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
                intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
                intentToStartUpdate.putExtra("locationId", locationsDbHelper.getLocationByOrderId(0).getId());
                startBackgroundService(intentToStartUpdate);
                return ret;
            }
            for (Location location: locationsDbHelper.getAllRows()) {
                if (location.getOrderId() == 0) {
                    continue;
                } else {
                    scheduleNextLocationWeatherUpdate(location);
                    break;
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
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        appendLog(getBaseContext(), TAG, "setAlarm:" + updatePeriodStr);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (locationsDbHelper.getLocationByOrderId(0).isEnabled()) {
            if ("0".equals(updateAutoPeriodStr)) {
                sendSensorStartIntent();
                sendScreenStartIntent();
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
                scheduleNextRegularAlarm(getBaseContext(), true, updateAutoPeriodMills);
            } else {
                sendSensorAndScreenStopIntent();
            }
        }
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            scheduleNextRegularAlarm(getBaseContext(), false, updatePeriodMills);
        }
    }

    public void cancelAlarm(boolean autoLocation) {
        appendLog(getBaseContext(), TAG, "cancelAlarm");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(getBaseContext(), autoLocation));
        getPendingIntent(getBaseContext(), autoLocation).cancel();
    }

    private static void scheduleNextRegularAlarm(Context context, boolean autoLocation, long updatePeriodMilis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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

    private void scheduleNextLocationWeatherUpdate(Location location) {
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    startWeatherUpdate(location));
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    startWeatherUpdate(location));
        }
    }

    private void sendSensorStartIntent() {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent);
        appendLog(getBaseContext(), TAG, "sendIntent:" + sendIntent);
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
        Intent sendIntent = new Intent("android.intent.action.START_SCREEN_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent);
        appendLog(getBaseContext(), TAG, "sendIntent:" + sendIntent);
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
        Intent sendIntent = new Intent("android.intent.action.STOP_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
        Intent sendStopScreenIntent = new Intent("android.intent.action.STOP_SCREEN_BASED_UPDATES");
        sendStopScreenIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendStopScreenIntent);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingSensorStartIntent(getBaseContext()));
        getPendingSensorStartIntent(getBaseContext()).cancel();
        alarmManager.cancel(getPendingScreenStartIntent(getBaseContext()));
        getPendingScreenStartIntent(getBaseContext()).cancel();
        appendLog(getBaseContext(), TAG, "sendIntent:" + sendIntent);
    }

    private static PendingIntent getPendingIntent(Context context, boolean autoLocation) {
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("autoLocation", autoLocation);
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

    private PendingIntent startWeatherUpdate(Location currentLocation) {
        Intent intentToCheckWeather = new Intent(this, CurrentWeatherService.class);
        intentToCheckWeather.putExtra("locationId", currentLocation.getId());
        startBackgroundService(intentToCheckWeather);
        return PendingIntent.getBroadcast(getBaseContext(),
                                          0,
                                            intentToCheckWeather,
                                          PendingIntent.FLAG_CANCEL_CURRENT);
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

    public static void scheduleStart(Context context) {
        appendLog(context, TAG, "scheduleStart at boot");
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(context);
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(context);
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        appendLog(context, TAG, "setAlarm:" + updatePeriodStr);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (locationsDbHelper.getLocationByOrderId(0).isEnabled()) {
            if ("0".equals(updateAutoPeriodStr)) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 10,
                        getPendingSensorStartIntent(context));
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingSensorStartIntent(context));
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 10,
                        getPendingScreenStartIntent(context));
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingScreenStartIntent(context));
            } else if (!"OFF".equals(updateAutoPeriodStr)) {
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                scheduleNextRegularAlarm(context, true, updateAutoPeriodMills);
            }
        }
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            scheduleNextRegularAlarm(context, false, updatePeriodMills);
        }
    }
}
