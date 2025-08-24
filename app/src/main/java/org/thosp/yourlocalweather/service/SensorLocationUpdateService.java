package org.thosp.yourlocalweather.service;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;

import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.NotificationUtils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SensorLocationUpdateService extends SensorLocationUpdater {

    private static final String TAG = "SensorLocationUpdateService";

    private final Lock receiversLock = new ReentrantLock();
    private static volatile boolean receiversRegistered;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return ret;
        }
        Notification notification = NotificationUtils.getNotificationForActivity(getBaseContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //check permission
            try {
                startForeground(NotificationUtils.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } catch (Exception e) {
                appendLog(getBaseContext(), TAG, "Failed to start foreground service", e);
                // Fallback or error handling
                // For example, stop the service if it cannot run in the foreground
                stopSelf();
            }
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID, notification);
        }
        appendLog(getBaseContext(), TAG, "onStartCommand:intent.getAction():", intent.getAction());

        switch (intent.getAction()) {
            case "org.thosp.yourlocalweather.action.START_SENSOR_BASED_UPDATES":
                performSensorBasedUpdates();
                return START_STICKY;
            case "org.thosp.yourlocalweather.action.STOP_SENSOR_BASED_UPDATES":
                stopSensorBasedUpdates();
                stopForeground(true);
                return START_NOT_STICKY;
            case "android.intent.action.CLEAR_SENSOR_VALUES":
                clearMeasuredLength();
                stopForeground(true);
                return START_NOT_STICKY;
            default:
                NotificationUtils.cancelUpdateNotification(getBaseContext());
        }
        stopForeground(true);
        return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiversRegistered) {
            //unregisterReceiver(mReceiver);
            unregisterListener();
        }
        stopForeground(true);
    }

    private void stopSensorBasedUpdates() {
        receiversLock.lock();
        try {
            NotificationUtils.cancelUpdateNotification(getBaseContext());
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

    private int startSensorBasedUpdates(int initialReturnValue) {
        sendIntent("org.thosp.yourlocalweather.action.START_SENSOR_BASED_UPDATES");
        return initialReturnValue;
    }
    
    private void performSensorBasedUpdates() {
        receiversLock.lock();
        try {
            if (receiversRegistered) {
                return;
            }
            appendLog(getBaseContext(),
                    TAG,
                    "startSensorBasedUpdates ", senSensorManager);
            if (senSensorManager != null) {
                stopForeground(true);
                return;
            }
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
            if ((autoLocation == null) || !autoLocation.isEnabled()) {
                return;
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
    }

    private void unregisterListener() {
        receiversLock.lock();
        try {
            receiversRegistered = false;
            if (senSensorManager != null) {
                senSensorManager.unregisterListener(this);
            }
        } finally {
            receiversLock.unlock();
        }
    }
    
    private void registerSensorListener() {
        appendLog(getBaseContext(), TAG, "START_SENSOR_BASED_UPDATES recieved");
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoLocation.isEnabled()) {
            return;
        }

        sensorResolutionMultiplayer = 1 / senAccelerometer.getResolution();
        int maxDelay = senAccelerometer.getMaxDelay();
        appendLog(getBaseContext(),
              TAG,
              "Selected accelerometer sensor:",
              senAccelerometer,
              ", sensor's resolution:",
              senAccelerometer.getResolution(),
              ", sensor's max delay: ",
              senAccelerometer.getMaxDelay());
        appendLog(getBaseContext(), TAG, "Result of registering (new) sensor listener: " + senSensorManager.registerListener(
                this,
                senAccelerometer ,
                maxDelay,
                maxDelay));
    }
}
