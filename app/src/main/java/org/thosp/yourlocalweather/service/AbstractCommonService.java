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
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetService;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetWithForecastService;
import org.thosp.yourlocalweather.widget.LessWidgetService;
import org.thosp.yourlocalweather.widget.MoreWidgetService;
import org.thosp.yourlocalweather.widget.WeatherForecastWidgetService;

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
        startRefreshRotation();
        Intent sendIntent = new Intent("android.intent.action.START_LOCATION_ONLY_UPDATE");
        sendIntent.putExtra("destinationPackageName", "org.thosp.yourlocalweather");
        sendIntent.putExtra("byLastLocationOnly", byLastLocationOnly);
        sendIntent.putExtra("isInteractive", isInteractive);
        startBackgroundService(sendIntent);
    }

    protected void updateWidgets() {
        stopRefreshRotation();
        startBackgroundService(new Intent(getBaseContext(), LessWidgetService.class));
        startBackgroundService(new Intent(getBaseContext(), MoreWidgetService.class));
        startBackgroundService(new Intent(getBaseContext(), ExtLocationWidgetService.class));
        startBackgroundService(new Intent(getBaseContext(), ExtLocationWidgetWithForecastService.class));
        startBackgroundService(new Intent(getBaseContext(), WeatherForecastWidgetService.class));
        if (updateSource != null) {
            switch (updateSource) {
                case "MAIN":
                    sendIntentToMain();
                    break;
                case "NOTIFICATION":
                    startBackgroundService(new Intent(getBaseContext(), NotificationService.class));
                    break;
            }
        }
    }

    protected void sendIntentToMain() {
        Intent intent = new Intent(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT);
        intent.putExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT, CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL);
        startBackgroundService(intent);
    }

    protected void requestWeatherCheck(String locationSource, boolean isInteractive) {
        appendLog(getBaseContext(), TAG, "startRefreshRotation");
        startRefreshRotation();
        boolean updateLocationInProcess = LocationUpdateService.updateLocationInProcess;
        appendLog(getBaseContext(), TAG, "requestWeatherCheck, updateLocationInProcess=" +
                updateLocationInProcess);
        if (updateLocationInProcess) {
            updateWidgets();
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
        startBackgroundService(intentToCheckWeather);
        startWeatherForecastUpdate(currentLocation.getId());
    }

    private void startWeatherForecastUpdate(long locationId) {
        if (!ForecastUtil.shouldUpdateForecast(this, locationId)) {
            return;
        }
        Intent intentToCheckWeather = new Intent(this, ForecastWeatherService.class);
        intentToCheckWeather.putExtra("locationId", locationId);
        startBackgroundService(intentToCheckWeather);
    }

    protected void startRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.START_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent);
    }

    protected void stopRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.STOP_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent);
    }

    protected boolean isInteractive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return powerManager.isInteractive();
        } else {
            return powerManager.isScreenOn();
        }
    }

    protected void startBackgroundService(Intent intent) {
        try {
            if (isInteractive()) {
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
