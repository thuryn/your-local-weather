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
    public static final int JOB_ID = 1992056442;

    private LocationUpdateService locationUpdateService;
    private ScreenOnOffUpdateService screenOnOffUpdateService;
    private JobParameters params;
    int connectedServicesCounter;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        connectedServicesCounter = 0;
        appendLog(this, TAG, "sending intent to get location update");
        Intent intent = new Intent(this, LocationUpdateService.class);
        bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        intent = new Intent(this, ScreenOnOffUpdateService.class);
        bindService(intent, screenOnOffUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (screenOnOffUpdateService != null) {
            unbindService(screenOnOffUpdateServiceConnection);
        }
        if (locationUpdateService != null) {
            unbindService(locationUpdateServiceConnection);
        }
        appendLog(this, TAG, "unbinding sensorLocationUpdate: " + sensorLocationUpdateServiceConnection);
        if (sensorLocationUpdateServiceConnection !=null) {
            try {
                unbindService(sensorLocationUpdateServiceConnection);
            } catch (Exception e) {
                appendLog(this, TAG, e.getMessage(), e);
            }
        }
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

    private void performUpdateOfLocation() {
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
                reScheduleNextAlarm(JOB_ID, AppAlarmService.START_SENSORS_CHECK_PERIOD, StartAutoLocationJob.class);
            } else if ("OFF".equals(updateAutoPeriodStr)) {
                sensorLocationUpdateService.stopSensorBasedUpdates();
            } else {
                reScheduleNextAlarm(JOB_ID, updateAutoPeriodStr, StartAutoLocationJob.class);
            }
            new Thread(new Runnable() {
                public void run() {
                    serviceConnected(sensorLocationUpdateServiceConnection);
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private ServiceConnection screenOnOffUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder binder =
                    (ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder) service;
            screenOnOffUpdateService = binder.getService();
            screenOnOffUpdateService.startSensorBasedUpdates();
            new Thread(new Runnable() {
                public void run() {
                    serviceConnected(screenOnOffUpdateServiceConnection);
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}