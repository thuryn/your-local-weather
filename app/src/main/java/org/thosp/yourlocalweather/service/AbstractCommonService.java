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

    private Messenger wakeUpService;
    private Lock wakeUpServiceLock = new ReentrantLock();
    private Queue<Message> wakeUpUnsentMessages = new LinkedList<>();
    private Messenger reconciliationDbService;
    private Lock reconciliationDbServiceLock = new ReentrantLock();
    private Queue<Message> reconciliationDbUnsentMessages = new LinkedList<>();

    private static Queue<LocationUpdateServiceActionsWithParams> locationUpdateServiceActions = new LinkedList<>();
    LocationUpdateService locationUpdateService;

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
        Intent intentToStartUpdate = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
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
        intent.putExtra(
                UpdateWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                UpdateWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        WidgetUtils.startBackgroundService(getBaseContext(), intent);
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
        sendBroadcast(intent);
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
        Intent intent = new Intent("android.intent.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(location.getId(),
                                                                             updateSource,
                                                                             forceUpdate,
                                                                             updateWeatherOnly,
                                                                             UpdateWeatherService.START_CURRENT_WEATHER_UPDATE));
        startService(intent);
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
        Intent intent = new Intent("android.intent.action.START_WEATHER_UPDATE");
        intent.setPackage("org.thosp.yourlocalweather");
        intent.putExtra("weatherRequest", new WeatherRequestDataHolder(locationId, updateSource, forceUpdate, UpdateWeatherService.START_WEATHER_FORECAST_UPDATE));
        startService(intent);
    }

    protected void sendMessageToWakeUpService(int wakeAction, int wakeupSource) {
        wakeUpServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    wakeAction,
                    wakeupSource,
                    0
            );
            if (checkIfWakeUpServiceIsNotBound()) {
                wakeUpUnsentMessages.add(msg);
                return;
            }
            wakeUpService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            wakeUpServiceLock.unlock();
        }
    }

    private boolean checkIfWakeUpServiceIsNotBound() {
        if (wakeUpService != null) {
            return false;
        }
        try {
            bindWakeUpService();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return (wakeUpService == null);
    }

    private void bindWakeUpService() {
        if (wakeUpService != null) {
            return;
        }
        appendLog(getBaseContext(), getClass().getName(), "bindWakeUpService ", wakeUpService);
        getApplicationContext().bindService(
                new Intent(getApplicationContext(), AppWakeUpManager.class),
                wakeUpServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindwakeUpService() {
        if (wakeUpService == null) {
            return;
        }
        getApplicationContext().unbindService(wakeUpServiceConnection);
    }

    private ServiceConnection wakeUpServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            wakeUpService = new Messenger(binderService);
            wakeUpServiceLock.lock();
            try {
                while (!wakeUpUnsentMessages.isEmpty()) {
                    wakeUpService.send(wakeUpUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                wakeUpServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            wakeUpService = null;
        }
    };

    protected void sendMessageToReconciliationDbService(boolean force) {
        appendLog(this,
                TAG,
                "going run reconciliation DB service");
        reconciliationDbServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    ReconciliationDbService.START_RECONCILIATION,
                    force?1:0
            );
            if (checkIfReconciliationDbServiceIsNotBound()) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                reconciliationDbUnsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            reconciliationDbService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            reconciliationDbServiceLock.unlock();
        }
    }
    
    protected void sendIntent(String intent) {
        Intent sendIntent = new Intent(intent);
        sendIntent.setPackage("org.thosp.yourlocalweather");
        ContextCompat.startForegroundService(getBaseContext(), sendIntent);
    }

    private boolean checkIfReconciliationDbServiceIsNotBound() {
        if (reconciliationDbService != null) {
            return false;
        }
        try {
            bindReconciliationDBService();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "weatherForecastServiceIsNotBound interrupted:", ie);
        }
        return (reconciliationDbService == null);
    }

    private void bindReconciliationDBService() {
        getApplicationContext().bindService(
                new Intent(getApplicationContext(), ReconciliationDbService.class),
                reconciliationDbServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindReconciliationDbService() {
        if (reconciliationDbService == null) {
            return;
        }
        getApplicationContext().unbindService(reconciliationDbServiceConnection);
    }

    private ServiceConnection reconciliationDbServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            reconciliationDbService = new Messenger(binderService);
            reconciliationDbServiceLock.lock();
            try {
                while (!reconciliationDbUnsentMessages.isEmpty()) {
                    reconciliationDbService.send(reconciliationDbUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                reconciliationDbServiceLock.unlock();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            reconciliationDbService = null;
        }
    };

    public class LocationUpdateServiceActionsWithParams {
        LocationUpdateService.LocationUpdateServiceActions locationUpdateServiceAction;
        boolean byLastLocationOnly;
        android.location.Location inputLocation;
        Address address;

        public LocationUpdateServiceActionsWithParams(
                LocationUpdateService.LocationUpdateServiceActions locationUpdateServiceAction,
                boolean byLastLocationOnly) {
            this.locationUpdateServiceAction = locationUpdateServiceAction;
            this.byLastLocationOnly = byLastLocationOnly;
        }

        public LocationUpdateServiceActionsWithParams(
                LocationUpdateService.LocationUpdateServiceActions locationUpdateServiceAction,
                boolean byLastLocationOnly,
                android.location.Location inputLocation,
                Address address) {
            this.locationUpdateServiceAction = locationUpdateServiceAction;
            this.byLastLocationOnly = byLastLocationOnly;
            this.address = address;
            this.inputLocation = inputLocation;
        }

        public LocationUpdateService.LocationUpdateServiceActions getLocationUpdateServiceAction() {
            return locationUpdateServiceAction;
        }

        public boolean isByLastLocationOnly() {
            return byLastLocationOnly;
        }

        public android.location.Location getInputLocation() {
            return inputLocation;
        }

        public Address getAddress() {
            return address;
        }
    }
}
