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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.WeatherJSONParser;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.NotificationUtils;
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

    private static volatile boolean gettingWeatherStarted;
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
                originalUpdateState = getString(R.string.location_weather_update_status_update_started);
            }
            appendLog(getBaseContext(), TAG, "originalUpdateState:", originalUpdateState);
            String newUpdateState = originalUpdateState;
            if (originalUpdateState.contains(getString(R.string.location_weather_update_status_location_from_network))) {
                appendLog(getBaseContext(), TAG, "originalUpdateState contains N");
                newUpdateState = originalUpdateState.replace(getString(R.string.location_weather_update_status_location_from_network), getString(R.string.location_weather_update_status_location_only));
            } else if (originalUpdateState.contains(getString(R.string.location_weather_update_status_location_from_gps))) {
                newUpdateState = getString(R.string.location_weather_update_status_location_only);
            }
            appendLog(getBaseContext(), TAG, "currentLocation:",
                    currentLocation,
                    ", newUpdateState:",
                    newUpdateState);
            if (currentLocation != null) {
                locationsDbHelper.updateLocationSource(currentLocation.getId(), newUpdateState);
            }
            sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext());
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        appendLog(getBaseContext(), TAG, "onStartCommand:", intent);
        if (intent == null) {
            return ret;
        }
        boolean forceUpdate = false;
        Long locationId = null;
        String updateSource = null;
        boolean updateWeatherOnly = false;
        if (intent.hasExtra("forceUpdate")) {
            forceUpdate = intent.getBooleanExtra("forceUpdate", false);
        }
        if (intent.hasExtra("locationId")) {
            locationId = intent.getLongExtra("locationId", 0);
        }
        if (intent.hasExtra("updateSource")) {
            updateSource = intent.getStringExtra("updateSource");
        }
        if (intent.hasExtra("updateWeatherOnly")) {
            updateWeatherOnly = intent.getBooleanExtra("updateWeatherOnly", false);
        }
        currentWeatherUpdateMessages.add(new WeatherRequestDataHolder(locationId, updateSource, forceUpdate, updateWeatherOnly));
        startCurrentWeatherUpdate(0);
        return ret;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void startCurrentWeatherUpdate(long incommingMessageTimestamp) {
        appendLog(getBaseContext(), TAG, "startCurrentWeatherUpdate");
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

        appendLog(getBaseContext(),
                TAG,
                "currentWeatherUpdateMessages.size before peek = ", currentWeatherUpdateMessages);

        WeatherRequestDataHolder updateRequest = currentWeatherUpdateMessages.peek();

        appendLog(getBaseContext(),
                TAG,
                "currentWeatherUpdateMessages.size after peek = ", currentWeatherUpdateMessages);

        if ((updateRequest == null) || (updateRequest.getTimestamp() < incommingMessageTimestamp)) {
            if (updateRequest != null) {
                appendLog(getBaseContext(),
                        TAG,
                        "updateRequest is older than current");
                if (!gettingWeatherStarted) {
                    resendTheIntentInSeveralSeconds(1);
                }
            } else {
                appendLog(getBaseContext(),
                        TAG,
                        "updateRequest is null");
            }
            appendLog(getBaseContext(),
                    TAG,
                    "currentWeatherUpdateMessages.size when request is old or null = ", currentWeatherUpdateMessages);
            return;
        }

        Location locationToCheck = locationsDbHelper.getLocationById(updateRequest.getLocationId());
        appendLog(getBaseContext(), TAG, "currentLocation=" + locationToCheck + ", updateSource=" + updateRequest.getUpdateSource());
        if (locationToCheck == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "current location is null");
            currentWeatherUpdateMessages.poll();
            appendLog(getBaseContext(),
                    TAG,
                    "currentWeatherUpdateMessages.size when current location is null = ", currentWeatherUpdateMessages);
            return;
        }

        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(locationToCheck.getId());

        long lastUpdateTimeInMilis = (weatherRecord != null)?weatherRecord.getLastUpdatedTime():0;
        long now = System.currentTimeMillis();

        long updatePeriodForLocation;
        if (locationToCheck.getOrderId() == 0) {
            String updateAutoPeriodStr = AppPreference.getLocationAutoUpdatePeriod(this);
            updatePeriodForLocation = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
        } else {
            String updatePeriodStr = AppPreference.getLocationUpdatePeriod(this);
            updatePeriodForLocation = Utils.intervalMillisForAlarm(updatePeriodStr);
        }

        appendLog(this.getBaseContext(), TAG,
                "Current weather requested for location.orderId=",
                locationToCheck.getOrderId(),
                ", updatePeriodForLocation=",
                updatePeriodForLocation,
                ", now=",
                now,
                ", lastUpdateTimeInMilis=",
                lastUpdateTimeInMilis);

        if (!updateRequest.isForceUpdate() &&
                (now <= (lastUpdateTimeInMilis + updatePeriodForLocation) &&
                (updateRequest.getUpdateSource() == null))) {
            appendLog(getBaseContext(),
                    TAG,
                    "Current weather is recent enough");
            return;
        }

        if (updateRequest.isUpdateWeatherOnly()) {
            locationsDbHelper.updateLocationSource(locationToCheck.getId(), getString(R.string.location_weather_update_status_update_started));
            locationToCheck = locationsDbHelper.getLocationById(locationToCheck.getId());
        }

        final Location currentLocation = locationToCheck;

        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        boolean networkAvailableAndConnected = connectionDetector.isNetworkAvailableAndConnected();
        appendLog(getBaseContext(), TAG, "networkAvailableAndConnected=", networkAvailableAndConnected);
        if (!networkAvailableAndConnected) {
            int numberOfAttempts = updateRequest.getAttempts();
            appendLog(getBaseContext(), TAG, "numberOfAttempts=", numberOfAttempts);
            if (numberOfAttempts > 2) {
                locationsDbHelper.updateLocationSource(
                        currentLocation.getId(),
                        getString(R.string.location_weather_update_status_location_not_reachable));
                appendLog(getBaseContext(),
                        TAG,
                        "currentWeatherUpdateMessages.size when attempts is more than 2 = ",
                        currentWeatherUpdateMessages);
                sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext());
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
                final String locale = currentLocation.getLocaleAbbrev();
                appendLog(context,
                        TAG,
                        "weather get params: latitude:",
                        currentLocation.getLatitude(),
                        ", longitude",
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
                                appendLog(context, TAG, "weather got, result:", weatherRaw);

                                final String locale = currentLocation.getLocaleAbbrev();
                                Weather weather = WeatherJSONParser.getWeather(context, weatherRaw, locale);
                                timerHandler.removeCallbacksAndMessages(null);
                                saveWeatherAndSendResult(context, weather);
                            } catch (JSONException e) {
                                appendLog(context, TAG, "JSONException:", e);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            appendLog(context, TAG, "onFailure:", statusCode, ":currentLocation=", currentLocation);
                            timerHandler.removeCallbacksAndMessages(null);
                            if (currentLocation != null) {
                                final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
                                if (statusCode == 401) {
                                    locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                            System.currentTimeMillis(), getString(R.string.location_weather_update_status_access_expired));

                                } else if (statusCode == 429) {
                                    locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                            System.currentTimeMillis(), getString(R.string.location_weather_update_status_access_banned));

                                } else {
                                    locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                            System.currentTimeMillis(), getString(R.string.location_weather_update_status_location_only));
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
                    appendLog(context, TAG, "MalformedURLException:", mue);
                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    private void sendResult(String result, Context context) {
        sendResult(result, context, null);
    }

    private void sendResult(String result, Context context, Long locationId) {
        stopRefreshRotation("STOP", 2);
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_CURRENT_WEATHER
        );
        gettingWeatherStarted = false;
        WeatherRequestDataHolder updateRequest = currentWeatherUpdateMessages.poll();
        appendLog(getBaseContext(),
                TAG,
                "currentWeatherUpdateMessages.size after pull when sending result = ", currentWeatherUpdateMessages);
        try {
            updateResultInUI(locationId, result, updateRequest);
            if (!currentWeatherUpdateMessages.isEmpty()) {
                resendTheIntentInSeveralSeconds(5);
            }
            if (WidgetRefreshIconService.isRotationActive) {
                return;
            }
            WidgetUtils.updateWidgets(getBaseContext());
        } catch (Throwable exception) {
            appendLog(context, TAG, "Exception occured when starting the service:", exception);
        }
    }

    private void updateResultInUI(Long locationId, String result, WeatherRequestDataHolder updateRequest) {
        if (updateRequest == null) {
            return;
        }
        String updateSource = updateRequest.getUpdateSource();
        if (updateSource != null) {
            switch (updateSource) {
                case "MAIN":
                    sendIntentToMain(result);
                    break;
            }
        }
        if (ACTION_WEATHER_UPDATE_OK.equals(result)) {
            weatherNotification(locationId, updateSource);
        }
    }

    private void saveWeatherAndSendResult(Context context, Weather weather) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Long locationId = locationsDbHelper.getLocationIdByCoordinates(weather.getLat(), weather.getLon());
        if (locationId == null) {
            appendLog(context,
                    TAG,
                    "Weather not saved because there is no location with coordinates:",
                            weather.getLat(),
                            ", ",
                            weather.getLon());
            sendResult(ACTION_WEATHER_UPDATE_FAIL, context);
            return;
        }
        appendLog(getBaseContext(), TAG, "saveWeatherAndSendResult:locationId:", locationId);
        Location currentLocation = locationsDbHelper.getLocationById(locationId);
        String locationSource = currentLocation.getLocationSource();
        appendLog(getBaseContext(), TAG, "saveWeatherAndSendResult:locationSource by location:", locationSource);
        if ((currentLocation.getOrderId() > 0) ||
            (locationSource == null) ||
                getString(R.string.location_weather_update_status_update_started).equals(locationSource) ||
                getString(R.string.location_weather_update_status_location_not_reachable).equals(locationSource)) {
            locationSource = getString(R.string.location_weather_update_status_weather_only);
        }
        appendLog(context,
                TAG,
                "Location source is:", locationSource);

        long now = System.currentTimeMillis();
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(context);
        currentWeatherDbHelper.saveWeather(locationId, now, weather);
        locationsDbHelper.updateLastUpdatedAndLocationSource(locationId, now, locationSource);
        sendResult(ACTION_WEATHER_UPDATE_OK, context, locationId);
    }

    private void resendTheIntentInSeveralSeconds(int seconds) {
        appendLog(getBaseContext(), TAG, "resendTheIntentInSeveralSeconds:SDK:", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ComponentName serviceComponent = new ComponentName(this, CurrentWeatherResendJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(CurrentWeatherResendJob.JOB_ID, serviceComponent);
            builder.setMinimumLatency(seconds * 1000); // wait at least
            builder.setOverrideDeadline((3 + seconds) * 1000); // maximum delay
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
            appendLog(getBaseContext(), TAG, "resendTheIntentInSeveralSeconds: sent");
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

    private void weatherNotification(Long locationId, String updateSource) {
        String notificationPresence = AppPreference.getNotificationPresence(this);
        if ("permanent".equals(notificationPresence)) {
            NotificationUtils.weatherNotification(this, locationId);
        } else if ("on_lock_screen".equals(notificationPresence) && NotificationUtils.isScreenLocked(this)) {
            NotificationUtils.weatherNotification(this, locationId);
        } else if ((updateSource != null) && "NOTIFICATION".equals(updateSource)) {
            NotificationUtils.weatherNotification(this, locationId);
        }
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_NOTIFICATION
        );
    }

    private class CurrentweatherMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            WeatherRequestDataHolder weatherRequestDataHolder = (WeatherRequestDataHolder) msg.obj;
            appendLog(getBaseContext(), TAG, "handleMessage:", msg.what, ":", weatherRequestDataHolder);
            appendLog(getBaseContext(),
                    TAG,
                    "currentWeatherUpdateMessages.size when adding new message = ", currentWeatherUpdateMessages);
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
