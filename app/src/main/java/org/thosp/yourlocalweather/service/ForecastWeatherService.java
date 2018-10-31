package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
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
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.Queue;

import cz.msebera.android.httpclient.Header;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ForecastWeatherService  extends AbstractCommonService {

    private static final String TAG = "ForecastWeatherService";

    public static final int START_WEATHER_FORECAST_UPDATE = 1;
    public static final int START_WEATHER_FORECAST_RETRY = 2;

    public static final String ACTION_WEATHER_UPDATE_OK = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_OK";
    public static final String ACTION_WEATHER_UPDATE_FAIL = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_FAIL";
    public static final String ACTION_FORECAST_UPDATE_RESULT = "org.thosp.yourlocalweather.action.FORECAST_UPDATE_RESULT";
    public static final String ACTION_GRAPHS_UPDATE_RESULT = "org.thosp.yourlocalweather.action.GRAPHS_UPDATE_RESULT";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private volatile boolean gettingWeatherStarted;
    private static Queue<WeatherRequestDataHolder> weatherForecastUpdateMessages = new LinkedList<>();
    final Messenger messenger = new Messenger(new WeatherForecastMessageHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
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
        startWeatherForecastUpdate(0);
        return ret;
    }

    public void startWeatherForecastUpdate(long incommingMessageTimestamp) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        WeatherRequestDataHolder updateRequest = weatherForecastUpdateMessages.peek();

        if ((updateRequest == null) || (updateRequest.getTimestamp() < incommingMessageTimestamp)) {
            return;
        }
        final Location currentLocation = locationsDbHelper.getLocationById(updateRequest.getLocationId());
        appendLog(getBaseContext(), TAG, "currentLocation=" + currentLocation + ", updateSource=" + updateRequest.getUpdateSource());

        if (currentLocation == null) {
            if (updateRequest != null) {
                appendLog(getBaseContext(),
                        TAG,
                        "updateRequest is older than current");
            } else {
                appendLog(getBaseContext(),
                        TAG,
                        "updateRequest is null");
            }
            return;
        }

        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        boolean networkAvailableAndConnected = connectionDetector.isNetworkAvailableAndConnected();
        appendLog(getBaseContext(), TAG, "networkAvailableAndConnected=" + networkAvailableAndConnected);
        if (!networkAvailableAndConnected) {
            int numberOfAttempts = updateRequest.getAttempts();
            appendLog(getBaseContext(), TAG, "numberOfAttempts=" + numberOfAttempts);
            if (numberOfAttempts > 2) {
                locationsDbHelper.updateLocationSource(
                        currentLocation.getId(),
                        ".");
                weatherForecastUpdateMessages.poll();
                return;
            }
            updateRequest.increaseAttempts();
            resendTheIntentInSeveralSeconds(20);
            return;
        }

        if (gettingWeatherStarted) {
            resendTheIntentInSeveralSeconds(10);
        }

        gettingWeatherStarted = true;
        timerHandler.postDelayed(timerRunnable, 20000);
        final Context context = this;
        appendLog(getBaseContext(), TAG, "startRefreshRotation");
        startRefreshRotation("START", 1);
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
                    sendMessageToWakeUpService(
                            AppWakeUpManager.WAKE_UP,
                            AppWakeUpManager.SOURCE_WEATHER_FORECAST
                    );
                    client.get(Utils.getWeatherForecastUrl(
                            context,
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
                            appendLog(context, TAG, "onFailure:" + statusCode);
                            timerHandler.removeCallbacksAndMessages(null);
                            if (statusCode == 401) {
                                locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                        System.currentTimeMillis(), "E");

                            } else if (statusCode == 429) {
                                locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                        System.currentTimeMillis(), "B");

                            }
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
    }

    private void sendResult(String result, Context context) {
        stopRefreshRotation("STOP", 1);
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_WEATHER_FORECAST
        );
        gettingWeatherStarted = false;
        WeatherRequestDataHolder updateRequest = weatherForecastUpdateMessages.poll();
        try {
            if (ACTION_WEATHER_UPDATE_OK.equals(result)) {
                startWeatherForecastUpdate(0);
            }
            String updateSource = updateRequest.getUpdateSource();
            if (updateSource != null) {
                WidgetUtils.updateWidgets(context);
                switch (updateSource) {
                    case "FORECAST":
                        sendIntentToForecast(result);
                        break;
                    case "GRAPHS":
                        sendIntentToGraphs(result);
                        break;
                    case "MAIN":
                        sendIntentToMain(result);
                        break;
                }
            }
            if (WidgetRefreshIconService.isRotationActive) {
                return;
            }
            WidgetUtils.updateWidgets(getBaseContext());
        } catch (Throwable exception) {
            appendLog(context, TAG, "Exception occured when starting the service:", exception);
        }
    }

    private void saveWeatherAndSendResult(Context context, CompleteWeatherForecast completeWeatherForecast) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        long lastUpdate = System.currentTimeMillis();
        WeatherRequestDataHolder updateRequest = weatherForecastUpdateMessages.peek();
        if (updateRequest == null) {
            return;
        }
        weatherForecastDbHelper.saveWeatherForecast(updateRequest.getLocationId(),
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

    private void resendTheIntentInSeveralSeconds(int seconds) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            ComponentName serviceComponent = new ComponentName(this, WeatherForecastResendJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
            builder.setMinimumLatency(seconds * 1000); // wait at least
            builder.setOverrideDeadline((3 + seconds) * 1000); // maximum delay
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
        } else {
            AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                    0,
                    new Intent(getBaseContext(), ForecastWeatherService.class),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (1000 * seconds), pendingIntent);
        }
    }

    private class WeatherForecastMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            WeatherRequestDataHolder weatherRequestDataHolder = (WeatherRequestDataHolder) msg.obj;
            appendLog(getBaseContext(), TAG, "handleMessage:" + msg.what + ":" + weatherRequestDataHolder);
            switch (msg.what) {
                case START_WEATHER_FORECAST_UPDATE:
                    weatherForecastUpdateMessages.add(weatherRequestDataHolder);
                    startWeatherForecastUpdate(weatherRequestDataHolder.getTimestamp());
                    break;
                case START_WEATHER_FORECAST_RETRY:
                    startWeatherForecastUpdate(0);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
