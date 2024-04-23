package org.thosp.yourlocalweather.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AbstractCommonService extends Service {

    private static final String TAG = "AbstractCommonService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        appendLog(getBaseContext(), TAG, "onUnbind all services");
        return false;
    }

    protected void updateNetworkLocation(boolean byLastLocationOnly) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        intentToStartUpdate.putExtra("forceUpdate", byLastLocationOnly);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
    }

    protected void updateWidgets(String updateSource) {
        sendMessageToReconciliationDbService(false);
        WidgetUtils.updateWidgets(getBaseContext());
        if (updateSource != null) {
            switch (updateSource) {
                case "MAIN":
                    sendIntentToMain();
                    break;
                case "NOTIFICATION":
                    Intent sendIntent = new Intent("android.intent.action.SHOW_WEATHER_NOTIFICATION");
                    sendIntent.setPackage("org.thosp.yourlocalweather");
                    WidgetUtils.startBackgroundService(
                            getBaseContext(),
                            sendIntent);
                    break;
            }
        }
    }

    protected void sendIntentToMain() {
        Intent intent = new Intent(UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra(
                UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                sendBroadcast(intent);
            }
        };
        mainHandler.post(myRunnable);
    }

    protected void sendIntentToMain(String result) {
        Intent intent = new Intent(UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        intent.setPackage("org.thosp.yourlocalweather");
        if (result.equals(UpdateWeatherService.ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(
                    UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                    UpdateWeatherService.ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(
                    UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                    UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                sendBroadcast(intent);
            }
        };
        mainHandler.post(myRunnable);
    }

    protected void requestWeatherCheck(long locationId, String updateSource, int wakeUpSource, boolean forceUpdate) {
        appendLog(getBaseContext(), TAG, "startRefreshRotation");
        boolean updateLocationInProcess = LocationUpdateService.updateLocationInProcess;
        appendLog(getBaseContext(), TAG, "requestWeatherCheck, updateLocationInProcess=",
                updateLocationInProcess);
        if (updateLocationInProcess) {
            return;
        }
        updateNetworkLocation(true);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationById(locationId);
        sendMessageToCurrentWeatherService(currentLocation, updateSource, wakeUpSource, forceUpdate, true);
        sendMessageToWeatherForecastService(currentLocation.getId(), updateSource, forceUpdate);
    }

    protected void sendMessageToCurrentWeatherService(Location location, int wakeUpSource, boolean updateWeatherOnly) {
        sendMessageToCurrentWeatherService(location, null, wakeUpSource, false, updateWeatherOnly);
    }

    protected void sendMessageToCurrentWeatherService(Location location,
                                                      String updateSource,
                                                      int wakeUpSource,
                                                      boolean forceUpdate,
                                                      boolean updateWeatherOnly) {
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(),
                                                                             updateSource,
                                                                             forceUpdate,
                                                                             updateWeatherOnly,
                                                                             UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    protected void sendMessageToWeatherForecastService(long locationId) {
        sendMessageToWeatherForecastService(locationId, null, false);
    }

    protected void sendMessageToWeatherForecastService(long locationId, String updateSource, boolean forceUpdate) {
        appendLog(this,
                  TAG,
                "going to check weather forecast");
        if (!ForecastUtil.shouldUpdateForecast(this, locationId, UpdateWeatherService.WEATHER_FORECAST_TYPE)) {
            appendLog(this,
                    TAG,
                    "weather forecast is recent enough");
            return;
        }
        appendLog(this,
                TAG,
                "sending message to get weather forecast");
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(locationId, updateSource, forceUpdate, UpdateWeatherService.START_WEATHER_FORECAST_UPDATE));
        ContextCompat.startForegroundService(getBaseContext(), intent);
    }

    protected void sendMessageToWakeUpService(int wakeAction, int wakeupSource) {
        Intent intent;
        if (wakeAction == 1) {
            intent = new Intent("org.thosp.yourlocalweather.action.WAKE_UP");
        } else {
            intent = new Intent("org.thosp.yourlocalweather.action.FALL_DOWN");
        }
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("wakeupSource", wakeupSource);
        ContextCompat.startForegroundService(getBaseContext(), intent);
    }

    protected void sendMessageToReconciliationDbService(boolean force) {
        appendLog(this,
                TAG,
                "going run reconciliation DB service");
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_RECONCILIATION");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("force", force);
        ContextCompat.startForegroundService(getBaseContext(), intent);
    }
    
    protected void sendIntent(String intent) {
        Intent sendIntent = new Intent(intent);
        sendIntent.setPackage("org.thosp.yourlocalweather");
        ContextCompat.startForegroundService(getBaseContext(), sendIntent);
    }
}
