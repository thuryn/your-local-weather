package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.WeatherJSONParser;
import org.thosp.yourlocalweather.licence.LicenseNotValidException;
import org.thosp.yourlocalweather.licence.TooEarlyUpdateException;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.LicenseKey;
import org.thosp.yourlocalweather.model.LicenseKeysDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.GraphUtils;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.WidgetUtils;
import org.thosp.yourlocalweather.widget.WidgetRefreshIconService;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cz.msebera.android.httpclient.Header;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class UpdateWeatherService extends AbstractCommonService {

    private static final String TAG = "UpdateWeatherService";

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
    public static final int LONG_WEATHER_FORECAST_TYPE = 2;

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static volatile boolean gettingWeatherStarted;

    private Messenger weatherByVoiceService;
    private Lock weatherByVoiceServiceLock = new ReentrantLock();
    private Queue<Message> weatherByvOiceUnsentMessages = new LinkedList<>();

    protected static final Queue<WeatherRequestDataHolder> updateWeatherUpdateMessages = new LinkedList<>();

    final Messenger messenger = new Messenger(new UpdateWeatherMessageHandler());

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
            sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), updateRequest.getUpdateType());
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        unbindWeatherByVoiceService();
        return false;
    }

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
        int updateType = WEATHER_FORECAST_TYPE;
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
        startWeatherUpdate();
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
        if (isCurrentWeather(updateType)) {
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
            readyForUpdate =  !(now <= (lastUpdateTimeInMilis + updatePeriodForLocation));
        } else {
            boolean longForecast = isLongWeatherForecast(updateRequest.getUpdateType());
            readyForUpdate = ForecastUtil.shouldUpdateForecast(this, locationToCheck.getId(),
                    longForecast ? UpdateWeatherService.LONG_WEATHER_FORECAST_TYPE : UpdateWeatherService.WEATHER_FORECAST_TYPE);
        }

        if (!updateRequest.isForceUpdate() &&
                        !readyForUpdate &&
                        (updateRequest.getUpdateSource() == null)) {
            appendLog(getBaseContext(),
                    TAG,
                    "Current weather is recent enough");
            updateWeatherUpdateMessages.poll();
            sendMessageToReconciliationDbService(false);
            WidgetUtils.updateWidgets(this);
            gettingWeatherStarted = false;
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
                sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), updateType);
                return;
            }
            updateRequest.increaseAttempts();
            resendTheIntentInSeveralSeconds(20);
            gettingWeatherStarted = false;
            sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), updateType);
            return;
        }

        boolean freeWeather = ApiKeys.isWeatherForecastFeaturesFree(getBaseContext());

        final String serviceURL;
        String requestUri = null;
        if (isCurrentWeather(updateType)) {
            if (freeWeather) {
                serviceURL = Constants.WEATHER_ENDPOINT;
            } else {
                serviceURL = Constants.SERVICE_WEATHER_ENDPOINT;
            }
            requestUri = "weather";
        } else if (isWeatherForecast(updateType)) {
            if (freeWeather) {
                serviceURL = Constants.WEATHER_FORECAST_ENDPOINT;
            } else {
                serviceURL = Constants.SERVICE_WEATHER_FORECAST_ENDPOINT;
            }
            requestUri = "forecast";
        } else if (isLongWeatherForecast(updateType)) {
            serviceURL = Constants.SERVICE_WEATHER_FORECAST_ENDPOINT_DAILY;
            requestUri = "forecast/daily";
        } else {
            appendLog(getBaseContext(), TAG, "serviceURL is null !!!");
            gettingWeatherStarted = false;
            updateWeatherUpdateMessages.poll();
            return;
        }

        final String license;
        if (!freeWeather) {
            LicenseKey licenseKey = licenseKeysDbHelper.getLicenseKeyByLocationRequestId(requestUri);
            if ((licenseKey != null) && (System.currentTimeMillis() <= (60000 + licenseKey.getLastCallTimeInMs()))) {
                appendLog(getBaseContext(), TAG, "Last call to licensed server is too recent.");
                resendTheIntentInSeveralSeconds(10);
                gettingWeatherStarted = false;
                sendResult(ACTION_WEATHER_UPDATE_FAIL, getBaseContext(), updateType);
                return;
            }
            license = ApiKeys.getLicenseKey(getBaseContext(), licenseKey);
        } else {
            license = null;
        }

        if (updateRequest.isUpdateWeatherOnly()) {
            locationsDbHelper.updateLocationSource(locationToCheck.getId(), getString(R.string.location_weather_update_status_update_started));
            locationToCheck = locationsDbHelper.getLocationById(locationToCheck.getId());
        }
        final Location currentLocation = locationToCheck;

        timerHandler.postDelayed(timerRunnable, 20000);
        final Context context = this;
        appendLog(getBaseContext(), TAG, "startRefreshRotation");

        if (isCurrentWeather(updateType)) {
            startRefreshRotation("START", 2);
        } else {
            startRefreshRotation("START", 1);
        }

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
                    if (isCurrentWeather(updateType)) {
                        sendMessageToWakeUpService(
                                AppWakeUpManager.WAKE_UP,
                                AppWakeUpManager.SOURCE_CURRENT_WEATHER
                        );
                    } else {
                        sendMessageToWakeUpService(
                                AppWakeUpManager.WAKE_UP,
                                AppWakeUpManager.SOURCE_WEATHER_FORECAST
                        );
                    }

                    client.get(Utils.getOwmUrl(
                            context,
                            serviceURL,
                            currentLocation,
                            "metric",
                            locale,
                            license).toString(), null, new AsyncHttpResponseHandler() {

                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                            try {
                                String weatherRaw = new String(response);
                                appendLog(context, TAG, "weather got, result:", weatherRaw);

                                timerHandler.removeCallbacksAndMessages(null);

                                final String locale = currentLocation.getLocaleAbbrev();

                                appendLog(context, TAG, "Going to store result with updateType:", updateType);
                                if (isCurrentWeather(updateType)) {
                                    appendLog(context, TAG, "Current weather type");
                                    Weather weather;
                                    if (ApiKeys.isWeatherForecastFeaturesFree(context)) {
                                        weather = WeatherJSONParser.getWeather(weatherRaw, locale);
                                    } else {
                                        WeatherJSONParser.JSONParseResult parseResult = WeatherJSONParser.parseServerResult(weatherRaw);
                                        licenseKeysDbHelper.updateToken("weather", parseResult.getToken());
                                        weather = WeatherJSONParser.getWeather(parseResult.getOwmResponse(), locale);
                                    }
                                    saveWeatherAndSendResult(context, weather, currentLocation, updateType);
                                } else if (isWeatherForecast(updateType)) {
                                    appendLog(context, TAG, "Weather forecast type");
                                    CompleteWeatherForecast completeWeatherForecast;
                                    if (ApiKeys.isWeatherForecastFeaturesFree(context)) {
                                        completeWeatherForecast = WeatherJSONParser.getWeatherForecast(weatherRaw);
                                    } else {
                                        WeatherJSONParser.JSONParseResult parseResult = WeatherJSONParser.parseServerResult(weatherRaw);
                                        licenseKeysDbHelper.updateToken("forecast", parseResult.getToken());
                                        completeWeatherForecast = WeatherJSONParser.getWeatherForecast(parseResult.getOwmResponse());
                                    }
                                    saveWeatherAndSendResult(context, completeWeatherForecast, WEATHER_FORECAST_TYPE, updateType);
                                } else if (isLongWeatherForecast(updateType)) {
                                    appendLog(context, TAG, "Weather long forecast type");
                                    WeatherJSONParser.JSONParseResult parseResult = WeatherJSONParser.parseServerResult(weatherRaw);
                                    licenseKeysDbHelper.updateToken("forecast/daily", parseResult.getToken());
                                    CompleteWeatherForecast completeWeatherForecast = WeatherJSONParser.getLongWeatherForecast(parseResult.getOwmResponse());
                                    saveWeatherAndSendResult(context, completeWeatherForecast, LONG_WEATHER_FORECAST_TYPE, updateType);
                                } else {
                                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                                }

                            } catch (TooEarlyUpdateException teue) {
                                //if (updateRequest.isUpdateWeatherOnly()) {
                                    locationsDbHelper.updateLocationSource(
                                            currentLocation.getId(),
                                            getString(R.string.location_weather_update_status_too_early_update));
                                //}
                                timerHandler.removeCallbacksAndMessages(null);
                                resendTheIntentInSeveralSeconds(70);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                            } catch (LicenseNotValidException lnve) {
                                if (!license.equals(ApiKeys.getInitialLicenseKey(getBaseContext()))) {
                                    timerHandler.removeCallbacksAndMessages(null);
                                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                                } else {
                                    appendLog(context, TAG, "license not valid, going to try it by initial license key");
                                    if (isCurrentWeather(updateType)) {
                                        licenseKeysDbHelper.updateToken("weather", null);
                                    } else if (isWeatherForecast(updateType)) {
                                        licenseKeysDbHelper.updateToken("forecast", null);
                                    } else if (isLongWeatherForecast(updateType)) {
                                        licenseKeysDbHelper.updateToken("forecast/daily", null);
                                    }
                                    timerHandler.removeCallbacksAndMessages(null);
                                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                                }
                            } catch (JSONException e) {
                                appendLog(context, TAG, "JSONException:", e);
                                timerHandler.removeCallbacksAndMessages(null);
                                sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            appendLog(context, TAG, "onFailure:", statusCode, ":currentLocation=", currentLocation);
                            timerHandler.removeCallbacksAndMessages(null);
                            if (currentLocation != null) {
                                final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());
                                switch (statusCode) {
                                    case 401:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), getString(R.string.location_weather_update_status_access_expired));
                                        break;
                                    case 429:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), getString(R.string.location_weather_update_status_access_banned));
                                        break;
                                    default:
                                        locationsDbHelper.updateLastUpdatedAndLocationSource(currentLocation.getId(),
                                                System.currentTimeMillis(), getString(R.string.location_weather_update_status_location_only));
                                        break;
                                }
                            }
                            sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    });
                } catch (MalformedURLException mue) {
                    appendLog(context, TAG, "MalformedURLException:", mue);
                    sendResult(ACTION_WEATHER_UPDATE_FAIL, context, updateType);
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    private void sendResult(String result, Context context, int updateType) {
        sendResult(result, context, null, updateType);
    }

    private void sendResult(String result, Context context, Long locationId, int updateType) {
        if (isCurrentWeather(updateType)) {
            stopRefreshRotation("STOP", 2);
            sendMessageToWakeUpService(
                    AppWakeUpManager.FALL_DOWN,
                    AppWakeUpManager.SOURCE_CURRENT_WEATHER
            );
        } else {
            stopRefreshRotation("STOP", 1);
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
            updateResultInUI(locationId, result, updateRequest);
            if (!updateWeatherUpdateMessages.isEmpty()) {
                resendTheIntentInSeveralSeconds(5);
            }
            WidgetUtils.updateWidgets(getBaseContext());
            if (WidgetRefreshIconService.isRotationActive) {
                return;
            }
            sendMessageToReconciliationDbService(false);
        } catch (Throwable exception) {
            appendLog(context, TAG, "Exception occured when starting the service:", exception);
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
        currentWeatherDbHelper.saveWeather(location.getId(), now, weather);

        sendMessageToWeatherByVoiceService(location, weather, now);
        locationsDbHelper.updateLastUpdatedAndLocationSource(location.getId(), now, locationSource);
        sendResult(ACTION_WEATHER_UPDATE_OK, context, location.getId(), updateType);
    }

    private void saveWeatherAndSendResult(Context context, CompleteWeatherForecast completeWeatherForecast, int forecastType, int updateType) {
        WeatherForecastDbHelper weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(context);
        long lastUpdate = System.currentTimeMillis();
        WeatherRequestDataHolder updateRequest = updateWeatherUpdateMessages.peek();
        if (updateRequest == null) {
            return;
        }
        weatherForecastDbHelper.saveWeatherForecast(updateRequest.getLocationId(),
                forecastType,
                lastUpdate,
                completeWeatherForecast);
        GraphUtils.invalidateGraph();
        sendResult(ACTION_WEATHER_UPDATE_OK, context, updateType);
    }

    private void sendIntentToForecast(String result) {
        Intent intent = new Intent(ACTION_FORECAST_UPDATE_RESULT);
        intent.setPackage("org.thosp.yourlocalweather");
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_FORECAST_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_FORECAST_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        sendBroadcast(intent);
    }

    private void sendIntentToGraphs(String result) {
        Intent intent = new Intent(ACTION_GRAPHS_UPDATE_RESULT);
        intent.setPackage("org.thosp.yourlocalweather");
        if (result.equals(ACTION_WEATHER_UPDATE_OK)) {
            intent.putExtra(ACTION_GRAPHS_UPDATE_RESULT, ACTION_WEATHER_UPDATE_OK);
        } else if (result.equals(ACTION_WEATHER_UPDATE_FAIL)) {
            intent.putExtra(ACTION_GRAPHS_UPDATE_RESULT, ACTION_WEATHER_UPDATE_FAIL);
        }
        sendBroadcast(intent);
    }

    private void resendTheIntentInSeveralSeconds(int seconds) {
        appendLog(getBaseContext(), TAG, "resendTheIntentInSeveralSeconds:SDK:", Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ComponentName serviceComponent = new ComponentName(this, UpdateWeatherResendJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(UpdateWeatherResendJob.JOB_ID, serviceComponent);

            builder.setMinimumLatency(seconds * 1000); // wait at least
            builder.setOverrideDeadline((3 + seconds) * 1000); // maximum delay
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            jobScheduler.schedule(builder.build());
            appendLog(getBaseContext(), TAG, "resendTheIntentInSeveralSeconds: sent");
        } else {
            AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(),
                    0,
                    new Intent(getBaseContext(), UpdateWeatherService.class),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (1000 * seconds), pendingIntent);
        }
    }

    private boolean isCurrentWeather(int updateType) {
        return (START_CURRENT_WEATHER_UPDATE == updateType) || (START_CURRENT_WEATHER_RETRY == updateType);
    }

    private boolean isWeatherForecast(int updateType) {
        return (START_WEATHER_FORECAST_UPDATE == updateType) || (START_WEATHER_FORECAST_RETRY == updateType);
    }

    private boolean isLongWeatherForecast(int updateType) {
        return (START_LONG_WEATHER_FORECAST_UPDATE == updateType) || (START_LONG_WEATHER_FORECAST_RETRY == updateType);
    }

    private void weatherNotification(Long locationId, String updateSource) {
        Location locationForNotification = NotificationUtils.getLocationForNotification(getBaseContext());
        sendMessageToWakeUpService(
                AppWakeUpManager.FALL_DOWN,
                AppWakeUpManager.SOURCE_NOTIFICATION
        );
        if ((locationForNotification == null) || (locationForNotification.getId() != locationId)) {
          return;
        }
        String notificationPresence = AppPreference.getNotificationPresence(this);
        if ("permanent".equals(notificationPresence)) {
            NotificationUtils.weatherNotification(this, locationId);
        } else if ("on_lock_screen".equals(notificationPresence) && NotificationUtils.isScreenLocked(this)) {
            NotificationUtils.weatherNotification(this, locationId);
        } else if ((updateSource != null) && "NOTIFICATION".equals(updateSource)) {
            NotificationUtils.weatherNotification(this, locationId);
        }
    }

    protected void sendMessageToWeatherByVoiceService(Location location,
                                                      Weather weather,
                                                      long now) {
        weatherByVoiceServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    WeatherByVoiceService.START_VOICE_WEATHER_UPDATED,
                    new WeatherByVoiceRequestDataHolder(location, weather, now)
            );
            if (checkIfWeatherByVoiceServiceIsNotBound()) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                weatherByvOiceUnsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            weatherByVoiceService.send(msg);
        } catch (RemoteException e) {
            appendLog(getBaseContext(), TAG, e.getMessage(), e);
        } finally {
            weatherByVoiceServiceLock.unlock();
        }
    }

    private boolean checkIfWeatherByVoiceServiceIsNotBound() {
        if (weatherByVoiceService != null) {
            return false;
        }
        try {
            bindWeatherByVoiceService();
        } catch (Exception ie) {
            appendLog(getBaseContext(), TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return (weatherByVoiceService == null);
    }

    private void bindWeatherByVoiceService() {
        getApplicationContext().bindService(
                new Intent(getApplicationContext(), WeatherByVoiceService.class),
                weatherByVoiceServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindWeatherByVoiceService() {
        if (weatherByVoiceService == null) {
            return;
        }
        getApplicationContext().unbindService(weatherByVoiceServiceConnection);
    }

    private ServiceConnection weatherByVoiceServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            weatherByVoiceService = new Messenger(binderService);
            weatherByVoiceServiceLock.lock();
            try {
                while (!weatherByvOiceUnsentMessages.isEmpty()) {
                    weatherByVoiceService.send(weatherByvOiceUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                weatherByVoiceServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            weatherByVoiceService = null;
        }
    };

    private class UpdateWeatherMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            WeatherRequestDataHolder weatherRequestDataHolder = (WeatherRequestDataHolder) msg.obj;
            appendLog(getBaseContext(), TAG, "handleMessage:", msg.what, ":", weatherRequestDataHolder);
            if (weatherRequestDataHolder == null) {
                return;
            }
            appendLog(getBaseContext(),
                    TAG,
                    "currentWeatherUpdateMessages.size when adding new message = ", updateWeatherUpdateMessages);
            switch (msg.what) {
                case START_PROCESS_CURRENT_QUEUE:
                case START_LONG_WEATHER_FORECAST_RETRY:
                case START_CURRENT_WEATHER_RETRY:
                case START_WEATHER_FORECAST_RETRY:
                case START_WEATHER_FORECAST_UPDATE:
                case START_CURRENT_WEATHER_UPDATE:
                case START_LONG_WEATHER_FORECAST_UPDATE:
                    if (!updateWeatherUpdateMessages.contains(weatherRequestDataHolder)) {
                        updateWeatherUpdateMessages.add(weatherRequestDataHolder);
                    }
                    startWeatherUpdate();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
