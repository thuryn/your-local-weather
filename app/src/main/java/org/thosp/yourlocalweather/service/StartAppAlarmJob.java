package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

@TargetApi(Build.VERSION_CODES.M)
public class StartAppAlarmJob extends JobService {
    private static final String TAG = "SyncService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Intent intent = new Intent(this, AppAlarmService.class);
        bindService(intent, appAlarmServiceConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private ServiceConnection appAlarmServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            AppAlarmService.AppAlarmServiceBinder binder =
                    (AppAlarmService.AppAlarmServiceBinder) service;
            AppAlarmService appAlarmService = binder.getService();
            appAlarmService.setAlarm();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}