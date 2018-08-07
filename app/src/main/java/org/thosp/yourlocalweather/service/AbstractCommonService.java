package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetService;
import org.thosp.yourlocalweather.widget.LessWidgetService;
import org.thosp.yourlocalweather.widget.MoreWidgetService;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AbstractCommonService extends Service {

    private static final String TAG = "AbstractCommonService";

    protected PowerManager powerManager;
    protected String updateSource;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }

    protected void updateNetworkLocation(boolean byLastLocationOnly,
                                         boolean isInteractive) {
        Intent sendIntent = new Intent("android.intent.action.START_LOCATION_ONLY_UPDATE");
        sendIntent.putExtra("destinationPackageName", "org.thosp.yourlocalweather");
        sendIntent.putExtra("byLastLocationOnly", byLastLocationOnly);
        sendIntent.putExtra("isInteractive", isInteractive);
        startBackgroundService(sendIntent, false);
    }

    protected void updateWidgets(boolean isInteractive) {
        stopRefreshRotation(isInteractive);
        startBackgroundService(new Intent(getBaseContext(), LessWidgetService.class), isInteractive);
        startBackgroundService(new Intent(getBaseContext(), MoreWidgetService.class), isInteractive);
        startBackgroundService(new Intent(getBaseContext(), ExtLocationWidgetService.class), isInteractive);
        if (updateSource != null) {
            switch (updateSource) {
                case "MAIN":
                    sendIntentToMain(isInteractive);
                    break;
                case "NOTIFICATION":
                    startBackgroundService(new Intent(getBaseContext(), NotificationService.class), false);
                    break;
            }
        }
    }

    protected void sendIntentToMain(boolean isInteractive) {
        Intent intent = new Intent(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        intent.putExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT, CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        startBackgroundService(intent, isInteractive);
    }

    protected void requestWeatherCheck(String locationSource, boolean isInteractive) {
        appendLog(getBaseContext(), TAG, "startRefreshRotation");
        startRefreshRotation(isInteractive);
        boolean updateLocationInProcess = LocationUpdateService.updateLocationInProcess;
        appendLog(getBaseContext(), TAG, "requestWeatherCheck, updateLocationInProcess=" +
                updateLocationInProcess);
        if (updateLocationInProcess) {
            updateWidgets(isInteractive);
            return;
        }
        updateNetworkLocation(true, isInteractive);
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        org.thosp.yourlocalweather.model.Location currentLocation = locationsDbHelper.getLocationByOrderId(0);
        if (locationSource != null) {
            locationsDbHelper.updateLocationSource(currentLocation.getId(), "-");
            currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
        }
        sendIntentToGetWeather(currentLocation, isInteractive);
    }

    protected void sendIntentToGetWeather(org.thosp.yourlocalweather.model.Location currentLocation, boolean isInteractive) {
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        currentWeatherDbHelper.updateLastUpdatedTime(currentLocation.getId(), System.currentTimeMillis());
        Intent intentToCheckWeather = new Intent(getBaseContext(), CurrentWeatherService.class);
        intentToCheckWeather.putExtra("locationId", currentLocation.getId());
        intentToCheckWeather.putExtra("updateSource", updateSource);
        intentToCheckWeather.putExtra("isInteractive", isInteractive);
        startBackgroundService(intentToCheckWeather, isInteractive);
    }

    protected void startRefreshRotation(boolean isInteractive) {
        Intent sendIntent = new Intent("android.intent.action.START_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent, isInteractive);
    }

    protected void stopRefreshRotation(boolean isInteractive) {
        Intent sendIntent = new Intent("android.intent.action.STOP_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent, isInteractive);
    }

    protected boolean isInteractive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }

    protected void startBackgroundService(Intent intent, boolean isInteractive) {
        try {
            if (isInteractive) {
                getBaseContext().startService(intent);
                return;
            }
        } catch (Exception ise) {
            //
        }
        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10,
                    pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10,
                    pendingIntent);
        }
    }
}
