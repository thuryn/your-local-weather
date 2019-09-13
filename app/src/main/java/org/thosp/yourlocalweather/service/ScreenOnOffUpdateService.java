package org.thosp.yourlocalweather.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ScreenOnOffUpdateService extends AbstractCommonService {

    private static final String TAG = "ScreenOnOffUpdateService";

    private final IBinder binder = new ScreenOnOffUpdateServiceBinder();

    private static final long UPDATE_WEATHER_ONLY_TIMEOUT = 900000; //15 min
    private static final long REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT = 180000; //3 min
    private static final long SCREEN_ON_RETRY_FIRST = 1000;
    private static final long SCREEN_ON_RETRY_NEXT = 1000;

    private Lock receiversLock = new ReentrantLock();
    private volatile boolean receiversRegistered;
    private NetworkConnectivityReceiver networkConnectivityReceiver;
    private NetworkConnectionReceiver networkConnectionReceiver;

    private volatile int screenOnRetryCounter;

    private BroadcastReceiver userUnlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "receive intent: ", intent);
            String notificationPresence = AppPreference.getNotificationPresence(context);
            if (AppPreference.isNotificationEnabled(context) &&
                    "on_lock_screen".equals(notificationPresence)) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
            }
        }
    };

    private BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "receive intent: ", intent);
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
            appendLog(context, TAG, "receive intent: ", intent);
            String notificationPresence = AppPreference.getNotificationPresence(context);
            if (AppPreference.isNotificationEnabled(context) &&
                    "on_lock_screen".equals(notificationPresence)) {
                NotificationUtils.weatherNotification(context, getLocationForNotification().getId());
            }
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

            appendLog(getBaseContext(), TAG, "timerScreenOnRunnable:weatherRecord=", weatherRecord);
            if (weatherRecord == null) {
                requestWeatherCheck(currentLocation.getId(), null, AppWakeUpManager.SOURCE_CURRENT_WEATHER, false);
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
                return;
            }
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
            long lastUpdateTimeInMilis = Utils.getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, currentLocation);
            long now = System.currentTimeMillis();

            appendLog(getBaseContext(), TAG, "screen timer called, lastUpdate=",
                    currentLocation.getLastLocationUpdate(),
                    ", now=",
                    now,
                    ", lastUpdateTimeInMilis=",
                    lastUpdateTimeInMilis);

            if ((now <= (lastUpdateTimeInMilis + UPDATE_WEATHER_ONLY_TIMEOUT)) || (now <= (currentLocation.getLastLocationUpdate() + REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT))) {
                timerScreenOnHandler.postDelayed(timerScreenOnRunnable, REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT);
                return;
            }
            appendLog(getBaseContext(), TAG, "timerScreenOnRunnable:requestWeatherCheck");
            requestWeatherCheck(currentLocation.getId(), null, AppWakeUpManager.SOURCE_CURRENT_WEATHER, false);
            timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return ret;
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():", intent.getAction());
        switch (intent.getAction()) {
            case "android.intent.action.START_SCREEN_BASED_UPDATES": startSensorBasedUpdates(); return START_STICKY;
            case "android.intent.action.STOP_SCREEN_BASED_UPDATES": stopSensorBasedUpdates(); return ret;
            default: return ret;
        }
    }

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            WidgetUtils.updateWidgets((Context) inputMessage.obj);
        }
    };

    private void processScreenOn(Context context) {
        Message completeMessage =
                handler.obtainMessage();
        completeMessage.obj = context;
        completeMessage.sendToTarget();
        processScreenOnInBg(context);
    }

    private void processScreenOnInBg(Context context) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());

        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);

        if (!currentLocation.isEnabled() || !"0".equals(updateAutoPeriodStr)) {
            checkNotification(context);
            return;
        }
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(currentLocation.getId());
        WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(currentLocation.getId());
        long lastUpdateTimeInMilis = Utils.getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, currentLocation);
        long now = System.currentTimeMillis();
        appendLog(context, TAG, "SCREEN_ON called, lastUpdate=",
                currentLocation.getLastLocationUpdate(),
                ", now=",
                now,
                ", lastUpdateTimeInMilis=",
                lastUpdateTimeInMilis);
        if ((now <= (lastUpdateTimeInMilis + UPDATE_WEATHER_ONLY_TIMEOUT)) || (now <= (currentLocation.getLastLocationUpdate() + REQUEST_UPDATE_WEATHER_ONLY_TIMEOUT))) {
            timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT - (now - lastUpdateTimeInMilis));
            checkNotification(context);
            return;
        }
        checkNotification(context);
        requestWeatherCheck(currentLocation.getId(), null, AppWakeUpManager.SOURCE_CURRENT_WEATHER, false);
        timerScreenOnHandler.postDelayed(timerScreenOnRunnable, UPDATE_WEATHER_ONLY_TIMEOUT);
    }

    private void checkNotification(Context context) {
        String notificationPresence = AppPreference.getNotificationPresence(context);
        if (!"on_lock_screen".equals(notificationPresence)) {
            return;
        }
        if (NotificationUtils.isScreenLocked(context)) {
            NotificationUtils.weatherNotification(context, getLocationForNotification().getId());
        } else {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
    }

    private Location getLocationForNotification() {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }

    public void stopSensorBasedUpdates() {
        appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
        try {
            getApplication().unregisterReceiver(screenOnReceiver);
            getApplication().unregisterReceiver(screenOffReceiver);
            getApplication().unregisterReceiver(userUnlockedReceiver);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                getApplicationContext().unregisterReceiver(networkConnectivityReceiver);
            } else {
                ConnectivityManager connectivityManager
                        = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                connectivityManager.unregisterNetworkCallback(networkConnectionReceiver);
            }
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Error unregistering screen receivers - receivers was not registered");
        }
    }

    public void startSensorBasedUpdates() {
        receiversLock.lock();
        try {
            appendLog(getBaseContext(), TAG,
                    "Check if receivers is going to be started:  receiversRegistered=",
                    receiversRegistered);
            if (receiversRegistered) {
                return;
            }
            registerScreenListeners();
            startNetworkConnectivityReceiver();
            receiversRegistered = true;
        } finally {
            receiversLock.unlock();
        }
    }

    private void registerScreenListeners() {
        IntentFilter filterScreenOn = new IntentFilter(Intent.ACTION_SCREEN_ON);
        IntentFilter filterScreenOff = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        IntentFilter filterUserUnlocked = new IntentFilter(Intent.ACTION_USER_PRESENT);
        getApplication().registerReceiver(screenOnReceiver, filterScreenOn);
        getApplication().registerReceiver(screenOffReceiver, filterScreenOff);
        getApplication().registerReceiver(userUnlockedReceiver, filterUserUnlocked);
    }

    private void startNetworkConnectivityReceiver() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            appendLog(getBaseContext(), TAG, "Start connectivity receiver with handler");
            networkConnectivityReceiver = new NetworkConnectivityReceiver();
            IntentFilter filterNetworkConnectivity = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            getApplicationContext().registerReceiver(networkConnectivityReceiver, filterNetworkConnectivity);
        } else {
            appendLog(getBaseContext(), TAG, "Start connectivity receiver with callback");
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkConnectionReceiver = new NetworkConnectionReceiver(this);
            connectivityManager.registerDefaultNetworkCallback(networkConnectionReceiver);
        }
    }

    public boolean networkIsOffline() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        appendLog(this, TAG, "networkIsOffline, networkInfo=", networkInfo);
        if (networkInfo == null) {
            return true;
        }
        appendLog(this, TAG, "networkIsOffline, networkInfo.isConnectedOrConnecting()=",
                networkInfo.isConnectedOrConnecting());
        return !networkInfo.isConnectedOrConnecting();
    }

    public void checkAndUpdateWeather() {
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this);
        final WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);

        List<Location> locations = locationsDbHelper.getAllRows();

        for (Location location: locations) {

            if (!location.isEnabled()) {
                continue;
            }

            CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(location.getId());

            appendLog(this, TAG, "weatherRecord=", weatherRecord);
            if (weatherRecord == null) {
                requestWeatherCheck(location.getId(), null, AppWakeUpManager.SOURCE_CURRENT_WEATHER, false);
                continue;
            }
            WeatherForecastDbHelper.WeatherForecastRecord weatherForecastRecord = weatherForecastDbHelper.getWeatherForecast(location.getId());
            long lastUpdateTimeInMilis = Utils.getLastUpdateTimeInMilis(weatherRecord, weatherForecastRecord, location);
            long now = System.currentTimeMillis();

            long updatePeriodForLocation;
            if (location.getOrderId() == 0) {
                String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(this);
                updatePeriodForLocation = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
            } else {
                String updatePeriodStr = AppPreference.getLocationUpdatePeriod(this);
                updatePeriodForLocation = Utils.intervalMillisForAlarm(updatePeriodStr);
            }

            appendLog(this, TAG, "network state changed, location.orderId=",
                    location.getOrderId(),
                    ", updatePeriodForLocation=",
                    updatePeriodForLocation,
                    ", now=",
                    now,
                    ", lastUpdateTimeInMilis=",
                    lastUpdateTimeInMilis);

            if (now <= (lastUpdateTimeInMilis + updatePeriodForLocation)) {
                appendLog(this, TAG, "network state changed, location is not going to update, because last update is recent enough. location.orderId=",
                        location.getOrderId());
                continue;
            }
            appendLog(this, TAG, "requestWeatherCheck");
            requestWeatherCheck(location.getId(), null, AppWakeUpManager.SOURCE_CURRENT_WEATHER, false);
        }
    }

    public class ScreenOnOffUpdateServiceBinder extends Binder {
        ScreenOnOffUpdateService getService() {
            return ScreenOnOffUpdateService.this;
        }
    }

    public class NetworkConnectivityReceiver extends BroadcastReceiver {

        private static final String TAG = "NetworkConnectivityReceiver";

        private boolean wasOffline;

        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(context, TAG, "onReceive start:", intent);
            if (networkIsOffline()) {
                wasOffline = true;
                return;
            }
            if (wasOffline) {
                checkAndUpdateWeather();
            }
            wasOffline = false;
        }
    }
}
