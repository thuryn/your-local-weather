package org.thosp.yourlocalweather.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.SystemClock;

import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.AppAlarmService;
import org.thosp.yourlocalweather.service.CurrentWeatherService;
import org.thosp.yourlocalweather.service.NotificationService;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        appendLog(context, TAG, "onReceive start");
        removeOldPreferences(context);
        appendLog(context, TAG, "scheduleStart start");
        scheduleStart(context);
        appendLog(context, TAG, "scheduleStart end");
        context.sendBroadcast(new Intent("android.appwidget.action.APPWIDGET_UPDATE"));
    }

    public void scheduleStart(Context context) {
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
                        SystemClock.elapsedRealtime() + AppAlarmService.START_SENSORS_CHECK_PERIOD,
                        AppAlarmService.START_SENSORS_CHECK_PERIOD,
                        getPendingSensorStartIntent(context));
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 10,
                        getPendingScreenStartIntent(context));
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + AppAlarmService.START_SENSORS_CHECK_PERIOD,
                        AppAlarmService.START_SENSORS_CHECK_PERIOD,
                        getPendingScreenStartIntent(context));
            } else if (!"OFF".equals(updateAutoPeriodStr)) {
                long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
                scheduleNextRegularAlarm(context, true, updateAutoPeriodMills);
            }
        }
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            scheduleNextRegularAlarm(context, false, updatePeriodMills);
        }
        scheduleNextNotificationAlarm(context);
    }

    private void scheduleNextNotificationAlarm(Context context) {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(context);
        if (!isNotificationEnabled) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        String intervalPref = AppPreference.getInterval(context);
        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);
        appendLog(context, TAG, "Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    getPendingIntentForNotifiation(context));
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    getPendingIntentForNotifiation(context));
        }
    }

    private PendingIntent getPendingIntentForNotifiation(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_WEATHER_NOTIFICATION_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pendingIntent;
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

    private static PendingIntent getPendingIntent(Context context, boolean autoLocation) {
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_WEATHER_ALARM");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("autoLocation", autoLocation);
        return PendingIntent.getService(context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent getPendingSensorStartIntent(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private static PendingIntent getPendingScreenStartIntent(Context context) {
        Intent sendIntent = new Intent("android.intent.action.START_SCREEN_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        return PendingIntent.getService(context,
                0,
                sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void removeOldPreferences(Context context) {
        SharedPreferences mSharedPreferences = context.getSharedPreferences(Constants.APP_SETTINGS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(Constants.APP_SETTINGS_ADDRESS_FOUND);
        editor.remove(Constants.APP_SETTINGS_GEO_CITY);
        editor.remove(Constants.APP_SETTINGS_GEO_COUNTRY_NAME);
        editor.remove(Constants.APP_SETTINGS_GEO_DISTRICT_OF_COUNTRY);
        editor.remove(Constants.APP_SETTINGS_GEO_DISTRICT_OF_CITY);
        editor.remove(Constants.LAST_UPDATE_TIME_IN_MS);
        editor.remove(Constants.APP_SETTINGS_CITY);
        editor.remove(Constants.APP_SETTINGS_COUNTRY_CODE);
        editor.remove(Constants.WEATHER_DATA_WEATHER_ID);
        editor.remove(Constants.WEATHER_DATA_TEMPERATURE);
        editor.remove(Constants.WEATHER_DATA_DESCRIPTION);
        editor.remove(Constants.WEATHER_DATA_PRESSURE);
        editor.remove(Constants.WEATHER_DATA_HUMIDITY);
        editor.remove(Constants.WEATHER_DATA_WIND_SPEED);
        editor.remove(Constants.WEATHER_DATA_CLOUDS);
        editor.remove(Constants.WEATHER_DATA_ICON);
        editor.remove(Constants.WEATHER_DATA_SUNRISE);
        editor.remove(Constants.WEATHER_DATA_SUNSET);
        editor.remove(Constants.APP_SETTINGS_LATITUDE);
        editor.remove(Constants.APP_SETTINGS_LONGITUDE);
        editor.remove(Constants.LAST_FORECAST_UPDATE_TIME_IN_MS);
        editor.remove(Constants.KEY_PREF_UPDATE_DETAIL);
        editor.remove(Constants.APP_SETTINGS_UPDATE_SOURCE);
        editor.remove(Constants.APP_SETTINGS_LOCATION_ACCURACY);
        editor.remove(Constants.LAST_LOCATION_UPDATE_TIME_IN_MS);
        editor.remove(Constants.LAST_WEATHER_UPDATE_TIME_IN_MS);
        editor.remove(Constants.KEY_PREF_LOCATION_UPDATE_STRATEGY);
        editor.remove("daily_forecast");
        editor.commit();
        context.getSharedPreferences(Constants.PREF_WEATHER_NAME,
                Context.MODE_PRIVATE).edit().clear().commit();
    }
}
