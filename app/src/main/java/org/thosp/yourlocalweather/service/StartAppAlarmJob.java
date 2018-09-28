package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.M)
public class StartAppAlarmJob extends JobService {
    private static final String TAG = "SyncService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_ALARM_SERVICE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(intentToStartUpdate);
        } else {
            getApplicationContext().startService(intentToStartUpdate);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

}