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

    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
        reScheduleNextAlarm(1, updateAutoPeriodStr, StartAutoLocationJob.class);
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        reScheduleNextAlarm(2, updatePeriodStr, StartRegularLocationJob.class);
        reScheduleNextAlarm(3, AppPreference.getInterval(getBaseContext()), StartNotificationJob.class);
        Intent intent = new Intent(this, ScreenOnOffUpdateService.class);
        bindService(intent, screenOnOffUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private ServiceConnection screenOnOffUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder binder =
                    (ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder) service;
            ScreenOnOffUpdateService screenOnOffUpdateService = binder.getService();
            screenOnOffUpdateService.startSensorBasedUpdates(0);
            jobFinished(params, false);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}