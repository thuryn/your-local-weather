package org.thosp.yourlocalweather.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ScreenOnOffUpdateService extends AbstractCommonService {

    private static final String TAG = "ScreenOnOffUpdateService";

    private static final long UPDATE_WEATHER_ONLY_TIMEOUT = 900000; //15 min
    private static final long REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT = 180000; //3 min
    private static final long SCREEN_ON_RETRY_FIRST = 1000;
    private static final long SCREEN_ON_RETRY_NEXT = 1000;

    private volatile int screenOnRetryCounter;

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "receive intent: " + intent);
            try {
                processScreenOn(context);
            } catch (Exception e) {
                appendLog(getBaseContext(), TAG, "Exception occured during database update", e);
                screenOnRetryCounter = 0;
                timerScreenOnRetryHandler.postDelayed(timerScreenOnRetryRunnable, SCREEN_ON_RETRY_FIRST);
            }
        }
    };

    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "receive intent: " + intent);
            timerScreenOnHandler.removeCallbacksAndMessages(null);
        }
    };

    Handler timerScreenOnRetryHandler = new Handler();
    Runnable timerScreenOnRetryRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                processScreenOn(getBaseContext());
            } catch (Exception e) {
                appendLog(getBaseContext(), TAG, "Exception occured during database update", e);
                if (screenOnRetryCounter < 3) {
                    screenOnRetryCounter++;
                    timerScreenOnRetryHandler.postDelayed(timerScreenOnRetryRunnable, SCREEN_ON_RETRY_NEXT);
                }
            }
        }
    };

    Handler timerScreenOnHandler = new Handler();
    Runnable timerScreenOnRunnable = new Runnable() {

        @Override
        public void run() {
            if (!WidgetUtils.isInteractive(getBaseContext())) {
                return;
            }
            CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
            final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(getBaseContext());
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
            CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());

            appendLog(getBaseContext(), TAG, "timerScreenOnRunnable:weatherRecord=" + weatherRecord);
            if (weatherRecord == null) {
                requestWeatherCheck("-", true, null, AppWakeUpManager.SOURCE_CURRENT_WEATHER);
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
                return;
            }
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
            long lastUpdateTimeInMilis = Utils.getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, currentLocation);
            long now = System.currentTimeMillis();

            appendLog(getBaseContext(), TAG, "screen timer called, lastUpdate=" +
                    currentLocation.getLastLocationUpdate() +
                    ", now=" +
                    now +
                    ", lastUpdateTimeInMilis=" +
                    lastUpdateTimeInMilis);

            if ((now <= (lastUpdateTimeInMilis + UPDATE_WEATHER_ONLY_TIMEOUT)) || (now <= (currentLocation.getLastLocationUpdate() + REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT))) {
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT);
                return;
            }
            appendLog(getBaseContext(), TAG, "timerScreenOnRunnable:requestWeatherCheck");
            requestWeatherCheck("-", true, null, AppWakeUpManager.SOURCE_CURRENT_WEATHER);
            timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return ret;
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():" + intent.getAction());
        switch (intent.getAction()) {
            case "android.intent.action.START_SCREEN_BASED_UPDATES": return startSensorBasedUpdates(ret, intent);
            case "android.intent.action.STOP_SCREEN_BASED_UPDATES": stopSensorBasedUpdates(); return ret;
            default: return ret;
        }
    }

    private void processScreenOn(Context context) {
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
        long lastUpdateTimeInMilis = Utils.getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, currentLocation);
        long now = System.currentTimeMillis();
        appendLog(context, TAG, "SCREEN_ON called, lastUpdate=" +
                currentLocation.getLastLocationUpdate() +
                ", now=" +
                now +
                ", lastUpdateTimeInMilis=" +
                lastUpdateTimeInMilis);
        if ((now <= (lastUpdateTimeInMilis + UPDATE_WEATHER_ONLY_TIMEOUT)) || (now <= (currentLocation.getLastLocationUpdate() + REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT))) {
            timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT - (now - lastUpdateTimeInMilis));
            return;
        }
        requestWeatherCheck("-", true, null, AppWakeUpManager.SOURCE_CURRENT_WEATHER);
        timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
    }

    private void stopSensorBasedUpdates() {
        appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
        try {
            getApplication().unregisterReceiver(screenOnReceiver);
            getApplication().unregisterReceiver(screenOffReceiver);
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Error unregistering screen receivers - receivers was not registered");
        }
    }

    private int startSensorBasedUpdates(int initialReturnValue, Intent intent) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoLocation.isEnabled()) {
            return initialReturnValue;
        }
        registerScreenListeners();
        return START_STICKY;
    }

    private void registerScreenListeners() {
        IntentFilter filterScreenOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
        IntentFilter filterScreenOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        getApplication().registerReceiver(screenOnReceiver, filterScreenOn);
        getApplication().registerReceiver(screenOffReceiver, filterScreenOff);
    }
}
