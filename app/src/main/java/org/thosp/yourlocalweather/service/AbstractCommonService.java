package org.thosp.yourlocalweather.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.WidgetUtils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AbstractCommonService extends Service {

    private static final String TAG = "AbstractCommonService";

    private Messenger widgetRefreshIconService;
    private Queue<Message> unsentMessages = new LinkedList<>();
    private Lock widgetRotationServiceLock = new ReentrantLock();
    private Messenger currentWeatherService;
    private Lock currentWeatherServiceLock = new ReentrantLock();
    private Queue<Message> currentWeatherUnsentMessages = new LinkedList<>();
    private Messenger weatherForecastService;
    private Lock weatherForecastServiceLock = new ReentrantLock();
    private Queue<Message> weatherForecastUnsentMessages = new LinkedList<>();
    private Messenger wakeUpService;
    private Lock wakeUpServiceLock = new ReentrantLock();
    private Queue<Message> wakeUpUnsentMessages = new LinkedList<>();

    private static Queue<LocationUpdateServiceActionsWithParams> locationUpdateServiceActions = new LinkedList<>();
    LocationUpdateService locationUpdateService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindServices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unbindServices();
        } catch (Throwable t) {
            appendLog(getBaseContext(), TAG, t.getMessage(), t);
        }
    }

    protected void updateNetworkLocation(boolean byLastLocationOnly) {
        startRefreshRotation("updateNetworkLocation", 3);
        if (locationUpdateService != null) {
            locationUpdateService.updateNetworkLocation(
                    byLastLocationOnly,
                    null,
                    0);
        } else {
            locationUpdateServiceActions.add(
                    new LocationUpdateServiceActionsWithParams(
                            LocationUpdateService.LocationUpdateServiceActions.START_LOCATION_ONLY_UPDATE,
                            byLastLocationOnly));
        }
    }

    protected void updateWidgets(String updateSource) {
        if (WidgetRefreshIconService.isRotationActive) {
            return;
        }
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
        Intent intent = new Intent(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        intent.putExtra(
                CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        WidgetUtils.startBackgroundService(getBaseContext(), intent);
    }

    protected void sendIntentToMain(String result) {
        Intent intent = new Intent(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        if (result.equals(CurrentWeatherService.ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(
                    CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                    CurrentWeatherService.ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(
                    CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT,
                    CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        }
        sendBroadcast(intent);
    }

    protected void requestWeatherCheck(String updateSource, int wakeUpSource) {
        appendLog(getBaseContext(), TAG, "startRefreshRotation");
        boolean updateLocationInProcess = LocationUpdateService.updateLocationInProcess;
        appendLog(getBaseContext(), TAG, "requestWeatherCheck, updateLocationInProcess=" +
                updateLocationInProcess);
        if (updateLocationInProcess) {
            return;
        }
        updateNetworkLocation(true);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (currentLocation.getLocationSource() != null) {
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "-");
            currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        }
        sendMessageToCurrentWeatherService(currentLocation, updateSource, wakeUpSource);
        sendMessageToWeatherForecastService(currentLocation.getId(), updateSource);
    }

    protected void startRefreshRotation(String where, int rotationSource) {
        appendLog(getBaseContext(), TAG, "startRefreshRotation:" + where);
        sendMessageToWidgetIconService(WidgetRefreshIconService.START_ROTATING_UPDATE, rotationSource);
    }

    protected void stopRefreshRotation(String where, int rotationSource) {
        appendLog(getBaseContext(), TAG, "stopRefreshRotation:" + where);
        sendMessageToWidgetIconService(WidgetRefreshIconService.STOP_ROTATING_UPDATE, rotationSource);
    }

    protected void sendMessageToWidgetIconService(int action, int rotationsource) {
        widgetRotationServiceLock.lock();
        try {
            Message msg = Message.obtain(null, action, rotationsource, 0);
            if (checkIfWidgetIconServiceIsNotBound()) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                unsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            widgetRefreshIconService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            widgetRotationServiceLock.unlock();
        }
    }

    private boolean checkIfWidgetIconServiceIsNotBound() {
        if (widgetRefreshIconService != null) {
            return false;
        }
        try {
            bindServices();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "checkIfWidgetIconServiceIsNotBound interrupted:", ie);
        }
        return (widgetRefreshIconService == null);
    }

    private void bindServices() {
        bindService(
                new Intent(this, WidgetRefreshIconService.class),
                widgetRefreshIconConnection,
                Context.BIND_AUTO_CREATE);
        Intent intent = new Intent(this, LocationUpdateService.class);
        bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindServices() {
        if (widgetRefreshIconService != null) {
            unbindService(widgetRefreshIconConnection);
        }
        if (locationUpdateServiceConnection != null) {
            unbindService(locationUpdateServiceConnection);
        }
    }


    private ServiceConnection widgetRefreshIconConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            widgetRefreshIconService = new Messenger(binderService);
            widgetRotationServiceLock.lock();
            try {
                while (!unsentMessages.isEmpty()) {
                    widgetRefreshIconService.send(unsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                widgetRotationServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            widgetRefreshIconService = null;
        }
    };

    protected void sendMessageToCurrentWeatherService(Location location, int wakeUpSource) {
        sendMessageToCurrentWeatherService(location, null, wakeUpSource);
    }

    protected void sendMessageToCurrentWeatherService(Location location,
                                                      String updateSource,
                                                      int wakeUpSource) {
        sendMessageToWakeUpService(
                AppWakeUpManager.WAKE_UP,
                wakeUpSource
        );
        currentWeatherServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    CurrentWeatherService.START_CURRENT_WEATHER_UPDATE,
                    new WeatherRequestDataHolder(location.getId(), updateSource)
            );
            if (checkIfCurrentWeatherServiceIsNotBound()) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                currentWeatherUnsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            currentWeatherService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            currentWeatherServiceLock.unlock();
        }
    }

    private boolean checkIfCurrentWeatherServiceIsNotBound() {
        if (currentWeatherService != null) {
            return false;
        }
        try {
            bindCurrentWeatherService();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return (currentWeatherService == null);
    }

    private void bindCurrentWeatherService() {
        bindService(
                new Intent(this, CurrentWeatherService.class),
                currentWeatherServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindCurrentWeatherService() {
        if (currentWeatherService == null) {
            return;
        }
        unbindService(currentWeatherServiceConnection);
    }

    private ServiceConnection currentWeatherServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            currentWeatherService = new Messenger(binderService);
            currentWeatherServiceLock.lock();
            try {
                while (!currentWeatherUnsentMessages.isEmpty()) {
                    currentWeatherService.send(currentWeatherUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                currentWeatherServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            currentWeatherService = null;
        }
    };

    protected void sendMessageToWeatherForecastService(long locationId) {
        sendMessageToWeatherForecastService(locationId, null);
    }

    protected void sendMessageToWeatherForecastService(long locationId, String updateSource) {
        if (!ForecastUtil.shouldUpdateForecast(this, locationId)) {
            return;
        }
        weatherForecastServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    ForecastWeatherService.START_WEATHER_FORECAST_UPDATE,
                    new WeatherRequestDataHolder(locationId, updateSource)
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

    private boolean checkIfWeatherForecastServiceIsNotBound() {
        if (weatherForecastService != null) {
            return false;
        }
        try {
            bindWeatherForecastService();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "weatherForecastServiceIsNotBound interrupted:", ie);
        }
        return (weatherForecastService == null);
    }

    private void bindWeatherForecastService() {
        bindService(
                new Intent(this, ForecastWeatherService.class),
                weatherForecastServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindWeatherForecastService() {
        if (weatherForecastService == null) {
            return;
        }
        unbindService(weatherForecastServiceConnection);
    }

    private ServiceConnection weatherForecastServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            weatherForecastService = new Messenger(binderService);
            weatherForecastServiceLock.lock();
            try {
                while (!weatherForecastUnsentMessages.isEmpty()) {
                    weatherForecastService.send(weatherForecastUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                weatherForecastServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            weatherForecastService = null;
        }
    };

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
        bindService(
                new Intent(this, AppWakeUpManager.class),
                wakeUpServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindwakeUpService() {
        if (wakeUpService == null) {
            return;
        }
        unbindService(wakeUpServiceConnection);
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

    private ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            LocationUpdateServiceActionsWithParams bindedServiceAction;
            while ((bindedServiceAction = locationUpdateServiceActions.poll()) != null) {
                switch (bindedServiceAction.getLocationUpdateServiceAction()) {
                    case START_LOCATION_AND_WEATHER_UPDATE:
                        locationUpdateService.startLocationAndWeatherUpdate();
                        break;
                    case START_LOCATION_ONLY_UPDATE:
                        locationUpdateService.updateNetworkLocation(
                                bindedServiceAction.isByLastLocationOnly(),
                                null,
                                0);
                        break;
                    case LOCATION_UPDATE:
                        locationUpdateService.onLocationChanged(
                                bindedServiceAction.getInputLocation(),
                                bindedServiceAction.getAddress());
                        break;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
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
