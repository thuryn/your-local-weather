package org.thosp.yourlocalweather.service;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.content.ContextCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.WeatherJSONParser;
import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.CompleteWeatherInfo;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LicenseKeysDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Queue;

import cz.msebera.android.httpclient.Header;

public class UpdateWeatherService extends AbstractCommonService {

    private static final String TAG = "UpdateWeatherService";

    private static final long MIN_WEATHER_UPDATE_TIME_IN_MS = 900000L; //15min
    private static final long MAX_WEATHER_UPDATE_TIME_IN_MS = 2700000L; //45m

    public static final int START_CURRENT_WEATHER_UPDATE = 1;
    public static final int START_CURRENT_WEATHER_RETRY = 2;
    public static final int START_WEATHER_FORECAST_UPDATE = 3;
    public static final int START_WEATHER_FORECAST_RETRY = 4;
    public static final int START_LONG_WEATHER_FORECAST_UPDATE = 5;
    public static final int START_LONG_WEATHER_FORECAST_RETRY = 6;
    public static final int START_PROCESS_CURRENT_QUEUE = 7;

    public static final String ACTION_WEATHER_UPDATE_OK = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_OK";
    public static final String ACTION_WEATHER_UPDATE_FAIL = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_FAIL";
    public static final String ACTION_WEATHER_UPDATE_RESULT = "org.thosp.yourlocalweather.action.WEATHER_UPDATE_RESULT";
    public static final String ACTION_FORECAST_UPDATE_RESULT = "org.thosp.yourlocalweather.action.FORECAST_UPDATE_RESULT";
    public static final String ACTION_GRAPHS_UPDATE_RESULT = "org.thosp.yourlocalweather.action.GRAPHS_UPDATE_RESULT";

    public static final int WEATHER_FORECAST_TYPE = 1;

    private static final AsyncHttpClient client = new AsyncHttpClient();

    private static volatile boolean gettingWeatherStarted;

    protected static final Queue<WeatherRequestDataHolder> updateWeatherUpdateMessages = new LinkedList<>();

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            if (!gettingWeatherStarted) {
                return;
            }
            final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

            WeatherRequestDataHolder updateRequest = updateWeatherUpdateMessages.peek();
            Location currentLocation = locationsDbHelper.getLocationById(updateRequest.getLocationId());
            if (currentLocation == null) {
                appendLog(getBaseContext(), TAG, "timerRunnable, currentLocation is null");
                gettingWeatherStarted = false;
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
            if ((currentLocation != null) && (updateRequest.isUpdateWeatherOnly())) {
                locationsDbHelper.updateLocationSource(currentLocation.getId(), newUpdateState);
            }
            sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), (currentLocation != null)?currentLocation.getId():null, updateRequest.getUpdateType());
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return ret;
        }
        YourLocalWeather.executor.submit(() -> {
            Notification notification = NotificationUtils.getNoWeatherNotification(getBaseContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NotificationUtils.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NotificationUtils.NOTIFICATION_ID, notification);
            }
            appendLog(getBaseContext(), TAG, "onStartCommand:", intent);
            boolean forceUpdate = false;
            Long locationId = null;
            String updateSource = null;
            boolean updateWeatherOnly = false;
            int updateType = WEATHER_FORECAST_TYPE;
            if (intent.getAction().equals("org.thosp.yourlocalweather.action.RESEND_WEATHER_UPDATE")) {
                startWeatherUpdate();
                return;
            }
            if (intent.hasExtra("weatherRequest")) {
                updateWeatherUpdateMessages.add((WeatherRequestDataHolder) intent.getSerializableExtra("weatherRequest"));
            } else {
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
                if (intent.hasExtra("updateType")) {
                    updateType = intent.getIntExtra("updateType", WEATHER_FORECAST_TYPE);
                }
                if (locationId != null) {
                    updateWeatherUpdateMessages.add(new WeatherRequestDataHolder(locationId, updateSource, forceUpdate, updateWeatherOnly, updateType));
                }
            }
            startWeatherUpdate();
        });
        return ret;
    }

    public void startWeatherUpdate() {
        if (gettingWeatherStarted) {
            return;
        }
        gettingWeatherStarted = true;
        appendLog(getBaseContext(), TAG, "startCurrentWeatherUpdate");
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
        final LicenseKeysDbHelper licenseKeysDbHelper = LicenseKeysDbHelper.getInstance(getBaseContext());

        appendLog(getBaseContext(),
                TAG,
                "currentWeatherUpdateMessages.size before peek = ", updateWeatherUpdateMessages);

        final WeatherRequestDataHolder updateRequest = updateWeatherUpdateMessages.peek();

        appendLog(getBaseContext(),
                TAG,
                "currentWeatherUpdateMessages.size after peek = ", updateWeatherUpdateMessages);

        if (updateRequest == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "updateRequest is null");
            gettingWeatherStarted = false;
            return;
        }

        final int updateType = updateRequest.getUpdateType();

        Location locationToCheck = locationsDbHelper.getLocationById(updateRequest.getLocationId());
        appendLog(getBaseContext(), TAG, "currentLocation=" + locationToCheck + ", updateSource=" + updateRequest.getUpdateSource());
        if (locationToCheck == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "current location is null");
            updateWeatherUpdateMessages.poll();
            appendLog(getBaseContext(),
                    TAG,
                    "currentWeatherUpdateMessages.size when current location is null = ", updateWeatherUpdateMessages);
            gettingWeatherStarted = false;
            startWeatherUpdate();
            return;
        }

        boolean readyForUpdate = false;
        appendLog(getBaseContext(),
                TAG,
                "checkWeatherUpdate time:",
                updateType);
        long now = System.currentTimeMillis();
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(locationToCheck.getId());
        long nextAllowedAttemptToUpdateTime = (weatherRecord != null)?weatherRecord.getNextAllowedAttemptToUpdateTime():0;

        if (isCurrentWeather(updateType)) {
            long lastUpdateTimeInMilis = 0;
            if (!"B".equals(locationToCheck.getLocationSource())) {
                lastUpdateTimeInMilis = (weatherRecord != null) ? weatherRecord.getLastUpdatedTime() : 0;
            }
            long updatePeriodForLocation;
            if (locationToCheck.getOrderId() == 0) {
                String updateAutoPeriodStr = AppPreference.getInstance().getLocationAutoUpdatePeriod(this);
                updatePeriodForLocation = Utils.intervalMillisForAlarm(updateAutoPeriodStr);
            } else {
                String updatePeriodStr = AppPreference.getInstance().getLocationUpdatePeriod(this);
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
                    lastUpdateTimeInMilis,
                    ", nextAllowedAttemptToUpdateTime=",
                    nextAllowedAttemptToUpdateTime);
            readyForUpdate = (now > (lastUpdateTimeInMilis + updatePeriodForLocation)) && (now > nextAllowedAttemptToUpdateTime);
        } else {
            readyForUpdate = false;
        }

        if (!readyForUpdate) {
            appendLog(getBaseContext(),
                    TAG,
                    "Current weather is recent enough");
            WeatherRequestDataHolder request = updateWeatherUpdateMessages.poll();
            sendMessageToReconciliationDbService(false);
            WidgetUtils.updateWidgets(this);
            gettingWeatherStarted = false;
            if (request != null) {
                updateResultInUI(request.getLocationId(), ACTION_WEATHER_UPDATE_OK, updateRequest);
            }
            startWeatherUpdate();
            return;
        }

        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        boolean networkAvailableAndConnected = connectionDetector.isNetworkAvailableAndConnected();
        appendLog(getBaseContext(), TAG, "networkAvailableAndConnected=", networkAvailableAndConnected);
        if (!networkAvailableAndConnected) {
            int numberOfAttempts = updateRequest.getAttempts();
            appendLog(getBaseContext(), TAG, "numberOfAttempts=", numberOfAttempts);
            if (numberOfAttempts > 2) {
                if (updateRequest.isUpdateWeatherOnly()) {
                    locationsDbHelper.updateLocationSource(
                            locationToCheck.getId(),
                            getString(R.string.location_weather_update_status_location_not_reachable));
                }
                appendLog(getBaseContext(),
                        TAG,
                        "currentWeatherUpdateMessages.size when attempts is more than 2 = ",
                        updateWeatherUpdateMessages);
                sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), locationToCheck.getId(), updateType);
                return;
            }
            updateRequest.increaseAttempts();
            resendTheIntentInSeveralSeconds(20);
            gettingWeatherStarted = false;
            sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), locationToCheck.getId(), updateType);
            return;
        }

        if (updateRequest.isUpdateWeatherOnly()) {
            locationsDbHelper.updateLocationSource(locationToCheck.getId(), getString(R.string.location_weather_update_status_update_started));
            locationToCheck = locationsDbHelper.getLocationById(locationToCheck.getId());
        }
        final Location currentLocation = locationToCheck;

        timerHandler.postDelayed(timerRunnable, 20000);
        final Context context = this;
        appendLog(getBaseContext(), TAG, "startRefreshRotation");

        if (currentLocation == null) {
            gettingWeatherStarted = false;
            appendLog(context,
                    TAG,
                    "currentLocation is null");
            return;
        }
        appendLog(context,
                TAG,
                "weather get params: latitude:",
                currentLocation.getLatitude(),
                ", longitude",
                currentLocation.getLongitude());

        sendMessageToWakeUpService(
                AppWakeUpManager.WAKE_UP,
                AppWakeUpManager.SOURCE_CURRENT_WEATHER
        );

        String weatherUrl = null;
        try {
            weatherUrl = Utils.getOwmUrl(
                    context,
                    currentLocation).toString();
        } catch (MalformedURLException mue) {
            appendLog(context, TAG, "MalformedURLException:", mue);
            sendResult(ACTION_WEATHER_UPDATE_FAIL, context, currentLocation.getId(), updateType);
            return;
        }
        String url = weatherUrl;
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {

                    TrafficStats.setThreadStatsTag(123);
                    client.get(url, null, new AsyncHttpResponseHandler() {

                    @Override
                    public void onStart() {
                        // called before request is started
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                        YourLocalWeather.executor.submit(() -> {
                            try {
                                String weatherRaw = new String(response);
                                appendLog(context, TAG, "weather got, result:", weatherRaw);

                                timerHandler.removeCallbacksAndMessages(null);

                                final String locale = currentLocation.getLocaleAbbrev();

                                appendLog(context, TAG, "Going to store result with updateType:", updateType);
                                if (isCurrentWeather(updateType)) {
                                    appendLog(context, TAG, "Current weather type");
                                    JSONObject weatherData = new JSONObject(weatherRaw);
                                    Weather weather = WeatherJSONParser.getWeather(weatherData, locale);
                                    saveWeatherAndSendResult(context, weather, currentLocation, updateType);
                                    CompleteWeatherForecast completeWeatherForecast = WeatherJSONParser.getWeatherForecast(context, weatherData);
                                    saveWeatherAndSendResult(context, currentLocation, completeWeatherForecast, WEATHER_FORECAST_TYPE, START_WEATHER_FORECAST_UPDATE);
                                    broadcastWeatherUpdate(currentLocation, weather, completeWeatherForecast);
                                } else {
                                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context, currentLocation.getId(), updateType);
                                }
                            } catch (JSONException | ParseException e) {
                                appendLog(context, TAG, "JSONException:", e);
                                timerHandler.removeCallbacksAndMessages(null);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, context, currentLocation.getId(), updateType);
                            }
                        });
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        YourLocalWeather.executor.submit(() -> {
                            appendLog(context, TAG, "onFailure:", statusCode, ":currentLocation=", currentLocation);
                            timerHandler.removeCallbacksAndMessages(null);
                            Long nextAllowedAttemptToUpdateTime = null;
                            if (currentLocation != null) {
                                final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
                                switch (statusCode) {
                                    case 401:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), getString(R.string.location_weather_update_status_access_expired));
                                        break;
                                    case 403:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), "F");
                                        break;
                                    case 429:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), getString(R.string.location_weather_update_status_access_banned));
                                        nextAllowedAttemptToUpdateTime = generateRandomNextAttemptTime();
                                        break;
                                    default:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), getString(R.string.location_weather_update_status_location_only));
                                        break;
                                }
                            }
                            sendResult(ACTION_WEATHER_UPDATE_FAIL, context, currentLocation.getId(), updateType, nextAllowedAttemptToUpdateTime);
                        });
                    }

                    @Override
                    public void onRetry(int retryNo) {
                        // called when request is retried
                    }
                });
            }
        };
        mainHandler.post(myRunnable);
    }

    private long generateRandomNextAttemptTime() {
        return new Double(MAX_WEATHER_UPDATE_TIME_IN_MS * Math.random()).longValue();
    }

    private void sendResult(String result, Context context, Long locationId, int updateType) {
        sendResult(result, context, locationId, updateType, null);
    }

    private void updateNextAllowedAttemptToUpdateTimeForUpdate(Context context, Long locationId, int updateType, Long nextAllowedAttemptToUpdateTime) {
        long nextAllowedAttemptToUpdateTimeForUpdate = System.currentTimeMillis() + MIN_WEATHER_UPDATE_TIME_IN_MS;
        if (nextAllowedAttemptToUpdateTime != null) {
            appendLog(context, TAG, "set nextAllowedAttemptToUpdateTime by :", nextAllowedAttemptToUpdateTime);
            nextAllowedAttemptToUpdateTimeForUpdate += nextAllowedAttemptToUpdateTime;
            appendLog(context, TAG, "set nextAllowedAttemptToUpdateTime to :", nextAllowedAttemptToUpdateTimeForUpdate);
        }
        CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        currentWeatherDbHelper.updateNextAllowedAttemptToUpdateTime(locationId, nextAllowedAttemptToUpdateTimeForUpdate);
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        weatherForecastDbHelper.updateNextAllowedAttemptToUpdateTime(locationId, WEATHER_FORECAST_TYPE, nextAllowedAttemptToUpdateTimeForUpdate);
    }

    private void sendResult(String result, Context context, Long locationId, int updateType, Long nextAllowedAttemptToUpdateTime) {
        if (ACTION_WEATHER_UPDATE_FAIL.equals(result) && (locationId != null)) {
            updateNextAllowedAttemptToUpdateTimeForUpdate(context, locationId, updateType, nextAllowedAttemptToUpdateTime);
        }
        if (isCurrentWeather(updateType)) {
            sendMessageToWakeUpService(
                    AppWakeUpManager.FALL_DOWN,
                    AppWakeUpManager.SOURCE_CURRENT_WEATHER
            );
        } else {
            sendMessageToWakeUpService(
                    AppWakeUpManager.FALL_DOWN,
                    AppWakeUpManager.SOURCE_WEATHER_FORECAST
            );
        }

        gettingWeatherStarted = false;
        WeatherRequestDataHolder updateRequest = updateWeatherUpdateMessages.poll();
        appendLog(getBaseContext(),
                  TAG,
                "Update request: " + updateRequest);
        appendLog(getBaseContext(),
                  TAG,
                "currentWeatherUpdateMessages.size after pull when sending result = ", updateWeatherUpdateMessages);
        try {
            appendLog(getBaseContext(),
                    TAG,
                    "sendResult: updateResultInUI");
            updateResultInUI(locationId, result, updateRequest);
            if (!updateWeatherUpdateMessages.isEmpty()) {
                resendTheIntentInSeveralSeconds(5);
            }
            appendLog(getBaseContext(),
                    TAG,
                    "sendResult: updateWidgets:", (updateRequest != null) ? updateRequest.getUpdateSource() : "");
            WidgetUtils.updateWidgets(getBaseContext());
            sendMessageToReconciliationDbService(false);
        } catch (Throwable exception) {
            appendLog(context, TAG, "Exception occured when starting the service:", exception);
        }
        NotificationUtils.cancelUpdateNotification(getBaseContext());
        stopForeground(true);
    }

    protected void broadcastWeatherUpdate(Location location, Weather weather, CompleteWeatherForecast completeWeatherForecast) {
        appendLog(this,
                TAG,
                "going to broadcast Weather update");
        try {
            CompleteWeatherInfo completeWeatherInfo = new CompleteWeatherInfo();
            completeWeatherInfo.updateLocation(this, location);
            completeWeatherInfo.updateCurrentWeather(this, weather);
            completeWeatherInfo.updateWeatherForecastList(this, completeWeatherForecast.getWeatherForecastList());
            Intent intent = new Intent("org.thosp.yourlocalweather.action.WEATHER_UPDATE");
            intent.putExtra("complete_weather_forecast", completeWeatherInfo);
            intent.setPackage("org.omnirom.omnijaws");
            sendBroadcast(intent);
        } catch (Exception e) {
            appendLog(this,
                    TAG,
                    e);
        }
    }

    private void updateResultInUI(Long locationId, String result, WeatherRequestDataHolder updateRequest) {
        if (updateRequest == null) {
            return;
        }
        int updateType = updateRequest.getUpdateType();
        appendLog(getBaseContext(), TAG, "Sending result with updateType:", updateType);
        if (isCurrentWeather(updateType)) {
            sendIntentToMain(result);
        } else if (isWeatherForecast(updateType) || isLongWeatherForecast(updateType)) {
            sendIntentToForecast(result);
            sendIntentToGraphs(result);
        }
        if (ACTION_WEATHER_UPDATE_OK.equals(result)) {
            weatherNotification(locationId, updateRequest.getUpdateSource());
        }
    }

    private void saveWeatherAndSendResult(Context context, Weather weather, Location location, int updateType) {
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        appendLog(getBaseContext(), TAG, "saveWeatherAndSendResult:locationId:", location.getId());
        String locationSource = location.getLocationSource();
        appendLog(getBaseContext(), TAG, "saveWeatherAndSendResult:locationSource by location:", locationSource);
        if ((location.getOrderId() > 0) ||
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
        currentWeatherDbHelper.saveWeather(location.getId(), now, now + MIN_WEATHER_UPDATE_TIME_IN_MS, weather);
        appendLog(context,
                TAG,
                "Current weather saved");
        sendMessageToWeatherByVoiceService(location, weather, now);
        locationsDbHelper.updateLastUpdatedAndLocationSource(location.getId(), now, locationSource);
        appendLog(context,
                TAG,
                "Going to send result with current weather");
        sendResult(ACTION_WEATHER_UPDATE_OK, context, location.getId(), updateType);
    }

    private void saveWeatherAndSendResult(Context context, Location location, CompleteWeatherForecast completeWeatherForecast, int forecastType, int updateType) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        long lastUpdate = System.currentTimeMillis();
        appendLog(context,
                TAG,
                "Going to store forecast for locationId: ",
                location.getId());
        weatherForecastDbHelper.saveWeatherForecast(location.getId(),
                forecastType,
                lastUpdate,
                lastUpdate + MIN_WEATHER_UPDATE_TIME_IN_MS,
                completeWeatherForecast);
        appendLog(context,
                TAG,
                "Forecast has been saved");
        GraphUtils.invalidateGraph();
        appendLog(context,
                TAG,
                "Graphs invalidated");
        sendResult(ACTION_WEATHER_UPDATE_OK, context, location.getId(), updateType);

        appendLog(context,
                TAG,
                    "Result sent");
    }

    private void sendIntentToForecast(String result) {
        Intent intent = new Intent(ACTION_FORECAST_UPDATE_RESULT);
        intent.setPackage("org.thosp.yourlocalweather");
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_FORECAST_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_FORECAST_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                sendBroadcast(intent);
            }
        };
        mainHandler.post(myRunnable);
    }

    private void sendIntentToGraphs(String result) {
        Intent intent = new Intent(ACTION_GRAPHS_UPDATE_RESULT);
        intent.setPackage("org.thosp.yourlocalweather");
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_GRAPHS_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_GRAPHS_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        Handler mainHandler = new Handler(Looper.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                sendBroadcast(intent);
            }
        };
        mainHandler.post(myRunnable);
    }

    private void resendTheIntentInSeveralSeconds(int seconds) {
        appendLog(getBaseContext(), TAG, "resendTheIntentInSeveralSeconds:SDK:", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            jobScheduler.cancelAll();
            ComponentName serviceComponent = new ComponentName(this, UpdateWeatherResendJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(UpdateWeatherResendJob.JOB_ID, serviceComponent);

            builder.setMinimumLatency(seconds * 1000L); // wait at least
            builder.setOverrideDeadline((3 + seconds) * 1000L); // maximum delay
            jobScheduler.schedule(builder.build());
            appendLog(getBaseContext(), TAG, "resendTheIntentInSeveralSeconds: sent");
        } else {
            AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                    0,
                    new Intent(getBaseContext(), UpdateWeatherService.class),
                    PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (1000L * seconds), pendingIntent);
        }
    }

    private boolean isCurrentWeather(int updateType) {
        return (START_CURRENT_WEATHER_UPDATE == updateType) || (START_CURRENT_WEATHER_RETRY == updateType);
    }

    private boolean isWeatherForecast(int updateType) {
        return (START_WEATHER_FORECAST_UPDATE == updateType) || (START_WEATHER_FORECAST_RETRY == updateType);
    }

    private boolean isLongWeatherForecast(int updateType) {
        return false;
        //return (START_LONG_WEATHER_FORECAST_UPDATE == updateType) || (START_LONG_WEATHER_FORECAST_RETRY == updateType);
    }

    private void weatherNotification(Long locationId, String updateSource) {
        Location locationForNotification = NotificationUtils.getLocationForNotification(getBaseContext());
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_NOTIFICATION
        );
        NotificationUtils.cancelUpdateNotification(getBaseContext());
        if ((locationForNotification == null) || (locationForNotification.getId() != locationId)) {
            stopForeground(true);
            return;
        }
        String notificationPresence = AppPreference.getNotificationPresence(this);
        if ("permanent".equals(notificationPresence)) {
            NotificationUtils.weatherNotification(this, locationId);
        } else if ("on_lock_screen".equals(notificationPresence) && NotificationUtils.isScreenLocked(this)) {
            NotificationUtils.weatherNotification(this, locationId);
        } else if ("NOTIFICATION".equals(updateSource)) {
            NotificationUtils.weatherNotification(this, locationId);
        }
        stopForeground(true);
    }

    protected void sendMessageToWeatherByVoiceService(Location location,
                                                      Weather weather,
                                                      long now) {
        Intent intentToStartUpdate = new Intent("org.thosp.yourlocalweather.action.START_VOICE_WEATHER_UPDATED");
        intentToStartUpdate.setPackage("org.thosp.yourlocalweather");
        intentToStartUpdate.putExtra("weatherByVoiceLocation", location);
        intentToStartUpdate.putExtra("weatherByVoiceWeather", weather);
        intentToStartUpdate.putExtra("weatherByVoiceTime", now);
        ContextCompat.startForegroundService(getBaseContext(), intentToStartUpdate);
    }
}
