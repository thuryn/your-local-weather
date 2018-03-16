package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
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

    private static final long START_SENSORS_CHECK_PERIOD = 3600000; //1 hour
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

            if (autoLocation && !"0".equals(updateAutoPeriodStr)) {
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                scheduleNextRegularAlarm(true, updateAutoPeriodMills);
            } else if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
                scheduleNextRegularAlarm(false, updatePeriodMills);
            }

            if (autoLocation) {
                Intent intentToStartUpdate = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
                intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
                intentToStartUpdate.putExtra("location", locationsDbHelper.getLocationByOrderId(0));
                startService(intentToStartUpdate);
                return ret;
            }
            int startUpdateOffset = 0;
            for (Location location: locationsDbHelper.getAllRows()) {
                if (location.getOrderId() == 0) {
                    continue;
                } else {
                    scheduleNextLocationWeatherUpdate(location, startUpdateOffset);
                    startUpdateOffset++;
                }
            }
        }
        return ret;
    }

    public void setAlarm() {
        if (alarmStarted) {
            return;
        }
        alarmStarted = true;
        cancelAlarm(true);
        cancelAlarm(false);
        sendSensorStopIntent();
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        appendLog(getBaseContext(), TAG, "setAlarm:" + updatePeriodStr);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (locationsDbHelper.getLocationByOrderId(0).isEnabled()) {
            if ("0".equals(updateAutoPeriodStr)) {
                sendSensorStartIntent();
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                        START_SENSORS_CHECK_PERIOD,
                        getPendingSensorStartIntent());
            } else {
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                scheduleNextRegularAlarm(true, updateAutoPeriodMills);
            }
        }
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            scheduleNextRegularAlarm(false, updatePeriodMills);
        }
    }

    public void cancelAlarm(boolean autoLocation) {
        appendLog(getBaseContext(), TAG, "cancelAlarm");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(autoLocation));
        getPendingIntent(autoLocation).cancel();
    }

    private void scheduleNextRegularAlarm(boolean autoLocation, long updatePeriodMilis) {
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + updatePeriodMilis,
                    getPendingIntent(autoLocation));
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + updatePeriodMilis,
                    getPendingIntent(autoLocation));
        }
    }

    private void scheduleNextLocationWeatherUpdate(Location location, int startUpdateOffset) {
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (startUpdateOffset * 60000),
                    startWeatherUpdate(location));
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (startUpdateOffset * 60000),
                    startWeatherUpdate(location));
        }
    }

    private void sendSensorStartIntent() {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
        appendLog(getBaseContext(), TAG, "sendIntent:" + sendIntent);
    }

    private void sendSensorStopIntent() {
        Intent sendIntent = new Intent("android.intent.action.STOP_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingSensorStartIntent());
        getPendingSensorStartIntent().cancel();
        appendLog(getBaseContext(), TAG, "sendIntent:" + sendIntent);
    }

    private PendingIntent getPendingSensorStartIntent() {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(getBaseContext(),
                0,
                sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent getPendingIntent(boolean autoLocation) {
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("autoLocation", autoLocation);
        return PendingIntent.getService(getBaseContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent startWeatherUpdate(Location currentLocation) {
        Intent intentToCheckWeather = new Intent(this, CurrentWeatherService.class);
        intentToCheckWeather.putExtra("location", currentLocation);
        startService(intentToCheckWeather);
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
}
