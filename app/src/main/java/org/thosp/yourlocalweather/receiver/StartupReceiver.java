package org.thosp.yourlocalweather.receiver;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.thosp.yourlocalweather.service.StartAutoLocationJob;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        appendLog(context, TAG, "onReceive start");
        removeOldPreferences(context);
        appendLog(context, TAG, "scheduleStart start");
        AppPreference.setLastSensorServicesCheckTimeInMs(context, 0);
        scheduleStart(context);
        appendLog(context, TAG, "scheduleStart end");
        context.sendBroadcast(new Intent("android.appwidget.action.APPWIDGET_UPDATE"));
    }

    private void scheduleStart(Context context) {
        appendLog(context, TAG, "scheduleStart at boot, SDK=", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            ComponentName serviceComponent = new ComponentName(context, StartAutoLocationJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(StartAutoLocationJob.JOB_ID, serviceComponent);
            builder.setMinimumLatency(1 * 1000); // wait at least
            builder.setOverrideDeadline(3 * 1000); // maximum delay
            JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
        } else {
            Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_ALARM_SERVICE");
            intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
            context.startService(intentToStartUpdate);
        }
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
