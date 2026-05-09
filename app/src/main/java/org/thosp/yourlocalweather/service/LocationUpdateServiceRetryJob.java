package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.Intent;
import android.os.Build;

public class LocationUpdateServiceRetryJob extends AbstractAppJob {

    private static final String TAG = "LocationUpdateServiceRetryJob";
    public static final int JOB_ID = 1355064090;

    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_ONLY_UPDATE");
        intent.setPackage(getBaseContext().getPackageName());
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
