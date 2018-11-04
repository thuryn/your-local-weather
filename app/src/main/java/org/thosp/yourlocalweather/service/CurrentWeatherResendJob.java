package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.os.Build;
import android.os.Message;
import android.os.RemoteException;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class CurrentWeatherResendJob extends AbstractAppJob {
    private static final String TAG = "CurrentWeatherResendJob";

    @Override
    public boolean onStartJob(JobParameters params) {
        appendLog(getBaseContext(), TAG, "onStartJob");
        sendRetryMessageToCurrentWeatherService();
        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        appendLog(getBaseContext(), TAG, "onStopJob");
        unbindAllServices();
        return true;
    }

    protected void sendRetryMessageToCurrentWeatherService() {
        appendLog(getBaseContext(), TAG, "sendRetryMessageToCurrentWeatherService:1");
        currentWeatherServiceLock.lock();
        appendLog(getBaseContext(), TAG, "sendRetryMessageToCurrentWeatherService:after lock");
        try {
            Message msg = Message.obtain(
                    null,
                    CurrentWeatherService.START_CURRENT_WEATHER_RETRY
            );
            if (checkIfCurrentWeatherServiceIsNotBound()) {
                appendLog(getBaseContext(), TAG, "sendRetryMessageToCurrentWeatherService:pushing into messages");
                currentWeatherUnsentMessages.add(msg);
                return;
            }
            appendLog(getBaseContext(), TAG, "sendMessageToService:");
            currentWeatherService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            currentWeatherServiceLock.unlock();
        }
    }
}
