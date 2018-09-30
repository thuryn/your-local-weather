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

    private static final float LENGTH_UPDATE_LOCATION_LIMIT = 1500;
    private static final float LENGTH_UPDATE_LOCATION_SECOND_LIMIT = 30000;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION = 200;
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN = 900000000000l; //15 min
    private static final long ACCELEROMETER_UPDATE_TIME_SECOND_SPAN = 300000000000l; //5 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION = 300000000000l; //5 min

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    private volatile long lastUpdatedPossition = 0;
    private volatile long lastUpdate = 0;
    private volatile float currentLength = 0;
    private float currentLengthLowPassed = 0;
    private float gravity[] = new float[3];
    private MoveVector lastMovement;
    public static volatile boolean autolocationForSensorEventAddressFound;

    private SensorEventListener sensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            try {
                Sensor mySensor = sensorEvent.sensor;

                if (mySensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                    return;
                }
                processSensorEvent(sensorEvent);
            } catch (Exception e) {
                appendLog(getBaseContext(), TAG, "Exception on onSensorChanged", e);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        currentLength = 0;
        currentLengthLowPassed = 0;
        lastUpdate = 0;
        lastUpdatedPossition = 0;
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
    }

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

    private void processSensorEvent(SensorEvent sensorEvent) {
        double countedtLength = 0;
        double countedAcc = 0;
        long now = sensorEvent.timestamp;
        try {
            final float dT = (float) (now - lastUpdate) / 1000000000.0f;
            lastUpdate = now;

            if (lastMovement != null) {
                countedAcc = (float) Math.sqrt((lastMovement.getX() * lastMovement.getX()) + (lastMovement.getY() * lastMovement.getY()) + (lastMovement.getZ() * lastMovement.getZ()));
                countedtLength = countedAcc * dT *dT;

                float lowPassConst = 0.1f;

                if (countedAcc < lowPassConst) {
                    if (dT > 1.0f) {
                        appendLog(getBaseContext(), TAG, "acc under limit, currentLength = " + String.format("%.8f", currentLength) +
                                ":counted length = " + String.format("%.8f", countedtLength) + ":countedAcc = " + countedAcc +
                                ", dT = " + String.format("%.8f", dT));
                    }
                    currentLengthLowPassed += countedtLength;
                    lastMovement = highPassFilter(sensorEvent);
                    return;
                }
                currentLength += countedtLength;
            } else {
                countedtLength = 0;
                countedAcc = 0;
            }
            lastMovement = highPassFilter(sensorEvent);

            if ((lastUpdate%1000 < 5) || (countedtLength > 10)) {
                appendLog(getBaseContext(), TAG, "current currentLength = " + String.format("%.8f", currentLength) +
                        ":counted length = " + String.format("%.8f", countedtLength) + ":countedAcc = " + countedAcc +
                        ", dT = " + String.format("%.8f", dT));
            }
            float absCurrentLength = Math.abs(currentLength);

            if (((lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SPAN)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT))
                    && ((lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SECOND_SPAN)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_SECOND_LIMIT))
                    && (autolocationForSensorEventAddressFound || (lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION))) {
                return;
            }

            appendLog(getBaseContext(), TAG, "end currentLength = " + String.format("%.8f", absCurrentLength) +
                    ", currentLengthLowPassed = " + String.format("%.8f", currentLengthLowPassed) +
                    ", lastUpdate=" + lastUpdate + ", lastUpdatePosition=" + lastUpdatedPossition +
                    ", autolocationForSensorEventAddressFound=" + autolocationForSensorEventAddressFound);
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception when processSensorQueue", e);
            return;
        }

        try {
            ConnectionDetector connectionDetector = new ConnectionDetector(getApplicationContext());
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getApplicationContext());
            org.thosp.yourlocalweather.model.Location currentLocationForSensorEvent = locationsDbHelper.getLocationByOrderId(0);
            if (!connectionDetector.isNetworkAvailableAndConnected()) {
                locationsDbHelper.updateLastUpdatedAndLocationSource(
                        currentLocationForSensorEvent.getId(),
                        System.currentTimeMillis(),
                        ".");
                return;
            }

            locationsDbHelper.updateLocationSource(currentLocationForSensorEvent.getId(), "-");
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception occured during database update", e);
            return;
        }

        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        lastUpdatedPossition = lastUpdate;
        currentLength = 0;
        currentLengthLowPassed = 0;

        updateNetworkLocation(false, false);
    }

    public void stopSensorBasedUpdates() {
        if (senSensorManager == null) {
            return;
        }
        appendLog(getBaseContext(), TAG, "STOP_SENSOR_BASED_UPDATES recieved");
        senSensorManager.unregisterListener(sensorListener);
        senSensorManager = null;
        senAccelerometer = null;
    }

    public int startSensorBasedUpdates(int initialReturnValue) {
        if (senSensorManager != null) {
            return initialReturnValue;
        }
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoLocation.isEnabled()) {
            return initialReturnValue;
        }
        autolocationForSensorEventAddressFound = autoLocation.isAddressFound();
        appendLog(getBaseContext(),
                  TAG,
                 "autolocationForSensorEventAddressFound=" +
                  autolocationForSensorEventAddressFound +
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
            senSensorManager.registerListener(sensorListener, senAccelerometer , 300000000, 600000000);
        } else {
            senSensorManager.registerListener(sensorListener, senAccelerometer , 300000000);
        }
    }

    private MoveVector highPassFilter(SensorEvent sensorEvent) {
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

        return new MoveVector(sensorEvent.values[0] - gravity[0], sensorEvent.values[1] - gravity[1], sensorEvent.values[2] - gravity[2]);
    }

    private class MoveVector {
        private final float x;
        private final float y;
        private final float z;

        public MoveVector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() {
            return x;
        }
        public float getY() {
            return y;
        }
        public float getZ() {
            return z;
        }
    }

    public class SensorLocationUpdateServiceBinder extends Binder {
        SensorLocationUpdateService getService() {
            return SensorLocationUpdateService.this;
        }
    }
}
