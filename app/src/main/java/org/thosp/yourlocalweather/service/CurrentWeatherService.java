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
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.Queue;

import cz.msebera.android.httpclient.Header;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class CurrentWeatherService extends AbstractCommonService {

    private static final String TAG = "CurrentWeatherService";

    public static final int START_CURRENT_WEATHER_UPDATE = 1;
    public static final int START_CURRENT_WEATHER_RETRY = 2;
    public static final String ACTION_WEATHER_UPDATE_OK = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_OK";
    public static final String ACTION_WEATHER_UPDATE_FAIL = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_FAIL";
    public static final String ACTION_WEATHER_UPDATE_RESULT = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_RESULT";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private volatile boolean gettingWeatherStarted;
    private static Queue<WeatherRequestDataHolder> currentWeatherUpdateMessages = new LinkedList<>();
    final Messenger messenger = new Messenger(new CurrentweatherMessageHandler());

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
            final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

            WeatherRequestDataHolder updateRequest = currentWeatherUpdateMessages.peek();
            Location currentLocation = locationsDbHelper.getLocationById(updateRequest.getLocationId());
            if (currentLocation == null) {
                appendLog(getBaseContext(), TAG, "timerRunnable, currentLocation is null");
                return;
            }

            String originalUpdateState = currentLocation.getLocationSource();
            if (originalUpdateState == null) {
                originalUpdateState = "-";
            }
            appendLog(getBaseContext(), TAG, "originalUpdateState:" + originalUpdateState);
            String newUpdateState = originalUpdateState;
            if (originalUpdateState.contains("N")) {
                appendLog(getBaseContext(), TAG, "originalUpdateState contains N");
                newUpdateState = originalUpdateState.replace("N", "L");
            } else if (originalUpdateState.contains("G")) {
                newUpdateState = "L";
            }
            appendLog(getBaseContext(), TAG, "currentLocation:" + currentLocation + ", newUpdateState:" + newUpdateState);
            if (currentLocation != null) {
                locationsDbHelper.updateLocationSource(currentLocation.getId(), newUpdateState);
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
        startCurrentWeatherUpdate(0);
        return ret;
    }

    public void startCurrentWeatherUpdate(long incommingMessageTimestamp) {
        appendLog(getBaseContext(), TAG, "startCurrentWeatherUpdate");
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

        WeatherRequestDataHolder updateRequest = currentWeatherUpdateMessages.peek();

        if ((updateRequest == null) || (updateRequest.getTimestamp() < incommingMessageTimestamp)) {
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

        final Location currentLocation = locationsDbHelper.getLocationById(updateRequest.getLocationId());
        appendLog(getBaseContext(), TAG, "currentLocation=" + currentLocation + ", updateSource=" + updateRequest.getUpdateSource());
        if (currentLocation == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "current location is null");
            currentWeatherUpdateMessages.poll();
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
                currentWeatherUpdateMessages.poll();
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
        startRefreshRotation("START", 2);
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
                            AppWakeUpManager.SOURCE_CURRENT_WEATHER
                    );
                    client.get(Utils.getWeatherForecastUrl(
                            context,
                            Constants.WEATHER_ENDPOINT,
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
                                String weatherRaw = new String(response);
                                appendLog(context, TAG, "weather got, result:" + weatherRaw);

                                Weather weather = WeatherJSONParser.getWeather(weatherRaw);
                                timerHandler.removeCallbacksAndMessages(null);
                                saveWeatherAndSendResult(context, weather);
                            } catch (JSONException e) {
                                appendLog(context, TAG, "JSONException:" + e);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            appendLog(context, TAG, "onFailure:" + statusCode + ":currentLocation=" + currentLocation);
                            timerHandler.removeCallbacksAndMessages(null);
                            if (currentLocation != null) {
                                final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
                                if (statusCode == 401) {
                                    locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                            System.currentTimeMillis(), "E");

                                } else if (statusCode == 429) {
                                    locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                            System.currentTimeMillis(), "B");

                                } else {
                                    locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                            System.currentTimeMillis(), "L");
                                }
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
        stopRefreshRotation("STOP", 2);
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_CURRENT_WEATHER
        );
        gettingWeatherStarted = false;
        WeatherRequestDataHolder updateRequest = currentWeatherUpdateMessages.poll();
        try {
            if (ACTION_WEATHER_UPDATE_OK.equals(result)) {
                startCurrentWeatherUpdate(0);
            }
            String updateSource = updateRequest.getUpdateSource();
            if (updateSource != null) {
                switch (updateSource) {
                    case "MAIN":
                        WidgetUtils.updateCurrentWeatherWidgets(context);
                        sendIntentToMain(result);
                        break;
                    case "NOTIFICATION":
                        Intent sendIntent = new Intent("android.intent.action.SHOW_WEATHER_NOTIFICATION");
                        sendIntent.setPackage("org.thosp.yourlocalweather");
                        WidgetUtils.startBackgroundService(
                                getBaseContext(),
                                sendIntent);
                        break;
                }
            }
            if (WidgetRefreshIconService.isRotationActive) {
                return;
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
            sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
            return;
        }
        appendLog(getBaseContext(), TAG, "saveWeatherAndSendResult:locationId:" + locationId);
        Location currentLocation = locationsDbHelper.getLocationById(locationId);
        String locationSource = currentLocation.getLocationSource();
        appendLog(getBaseContext(), TAG, "saveWeatherAndSendResult:locationSource by location:" + locationSource);
        if ((currentLocation.getOrderId() > 0) ||
            (locationSource == null) ||
            "-".equals(locationSource) ||
            ".".equals(locationSource)) {
            locationSource = "W";
        }
        appendLog(context,
                TAG,
                "Location source is:" + locationSource);

        long now = System.currentTimeMillis();
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        currentWeatherDbHelper.saveWeather(locationId, now, weather);
        locationsDbHelper.updateLastUpdatedAndLocationSource(locationId, now, locationSource);
        sendResult(ACTION_WEATHER_UPDATE_OK, context);
    }

    private void resendTheIntentInSeveralSeconds(int seconds) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            ComponentName serviceComponent = new ComponentName(this, CurrentWeatherResendJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
            builder.setMinimumLatency(seconds * 1000); // wait at least
            builder.setOverrideDeadline((3 + seconds) * 1000); // maximum delay
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
        } else {
            AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                    0,
                    new Intent(getBaseContext(), CurrentWeatherService.class),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (1000 * seconds), pendingIntent);
        }
    }

    private class CurrentweatherMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            WeatherRequestDataHolder weatherRequestDataHolder = (WeatherRequestDataHolder) msg.obj;
            appendLog(getBaseContext(), TAG, "handleMessage:" + msg.what + ":" + weatherRequestDataHolder);
            switch (msg.what) {
                case START_CURRENT_WEATHER_UPDATE:
                    currentWeatherUpdateMessages.add(weatherRequestDataHolder);
                    startCurrentWeatherUpdate(weatherRequestDataHolder.getTimestamp());
                    break;
                case START_CURRENT_WEATHER_RETRY:
                    startCurrentWeatherUpdate(0);
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
