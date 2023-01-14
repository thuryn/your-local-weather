package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class LocationUpdateServiceRetryJob extends AbstractAppJob {

    private static final String TAG = "LocationUpdateServiceRetryJob";
    public static final int JOB_ID = 1355064090;

    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        Intent intent = new Intent("android.intent.action.START_LOCATION_ONLY_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("byLastLocationOnly", params.getExtras().getBoolean("byLastLocationOnly"));
        intent.putExtra("attempts", params.getExtras().getInt("attempts"));
        startService(intent);
        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
