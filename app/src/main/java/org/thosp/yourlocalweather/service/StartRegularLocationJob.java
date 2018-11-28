package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
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

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class StartRegularLocationJob extends AbstractAppJob {
    private static final String TAG = "StartRegularLocationJob";
    public static final int JOB_ID = 894325273;

    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        performUpdateOfWeather();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        unbindAllServices();
        return true;
    }

    @Override
    protected void serviceConnected(ServiceConnection serviceConnection) {
        if (currentWeatherUnsentMessages.isEmpty() && weatherForecastUnsentMessages.isEmpty()) {
            jobFinished(params, false);
        }
    }

    private void performUpdateOfWeather() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
        if (!"0".equals(updatePeriodStr) && (locationsDbHelper.getAllRows().size() > 1)) {
            reScheduleNextAlarm(JOB_ID, updatePeriodStr, StartRegularLocationJob.class);
        }
        List<Location> locations = locationsDbHelper.getAllRows();
        for (Location location: locations) {
            if (location.getOrderId() == 0) {
                continue;
            } else {
                sendMessageToCurrentWeatherService(location, AppWakeUpManager.SOURCE_CURRENT_WEATHER);
                sendMessageToWeatherForecastService(location.getId());
            }
        }
        if (currentWeatherUnsentMessages.isEmpty() && weatherForecastUnsentMessages.isEmpty()) {
            jobFinished(params, false);
        }
    }
}