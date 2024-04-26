package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.Utils;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogWithDate;

import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

@TargetApi(Build.VERSION_CODES.M)
public class StartAutoLocationJob extends AbstractAppJob {
    private static final String TAG = "StartAutoLocationJob";
    public static final int JOB_ID = 1992056442;

    private enum Updated {
        REGULARLY,
        BY_NOTIFICATION,
        NOTHING
    }

    private JobParameters params;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setNotification(params, NotificationUtils.NOTIFICATION_ID, NotificationUtils.getNotificationForActivity(this),
                    JobService.JOB_END_NOTIFICATION_POLICY_DETACH);
        }
        YourLocalWeather.executor.submit(() -> {
            appendLog(this, TAG, "onStartJob");
            try {
                performUpdateOfLocation();
                startSensorBasedUpdates();
            } catch (Exception ie) {
                appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
            }
        });
        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private void performUpdateOfLocation() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        Calendar now = Calendar.getInstance();
        String updateAutoPeriodStr = AppPreference.getInstance().getLocationAutoUpdatePeriod(getBaseContext());
        long updateAutoPeriodMills = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
        String updatePeriodStr = AppPreference.getInstance().getLocationUpdatePeriod(getBaseContext());
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        boolean notificationEnabled = AppPreference.getInstance().isNotificationEnabled(getBaseContext());
        String notificationPeriodStr = AppPreference.getInterval(getBaseContext());
        if ("regular_only".equals(notificationPeriodStr)) {
            notificationEnabled = false;
        }
        long notificationPeriodMillis = Utils.intervalMillisForAlarm(notificationPeriodStr);
        long lastNotificationTimeInMs = AppPreference.getLastNotificationTimeInMs(getBaseContext());
        Location locationForNotification = checkNotificationAndReturnLocationForNotification(
                now,
                notificationEnabled,
                notificationPeriodMillis,
                lastNotificationTimeInMs);

        appendLog(getBaseContext(),
                TAG,
                "updateAutoPeriodStr:", updateAutoPeriodStr,
                ", updatePeriodStr:", updatePeriodStr,
                ", notificationPeriodStr:", notificationPeriodStr);
        long nextAlarmWakeup = AppAlarmService.START_SENSORS_CHECK_PERIOD;
        appendLog(getBaseContext(), TAG, "1:nextAlarmWakeup=", nextAlarmWakeup);
        List<Location> locations = locationsDbHelper.getAllRows();
        for (Location location: locations) {
            appendLog(getBaseContext(),
                    TAG,
                    "location:", location,
                    ", location.isEnabled:", location.isEnabled());
            boolean notificationForLocation = (locationForNotification != null) && location.getId().equals(locationForNotification.getId());
            if ((location.getOrderId() == 0) && (location.isEnabled())) {
                long lastUpdate = location.getLastLocationUpdate();
                Updated updated = performUpdateOfAutolocation(now, location, updateAutoPeriodStr, updateAutoPeriodMills, notificationForLocation);
                nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup, now, lastUpdate, updateAutoPeriodMills, !Updated.NOTHING.equals(updated));
                appendLog(getBaseContext(), TAG, "2:nextAlarmWakeup=", nextAlarmWakeup);
                if (notificationEnabled) {
                    nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup,
                            now,
                            lastNotificationTimeInMs,
                            notificationPeriodMillis,
                            Updated.BY_NOTIFICATION.equals(updated));
                    appendLog(getBaseContext(), TAG, "3:nextAlarmWakeup=", nextAlarmWakeup);
                }
            } else if ((location.getOrderId() != 0) /*location.isEnabled()*/) {
                long lastUpdate = location.getLastLocationUpdate();
                Updated updated = performUpdateOfWeather(now, location, updatePeriodStr, updatePeriodMills, notificationForLocation);
                nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup, now, lastUpdate, updatePeriodMills, !Updated.NOTHING.equals(updated));
                appendLog(getBaseContext(), TAG, "4:nextAlarmWakeup=", nextAlarmWakeup);
                if (notificationEnabled) {
                    nextAlarmWakeup = getNextTimeForNotification(nextAlarmWakeup,
                            now,
                            lastNotificationTimeInMs,
                            notificationPeriodMillis,
                            Updated.BY_NOTIFICATION.equals(updated));
                    appendLog(getBaseContext(), TAG, "5:nextAlarmWakeup=", nextAlarmWakeup);
                }
            }
        }
        long nextTimeForLog = now.getTimeInMillis() + nextAlarmWakeup;
        appendLog(getBaseContext(), TAG, "1:nextTimeForLog=", nextTimeForLog);
        appendLogWithDate(getBaseContext(),
                TAG,
                "next scheduler time:", nextTimeForLog);
        reScheduleNextAlarm(JOB_ID, nextAlarmWakeup, StartAutoLocationJob.class);
    }

    private Updated performUpdateOfAutolocation(Calendar now,
                                             Location location,
                                             String updateAutoPeriodStr,
                                             long updateAutoPeriodMills,
                                             boolean notificationForLocation) {
        long lastSensorServicesCheckTimeInMs = AppPreference.getLastSensorServicesCheckTimeInMs(getBaseContext());
        if ("0".equals(updateAutoPeriodStr)) {
            if ((now.getTimeInMillis() >= (lastSensorServicesCheckTimeInMs + AppAlarmService.START_SENSORS_CHECK_PERIOD))) {
                startSensorBasedUpdates();
                AppPreference.setLastSensorServicesCheckTimeInMs(getBaseContext(), now.getTimeInMillis());
            }
            if (notificationForLocation)  {
                startLocationAndWeatherUpdate("NOTIFICATION");
                AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
                return Updated.BY_NOTIFICATION;
            }
        } else if ("OFF".equals(updateAutoPeriodStr)) {
            stopSensorBasedUpdates();
            if (notificationForLocation)  {
                startLocationAndWeatherUpdate("NOTIFICATION");
                AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
                return Updated.BY_NOTIFICATION;
            }
        } else if (notificationForLocation || (now.getTimeInMillis() >= (location.getLastLocationUpdate() + updateAutoPeriodMills))) {
            stopSensorBasedUpdates();
            if (notificationForLocation) {
                startLocationAndWeatherUpdate("NOTIFICATION");
                AppPreference.setLastNotificationTimeInMs(getBaseContext(), now.getTimeInMillis());
                return Updated.BY_NOTIFICATION;
            } else {
                startLocationAndWeatherUpdate(false);
                return Updated.REGULARLY;
            }
        }
        return Updated.NOTHING;
    }

    public void startSensorBasedUpdates() {
        Intent sendIntent = new Intent("org.thosp.yourlocalweather.action.START_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        ContextCompat.startForegroundService(this, sendIntent);
    }

    public void stopSensorBasedUpdates() {
        Intent sendIntent = new Intent("org.thosp.yourlocalweather.action.STOP_SENSOR_BASED_UPDATES");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        ContextCompat.startForegroundService(this, sendIntent);
    }

    protected void sendIntent(String intent) {
        Intent sendIntent = new Intent(intent);
        sendIntent.setPackage("org.thosp.yourlocalweather");
        ContextCompat.startForegroundService(getBaseContext(), sendIntent);
    }

    private void startLocationAndWeatherUpdate(boolean forceUpdate) {
        appendLog(getBaseContext(), TAG, "startLocationAndWeatherUpdate:forceUpdate=", forceUpdate);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        intentToStartUpdate.putExtra("forceUpdate", forceUpdate);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
    }

    private void startLocationAndWeatherUpdate(String updateSource) {
        appendLog(getBaseContext(), TAG, "startLocationAndWeatherUpdate:2:updateSource=", updateSource);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        long locationId = locationsDbHelper.getLocationByOrderId(0).getId();
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("locationId", locationId);
        intentToStartUpdate.putExtra("updateSource", updateSource);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
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
        if ((currentLocation == null) || !currentLocation.isEnabled()) {
            currentLocation = locationsDbHelper.getLocationByOrderId(1);
        }
        return currentLocation;
    }

    private long getNextTimeForNotification(long currentAlarmWakeup, Calendar now, long lastUpdate, long updatePeriod, boolean updatedNow) {
        long nextUpdateForLocation;
        appendLog(getBaseContext(), TAG,
                "updatedNow=",
                updatedNow,
                ", updatePeriod=",
                updatePeriod);
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
}