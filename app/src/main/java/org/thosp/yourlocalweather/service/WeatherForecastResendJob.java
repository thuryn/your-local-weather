package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Message;
import android.os.RemoteException;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class WeatherForecastResendJob extends AbstractAppJob {
    private static final String TAG = "WeatherForecastResendJob";

    private JobParameters params;
    int connectedServicesCounter;

    @Override
    public boolean onStartJob(JobParameters params) {
        sendRetryMessageToWeatherForecastService();
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

    protected void sendRetryMessageToWeatherForecastService() {
        weatherForecastServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    ForecastWeatherService.START_WEATHER_FORECAST_RETRY
            );
            if (checkIfWeatherForecastServiceIsNotBound()) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                weatherForecastUnsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            weatherForecastService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            weatherForecastServiceLock.unlock();
        }
    }
}
