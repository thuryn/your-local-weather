package org.thosp.yourlocalweather.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import org.thosp.yourlocalweather.model.LocationsDbHelper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import org.thosp.yourlocalweather.utils.NotificationUtils;

public class SensorLocationUpdateService extends SensorLocationUpdater {

    private static final String TAG = "SensorLocationUpdateService";
    
    public static final int SCREEN_OFF_RECEIVER_DELAY = 500;
    
    private final IBinder binder = new SensorLocationUpdateServiceBinder();

    private final Lock receiversLock = new ReentrantLock();
    private boolean receiversRegistered;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

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
            case "android.intent.action.START_SENSOR_BASED_UPDATES": return performSensorBasedUpdates(ret);
            case "android.intent.action.STOP_SENSOR_BASED_UPDATES": stopSensorBasedUpdates(); return ret;
            case "android.intent.action.CLEAR_SENSOR_VALUES": clearMeasuredLength(); return ret;
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiversRegistered) {
            unregisterReceiver(mReceiver);
            unregisterListener();
        }
        stopForeground(true);
    }

    public void stopSensorBasedUpdates() {
        receiversLock.lock();
        try {
            if (!receiversRegistered || (senSensorManager == null)) {
                return;
            }
            appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
            //senSensorManager.unregisterListener(SensorLocationUpdater.getInstance(getBaseContext()));
            senSensorManager = null;
            senAccelerometer = null;
            receiversRegistered = false;
        } finally {
            receiversLock.unlock();
        }
    }

    public int startSensorBasedUpdates(int initialReturnValue) {
        sendIntent("android.intent.action.START_SENSOR_BASED_UPDATES");
        return initialReturnValue;
    }
    
    public int performSensorBasedUpdates(int initialReturnValue) {
        receiversLock.lock();
        try {
            if (receiversRegistered) {
                return initialReturnValue;
            }
            appendLog(getBaseContext(),
                    TAG,
                    "startSensorBasedUpdates ", senSensorManager);
            if (senSensorManager != null) {
                return initialReturnValue;
            }
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
            if (!autoLocation.isEnabled()) {
                return initialReturnValue;
            }
            SensorLocationUpdater.autolocationForSensorEventAddressFound = autoLocation.isAddressFound();
            appendLog(getBaseContext(),
                    TAG,
                    "autolocationForSensorEventAddressFound=",
                    SensorLocationUpdater.autolocationForSensorEventAddressFound,
                    "autoLocation.isAddressFound()=",
                    autoLocation.isAddressFound());
            registerSensorListener();
            receiversRegistered = true;
        } finally {
            receiversLock.unlock();
        }
        return START_STICKY;
    }

    private void unregisterListener() {
        if (senSensorManager != null) {
            senSensorManager.unregisterListener(this);
        }
    }
    
    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            appendLog(getBaseContext(), TAG, "onReceive("+intent+")");

            if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }
             
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    appendLog(getBaseContext(), TAG, "Runnable executing.");
                    unregisterListener();
                    registerSensorListener();
                }
            };

            new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
        }
};
    
    private void registerSensorListener() {
        appendLog(getBaseContext(), TAG, "START_SENSOR_BASED_UPDATES recieved");
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoLocation.isEnabled()) {
            return;
        }
        
        startForeground(android.os.Process.myPid(), NotificationUtils.getWeatherNotification(this, autoLocation.getId()));
        
        sensorResolutionMultiplayer = 1 / senAccelerometer.getResolution();
        int maxDelay = 10000;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            maxDelay = senAccelerometer.getMaxDelay();
            appendLog(getBaseContext(),
                  TAG,
                  "Selected accelerometer sensor:",
                  senAccelerometer,
                  ", sensor's resolution:",
                  senAccelerometer.getResolution(),
                  ", sensor's max delay: ",
                  senAccelerometer.getMaxDelay());
        } else {
            appendLog(getBaseContext(),
                  TAG,
                  "Selected accelerometer sensor:",
                  senAccelerometer,
                  ", sensor's resolution:",
                  senAccelerometer.getResolution());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            appendLog(getBaseContext(), TAG, "Result of registering (new) sensor listener: " + senSensorManager.registerListener(
                    this,
                    senAccelerometer ,
                    maxDelay,
                    maxDelay));
        } else {
            appendLog(getBaseContext(), TAG, "Result of registering sensor listener: " + senSensorManager.registerListener(
                    this,
                    senAccelerometer ,
                    maxDelay));
        }
    }
    
    public class SensorLocationUpdateServiceBinder extends Binder {
        SensorLocationUpdateService getService() {
            return SensorLocationUpdateService.this;
        }
    }
}
