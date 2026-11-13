package org.thosp.yourlocalweather.service;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.LocationServiceDispatcher;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

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
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (autoLocation == null) {
            return;
        }
        long locationId = autoLocation.getId();
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage(getBaseContext().getPackageName());
        intentToStartUpdate.putExtra("locationId", locationId);
        intentToStartUpdate.putExtra("forceUpdate", byLastLocationOnly);
        boolean started = LocationServiceDispatcher.startLocationAndWeatherUpdate(getBaseContext(), intentToStartUpdate);
        if (!started) {
            sendMessageToCurrentWeatherService(autoLocation, AppWakeUpManager.SOURCE_CURRENT_WEATHER, true);
        }
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
                    sendIntent.setPackage(getBaseContext().getPackageName());
                    WidgetUtils.startBackgroundService(
                            getBaseContext(),
                            sendIntent);
                    break;
            }
        }
    }

    protected void sendIntentToMain() {
        org.thosp.yourlocalweather.events.AppEventsBus.postMainWeatherUpdate(UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL);
    }

    protected void sendIntentToMain(String result) {
        if (result.equals(UpdateWeatherService.ACTION_WEATHER_UPDATE_OK)) {
            org.thosp.yourlocalweather.events.AppEventsBus.postMainWeatherUpdate(UpdateWeatherService.ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL)) {
            org.thosp.yourlocalweather.events.AppEventsBus.postMainWeatherUpdate(UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        }
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
        intent.setPackage(getBaseContext().getPackageName());
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(),
                                                                             updateSource,
                                                                             forceUpdate,
                                                                             updateWeatherOnly,
                                                                             UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        ContextCompat.startForegroundService(this, intent);
    }

    protected void sendMessageToWakeUpService(int wakeAction, int wakeupSource) {
        Intent intent;
        if (wakeAction == 1) {
            intent = new Intent("org.thosp.yourlocalweather.action.WAKE_UP");
        } else {
            intent = new Intent("org.thosp.yourlocalweather.action.FALL_DOWN");
            NotificationUtils.cancelUpdateNotification(getBaseContext());
        }
        intent.setPackage(getBaseContext().getPackageName());
        intent.putExtra("wakeupSource", wakeupSource);
        ContextCompat.startForegroundService(getBaseContext(), intent);
    }

    protected void sendMessageToReconciliationDbService(boolean force) {
        appendLog(this,
                TAG,
                "going run reconciliation DB service");
        Intent intent = new Intent("org.thosp.yourlocalweather.action.START_RECONCILIATION");
        intent.setPackage(getBaseContext().getPackageName());
        intent.putExtra("force", force);
        startService(intent);
    }
    
    protected void sendIntent(String intent) {
        Intent sendIntent = new Intent(intent);
        sendIntent.setPackage(getBaseContext().getPackageName());
        ContextCompat.startForegroundService(getBaseContext(), sendIntent);
    }
}
