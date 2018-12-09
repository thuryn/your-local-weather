package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import java.util.LinkedList;
import java.util.Queue;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class NetworkLocationCellsOnlyJob extends JobService {
    private static final String TAG = "NetworkLocationCellsOnlyJob";
    public static final int JOB_ID = 47038763;

    NetworkLocationProvider networkLocationProvider;
    JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        appendLog(this, TAG, "starting cells only location lookup");
        Intent intent = new Intent(getApplicationContext(), NetworkLocationProvider.class);
        try {
            getApplicationContext().bindService(intent, networkLocationProviderConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (networkLocationProvider != null) {
            getApplicationContext().unbindService(networkLocationProviderConnection);
        }
        return true;
    }

    private ServiceConnection networkLocationProviderConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            NetworkLocationProvider.NetworkLocationProviderBinder binder =
                    (NetworkLocationProvider.NetworkLocationProviderBinder) service;
            networkLocationProvider = binder.getService();
            networkLocationProvider.startLocationUpdateCellsOnly();
            jobFinished(params, false);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            networkLocationProvider = null;
        }
    };
}
