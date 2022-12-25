package org.thosp.yourlocalweather.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.IBinder;

import org.thosp.yourlocalweather.model.LocationsDbHelper;

import java.util.LinkedList;
import java.util.Queue;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogSensorsCheck;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogSensorsEnd;

import androidx.core.content.ContextCompat;

public class SensorLocationUpdater extends AbstractCommonService implements SensorEventListener {

    private static final String TAG = "SensorLocationUpdater";

    private static final float REFERENCE_ACCELEROMETER_RESOLUTION = 104.418936291f;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT = 1500;
    private static final float LENGTH_UPDATE_LOCATION_SECOND_LIMIT = 10000;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION = 200;
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN = 900000l; //15 min
    private static final long ACCELEROMETER_UPDATE_TIME_SECOND_SPAN = 300000l; //5 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION = 300000l; //5 min

    private volatile long lastUpdate = 0;
    private volatile float currentLength = 0;
    private float currentLengthLowPassed = 0;
    private float gravity[] = new float[3];
    private MoveVector lastMovement;
    protected float sensorResolutionMultiplayer = 1;

    public static volatile boolean autolocationForSensorEventAddressFound;

    private static volatile boolean processLocationUpdate;
    private static final Queue<LocationUpdateService.LocationUpdateServiceActions> locationUpdateServiceActions = new LinkedList<>();

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        currentLength = 0;
        currentLengthLowPassed = 0;
        lastUpdate = 0;
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationUpdateService != null) {
            unbindLocationUpdateService();
        }
    }

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

    private void processSensorEvent(SensorEvent sensorEvent) {
        double countedLength;
        double countedAcc;
        long now = sensorEvent.timestamp;
        try {
            final float dT = (float) (now - lastUpdate) / 1000000000.0f;
            lastUpdate = now;

            if (lastMovement != null) {
                countedAcc = (float) Math.sqrt((lastMovement.getX() * lastMovement.getX()) + (lastMovement.getY() * lastMovement.getY()) + (lastMovement.getZ() * lastMovement.getZ()));
                countedLength = countedAcc * dT *dT;

                float lowPassConst = 0.1f;

                if ((countedAcc < lowPassConst) || (dT > 1000f)) {
                    if (dT > 1.0f) {
                        appendLogSensorsCheck(getBaseContext(),
                                              TAG,
                                  "acc under limit",
                                              currentLength,
                                              countedLength,
                                              countedAcc,
                                              dT);
                    }
                    currentLengthLowPassed += countedLength;
                    lastMovement = highPassFilter(sensorEvent);
                    return;
                }
                currentLength += countedLength;
            } else {
                countedLength = 0;
                countedAcc = 0;
            }
            lastMovement = highPassFilter(sensorEvent);

            if ((lastUpdate%1000 < 5) || (countedLength > 10)) {
                appendLogSensorsCheck(getBaseContext(), TAG, "current", currentLength, countedLength, countedAcc, dT);
            }
            float absCurrentLength = Math.abs(currentLength) * (REFERENCE_ACCELEROMETER_RESOLUTION + sensorResolutionMultiplayer);

            long lastUpdatedPosition = getLastPossitionUodateTime();
            long nowInMillis = System.currentTimeMillis();

            boolean nowIsBeforeTheLastUpdatedAndTimeSpan = (nowInMillis < (lastUpdatedPosition + ACCELEROMETER_UPDATE_TIME_SPAN));
            boolean currentLengthIsUnderLimit = (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT);
            boolean nowIsBeforeTheLastUpdatedAndFastTimeSpan = (nowInMillis < (lastUpdatedPosition + ACCELEROMETER_UPDATE_TIME_SECOND_SPAN));
            boolean currentLengthIsUnderFastLimit = (absCurrentLength < LENGTH_UPDATE_LOCATION_SECOND_LIMIT);
            boolean nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation = (nowInMillis < (lastUpdatedPosition + ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION));
            boolean currentLengthIsUnderNoLocationLimit = (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION);

            if (processLocationUpdate ||
                    (nowIsBeforeTheLastUpdatedAndTimeSpan || currentLengthIsUnderLimit)
                 && (nowIsBeforeTheLastUpdatedAndFastTimeSpan || currentLengthIsUnderFastLimit)
                 && (autolocationForSensorEventAddressFound || nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation || currentLengthIsUnderNoLocationLimit)) {
                return;
            }
            processLocationUpdate = true;
            appendLogSensorsEnd(getBaseContext(),
                             TAG,
                             absCurrentLength,
                             currentLengthLowPassed,
                             nowInMillis,
                             lastUpdatedPosition,
                             nowIsBeforeTheLastUpdatedAndTimeSpan,
                             currentLengthIsUnderLimit,
                             nowIsBeforeTheLastUpdatedAndFastTimeSpan,
                             currentLengthIsUnderFastLimit,
                             autolocationForSensorEventAddressFound,
                             nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation,
                             currentLengthIsUnderNoLocationLimit);
        } catch (Exception e) {
            appendLog(getBaseContext(), TAG, "Exception when processSensorQueue", e);
            processLocationUpdate = false;
            return;
        }

        clearMeasuredLength();

        if (!locationUpdateServiceActions.isEmpty()) {
            return;
        }

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
    }

    public void clearMeasuredLength() {
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        currentLength = 0;
        currentLengthLowPassed = 0;
    }

    protected boolean updateNetworkLocation() {
        if (locationUpdateService != null) {
            boolean result = locationUpdateService.updateNetworkLocation(false, null, 0);
            processLocationUpdate = false;
            return result;
        } else {
            locationUpdateServiceActions.add(
                    LocationUpdateService.LocationUpdateServiceActions.START_LOCATION_ONLY_UPDATE);
            bindLocationUpdateService();
            return false;
        }
    }

    private long getLastPossitionUodateTime() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext().getApplicationContext());
        org.thosp.yourlocalweather.model.Location currentLocationForSensorEvent = locationsDbHelper.getLocationByOrderId(0);
        return currentLocationForSensorEvent.getLastLocationUpdate();
    }

    private void bindLocationUpdateService() {
        Intent intent = new Intent(getBaseContext().getApplicationContext(), LocationUpdateService.class);
        getBaseContext().getApplicationContext().bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindLocationUpdateService() {
        if (locationUpdateService == null) {
            return;
        }
        getBaseContext().getApplicationContext().unbindService(locationUpdateServiceConnection);
    }

    private final ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            LocationUpdateService.LocationUpdateServiceActions bindedServiceAction;
            while ((bindedServiceAction = locationUpdateServiceActions.poll()) != null) {
                if (locationUpdateService.updateNetworkLocation(false, null, 0)) {
                    processLocationUpdate = false;
                    gravity[0] = 0;
                    gravity[1] = 0;
                    gravity[2] = 0;
                    currentLength = 0;
                    currentLengthLowPassed = 0;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationUpdateService = null;
        }
    };

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
}
