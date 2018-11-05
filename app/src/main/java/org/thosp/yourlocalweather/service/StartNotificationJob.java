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
public class StartNotificationJob extends AbstractAppJob {
    private static final String TAG = "StartNotificationJob";

    private JobParameters params;
    int connectedServicesCounter;

    @Override
    public boolean onStartJob(JobParameters params) {
        performNotification();
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

    private void performNotification() {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(getBaseContext());
        if (!isNotificationEnabled) {
            return;
        }
        Location currentLocation = getLocationForNotification();
        if (currentLocation == null) {
            return;
        }
        sendMessageToCurrentWeatherService(currentLocation, "NOTIFICATION", AppWakeUpManager.SOURCE_NOTIFICATION);
        reScheduleNextAlarm(3, AppPreference.getInterval(getBaseContext()), StartNotificationJob.class);
    }

    private Location getLocationForNotification() {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }
}