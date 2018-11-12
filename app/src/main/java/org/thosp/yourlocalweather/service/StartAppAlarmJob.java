package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import org.thosp.yourlocalweather.utils.AppPreference;

@TargetApi(Build.VERSION_CODES.M)
public class StartAppAlarmJob extends AbstractAppJob {
    private static final String TAG = "StartAppAlarmJob";
    public static final int JOB_ID = 439162570;

    private JobParameters params;
    int connectedServicesCounter;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        connectedServicesCounter = 0;
        reScheduleNextAlarm(StartAutoLocationJob.JOB_ID, 1000, StartAutoLocationJob.class);
        reScheduleNextAlarm(StartRegularLocationJob.JOB_ID, 2000, StartRegularLocationJob.class);
        reScheduleNextAlarm(StartNotificationJob.JOB_ID, 3000, StartNotificationJob.class);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        unbindAllServices();
        return true;
    }

    @Override
    protected void serviceConnected(ServiceConnection serviceConnection) {
        connectedServicesCounter++;
        if (connectedServicesCounter >= 3) {
            jobFinished(params, false);
        }
    }
}