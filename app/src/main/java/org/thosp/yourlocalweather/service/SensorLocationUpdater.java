package org.thosp.yourlocalweather.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogSensorsEnd;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogSensorsCheck;

public class SensorLocationUpdater implements SensorEventListener {

    private static final String TAG = "SensorLocationUpdater";

    private static final float LENGTH_UPDATE_LOCATION_LIMIT = 1500;
    private static final float LENGTH_UPDATE_LOCATION_SECOND_LIMIT = 300;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION = 200;
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN = 900000l; //15 min
    private static final long ACCELEROMETER_UPDATE_TIME_SECOND_SPAN = 300000l; //5 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION = 300000l; //5 min

    private volatile long lastUpdate = 0;
    private volatile float currentLength = 0;
    private float currentLengthLowPassed = 0;
    private float gravity[] = new float[3];
    private MoveVector lastMovement;

    public static volatile boolean autolocationForSensorEventAddressFound;

    private Messenger widgetRefreshIconService;
    private Queue<Message> unsentMessages = new LinkedList<>();
    private Lock widgetRotationServiceLock = new ReentrantLock();
    private Context context;
    private static SensorLocationUpdater instance;
    private LocationUpdateService locationUpdateService;
    private static Queue<LocationUpdateService.LocationUpdateServiceActions> locationUpdateServiceActions = new LinkedList<>();

    public synchronized static SensorLocationUpdater getInstance(Context context) {
        if (instance == null) {
            instance = new SensorLocationUpdater(context);
        }
        return instance;
    }

    @Override
    protected void finalize() throws Throwable {
        if (locationUpdateService != null) {
            unbindLocationUpdateService();
        }
        if (widgetRefreshIconService != null) {
            unbindWidgetRefreshIconService();
        }
        super.finalize();
    }

    public SensorLocationUpdater(Context context) {
        super();
        appendLog(context, TAG, "SensorLocationUpdater created");
        this.context = context;
        currentLength = 0;
        currentLengthLowPassed = 0;
        lastUpdate = 0;
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
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
            appendLog(context, TAG, "Exception on onSensorChanged", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void processSensorEvent(SensorEvent sensorEvent) {
        double countedLength = 0;
        double countedAcc = 0;
        long now = sensorEvent.timestamp;
        try {
            final float dT = (float) (now - lastUpdate) / 1000000000.0f;
            lastUpdate = now;

            if (lastMovement != null) {
                countedAcc = (float) Math.sqrt((lastMovement.getX() * lastMovement.getX()) + (lastMovement.getY() * lastMovement.getY()) + (lastMovement.getZ() * lastMovement.getZ()));
                countedLength = countedAcc * dT *dT;

                float lowPassConst = 0.1f;

                if (countedAcc < lowPassConst) {
                    if (dT > 1.0f) {
                        appendLogSensorsCheck(context,
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
                appendLogSensorsCheck(context, TAG, "current", currentLength, countedLength, countedAcc, dT);
            }
            float absCurrentLength = Math.abs(currentLength);

            long lastUpdatedPosition = getLastPossitionUodateTime();
            long nowInMillis = System.currentTimeMillis();

            boolean nowIsBeforeTheLastUpdatedAndTimeSpan = (nowInMillis < (lastUpdatedPosition + ACCELEROMETER_UPDATE_TIME_SPAN));
            boolean currentLengthIsUnderLimit = (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT);
            boolean nowIsBeforeTheLastUpdatedAndFastTimeSpan = (nowInMillis < (lastUpdatedPosition + ACCELEROMETER_UPDATE_TIME_SECOND_SPAN));
            boolean currentLengthIsUnderFastLimit = (absCurrentLength < LENGTH_UPDATE_LOCATION_SECOND_LIMIT);
            boolean nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation = (nowInMillis < (lastUpdatedPosition + ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION));
            boolean currentLengthIsUnderNoLocationLimit = (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION);

            if ((nowIsBeforeTheLastUpdatedAndTimeSpan || currentLengthIsUnderLimit)
                 && (nowIsBeforeTheLastUpdatedAndFastTimeSpan || currentLengthIsUnderFastLimit)
                 && (autolocationForSensorEventAddressFound || nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation || currentLengthIsUnderNoLocationLimit)) {
                return;
            }

            appendLogSensorsEnd(context,
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
            appendLog(context, TAG, "Exception when processSensorQueue", e);
            return;
        }

        if (!locationUpdateServiceActions.isEmpty()) {
            return;
        }

        if (updateNetworkLocation()) {
            clearMeasuredLength();
        }
    }

    public void clearMeasuredLength() {
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        currentLength = 0;
        currentLengthLowPassed = 0;
    }

    protected boolean updateNetworkLocation() {
        startRefreshRotation("updateNetworkLocation", 3);
        if (locationUpdateService != null) {
            return locationUpdateService.updateNetworkLocation(false, null, 0);
        } else {
            locationUpdateServiceActions.add(
                    LocationUpdateService.LocationUpdateServiceActions.START_LOCATION_ONLY_UPDATE);
            bindLocationUpdateService();
            return false;
        }
    }

    protected void startRefreshRotation(String where, int rotationSource) {
        appendLog(context, TAG, "startRefreshRotation:", where);
        sendMessageToWidgetIconService(WidgetRefreshIconService.START_ROTATING_UPDATE, rotationSource);
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
            appendLog(context, TAG, e.getMessage(), e);
        } finally {
            widgetRotationServiceLock.unlock();
        }
    }

    private long getLastPossitionUodateTime() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context.getApplicationContext());
        org.thosp.yourlocalweather.model.Location currentLocationForSensorEvent = locationsDbHelper.getLocationByOrderId(0);
        return currentLocationForSensorEvent.getLastLocationUpdate();
    }

    private boolean checkIfWidgetIconServiceIsNotBound() {
        if (widgetRefreshIconService != null) {
            return false;
        }
        try {
            bindWidgetRefreshIconService();
        } catch (Exception ie) {
            appendLog(context, TAG, "checkIfWidgetIconServiceIsNotBound interrupted:", ie);
        }
        return (widgetRefreshIconService == null);
    }

    private void bindWidgetRefreshIconService() {
        context.getApplicationContext().bindService(
                new Intent(context.getApplicationContext(), WidgetRefreshIconService.class),
                widgetRefreshIconConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindWidgetRefreshIconService() {
        if (widgetRefreshIconService == null) {
            return;
        }
        context.getApplicationContext().unbindService(widgetRefreshIconConnection);
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
                appendLog(context, TAG, e.getMessage(), e);
            } finally {
                widgetRotationServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            widgetRefreshIconService = null;
        }
    };

    private void bindLocationUpdateService() {
        Intent intent = new Intent(context.getApplicationContext(), LocationUpdateService.class);
        context.getApplicationContext().bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindLocationUpdateService() {
        if (locationUpdateService == null) {
            return;
        }
        context.getApplicationContext().unbindService(locationUpdateServiceConnection);
    }

    private ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            LocationUpdateService.LocationUpdateServiceActions bindedServiceAction;
            while ((bindedServiceAction = locationUpdateServiceActions.poll()) != null) {
                if (locationUpdateService.updateNetworkLocation(false, null, 0)) {
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
