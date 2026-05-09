package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.utils.Utils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.core.content.ContextCompat;

@androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
public abstract class AbstractAppJob extends JobService {

    private static final String TAG = "AbstractAppJob";

    protected void reScheduleNextAlarm(int jobId, String updatePeriodStr, Class serviceClass) {
        long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        reScheduleNextAlarm(jobId, updateAutoPeriodMills, serviceClass);
    }

    protected void reScheduleNextAlarm(int jobId, long updatePeriod, Class serviceClass) {
        appendLog(getBaseContext(), TAG, "next alarm:", updatePeriod,
                ", serviceClass=", serviceClass);
        ComponentName serviceComponent = new ComponentName(this, serviceClass);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);
        builder.setMinimumLatency(updatePeriod); // wait at least
        builder.setOverrideDeadline(updatePeriod + (3 * 1000)); // maximum delay
        JobScheduler jobScheduler = getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    protected void sendMessageToWeatherForecastService(long locationId) {
        sendMessageToWeatherForecastService(locationId, null);
    }

    protected void sendMessageToWeatherForecastService(long locationId, String updateSource) {
        appendLog(getBaseContext(), TAG, "sendMessageToWeatherForecastService:locationId=", locationId);
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_WEATHER_UPDATE");
        intent.setPackage(getBaseContext().getPackageName());
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(locationId, updateSource, UpdateWeatherService.START_WEATHER_FORECAST_UPDATE));
        ContextCompat.startForegroundService(this, intent);
    }

    protected void sendMessageToCurrentWeatherService(Location location,
                                                      int wakeUpSource,
                                                      boolean updateWeatherOnly) {
        sendMessageToCurrentWeatherService(location, null, wakeUpSource, updateWeatherOnly);
    }

    protected void sendMessageToCurrentWeatherService(Location location,
                                                      String updateSource,
                                                      int wakeUpSource,
                                                      boolean updateWeatherOnly) {
        appendLog(getBaseContext(), TAG, "sendMessageToCurrentWeatherService:locationId=", location.getId());
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_WEATHER_UPDATE");
        intent.setPackage(getBaseContext().getPackageName());
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(), updateSource, updateWeatherOnly, UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        ContextCompat.startForegroundService(this, intent);
    }
}
