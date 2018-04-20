package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.model.CurrentWeather;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.AppWakeUpManager;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetService;
import org.json.JSONException;

import org.thosp.yourlocalweather.WeatherJSONParser;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.widget.LessWidgetService;
import org.thosp.yourlocalweather.widget.MoreWidgetService;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class CurrentWeatherService extends Service {

    private static final String TAG = "WeatherService";

    public static final String ACTION_WEATHER_UPDATE_OK = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_OK";
    public static final String ACTION_WEATHER_UPDATE_FAIL = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_FAIL";
    public static final String ACTION_WEATHER_UPDATE_RESULT = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_RESULT";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private String updateSource;
    private volatile boolean gettingWeatherStarted;
    private Location currentLocation;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
                        
            if (!gettingWeatherStarted) {
                return;
            }

            if (currentLocation == null) {
                appendLog(getBaseContext(), TAG, "timerRunnable, currentLocation is null");
                return;
            }

            String originalUpdateState = currentLocation.getLocationSource();
            appendLog(getBaseContext(), TAG, "originalUpdateState:" + originalUpdateState);
            String newUpdateState = originalUpdateState;
            if (originalUpdateState.contains("N")) {
                appendLog(getBaseContext(), TAG, "originalUpdateState contains N");
                newUpdateState = originalUpdateState.replace("N", "L");
            } else if (originalUpdateState.contains("G")) {
                newUpdateState = "L";
            }
            appendLog(getBaseContext(), TAG, "currentLocation:" + currentLocation + ", newUpdateState:" + newUpdateState);
            LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
            if (currentLocation != null) {
                locationsDbHelper.updateLocationSource(currentLocation.getId(), newUpdateState);
                currentLocation = locationsDbHelper.getLocationById(currentLocation.getId());
            }
            sendResult(ACTION_WEATHER_UPDATE_FAIL, null, getBaseContext());
        }
    };
    
    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return ret;
        }
        
        if (intent.getExtras() != null) {
            String currentUpdateSource = intent.getExtras().getString("updateSource");
            if(!TextUtils.isEmpty(currentUpdateSource)) {
                updateSource = currentUpdateSource;
            }
            currentLocation = intent.getExtras().getParcelable("location");
        }

        if (currentLocation == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "current location is null");
            return ret;
        }

        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        if (!connectionDetector.isNetworkAvailableAndConnected()) {
            return ret;
        }

        if (gettingWeatherStarted) {
            AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + 10000, pendingIntent);
        }

        gettingWeatherStarted = true;
        timerHandler.postDelayed(timerRunnable, 20000);
        final Context context = this;
        startRefreshRotation();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                final String locale = currentLocation.getLocale();
                appendLog(context,
                        TAG,
                        "weather get params: latitude:" +
                        currentLocation.getLatitude() +
                        ", longitude" +
                        currentLocation.getLongitude());
                try {
                    AppWakeUpManager.getInstance(getBaseContext()).wakeUp();
                    client.get(Utils.getWeatherForecastUrl(Constants.WEATHER_ENDPOINT,
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            "metric",
                            locale).toString(), null, new AsyncHttpResponseHandler() {

                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            try {
                                AppWakeUpManager.getInstance(getBaseContext()).wakeDown();
                                String weatherRaw = new String(response);
                                appendLog(context, TAG, "weather got, result:" + weatherRaw);

                                Weather weather = WeatherJSONParser.getWeather(weatherRaw);
                                timerHandler.removeCallbacksAndMessages(null);
                                saveWeatherAndSendResult(context, weather);
                            } catch (JSONException e) {
                                appendLog(context, TAG, "JSONException:" + e);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, null, context);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            AppWakeUpManager.getInstance(getBaseContext()).wakeDown();
                            appendLog(context, TAG, "onFailure:" + statusCode);
                            sendResult(ACTION_WEATHER_UPDATE_FAIL, null, context);
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    });
                } catch (MalformedURLException mue) {
                    appendLog(context, TAG, "MalformedURLException:" + mue);
                    sendResult(ACTION_WEATHER_UPDATE_FAIL, null, context);
                }
            }
        };
        mainHandler.post(myRunnable);
        return ret;
    }

    private void sendResult(String result, Weather weather, Context context) {
        stopRefreshRotation();
        gettingWeatherStarted = false;
        try {
            startService(new Intent(getBaseContext(), LessWidgetService.class));
            startService(new Intent(getBaseContext(), MoreWidgetService.class));
            startService(new Intent(getBaseContext(), ExtLocationWidgetService.class));
            if (updateSource != null) {
                switch (updateSource) {
                    case "MAIN":
                        sendIntentToMain(result, weather);
                        break;
                    case "NOTIFICATION":
                        startService(new Intent(getBaseContext(), NotificationService.class));
                        break;
                }
            }
        } catch (Throwable exception) {
            appendLog(context, TAG, "Exception occured when starting the service:", exception);
        }
    }

    private void saveWeatherAndSendResult(Context context, Weather weather) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Long locationId = locationsDbHelper.getLocationIdByCoordinates(weather.getLat(), weather.getLon());
        if (locationId == null) {
            appendLog(context,
                    TAG,
                    "Weather not saved because there is no location with coordinates:" +
                            weather.getLat() +
                            ", " +
                            weather.getLon());
            sendResult(ACTION_WEATHER_UPDATE_FAIL, null, context);
            return;
        }
        currentLocation = locationsDbHelper.getLocationById(locationId);
        String locationSource = currentLocation.getLocationSource();
        if ((locationSource == null) || "-".equals(locationSource)) {
            locationSource = "W";
        }
        appendLog(context,
                TAG,
                "Location source is:" + locationSource);

        long now = System.currentTimeMillis();
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        currentWeatherDbHelper.saveWeather(locationId, now, weather);
        locationsDbHelper.updateLastUpdatedAndLocationSource(locationId, now, locationSource);
        currentLocation = locationsDbHelper.getLocationById(locationId);
        sendResult(ACTION_WEATHER_UPDATE_OK, weather, context);
    }
    
    private void sendIntentToMain(String result, Weather weather) {
        Intent intent = new Intent(ACTION_WEATHER_UPDATE_RESULT);
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_WEATHER_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_WEATHER_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.START_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
    }

    private void stopRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.STOP_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startService(sendIntent);
    }
}
