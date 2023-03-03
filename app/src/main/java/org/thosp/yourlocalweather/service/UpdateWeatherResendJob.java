package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Message;
import android.os.RemoteException;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import org.thosp.yourlocalweather.YourLocalWeather;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TargetApi(Build.VERSION_CODES.M)
public class UpdateWeatherResendJob extends AbstractAppJob {
    private static final String TAG = "UpdateWeatherResendJob";
    public static final int JOB_ID = 1537091709;

    private JobParameters params;
    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        YourLocalWeather.executor.submit(() -> {
            appendLog(getBaseContext(), TAG, "onStartJob");
            sendRetryMessageToCurrentWeatherService();
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        YourLocalWeather.executor.submit(() -> {
            appendLog(getBaseContext(), TAG, "onStopJob");
        });
        return true;
    }

    protected void sendRetryMessageToCurrentWeatherService() {
        appendLog(getBaseContext(), TAG, "sendRetryMessageToCurrentWeatherService:1");
        Intent intent = new Intent("org.thosp.yourlocalweather.action.RESEND_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        startService(intent);
        jobFinished(params, false);
    }
}
