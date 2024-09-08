package org.thosp.yourlocalweather.service;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogSensorsCheck;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogSensorsEnd;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.LocationsDbHelper;

public class SensorLocationUpdater extends AbstractCommonService implements SensorEventListener {

    private static final String TAG = "SensorLocationUpdater";

    private static final float REFERENCE_ACCELEROMETER_RESOLUTION = 104.418936291f;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT = 1500;
    private static final float LENGTH_UPDATE_LOCATION_SECOND_LIMIT = 10000;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION = 200;
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN = 900000L; //15 min
    private static final long ACCELEROMETER_UPDATE_TIME_SECOND_SPAN = 300000L; //5 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION = 300000L; //5 min

    private volatile long lastUpdate = 0;
    private volatile float currentLength = 0;
    private float currentLengthLowPassed = 0;
    private final float[] gravity = new float[3];
    private MoveVector lastMovement;
    protected float sensorResolutionMultiplayer = 1;

    public static volatile boolean autolocationForSensorEventAddressFound;

    private static volatile boolean processLocationUpdate;

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
    public void onSensorChanged(SensorEvent sensorEvent) {
        try {
            Sensor mySensor = sensorEvent.sensor;

            if (mySensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }
            YourLocalWeather.executor.submit(() -> processSensorEvent(sensorEvent));
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
                    currentLengthLowPassed += (float) countedLength;
                    lastMovement = highPassFilter(sensorEvent);
                    return;
                }
                currentLength += (float) countedLength;
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

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
        processLocationUpdate = false;
    }

    public void clearMeasuredLength() {
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        currentLength = 0;
        currentLengthLowPassed = 0;
    }

    private long getLastPossitionUodateTime() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext().getApplicationContext());
        org.thosp.yourlocalweather.model.Location currentLocationForSensorEvent = locationsDbHelper.getLocationByOrderId(0);
        return currentLocationForSensorEvent.getLastLocationUpdate();
    }

    private MoveVector highPassFilter(SensorEvent sensorEvent) {
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

        return new MoveVector(sensorEvent.values[0] - gravity[0], sensorEvent.values[1] - gravity[1], sensorEvent.values[2] - gravity[2]);
    }

    private static class MoveVector {
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
