package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
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
        if (!ForecastUtil.shouldUpdateForecast(this, locationId, UpdateWeatherService.WEATHER_FORECAST_TYPE)) {
            return;
        }
        Intent intent = new Intent("android.intent.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(locationId, updateSource, UpdateWeatherService.START_WEATHER_FORECAST_UPDATE));
        startService(intent);
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
        Intent intent = new Intent("android.intent.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(), updateSource, updateWeatherOnly, UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        startService(intent);
    }
}
