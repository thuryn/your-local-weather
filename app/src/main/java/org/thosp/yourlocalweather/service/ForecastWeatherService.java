package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.WeatherJSONParser;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.AppWakeUpManager;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetService;
import org.thosp.yourlocalweather.widget.ExtLocationWidgetWithForecastService;
import org.thosp.yourlocalweather.widget.LessWidgetService;
import org.thosp.yourlocalweather.widget.MoreWidgetService;
import org.thosp.yourlocalweather.widget.WeatherForecastWidgetService;

import java.net.MalformedURLException;

import cz.msebera.android.httpclient.Header;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ForecastWeatherService  extends Service {

    private static final String TAG = "ForecastWeatherService";

    public static final String ACTION_WEATHER_UPDATE_OK = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_OK";
    public static final String ACTION_WEATHER_UPDATE_FAIL = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_FAIL";
    public static final String ACTION_FORECAST_UPDATE_RESULT = "org.thosp.yourlocalweather.action.FORECAST_UPDATE_RESULT";
    public static final String ACTION_GRAPHS_UPDATE_RESULT = "org.thosp.yourlocalweather.action.GRAPHS_UPDATE_RESULT";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private String updateSource;
    private volatile boolean gettingWeatherStarted;
    private Location currentLocation;
    private boolean isInteractive;

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
            sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext());
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        appendLog(getBaseContext(), TAG, "onStartCommand:" + intent);

        if (intent == null) {
            return ret;
        }

        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

        if (intent.getExtras() != null) {
            isInteractive = intent.getBooleanExtra("isInteractive", false);
            String currentUpdateSource = intent.getExtras().getString("updateSource");
            if (!TextUtils.isEmpty(currentUpdateSource)) {
                updateSource = currentUpdateSource;
            }
            currentLocation = locationsDbHelper.getLocationById(intent.getExtras().getLong("locationId"));
        }
        appendLog(getBaseContext(), TAG, "currentLocation=" + currentLocation + ", updateSource=" + updateSource);

        if (currentLocation == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "current location is null");
            return ret;
        }

        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        boolean networkAvailableAndConnected = connectionDetector.isNetworkAvailableAndConnected();
        appendLog(getBaseContext(), TAG, "networkAvailableAndConnected=" + networkAvailableAndConnected);
        if (!networkAvailableAndConnected) {
            int numberOfAttempts = intent.getIntExtra("attempts", 0);
            appendLog(getBaseContext(), TAG, "numberOfAttempts=" + numberOfAttempts);
            intent.putExtra("attempts", ++numberOfAttempts);
            resendTheIntentInSeveralSeconds(20, intent);
            return ret;
        }

        if (gettingWeatherStarted) {
            resendTheIntentInSeveralSeconds(10, intent);
        }

        gettingWeatherStarted = true;
        timerHandler.postDelayed(timerRunnable, 20000);
        final Context context = this;
        appendLog(getBaseContext(), TAG, "startRefreshRotation");
        startRefreshRotation();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentLocation == null) {
                    appendLog(context,
                            TAG,
                            "currentLocation is null");
                    return;
                }
                final String locale = currentLocation.getLocale();
                appendLog(context,
                        TAG,
                        "weather get params: latitude:" +
                                currentLocation.getLatitude() +
                                ", longitude" +
                                currentLocation.getLongitude());
                try {
                    AppWakeUpManager.getInstance(getBaseContext()).wakeUp();
                    client.get(Utils.getWeatherForecastUrl(
                            Constants.WEATHER_FORECAST_ENDPOINT,
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            "metric",
                            currentLocation.getLocale()).toString(), null, new AsyncHttpResponseHandler() {

                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] weatherForecastResponse) {
                            try {
                                AppWakeUpManager.getInstance(getBaseContext()).wakeDown();
                                String weatherForecastRaw = new String(weatherForecastResponse);
                                appendLog(context, TAG, "weather got, result:" + weatherForecastRaw);

                                CompleteWeatherForecast completeWeatherForecast = WeatherJSONParser.getWeatherForecast(
                                        context,
                                        currentLocation.getId(),
                                        weatherForecastRaw);
                                timerHandler.removeCallbacksAndMessages(null);
                                saveWeatherAndSendResult(context, completeWeatherForecast);
                            } catch (JSONException e) {
                                appendLog(context, TAG, "JSONException:" + e);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            AppWakeUpManager.getInstance(getBaseContext()).wakeDown();
                            appendLog(context, TAG, "onFailure:" + statusCode);
                            sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    });
                } catch (MalformedURLException mue) {
                    appendLog(context, TAG, "MalformedURLException:" + mue);
                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
                }
            }
        };
        mainHandler.post(myRunnable);
        return START_STICKY;
    }

    private void sendResult(String result, Context context) {
        stopRefreshRotation();
        gettingWeatherStarted = false;
        try {
            startBackgroundService(new Intent(getBaseContext(), LessWidgetService.class));
            startBackgroundService(new Intent(getBaseContext(), MoreWidgetService.class));
            startBackgroundService(new Intent(getBaseContext(), ExtLocationWidgetService.class));
            startBackgroundService(new Intent(getBaseContext(), ExtLocationWidgetWithForecastService.class));
            startBackgroundService(new Intent(getBaseContext(), WeatherForecastWidgetService.class));
            if (updateSource != null) {
                switch (updateSource) {
                    case "FORECAST":
                        sendIntentToForecast(result);
                        break;
                    case "GRAPHS":
                        sendIntentToGraphs(result);
                        break;
                }
            }
        } catch (Throwable exception) {
            appendLog(context, TAG, "Exception occured when starting the service:", exception);
        }
    }

    private void saveWeatherAndSendResult(Context context, CompleteWeatherForecast completeWeatherForecast) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        long lastUpdate = System.currentTimeMillis();
        weatherForecastDbHelper.saveWeatherForecast(currentLocation.getId(),
                lastUpdate,
                completeWeatherForecast);
        sendResult(ACTION_WEATHER_UPDATE_OK, context);
    }

    private void sendIntentToForecast(String result) {
        Intent intent = new Intent(ACTION_FORECAST_UPDATE_RESULT);
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_FORECAST_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_FORECAST_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        sendBroadcast(intent);
    }

    private void sendIntentToGraphs(String result) {
        Intent intent = new Intent(ACTION_GRAPHS_UPDATE_RESULT);
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_GRAPHS_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_GRAPHS_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        sendBroadcast(intent);
    }

    private void startRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.START_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent);
    }

    private void stopRefreshRotation() {
        Intent sendIntent = new Intent("android.intent.action.STOP_ROTATING_UPDATE");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        startBackgroundService(sendIntent);
    }

    private void startBackgroundService(Intent intent) {
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
                    SystemClock.elapsedRealtime() + 500,
                    pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 500,
                    pendingIntent);
        }
    }

    private void resendTheIntentInSeveralSeconds(int seconds, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + (1000 + seconds), pendingIntent);
    }
}
