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

    private LocationUpdateService locationUpdateService;
    private JobParameters params;
    int connectedServicesCounter;


    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        appendLog(this, TAG, "starting cells only location lookup");
        if (locationUpdateService == null) {
            Intent intent = new Intent(this, LocationUpdateService.class);
            bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            locationUpdateService.updateNetworkLocation(
                params.getExtras().getBoolean("byLastLocationOnly"),
                null,
                params.getExtras().getInt("attempts"));
            jobFinished(params, false);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        unbindService(locationUpdateServiceConnection);
        unbindAllServices();
        return true;
    }

    @Override
    protected void serviceConnected(ServiceConnection serviceConnection) {
        connectedServicesCounter++;
        if (connectedServicesCounter >= 4) {
            jobFinished(params, false);
        }
    }

    private ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            locationUpdateService.updateNetworkLocation(
                    params.getExtras().getBoolean("byLastLocationOnly"),
                    null,
                    params.getExtras().getInt("attempts"));
            new Thread(new Runnable() {
                public void run() {
                    serviceConnected(locationUpdateServiceConnection);
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationUpdateService = null;
        }
    };
}
