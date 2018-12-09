package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.DATE_FORMATTER;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class StartAutoLocationJob extends AbstractAppJob {
    private static final String TAG = "StartAutoLocationJob";
    public static final int JOB_ID = 1992056442;

    private enum Updated {
        REGULARLY,
        BY_NOTIFICATION,
        NOTHING
    }

    private LocationUpdateService locationUpdateService;
    private ScreenOnOffUpdateService screenOnOffUpdateService;
    private SensorLocationUpdateService sensorLocationUpdateService;
    private JobParameters params;
    private int connectedServicesCounter;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        connectedServicesCounter = 0;
        appendLog(this, TAG, "onStartJob");
        try {
            Intent intent = new Intent(getApplicationContext(), LocationUpdateService.class);
            getApplicationContext().bindService(intent, locationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
            intent = new Intent(getApplicationContext(), ScreenOnOffUpdateService.class);
            getApplicationContext().bindService(intent, screenOnOffUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (screenOnOffUpdateService != null) {
            getApplicationContext().unbindService(screenOnOffUpdateServiceConnection);
        }
        if (locationUpdateService != null) {
            getApplicationContext().unbindService(locationUpdateServiceConnection);
        }
        appendLog(this, TAG, "unbinding sensorLocationUpdate: " + sensorLocationUpdateServiceConnection);
        if (sensorLocationUpdateServiceConnection !=null) {
            try {
                getApplicationContext().unbindService(sensorLocationUpdateServiceConnection);
            } catch (Exception e) {
                appendLog(this, TAG, e.getMessage(), e);
            }
        }
        unbindAllServices();
        return true;
    }

    @Override
    protected void serviceConnected(ServiceConnection serviceConnection) {
        connectedServicesCounter++;
        if (connectedServicesCounter >= 5) {
            jobFinished(params, false);
        }
    }

    private void performUpdateOfLocation() {
        try {
            Intent intent = new Intent(getApplicationContext(), SensorLocationUpdateService.class);
            getApplicationContext().bindService(intent, sensorLocationUpdateServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
    }

    private Updated performUpdateOfAutolocation(Calendar now,
                                             Location location,
                                             String updateAutoPeriodStr,
                                             long updateAutoPeriodMills,
                                             boolean notificationForLocation) {
        long lastSensorServicesCheckTimeInMs = AppPreference.getLastSensorServicesCheckTimeInMs(getBaseContext());
        if ("0".equals(updateAutoPeriodStr)) {
            if ((now.getTimeInMillis() >= (lastSensorServicesCheckTimeInMs + AppAlarmService.START_SENSORS_CHECK_PERIOD))) {
                sensorLocationUpdateService.startSensorBasedUpdates(0);
                AppPreference.setLastSensorServicesCheckTimeInMs(getBaseContext(), now.getTimeInMillis());
            }
            if (notificationForLocation)  {
                locationUpdateService.startLocationAndWeatherUpdate("NOTIFICATION");
                AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
                return Updated.BY_NOTIFICATION;
            }
        } else if ("OFF".equals(updateAutoPeriodStr)) {
            sensorLocationUpdateService.stopSensorBasedUpdates();
            if (notificationForLocation)  {
                locationUpdateService.startLocationAndWeatherUpdate("NOTIFICATION");
                AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
                return Updated.BY_NOTIFICATION;
            }
        } else if (notificationForLocation || (now.getTimeInMillis() >= (location.getLastLocationUpdate() + updateAutoPeriodMills))) {
            sensorLocationUpdateService.stopSensorBasedUpdates();
            if (notificationForLocation) {
                locationUpdateService.startLocationAndWeatherUpdate("NOTIFICATION");
                AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
                return Updated.BY_NOTIFICATION;
            } else {
                locationUpdateService.startLocationAndWeatherUpdate();
                return Updated.REGULARLY;
            }
        }
        return Updated.NOTHING;
    }

    private Updated performUpdateOfWeather(Calendar now,
                                        Location location,
                                        String updatePeriodStr,
                                        long updatePeriodMills,
                                        boolean notificationForLocation) {
        if (!notificationForLocation &&
                ("0".equals(updatePeriodStr) || (now.getTimeInMillis() < (location.getLastLocationUpdate() + updatePeriodMills)))) {
            return Updated.NOTHING;
        }
        if (notificationForLocation) {
            sendMessageToCurrentWeatherService(location, "NOTIFICATION", AppWakeUpManager.SOURCE_NOTIFICATION, true);
            AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
            sendMessageToWeatherForecastService(location.getId());
            return Updated.BY_NOTIFICATION;
        } else {
            sendMessageToCurrentWeatherService(location, AppWakeUpManager.SOURCE_CURRENT_WEATHER, true);
            sendMessageToWeatherForecastService(location.getId());
            return Updated.REGULARLY;
        }
    }

    private Location checkNotificationAndReturnLocationForNotification(Calendar now,
                                                                       boolean notificationEnabled,
                                                                       long notificationPeriodMillis,
                                                                       long lastNotificationTimeInMs) {
        if (!notificationEnabled) {
            return null;
        }
        Location currentLocation = getLocationForNotification();
        if (currentLocation == null) {
            return null;
        }
        if (now.getTimeInMillis() < (lastNotificationTimeInMs + notificationPeriodMillis)) {
            return null;
        }
        return currentLocation;
    }

    private Location getLocationForNotification() {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }

    private long getNextTimeForNotification(long currentAlarmWakeup, Calendar now, long lastUpdate, long updatePeriod, boolean updatedNow) {
        long nextUpdateForLocation;
        appendLog(getBaseContext(), TAG, "updatedNow=" + updatedNow + ", updatePeriod=" + updatePeriod);
        if (updatedNow || (updatePeriod == 0)) {
            nextUpdateForLocation = updatePeriod;
        } else {
            nextUpdateForLocation = (lastUpdate + updatePeriod) - now.getTimeInMillis();
        }
        if ((nextUpdateForLocation <= 0) || (currentAlarmWakeup < nextUpdateForLocation)) {
            return currentAlarmWakeup;
        }
        return nextUpdateForLocation;
    }

    private ServiceConnection locationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LocationUpdateService.LocationUpdateServiceBinder binder =
                    (LocationUpdateService.LocationUpdateServiceBinder) service;
            locationUpdateService = binder.getService();
            appendLog(getBaseContext(), TAG, "got locationUpdateServiceConnection");
            performUpdateOfLocation();
            serviceConnected(locationUpdateServiceConnection);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            locationUpdateService = null;
        }
    };

    private ServiceConnection sensorLocationUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SensorLocationUpdateService.SensorLocationUpdateServiceBinder binder =
                    (SensorLocationUpdateService.SensorLocationUpdateServiceBinder) service;
            sensorLocationUpdateService = binder.getService();
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            Calendar now = Calendar.getInstance();
            String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(getBaseContext());
            long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
            String updatePeriodStr = AppPreference.getLocationUpdatePeriod(getBaseContext());
            long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
            boolean notificationEnabled = AppPreference.isNotificationEnabled(getBaseContext());
            String notificationPeriodStr = AppPreference.getInterval(getBaseContext());
            long notificationPeriodMillis = Utils.intervalMillisForAlarm(notificationPeriodStr);
            long lastNotificationTimeInMs = AppPreference.getLastNotificationTimeInMs(getBaseContext());
            Location locationForNotification = checkNotificationAndReturnLocationForNotification(
                    now,
                    notificationEnabled,
                    notificationPeriodMillis,
                    lastNotificationTimeInMs);

            appendLog(getBaseContext(),
                    TAG,
                    "updateAutoPeriodStr:" + updateAutoPeriodStr +
                            ", updatePeriodStr:" + updatePeriodStr +
                            ", notificationPeriodStr:" + notificationPeriodStr);
            long nextAlarmWakeup = AppAlarmService.START_SENSORS_CHECK_PERIOD;
            appendLog(getBaseContext(), TAG, "1:nextAlarmWakeup=" + nextAlarmWakeup);
            List<Location> locations = locationsDbHelper.getAllRows();
            for (Location location: locations) {
                appendLog(getBaseContext(),
                        TAG,
                        "location:" + location +
                                ", location.isEnabled:" + location.isEnabled());
                boolean notificationForLocation = (locationForNotification != null) && location.getId().equals(locationForNotification.getId());
                if ((location.getOrderId() == 0) && (location.isEnabled())) {
                    long lastUpdate = location.getLastLocationUpdate();
                    Updated updated = performUpdateOfAutolocation(now, location, updateAutoPeriodStr, updateAutoPeriodMills, notificationForLocation);
                    nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup, now, lastUpdate, updateAutoPeriodMills, !Updated.NOTHING.equals(updated));
                    appendLog(getBaseContext(), TAG, "2:nextAlarmWakeup=" + nextAlarmWakeup);
                    if (notificationEnabled) {
                        nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup,
                                                                     now,
                                                                     lastNotificationTimeInMs,
                                                                     notificationPeriodMillis,
                                                                     Updated.BY_NOTIFICATION.equals(updated));
                        appendLog(getBaseContext(), TAG, "3:nextAlarmWakeup=" + nextAlarmWakeup);
                    }
                } else if ((location.getOrderId() != 0) /*location.isEnabled()*/) {
                    long lastUpdate = location.getLastLocationUpdate();
                    Updated updated = performUpdateOfWeather(now, location, updatePeriodStr, updatePeriodMills, notificationForLocation);
                    nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup, now, lastUpdate, updatePeriodMills, !Updated.NOTHING.equals(updated));
                    appendLog(getBaseContext(), TAG, "4:nextAlarmWakeup=" + nextAlarmWakeup);
                    if (notificationEnabled) {
                        nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup,
                                now,
                                lastNotificationTimeInMs,
                                notificationPeriodMillis,
                                Updated.BY_NOTIFICATION.equals(updated));
                        appendLog(getBaseContext(), TAG, "5:nextAlarmWakeup=" + nextAlarmWakeup);
                    }
                }
            }
            long nextTimeForLog = now.getTimeInMillis() + nextAlarmWakeup;
            appendLog(getBaseContext(), TAG, "1:nextTimeForLog=" + nextTimeForLog);
            appendLog(getBaseContext(),
                    TAG,
                    "next scheduler time:" + DATE_FORMATTER.format(new Date(nextTimeForLog)));
            reScheduleNextAlarm(JOB_ID, nextAlarmWakeup, StartAutoLocationJob.class);
            serviceConnected(sensorLocationUpdateServiceConnection);
            if (currentWeatherUnsentMessages.isEmpty() && weatherForecastUnsentMessages.isEmpty()) {
                jobFinished(params, false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    private ServiceConnection screenOnOffUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder binder =
                    (ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder) service;
            screenOnOffUpdateService = binder.getService();
            screenOnOffUpdateService.startSensorBasedUpdates();
            new Thread(new Runnable() {
                public void run() {
                    serviceConnected(screenOnOffUpdateServiceConnection);
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
}