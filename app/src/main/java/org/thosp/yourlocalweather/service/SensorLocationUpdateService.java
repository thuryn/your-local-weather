package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import org.thosp.yourlocalweather.model.LocationsDbHelper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class SensorLocationUpdateService extends AbstractCommonService {

    private static final String TAG = "SensorLocationUpdateService";

    private final IBinder binder = new SensorLocationUpdateServiceBinder();

    private Lock receiversLock = new ReentrantLock();
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
            case "android.intent.action.START_SENSOR_BASED_UPDATES": return startSensorBasedUpdates(ret);
            case "android.intent.action.STOP_SENSOR_BASED_UPDATES": stopSensorBasedUpdates(); return ret;
        }
        return ret;
    }

    public void stopSensorBasedUpdates() {
        receiversLock.lock();
        try {
            if (!receiversRegistered || (senSensorManager == null)) {
                return;
            }
            appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
            senSensorManager.unregisterListener(SensorLocationUpdater.getInstance(getBaseContext()));
            senSensorManager = null;
            senAccelerometer = null;
            receiversRegistered = false;
        } finally {
            receiversLock.unlock();
        }
    }

    public int startSensorBasedUpdates(int initialReturnValue) {
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

    private void registerSensorListener() {
        appendLog(getBaseContext(), TAG, "START_SENSOR_BASED_UPDATES recieved");
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        appendLog(getBaseContext(), TAG, "Selected accelerometer sensor:", senAccelerometer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            senSensorManager.registerListener(
                    SensorLocationUpdater.getInstance(getBaseContext()),
                    senAccelerometer ,
                    300000000,
                    600000000);
        } else {
            senSensorManager.registerListener(
                    SensorLocationUpdater.getInstance(getBaseContext()),
                    senAccelerometer ,
                    300000000);
        }
    }

    public class SensorLocationUpdateServiceBinder extends Binder {
        SensorLocationUpdateService getService() {
            return SensorLocationUpdateService.this;
        }
    }
}
