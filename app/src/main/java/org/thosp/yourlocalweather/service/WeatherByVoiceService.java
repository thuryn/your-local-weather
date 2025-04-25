package org.thosp.yourlocalweather.service;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import androidx.annotation.Nullable;

import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.YourLocalWeather;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.NotificationUtils;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.TimeUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;
import org.thosp.yourlocalweather.utils.WindWithUnit;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class WeatherByVoiceService extends Service {

    private static final String TAG = "WeatherByVoiceService";

    private TextToSpeech tts;
    final private static String TTS_DELAY_BETWEEN_ITEM = "...---...";
    final private static String TTS_END = "_________";
    final private static long TTS_DELAY_BETWEEN_ITEM_IN_MS = 200;

    private static final Queue<WeatherByVoiceRequestDataHolder> weatherByVoiceMessages = new LinkedList<>();

    public LinkedList<String> sayWhatWhenRecreated;
    private String rainSnowUnitFromPreferences;
    private String temperatureUnitFromPreferences;
    private String windUnitFromPreferences;
    private String timeStylePreference;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            recreateTts(sayWhatWhenRecreated);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        YourLocalWeather.executor.submit(() -> {
            rainSnowUnitFromPreferences = AppPreference.getRainSnowUnitFromPreferences(getBaseContext());
            temperatureUnitFromPreferences = AppPreference.getTemperatureUnitFromPreferences(getBaseContext());
            windUnitFromPreferences = AppPreference.getWindUnitFromPreferences(getBaseContext());
            timeStylePreference = AppPreference.getTimeStylePreference(getBaseContext());
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if (intent == null) {
            return ret;
        }
        YourLocalWeather.executor.submit(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.getNotificationForActivity(getBaseContext()), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.getNotificationForActivity(getBaseContext()));
            }
            appendLog(getBaseContext(), TAG, "onStartCommand:", intent);
            switch (intent.getAction()) {
                case "org.thosp.yourlocalweather.action.SAY_WEATHER": sayWeatherByTime(intent); return;
                case "org.thosp.yourlocalweather.action.START_VOICE_WEATHER_UPDATED": startVoiceCommand(intent); return;
                default:
            }
        });
        return ret;
    }

    private void sayWeatherByTime(Intent intent) {
        Long voiceSettingId = intent.getLongExtra("voiceSettingId", Long.MAX_VALUE);
        boolean initiatedFromBtDevice = intent.getBooleanExtra("initiatedFromBtDevice", false);
        appendLog(getBaseContext(), TAG, "sayWeatherByTime:", voiceSettingId);

        if (voiceSettingId == Long.MAX_VALUE) {
            return;
        }
        TimeUtils.setupAlarmForVoice(getBaseContext());
        sayForLocation(voiceSettingId, initiatedFromBtDevice);
    }

    private void startVoiceCommand(Intent intent) {
        startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.getNotificationForActivity(getBaseContext()));
        Location weatherByVoiceLocation = intent.getParcelableExtra("weatherByVoiceLocation");
        Weather weatherByVoiceWeather = intent.getParcelableExtra("weatherByVoiceWeather");
        Long weatherByVoiceTime = intent.getLongExtra("weatherByVoiceTime", 0);
        WeatherByVoiceRequestDataHolder weatherByVoiceRequest = new WeatherByVoiceRequestDataHolder(weatherByVoiceLocation, weatherByVoiceWeather, weatherByVoiceTime);
        appendLog(getBaseContext(), TAG, "weatherByVoiceLocation:", weatherByVoiceRequest);
        weatherByVoiceMessages.add(weatherByVoiceRequest);
        startVoiceCommand();
    }

    private void sayCurrentWeatherForLocation(WeatherByVoiceRequestDataHolder updateRequest) {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());

        Map<Long, Boolean> allLocations = voiceSettingParametersDbHelper.getBooleanParam(
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
        appendLog(getBaseContext(), TAG, "sayForLocation:allLocations:" + allLocations);
        for (Long voiceSettingId: allLocations.keySet()) {
            Long triggerType = voiceSettingParametersDbHelper.getLongParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId());
            if (triggerType != 0) {
                continue;
            }
            Boolean locations = allLocations.get(voiceSettingId);
            if ((locations != null) && locations) {
                sayCurrentWeather(
                        updateRequest.getWeather(),
                        updateRequest.getLocation(),
                        updateRequest.getTimestamp(),
                        voiceSettingId,
                        false);
                return;
            }
            String enabledLocationIds = voiceSettingParametersDbHelper.getStringParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
            appendLog(getBaseContext(), TAG, "sayForLocation:enabledLocationIds:" + enabledLocationIds);
            if ((enabledLocationIds != null) &&
                    (updateRequest != null) &&
                    (updateRequest.getLocation() != null) &&
                    (updateRequest.getLocation().getId() != null) &&
                    enabledLocationIds.contains(updateRequest.getLocation().getId().toString())) {
                sayCurrentWeather(
                        updateRequest.getWeather(),
                        updateRequest.getLocation(),
                        updateRequest.getTimestamp(),
                        voiceSettingId,
                        false);
                return;
            }
        }
        startVoiceCommand();
    }

    private void sayForLocation(Long voiceSettingId, boolean initiatedFromBtDEvice) {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());
        final CurrentWeatherDbHelper currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(getBaseContext());
        final LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(getBaseContext());

        Boolean allLocations = voiceSettingParametersDbHelper.getBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
        appendLog(getBaseContext(), TAG, "sayForLocation:allLocations:" + allLocations);
        if ((allLocations != null) && allLocations) {
            for (Location location: locationsDbHelper.getAllRows()) {
                CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(location.getId());
                sayCurrentWeather(
                        weatherRecord.getWeather(),
                        location,
                        weatherRecord.getLastUpdatedTime(),
                        voiceSettingId,
                        initiatedFromBtDEvice);
            }
            return;
        }
        String enabledLocationIds = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
        appendLog(getBaseContext(), TAG, "sayForLocation:enabledLocationIds:" + enabledLocationIds);
        List<Location> locations = locationsDbHelper.getAllRows();
        if (locations.isEmpty()) {
            return;
        }
        for (Location location: locations) {
            if (enabledLocationIds.contains(location.getId().toString())) {
                CurrentWeatherDbHelper.WeatherRecord weatherRecord = currentWeatherDbHelper.getWeather(location.getId());
                if (weatherRecord == null) {
                    continue;
                }
                sayCurrentWeather(
                        weatherRecord.getWeather(),
                        location,
                        weatherRecord.getLastUpdatedTime(),
                        voiceSettingId,
                        initiatedFromBtDEvice);
            }
        }
        startVoiceCommand();
    }

    public void startAllLocationsVoiceCommand(Long voiceSettingId) {
        appendLog(getBaseContext(), TAG, "startVoiceCommand");

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size before peek = ", weatherByVoiceMessages);

        WeatherByVoiceRequestDataHolder updateRequest = weatherByVoiceMessages.poll();

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size after peek = ", weatherByVoiceMessages);

        if (updateRequest == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "updateRequest is null");
            return;
        }
        sayForLocation(voiceSettingId, true);
    }


    public void startVoiceCommand() {
        appendLog(getBaseContext(), TAG, "startVoiceCommand");

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size before peek = ", weatherByVoiceMessages);

        WeatherByVoiceRequestDataHolder updateRequest = weatherByVoiceMessages.poll();

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size after peek = ", weatherByVoiceMessages);

        if (updateRequest == null) {
            appendLog(getBaseContext(),
                    TAG,
                    "updateRequest is null");
            return;
        }
        sayCurrentWeatherForLocation(updateRequest);
    }

    private void sayCurrentWeather(Weather weather,
                                   Location currentLocation,
                                   long now,
                                   Long voiceSettingId,
                                   boolean initiatedFromBtDevice) {
        appendLog(getBaseContext(), TAG, "sayCurrentWeather voiceSettingIdFromSettings: " + voiceSettingId + ":" + now + ":" + currentLocation);
        Long voiceSettingIdFromSettings = isAnySettingValidToTellWeather(voiceSettingId, initiatedFromBtDevice);
        appendLog(getBaseContext(), TAG, "sayCurrentWeather voiceSettingIdFromSettings: " + voiceSettingIdFromSettings);
        if (voiceSettingIdFromSettings == null) {
            return;
        }
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());

        LinkedList<String> textToSay = new LinkedList<>();

        Long partsToSay = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingIdFromSettings,
                VoiceSettingParamType.VOICE_SETTING_PARTS_TO_SAY.getVoiceSettingParamTypeId());
        if (partsToSay == null) {
            return;
        }

        if (TimeUtils.isCurrentSettingIndex(partsToSay, 0)) {
            if (TimeUtils.isCurrentSettingIndex(partsToSay, 1)) {
                textToSay.add(getCustomGreeting(voiceSettingParametersDbHelper, voiceSettingIdFromSettings));
            } else {
                textToSay.add(getString(getGreetingId()));
            }
            textToSay.add(TTS_DELAY_BETWEEN_ITEM);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 2)) {
            if (TimeUtils.isCurrentSettingIndex(partsToSay, 3)) {
                textToSay.add(String.format(voiceSettingParametersDbHelper.getStringParam(
                        voiceSettingIdFromSettings,
                        VoiceSettingParamType.VOICE_SETTING_LOCATION_CUSTOM.getVoiceSettingParamTypeId()),
                        Utils.getLocationForVoiceFromAddress(currentLocation.getAddress())));
            } else {
                textToSay.add(getString(R.string.tts_say_current_weather_with_location,
                        Utils.getLocationForVoiceFromAddress(currentLocation.getAddress())));
            }
        } else if (TimeUtils.isCurrentSettingIndex(partsToSay, 4)) {
            if (TimeUtils.isCurrentSettingIndex(partsToSay, 5)) {
                textToSay.add(voiceSettingParametersDbHelper.getStringParam(
                        voiceSettingIdFromSettings,
                        VoiceSettingParamType.VOICE_SETTING_WEATHER_DESCRIPTION_CUSTOM.getVoiceSettingParamTypeId()));
            } else {
                textToSay.add(getString(R.string.tts_say_current_weather));
            }
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 4) || TimeUtils.isCurrentSettingIndex(partsToSay, 2)) {
            textToSay.add(TTS_DELAY_BETWEEN_ITEM);
            String weatherDescriptionToSay = " " +
                    Utils.getWeatherDescription(getBaseContext(), weather) +
                    " ";
            textToSay.add(weatherDescriptionToSay);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 6)) {
            textToSay.add(TTS_DELAY_BETWEEN_ITEM);
            StringBuilder temperatureToSay = new StringBuilder();
            if (TimeUtils.isCurrentSettingIndex(partsToSay, 7)) {
                temperatureToSay.append(String.format(voiceSettingParametersDbHelper.getStringParam(
                        voiceSettingIdFromSettings,
                        VoiceSettingParamType.VOICE_SETTING_TEMPERATURE_CUSTOM.getVoiceSettingParamTypeId()),
                        TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weather.getTemperature(), temperatureUnitFromPreferences, currentLocation.getLocale())));
            } else {
                temperatureToSay.append(getString(R.string.tty_say_temperature,
                        TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weather.getTemperature(), temperatureUnitFromPreferences, currentLocation.getLocale())));
            }
            temperatureToSay.append(" ");
            textToSay.add(temperatureToSay.toString());
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 8)) {
            textToSay.add(TTS_DELAY_BETWEEN_ITEM);
            StringBuilder windToSay = new StringBuilder();

            WindWithUnit windWithUnit = AppPreference.getWindWithUnit(getBaseContext(),
                    weather.getWindSpeed(),
                    weather.getWindDirection(),
                    windUnitFromPreferences,
                    currentLocation.getLocale());
            if (TimeUtils.isCurrentSettingIndex(partsToSay, 9)) {
                windToSay.append(String.format(voiceSettingParametersDbHelper.getStringParam(
                        voiceSettingIdFromSettings,
                        VoiceSettingParamType.VOICE_SETTING_WIND_CUSTOM.getVoiceSettingParamTypeId()),
                        windWithUnit.getWindSpeed(0),
                        windWithUnit.getWindUnit(),
                        windWithUnit.getWindDirectionByVoice()));
            } else {
                windToSay.append(getString(R.string.tty_say_wind,
                        windWithUnit.getWindSpeed(0),
                        windWithUnit.getWindUnit(),
                        windWithUnit.getWindDirectionByVoice()));
            }
            windToSay.append(" ");
            textToSay.add(windToSay.toString());
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 10)) {
            ForecastUtil.WeatherForecastForVoice weatherForecastForVoice = ForecastUtil.calculateWeatherVoiceForecast(getBaseContext(), currentLocation.getId());
            if (weatherForecastForVoice != null) {
                textToSay.add(TTS_DELAY_BETWEEN_ITEM);
                StringBuilder forecastToSay = new StringBuilder();
                int currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                forecastToSay.append(getString(R.string.tty_say_weather_forecast));
                forecastToSay.append(" ");
                if (currentDayOfYear == weatherForecastForVoice.dayOfYear) {
                    forecastToSay.append(getString(R.string.tty_say_weather_forecast_today));
                    forecastToSay.append(" ");
                } else if ((currentDayOfYear + 1) == weatherForecastForVoice.dayOfYear) {
                    forecastToSay.append(getString(R.string.tty_say_weather_forecast_tomorrow));
                    forecastToSay.append(" ");
                }

                String forecastCommonWeatherForecastToSay = sayCommonWeatherForecastParts(weatherForecastForVoice, rainSnowUnitFromPreferences, currentLocation);
                if (forecastCommonWeatherForecastToSay != null) {
                    forecastToSay.append(forecastCommonWeatherForecastToSay);
                }

                boolean commonPartsAreComplete = forecastCommonWeatherForecastToSay != null;
                boolean nightWeather = weatherForecastForVoice.nightWeatherIds != null;
                boolean morningWeather = weatherForecastForVoice.morningWeatherIds != null;
                boolean afternoonWeather = weatherForecastForVoice.afternoonWeatherIds != null;
                boolean eveningWeather = weatherForecastForVoice.eveningWeatherIds != null;

                if (nightWeather && !commonPartsAreComplete) {
                    forecastToSay.append(getString(R.string.tty_say_weather_forecast_night));
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.nightWeatherIds.mainWeatherId,
                            getBaseContext()));
                    forecastToSay.append(" ");
                    if (weatherForecastForVoice.nightWeatherIds.warningWeatherId != null) {
                        forecastToSay.append(getString(R.string.tty_say_weather_forecast_rarely));
                        forecastToSay.append(" ");
                        forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.nightWeatherIds.warningWeatherId,
                                getBaseContext()));
                        forecastToSay.append(" ");
                    }
                    forecastToSay.append(sayRainSnow(weatherForecastForVoice.nightWeatherMaxMin.maxRain, weatherForecastForVoice.nightWeatherMaxMin.maxSnow, rainSnowUnitFromPreferences, currentLocation));
                }
                if (morningWeather && !commonPartsAreComplete) {
                    forecastToSay.append(getString(R.string.tty_say_weather_forecast_morning));
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                            getBaseContext()));
                    forecastToSay.append(" ");
                    if (weatherForecastForVoice.morningWeatherIds.warningWeatherId != null) {
                        forecastToSay.append(getString(R.string.tty_say_weather_forecast_rarely));
                        forecastToSay.append(" ");
                        forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.warningWeatherId,
                                getBaseContext()));
                        forecastToSay.append(" ");
                    }
                    forecastToSay.append(sayRainSnow(weatherForecastForVoice.morningWeatherMaxMin.maxRain, weatherForecastForVoice.morningWeatherMaxMin.maxSnow, rainSnowUnitFromPreferences, currentLocation));
                }
                if (afternoonWeather && !commonPartsAreComplete) {
                    forecastToSay.append(getString(R.string.tty_say_weather_forecast_afternoon));
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.afternoonWeatherIds.mainWeatherId,
                            getBaseContext()));
                    forecastToSay.append(" ");
                    if (weatherForecastForVoice.afternoonWeatherIds.warningWeatherId != null) {
                        forecastToSay.append(getString(R.string.tty_say_weather_forecast_rarely));
                        forecastToSay.append(" ");
                        forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.afternoonWeatherIds.warningWeatherId,
                                getBaseContext()));
                        forecastToSay.append(" ");
                    }
                    forecastToSay.append(sayRainSnow(weatherForecastForVoice.afternoonWeatherMaxMin.maxRain, weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow, rainSnowUnitFromPreferences,currentLocation));
                }
                if (eveningWeather && !commonPartsAreComplete) {
                    forecastToSay.append(getString(R.string.tty_say_weather_forecast_evening));
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                            getBaseContext()));
                    forecastToSay.append(" ");
                    if (weatherForecastForVoice.eveningWeatherIds.warningWeatherId != null) {
                        forecastToSay.append(getString(R.string.tty_say_weather_forecast_rarely));
                        forecastToSay.append(" ");
                        forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.warningWeatherId,
                                getBaseContext()));
                        forecastToSay.append(" ");
                    }
                    forecastToSay.append(sayRainSnow(weatherForecastForVoice.eveningWeatherMaxMin.maxRain, weatherForecastForVoice.eveningWeatherMaxMin.maxSnow, rainSnowUnitFromPreferences, currentLocation));
                }
                forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);

                if ((weatherForecastForVoice.minTempTime != null) && (weatherForecastForVoice.maxTempTime != null)) {
                    if (Math.round(weatherForecastForVoice.minTempForDay) == Math.round(weatherForecastForVoice.maxTempForDay)) {
                        if (weatherForecastForVoice.minTempForDay >= 0) {
                            forecastToSay.append(getString(R.string.tty_say_temp_max,
                                    TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, temperatureUnitFromPreferences, currentLocation.getLocale()),
                                    AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), timeStylePreference, currentLocation.getLocale())));
                            forecastToSay.append(" ");
                        } else {
                            forecastToSay.append(getString(R.string.tty_say_temp_min,
                                    TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, temperatureUnitFromPreferences, currentLocation.getLocale()),
                                    AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), timeStylePreference, currentLocation.getLocale())));
                            forecastToSay.append(" ");
                        }
                    } else {
                        if (weatherForecastForVoice.minTempTime < weatherForecastForVoice.maxTempTime) {
                            forecastToSay.append(getString(R.string.tty_say_temp_min,
                                    TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, temperatureUnitFromPreferences, currentLocation.getLocale()),
                                    AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), timeStylePreference, currentLocation.getLocale())));
                            forecastToSay.append(" ");
                            forecastToSay.append(getString(R.string.tty_say_temp_max,
                                    TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.maxTempForDay, temperatureUnitFromPreferences, currentLocation.getLocale()),
                                    AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.maxTempTime), timeStylePreference, currentLocation.getLocale())));
                            forecastToSay.append(" ");
                        } else {
                            forecastToSay.append(getString(R.string.tty_say_temp_max,
                                    TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.maxTempForDay, temperatureUnitFromPreferences, currentLocation.getLocale()),
                                    AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.maxTempTime), timeStylePreference, currentLocation.getLocale())));
                            forecastToSay.append(" ");
                            forecastToSay.append(getString(R.string.tty_say_temp_min,
                                    TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, temperatureUnitFromPreferences, currentLocation.getLocale()),
                                    AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), timeStylePreference, currentLocation.getLocale())));
                            forecastToSay.append(" ");
                        }
                    }
                } else {
                    appendLog(getBaseContext(), TAG, "min a max time null: ", weatherForecastForVoice.minTempTime, weatherForecastForVoice.maxTempTime, weatherForecastForVoice.minTempForDay, weatherForecastForVoice.maxTempForDay);
                }
                forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);
                WindWithUnit windWithUnit = AppPreference.getWindWithUnit(getBaseContext(),
                        (float) weatherForecastForVoice.maxWindForDay,
                        (float) weatherForecastForVoice.windDegreeForDay,
                        windUnitFromPreferences,
                        currentLocation.getLocale());
                forecastToSay.append(getString(R.string.tty_say_max_wind,
                        windWithUnit.getWindSpeed(0),
                        windWithUnit.getWindUnit(),
                        windWithUnit.getWindDirectionByVoice()));
                forecastToSay.append(" ");
                textToSay.add(forecastToSay.toString());
            }
        }
        textToSay.add(TTS_END);
        sayWeather(textToSay);
    }

    private String sayRainSnow(double rain, double snow, String rainSnowUnitFromPreferences, Location location) {
        StringBuilder forecastToSay = new StringBuilder();
        if (rain > MIN_RAIN_SNOW_MM) {
            forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);
            forecastToSay.append(getString(R.string.tty_say_max_rain,
                    AppPreference.getFormatedRainOrSnow(rainSnowUnitFromPreferences, rain, location.getLocale())));
            forecastToSay.append(" ");
        }
        if (snow > MIN_RAIN_SNOW_MM) {
            forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);
            forecastToSay.append(getString(R.string.tty_say_max_rain,
                    AppPreference.getFormatedRainOrSnow(rainSnowUnitFromPreferences, snow, location.getLocale())));
            forecastToSay.append(" ");
        }
        return forecastToSay.toString();
    }

    private final double MIN_RAIN_SNOW_MM = 0.5;

    private String sayCommonWeatherForecastParts(ForecastUtil.WeatherForecastForVoice weatherForecastForVoice, String rainSnowUnitFromPreferences,
                                                 Location currentLocation) {

        boolean nightWeather = weatherForecastForVoice.nightWeatherIds != null;
        boolean morningWeather = weatherForecastForVoice.morningWeatherIds != null;
        boolean afternoonWeather = weatherForecastForVoice.afternoonWeatherIds != null;
        boolean eveningWeather = weatherForecastForVoice.eveningWeatherIds != null;
        boolean nightMorningAreSame = false;
        boolean nightMorningWarningAreSame = false;
        boolean morningAfternoonAreSame = false;
        boolean morningEveningAreSame = false;
        boolean afternoonEveningAreSame = false;
        boolean morningAfternoonWarningAreSame = false;
        boolean morningEveningWarningAreSame = false;
        boolean afternoonEveningWarningAreSame = false;
        double nightMorningAreSameMaxRain = 0;
        double nightMorningAreSameMaxSnow = 0;
        double morningAfternoonAreSameMaxRain = 0;
        double morningAfternoonAreSameMaxSnow = 0;
        double morningEveningAreSameMaxRain = 0;
        double morningEveningAreSameMaxSnow = 0;
        double afternoonEveningAreSameMaxRain = 0;
        double afternoonEveningAreSameMaxSnow = 0;

        appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:" + nightWeather + ":" + morningWeather + ":" + afternoonWeather + ':' + eveningWeather);

        if (nightWeather && morningWeather) {
            nightMorningAreSame = weatherForecastForVoice.nightWeatherIds.mainWeatherId.equals(weatherForecastForVoice.morningWeatherIds.mainWeatherId);
            nightMorningWarningAreSame = (weatherForecastForVoice.nightWeatherIds.warningWeatherId != null) && weatherForecastForVoice.nightWeatherIds.warningWeatherId.equals(weatherForecastForVoice.morningWeatherIds.warningWeatherId);
            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:nightWeatherIds:morningWeatherIds:" + weatherForecastForVoice.nightWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.morningWeatherIds.mainWeatherId + ":" + nightMorningAreSame);
            if ((weatherForecastForVoice.nightWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.morningWeatherMaxMin.maxRain < weatherForecastForVoice.nightWeatherMaxMin.maxRain)) {
                nightMorningAreSameMaxRain = weatherForecastForVoice.nightWeatherMaxMin.maxRain;
            } else if (weatherForecastForVoice.morningWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) {
                nightMorningAreSameMaxRain = weatherForecastForVoice.morningWeatherMaxMin.maxRain;
            }
            if ((weatherForecastForVoice.nightWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.morningWeatherMaxMin.maxSnow < weatherForecastForVoice.nightWeatherMaxMin.maxSnow)) {
                nightMorningAreSameMaxSnow = weatherForecastForVoice.nightWeatherMaxMin.maxSnow;
            } else if (weatherForecastForVoice.morningWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) {
                nightMorningAreSameMaxSnow = weatherForecastForVoice.morningWeatherMaxMin.maxSnow;
            }
        }
        if (morningWeather && afternoonWeather) {
            morningAfternoonAreSame = weatherForecastForVoice.morningWeatherIds.mainWeatherId.equals(weatherForecastForVoice.afternoonWeatherIds.mainWeatherId);
            morningAfternoonWarningAreSame = (weatherForecastForVoice.morningWeatherIds.warningWeatherId != null) && weatherForecastForVoice.morningWeatherIds.warningWeatherId.equals(weatherForecastForVoice.afternoonWeatherIds.warningWeatherId);

            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:morningWeatherIds:afternoonWeatherIds:" + weatherForecastForVoice.morningWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.afternoonWeatherIds.mainWeatherId + ":" + morningAfternoonAreSame);
            if ((weatherForecastForVoice.morningWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.afternoonWeatherMaxMin.maxRain < weatherForecastForVoice.morningWeatherMaxMin.maxRain)) {
                morningAfternoonAreSameMaxRain = weatherForecastForVoice.morningWeatherMaxMin.maxRain;
            } else if (weatherForecastForVoice.afternoonWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) {
                morningAfternoonAreSameMaxRain = weatherForecastForVoice.afternoonWeatherMaxMin.maxRain;
            }
            if ((weatherForecastForVoice.morningWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow < weatherForecastForVoice.morningWeatherMaxMin.maxSnow)) {
                morningAfternoonAreSameMaxSnow = weatherForecastForVoice.morningWeatherMaxMin.maxSnow;
            } else if (weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) {
                morningAfternoonAreSameMaxSnow = weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow;
            }
        }
        if (morningWeather && eveningWeather) {

            morningEveningAreSame = weatherForecastForVoice.morningWeatherIds.mainWeatherId.equals(weatherForecastForVoice.eveningWeatherIds.mainWeatherId);
            morningEveningWarningAreSame = (weatherForecastForVoice.morningWeatherIds.warningWeatherId != null) && weatherForecastForVoice.morningWeatherIds.warningWeatherId.equals(weatherForecastForVoice.eveningWeatherIds.warningWeatherId);

            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:morningWeatherIds:eveningWeatherIds:" + weatherForecastForVoice.morningWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.eveningWeatherIds.mainWeatherId + ":" + morningEveningAreSame);
            if ((weatherForecastForVoice.morningWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.eveningWeatherMaxMin.maxRain < weatherForecastForVoice.morningWeatherMaxMin.maxRain)) {
                morningEveningAreSameMaxRain = weatherForecastForVoice.morningWeatherMaxMin.maxRain;
            } else if (weatherForecastForVoice.eveningWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) {
                morningEveningAreSameMaxRain = weatherForecastForVoice.eveningWeatherMaxMin.maxRain;
            }
            if ((weatherForecastForVoice.morningWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.eveningWeatherMaxMin.maxSnow < weatherForecastForVoice.morningWeatherMaxMin.maxSnow)) {
                morningEveningAreSameMaxSnow = weatherForecastForVoice.morningWeatherMaxMin.maxSnow;
            } else if (weatherForecastForVoice.eveningWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) {
                morningEveningAreSameMaxSnow = weatherForecastForVoice.eveningWeatherMaxMin.maxSnow;
            }
        }
        if (afternoonWeather && eveningWeather) {
            afternoonEveningAreSame = weatherForecastForVoice.afternoonWeatherIds.mainWeatherId.equals(weatherForecastForVoice.eveningWeatherIds.mainWeatherId);
            afternoonEveningWarningAreSame = (weatherForecastForVoice.afternoonWeatherIds.warningWeatherId != null) && weatherForecastForVoice.afternoonWeatherIds.warningWeatherId.equals(weatherForecastForVoice.eveningWeatherIds.warningWeatherId);

            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:afternoonWeatherIds:eveningWeatherIds:" + weatherForecastForVoice.afternoonWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.eveningWeatherIds.mainWeatherId + ":" + afternoonEveningAreSame);
            if ((weatherForecastForVoice.afternoonWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.eveningWeatherMaxMin.maxRain < weatherForecastForVoice.afternoonWeatherMaxMin.maxRain)) {
                afternoonEveningAreSameMaxRain = weatherForecastForVoice.afternoonWeatherMaxMin.maxRain;
            } else if (weatherForecastForVoice.eveningWeatherMaxMin.maxRain > MIN_RAIN_SNOW_MM) {
                afternoonEveningAreSameMaxRain = weatherForecastForVoice.eveningWeatherMaxMin.maxRain;
            }
            if ((weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) && (weatherForecastForVoice.eveningWeatherMaxMin.maxSnow < weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow)) {
                afternoonEveningAreSameMaxSnow = weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow;
            } else if (weatherForecastForVoice.eveningWeatherMaxMin.maxSnow > MIN_RAIN_SNOW_MM) {
                afternoonEveningAreSameMaxSnow = weatherForecastForVoice.eveningWeatherMaxMin.maxSnow;
            }
        }

        double maxRain = Math.max(Math.max(Math.max(nightMorningAreSameMaxRain, morningAfternoonAreSameMaxRain), morningEveningAreSameMaxRain), afternoonEveningAreSameMaxRain);
        double maxSnow = Math.max(Math.max(Math.max(nightMorningAreSameMaxSnow, morningAfternoonAreSameMaxSnow), morningEveningAreSameMaxSnow), afternoonEveningAreSameMaxSnow);

        if ((!nightWeather || nightMorningAreSame) &&
                (!morningWeather || morningAfternoonAreSame) &&
                (!morningWeather || morningEveningAreSame) &&
                (!afternoonWeather || afternoonEveningAreSame)) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            Integer mainWeatherId = null;
            String mainWeatherDescription = null;
            Integer warningWeatherId = null;
            String warningWeatherDescription = null;
            if (weatherForecastForVoice.nightWeatherIds != null) {
                mainWeatherId = weatherForecastForVoice.nightWeatherIds.mainWeatherId;
                mainWeatherDescription = weatherForecastForVoice.nightWeatherIds.mainWeatherDescriptionsFromOwm;
                warningWeatherId = weatherForecastForVoice.nightWeatherIds.warningWeatherId;
                warningWeatherDescription = weatherForecastForVoice.nightWeatherIds.warningWeatherDescriptionsFromOwm;
            } else if (weatherForecastForVoice.morningWeatherIds != null) {
                mainWeatherId = weatherForecastForVoice.morningWeatherIds.mainWeatherId;
                mainWeatherDescription = weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm;
                warningWeatherId = weatherForecastForVoice.morningWeatherIds.warningWeatherId;
                warningWeatherDescription = weatherForecastForVoice.morningWeatherIds.warningWeatherDescriptionsFromOwm;
            } else if (weatherForecastForVoice.afternoonWeatherIds != null) {
                mainWeatherId = weatherForecastForVoice.afternoonWeatherIds.mainWeatherId;
                mainWeatherDescription = weatherForecastForVoice.afternoonWeatherIds.mainWeatherDescriptionsFromOwm;
                warningWeatherId = weatherForecastForVoice.afternoonWeatherIds.warningWeatherId;
                warningWeatherDescription = weatherForecastForVoice.afternoonWeatherIds.warningWeatherDescriptionsFromOwm;
            } else if (weatherForecastForVoice.nightWeatherIds != null) {
                mainWeatherId = weatherForecastForVoice.nightWeatherIds.mainWeatherId;
                mainWeatherDescription = weatherForecastForVoice.nightWeatherIds.mainWeatherDescriptionsFromOwm;
                warningWeatherId = weatherForecastForVoice.nightWeatherIds.warningWeatherId;
                warningWeatherDescription = weatherForecastForVoice.nightWeatherIds.warningWeatherDescriptionsFromOwm;
            }
            if (mainWeatherId == null) {
                return "";
            }

            forecastToSay.append(Utils.getWeatherDescription(mainWeatherId,
                    getBaseContext()));
            if (nightMorningWarningAreSame && morningAfternoonWarningAreSame && morningEveningWarningAreSame && afternoonEveningWarningAreSame) {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_rarely));
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(warningWeatherId,
                        getBaseContext()));
            }
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, rainSnowUnitFromPreferences, currentLocation));
            return forecastToSay.toString();
        }
        if (nightMorningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            if (morningAfternoonAreSame) {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_night));
                forecastToSay.append(", ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_morning));
                forecastToSay.append(" ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_and));
                forecastToSay.append(" ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_afternoon));
                forecastToSay.append(" ");
            } else {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_night));
                forecastToSay.append(" ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_and));
                forecastToSay.append(" ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_morning));
                forecastToSay.append(" ");
            }
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    getBaseContext()));
            forecastToSay.append(" ");
        }
        if (morningAfternoonAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            if (!nightMorningAreSame) {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_morning));
                forecastToSay.append(" ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_and));
                forecastToSay.append(" ");
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_afternoon));
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                        getBaseContext()));
                forecastToSay.append(" ");
            }
            if (eveningWeather) {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_evening));
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        getBaseContext()));
                forecastToSay.append(" ");
            }
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, rainSnowUnitFromPreferences, currentLocation));
            return forecastToSay.toString();
        } else if (morningEveningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            forecastToSay.append(getString(R.string.tty_say_weather_forecast_morning));
            forecastToSay.append(" ");
            forecastToSay.append(getString(R.string.tty_say_weather_forecast_and));
            forecastToSay.append(" ");
            forecastToSay.append(getString(R.string.tty_say_weather_forecast_evening));
            forecastToSay.append(" ");
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    getBaseContext()));
            forecastToSay.append(" ");
            if (afternoonWeather) {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_afternoon));
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        getBaseContext()));
                forecastToSay.append(" ");
            }
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, rainSnowUnitFromPreferences, currentLocation));
            forecastToSay.append(" ");
            return forecastToSay.toString();
        } else if (afternoonEveningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            if (morningWeather) {
                forecastToSay.append(getString(R.string.tty_say_weather_forecast_morning));
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        getBaseContext()));
                forecastToSay.append(" ");
            }
            forecastToSay.append(getString(R.string.tty_say_weather_forecast_afternoon));
            forecastToSay.append(" ");
            forecastToSay.append(getString(R.string.tty_say_weather_forecast_and));
            forecastToSay.append(" ");
            forecastToSay.append(getString(R.string.tty_say_weather_forecast_evening));
            forecastToSay.append(" ");
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    getBaseContext()));
            forecastToSay.append(" ");
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, rainSnowUnitFromPreferences, currentLocation));
            forecastToSay.append(" ");
            return forecastToSay.toString();
        }
        return null;
    }

    private void sayWeather(final LinkedList<String> what) {
        appendLog(getBaseContext(), TAG, "Going to say: " + what);
        if (tts != null) {
            say(what);
            return;
        }
        recreateTts(what);
    }

    private Locale getLocaleForVoice() {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());
        String localeForVoiceId = voiceSettingParametersDbHelper.getGeneralStringParam(VoiceSettingParamType.VOICE_SETTING_VOICE_LANG.getVoiceSettingParamTypeId());
        if ((localeForVoiceId == null) || ("Default".equals(localeForVoiceId))) {
            return new Locale(AppPreference.getInstance().getLanguage(this));
        } else {
            return new Locale(localeForVoiceId);
        }
    }

    private void say(LinkedList<String> texts) {
        Locale locale = getLocaleForVoice();
        int available = tts.isLanguageAvailable(locale);
        if (available >= TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
            tts.setSpeechRate(1.0f);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int counter = 0;
                for (String text: texts) {
                    if (TTS_DELAY_BETWEEN_ITEM.equals(text)) {
                        tts.playSilentUtterance(TTS_DELAY_BETWEEN_ITEM_IN_MS, TextToSpeech.QUEUE_ADD, "111111" + (counter++));
                    } else if (TTS_END.equals(text)) {
                        tts.playSilentUtterance(TTS_DELAY_BETWEEN_ITEM_IN_MS, TextToSpeech.QUEUE_ADD, TTS_END);
                    } else {
                        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "111111" + (counter++));
                    }
                }
            } else {
                for (String text: texts) {
                    if (TTS_DELAY_BETWEEN_ITEM.equals(text)) {
                        tts.playSilence(TTS_DELAY_BETWEEN_ITEM_IN_MS, TextToSpeech.QUEUE_ADD, null);
                    } else {
                        tts.speak(text, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }
            sayWhatWhenRecreated = null;
        } else {
            appendLog(getBaseContext(), TAG, "Locale " + locale + " is not available in TTS");
            if (sayWhatWhenRecreated != null) {
                return;
            }
            sayWhatWhenRecreated = texts;
            timerHandler.postDelayed(timerRunnable, 1000);
        }
    }

    private Long isAnySettingValidToTellWeather(Long voiceSettingId, boolean initiatedFromBtDevice) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (isActiveCall(audioManager)) {
            appendLog(getBaseContext(), TAG, "There is active phone call, not going to say anything");
            return null;
        }

        if (initiatedFromBtDevice) {
            appendLog(getBaseContext(), TAG, "Initiated from BT device, ommitin the rest of the settings");
            return voiceSettingId;
        }

        boolean isHeadsetConnected = audioManager.isWiredHeadsetOn();
        boolean isBluetoothConnected = Utils.isBluetoothHeadsetEnabledConnected(getBaseContext());

        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());
        Map<Long, Long> enabledVoiceDevices = voiceSettingParametersDbHelper.getLongParam(
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId());
        appendLog(getBaseContext(), TAG, "isAnySettingValidToTellWeather enabledVoiceDevices: " + enabledVoiceDevices);
        if (enabledVoiceDevices == null) {
            appendLog(getBaseContext(), TAG, "Bluetooth or wired headset is not enabled or connected");
            return null;
        }
        appendLog(getBaseContext(), TAG, "isAnySettingValidToTellWeather voiceSettingId: " + voiceSettingId);
        if (voiceSettingId != null) {
            if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices.get(voiceSettingId), 2)) {
                appendLog(getBaseContext(), TAG, "speaker_enabled");
                return !(isBluetoothConnected || isHeadsetConnected)? voiceSettingId : null;
            }
            if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices.get(voiceSettingId), 1)) {
                appendLog(getBaseContext(), TAG, "wired_enabled");
                return isHeadsetConnected ? voiceSettingId : null;
            }
            if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices.get(voiceSettingId), 0)) {
                appendLog(getBaseContext(), TAG, "bt_enabled");
                return (isBluetoothConnected && isBtDeviceEnabled(voiceSettingId)) ? voiceSettingId : null;
            }
        } else {
            for (Long currentVoiceSettingId : enabledVoiceDevices.keySet()) {
                Long enabledVoiceDevice = enabledVoiceDevices.get(currentVoiceSettingId);
                appendLog(getBaseContext(), TAG, "isAnySettingValidToTellWeather enabledVoiceDevice: " + enabledVoiceDevice);
                if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevice, 2)) {
                    appendLog(getBaseContext(), TAG, "speaker_enabled");
                    return !(isBluetoothConnected || isHeadsetConnected)? currentVoiceSettingId : null;
                }
                if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevice, 1)) {
                    appendLog(getBaseContext(), TAG, "wired_enabled");
                    return isHeadsetConnected ? currentVoiceSettingId : null;
                }
                if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevice, 0)) {
                    appendLog(getBaseContext(), TAG, "bt_enabled");
                    return (isBluetoothConnected && isBtDeviceEnabled(currentVoiceSettingId)) ? currentVoiceSettingId : null;
                }
            }
        }
        return null;
    }

    private boolean isBtDeviceEnabled(Long voiceSettingId) {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());
        Boolean allBtDevices = voiceSettingParametersDbHelper.getBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES.getVoiceSettingParamTypeId());
        appendLog(getBaseContext(), TAG, "isBtDeviceEnabled:allBtDevices:", allBtDevices);
        if ((allBtDevices != null) && allBtDevices) {
            return true;
        }
        String enabledBtDevices = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES.getVoiceSettingParamTypeId());
        appendLog(getBaseContext(), TAG, "isBtDeviceEnabled:enabledBtDevices:", enabledBtDevices);
        Set<String> bluetoothDevices = Utils.getAllConnectedBtDevices(getBaseContext());
        if (bluetoothDevices.isEmpty()) {
            appendLog(getBaseContext(), TAG, "isBtDeviceEnabled:enabledBtDevices is empty");
            return false;
        }
        appendLog(getBaseContext(), TAG, "isBtDeviceEnabled:bluetoothDevices:" + bluetoothDevices);
        for (String bluetoothDevice: bluetoothDevices) {
            appendLog(getBaseContext(), TAG, "isBtDeviceEnabled:bluetoothDevice.getName():", bluetoothDevice);
            if (enabledBtDevices.contains(bluetoothDevice)) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveCall(AudioManager audioManager) {
        return audioManager.getMode() == AudioManager.MODE_IN_CALL;
    }

    private int getGreetingId() {
        int hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if ((hours >= 3) && (hours < 10)) {
            return R.string.tts_say_greeting_morning;
        } else if ((hours >= 10) && (hours < 18)) {
            return R.string.tts_say_greeting_day;
        } else if (hours >= 18) {
            return R.string.tts_say_greeting_evening;
        } else {
            return R.string.tts_say_greeting_evening;
        }
    }

    private String getCustomGreeting(VoiceSettingParametersDbHelper voiceSettingParametersDbHelper, Long voiceSettingId) {
        int hours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if ((hours >= 3) && (hours < 10)) {
            return voiceSettingParametersDbHelper.getStringParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_MORNING.getVoiceSettingParamTypeId());
        } else if ((hours >= 10) && (hours < 18)) {
            return voiceSettingParametersDbHelper.getStringParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_DAY.getVoiceSettingParamTypeId());
        } else if (hours >= 18) {
            return voiceSettingParametersDbHelper.getStringParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_EVENING.getVoiceSettingParamTypeId());
        } else {
            return voiceSettingParametersDbHelper.getStringParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_DAY.getVoiceSettingParamTypeId());
        }
    }

    private void recreateTts(final LinkedList<String> what) {
        TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                appendLog(getBaseContext(), TAG, "TextToSpeech initialized with status: " + status);
                if ((tts != null) && (status == TextToSpeech.SUCCESS) && (what != null)) {
                    say(what);
                }
            }
        };
        tts = new TextToSpeech(getBaseContext(), onInitListener);
    }
}
