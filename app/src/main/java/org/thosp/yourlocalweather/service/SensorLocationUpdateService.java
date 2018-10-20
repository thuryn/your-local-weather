package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class SensorLocationUpdateService extends AbstractCommonService {

    private static final String TAG = "SensorLocationUpdateService";

    private final IBinder binder = new SensorLocationUpdateServiceBinder();

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
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():" + intent.getAction());
        switch (intent.getAction()) {
            case "android.intent.action.START_SENSOR_BASED_UPDATES": return startSensorBasedUpdates(ret);
            case "android.intent.action.STOP_SENSOR_BASED_UPDATES": stopSensorBasedUpdates(); return ret;
        }
        return ret;
    }

    public void stopSensorBasedUpdates() {
        if (senSensorManager == null) {
            return;
        }
        appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
        senSensorManager.unregisterListener(SensorLocationUpdater.getInstance(getBaseContext()));
        senSensorManager = null;
        senAccelerometer = null;
    }

    public int startSensorBasedUpdates(int initialReturnValue) {
        appendLog(getBaseContext(),
                TAG,
                "startSensorBasedUpdates " + senSensorManager);
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
                 "autolocationForSensorEventAddressFound=" +
                  SensorLocationUpdater.autolocationForSensorEventAddressFound +
                  "autoLocation.isAddressFound()=" +
                         autoLocation.isAddressFound());
        registerSensorListener();
        return START_STICKY;
    }

    private void registerSensorListener() {
        appendLog(getBaseContext(), TAG, "START_SENSOR_BASED_UPDATES recieved");
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        appendLog(getBaseContext(), TAG, "Selected accelerometer sensor:" + senAccelerometer);
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
