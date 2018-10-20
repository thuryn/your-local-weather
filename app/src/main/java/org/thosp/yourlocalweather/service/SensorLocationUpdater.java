package org.thosp.yourlocalweather.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Location;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.WidgetUtils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class SensorLocationUpdater implements SensorEventListener {

    private static final String TAG = "SensorLocationUpdater";

    private static final float LENGTH_UPDATE_LOCATION_LIMIT = 1500;
    private static final float LENGTH_UPDATE_LOCATION_SECOND_LIMIT = 30000;
    private static final float LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION = 200;
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN = 900000000000l; //15 min
    private static final long ACCELEROMETER_UPDATE_TIME_SECOND_SPAN = 300000000000l; //5 min
    private static final long ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION = 300000000000l; //5 min

    private volatile long lastUpdatedPossition = 0;
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
            Intent intent = new Intent(context, LocationUpdateService.class);
            context.bindService(intent, instance.locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        }
        return instance;
    }

    public SensorLocationUpdater(Context context) {
        super();
        appendLog(context, TAG, "SensorLocationUpdater created");
        this.context = context;
        currentLength = 0;
        currentLengthLowPassed = 0;
        lastUpdate = 0;
        lastUpdatedPossition = 0;
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
                        appendLog(context, TAG, "acc under limit, currentLength = " + String.format("%.8f", currentLength) +
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
                appendLog(context, TAG, "current currentLength = " + String.format("%.8f", currentLength) +
                        ":counted length = " + String.format("%.8f", countedtLength) + ":countedAcc = " + countedAcc +
                        ", dT = " + String.format("%.8f", dT));
            }
            float absCurrentLength = Math.abs(currentLength);

            if (((lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SPAN)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT))
                    && ((lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SECOND_SPAN)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_SECOND_LIMIT))
                    && (autolocationForSensorEventAddressFound || (lastUpdate < (lastUpdatedPossition + ACCELEROMETER_UPDATE_TIME_SPAN_NO_LOCATION)) || (absCurrentLength < LENGTH_UPDATE_LOCATION_LIMIT_NO_LOCATION))) {
                return;
            }

            appendLog(context, TAG, "end currentLength = " + String.format("%.8f", absCurrentLength) +
                    ", currentLengthLowPassed = " + String.format("%.8f", currentLengthLowPassed) +
                    ", lastUpdate=" + lastUpdate + ", lastUpdatePosition=" + lastUpdatedPossition +
                    ", autolocationForSensorEventAddressFound=" + autolocationForSensorEventAddressFound);
        } catch (Exception e) {
            appendLog(context, TAG, "Exception when processSensorQueue", e);
            return;
        }

        try {
            ConnectionDetector connectionDetector = new ConnectionDetector(context.getApplicationContext());
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context.getApplicationContext());
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
            appendLog(context, TAG, "Exception occured during database update", e);
            return;
        }

        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        lastUpdatedPossition = lastUpdate;
        currentLength = 0;
        currentLengthLowPassed = 0;

        updateNetworkLocation();
    }

    protected void updateNetworkLocation() {
        startRefreshRotation("updateNetworkLocation", 3);
        if (locationUpdateService != null) {
            locationUpdateService.updateNetworkLocation(false, null, 0);
        } else {
            locationUpdateServiceActions.add(
                    LocationUpdateService.LocationUpdateServiceActions.START_LOCATION_ONLY_UPDATE);
        }
    }

    protected void startRefreshRotation(String where, int rotationSource) {
        appendLog(context, TAG, "startRefreshRotation:" + where);
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
        context.bindService(
                new Intent(context, WidgetRefreshIconService.class),
                widgetRefreshIconConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindWidgetRefreshIconService() {
        if (widgetRefreshIconService == null) {
            return;
        }
        context.unbindService(widgetRefreshIconConnection);
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

    private ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            LocationUpdateService.LocationUpdateServiceActions bindedServiceAction;
            while ((bindedServiceAction = locationUpdateServiceActions.poll()) != null) {
                locationUpdateService.updateNetworkLocation(false, null, 0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}
