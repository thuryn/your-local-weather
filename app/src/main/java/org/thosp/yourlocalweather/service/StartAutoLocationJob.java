package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class StartAutoLocationJob extends AbstractAppJob {
    private static final String TAG = "StartAutoLocationJob";

    LocationUpdateService locationUpdateService;
    JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        appendLog(this, TAG, "sending intent to get location update");
        Intent intent = new Intent(this, LocationUpdateService.class);
        bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (locationUpdateService != null) {
            unbindService(locationUpdateServiceConnection);
        }
        unbindService(sensorLocationUpdateServiceConnection);
        unbindAllServices();
        return true;
    }

    private void performUpdateOfLocation() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        Location location = locationsDbHelper.getLocationByOrderId(0);
        if (location.isEnabled()) {
            locationUpdateService.startLocationAndWeatherUpdate();
        }
        Intent intent = new Intent(this, SensorLocationUpdateService.class);
        bindService(intent, sensorLocationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            appendLog(getBaseContext(), TAG, "got locationUpdateServiceConnection");
            performUpdateOfLocation();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationUpdateService = null;
        }
    };

    private ServiceConnection sensorLocationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SensorLocationUpdateService.SensorLocationUpdateServiceBinder binder =
                    (SensorLocationUpdateService.SensorLocationUpdateServiceBinder) service;
            SensorLocationUpdateService sensorLocationUpdateService = binder.getService();
            String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
            appendLog(getBaseContext(),
                    TAG,
                    "updateAutoPeriodStr:" + updateAutoPeriodStr);
            if ("0".equals(updateAutoPeriodStr)) {
                sensorLocationUpdateService.startSensorBasedUpdates(0);
            } else if ("OFF".equals(updateAutoPeriodStr)) {
                sensorLocationUpdateService.stopSensorBasedUpdates();
            } else {
                reScheduleNextAlarm(1, updateAutoPeriodStr, StartAutoLocationJob.class);
            }
            jobFinished(params, false);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}