package org.thosp.yourlocalweather.service;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.widget.CheckBox;

import org.thosp.yourlocalweather.AddVoiceSettingActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.ForecastUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.TemperatureUtil;
import org.thosp.yourlocalweather.utils.TimeUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;
import org.thosp.yourlocalweather.utils.WindWithUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherByVoiceService extends Service {

    private static final String TAG = "WeatherByVoiceService";

    public static final int START_VOICE_WEATHER_UPDATED = 1;
    public static final int START_VOICE_WEATHER_ALL = 2;

    private TextToSpeech tts;
    private static String TTS_DELAY_BETWEEN_ITEM = "...---...";
    private static String TTS_END = "_________";
    private static long TTS_DELAY_BETWEEN_ITEM_IN_MS = 200;

    private static final Queue<WeatherByVoiceRequestDataHolder> weatherByVoiceMessages = new LinkedList<>();
    final Messenger messenger = new Messenger(new WeatherByVoiceMessageHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        appendLog(getBaseContext(), TAG, "onUnbind all services");
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        appendLog(getBaseContext(), TAG, "onStartCommand:", intent);

        if (intent == null) {
            return ret;
        }
        switch (intent.getAction()) {
            case "android.intent.action.SAY_WEATHER": sayWeatherByTime(intent); return ret;
            default: return ret;
        }
    }

    private void sayWeatherByTime(Intent intent) {
        Long voiceSettingId = intent.getLongExtra("voiceSettingId", Long.MAX_VALUE);
        appendLog(getBaseContext(), TAG, "sayWeatherByTime:" + voiceSettingId);

        if (voiceSettingId == Long.MAX_VALUE) {
            return;
        }
        TimeUtils.setupAlarmForVoice(getBaseContext());
        sayForLocation(voiceSettingId, false);
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
                sayCurrentWeather(
                        weatherRecord.getWeather(),
                        location,
                        weatherRecord.getLastUpdatedTime(),
                        voiceSettingId,
                        initiatedFromBtDEvice);
            }
        }
        return;
    }

    public void startAllLocationsVoiceCommand(long incomingMessageTimestamp,
                                              Long voiceSettingId) {
        appendLog(getBaseContext(), TAG, "startVoiceCommand");

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size before peek = ", weatherByVoiceMessages);

        WeatherByVoiceRequestDataHolder updateRequest = weatherByVoiceMessages.peek();

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size after peek = ", weatherByVoiceMessages);

        if ((updateRequest == null) || (updateRequest.getTimestamp() < incomingMessageTimestamp)) {
            if (updateRequest != null) {
                appendLog(getBaseContext(),
                        TAG,
                        "updateRequest is older than current");
            } else {
                appendLog(getBaseContext(),
                        TAG,
                        "updateRequest is null");
            }
            appendLog(getBaseContext(),
                    TAG,
                    "weatherByVoiceMessages.size when request is old or null = ", weatherByVoiceMessages);
            return;
        }
        sayForLocation(voiceSettingId, true);
    }


    public void startVoiceCommand(long incommingMessageTimestamp) {
        appendLog(getBaseContext(), TAG, "startVoiceCommand");

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size before peek = ", weatherByVoiceMessages);

        WeatherByVoiceRequestDataHolder updateRequest = weatherByVoiceMessages.peek();

        appendLog(getBaseContext(),
                TAG,
                "weatherByVoiceMessages.size after peek = ", weatherByVoiceMessages);

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
            appendLog(getBaseContext(),
                    TAG,
                    "weatherByVoiceMessages.size when request is old or null = ", weatherByVoiceMessages);
            return;
        }
        sayCurrentWeather(
                updateRequest.getWeather(),
                updateRequest.getLocation(),
                updateRequest.getTimeNow(),
                null,
                false);
    }

    private void sayCurrentWeather(Weather weather,
                                   Location currentLocation,
                                   long now,
                                   Long voiceSettingId,
                                   boolean initiatedFromBtDevice) {
        weatherByVoiceMessages.clear();
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
            StringBuilder weatherDescriptionToSay = new StringBuilder();
            weatherDescriptionToSay.append(" ");
            weatherDescriptionToSay.append(Utils.getWeatherDescription(getBaseContext(), currentLocation.getLocaleAbbrev(), weather));
            weatherDescriptionToSay.append(" ");
            textToSay.add(weatherDescriptionToSay.toString());
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 6)) {
            textToSay.add(TTS_DELAY_BETWEEN_ITEM);
            StringBuilder temperatureToSay = new StringBuilder();
            if (TimeUtils.isCurrentSettingIndex(partsToSay, 7)) {
                temperatureToSay.append(String.format(voiceSettingParametersDbHelper.getStringParam(
                        voiceSettingIdFromSettings,
                        VoiceSettingParamType.VOICE_SETTING_TEMPERATURE_CUSTOM.getVoiceSettingParamTypeId()),
                        TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weather.getTemperature(), currentLocation.getLocale())));
            } else {
                temperatureToSay.append(getString(R.string.tty_say_temperature,
                        TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weather.getTemperature(), currentLocation.getLocale())));
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
            textToSay.add(TTS_DELAY_BETWEEN_ITEM);
            StringBuilder forecastToSay = new StringBuilder();
            int currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            forecastToSay.append(R.string.tty_say_weather_forecast);
            forecastToSay.append(" ");
            if (currentDayOfYear == weatherForecastForVoice.dayOfYear) {
                forecastToSay.append(R.string.tty_say_weather_forecast_today);
                forecastToSay.append(" ");
            } else if ((currentDayOfYear + 1) == weatherForecastForVoice.dayOfYear) {
                forecastToSay.append(R.string.tty_say_weather_forecast_tomorrow);
                forecastToSay.append(" ");
            }

            String forecastCommonWeatherForecastToSay = sayCommonWeatherForecastParts(weatherForecastForVoice, currentLocation);
            if (forecastCommonWeatherForecastToSay != null) {
                forecastToSay.append(forecastCommonWeatherForecastToSay);
            }

            boolean commonPartsAreComplete = forecastCommonWeatherForecastToSay != null;
            boolean nightWeather = weatherForecastForVoice.nightWeatherIds != null;
            boolean morningWeather = weatherForecastForVoice.morningWeatherIds != null;
            boolean afternoonWeather = weatherForecastForVoice.afternoonWeatherIds != null;
            boolean eveningWeather = weatherForecastForVoice.eveningWeatherIds != null;

            if (nightWeather && !commonPartsAreComplete) {
                forecastToSay.append(R.string.tty_say_weather_forecast_night);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.nightWeatherIds.mainWeatherId,
                        weatherForecastForVoice.nightWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
                if (weatherForecastForVoice.nightWeatherIds.warningWeatherId != null) {
                    forecastToSay.append(R.string.tty_say_weather_forecast_rarely);
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.nightWeatherIds.warningWeatherId,
                            weatherForecastForVoice.nightWeatherIds.warningWeatherDescriptionsFromOwm,
                            currentLocation.getLocaleAbbrev(),
                            getBaseContext()));
                }
                forecastToSay.append(sayRainSnow(weatherForecastForVoice.nightWeatherMaxMin.maxRain, weatherForecastForVoice.nightWeatherMaxMin.maxSnow, currentLocation));
            }
            if (morningWeather && !commonPartsAreComplete) {
                forecastToSay.append(R.string.tty_say_weather_forecast_morning);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                        weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
                if (weatherForecastForVoice.morningWeatherIds.warningWeatherId != null) {
                    forecastToSay.append(R.string.tty_say_weather_forecast_rarely);
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.warningWeatherId,
                            weatherForecastForVoice.morningWeatherIds.warningWeatherDescriptionsFromOwm,
                            currentLocation.getLocaleAbbrev(),
                            getBaseContext()));
                }
                forecastToSay.append(sayRainSnow(weatherForecastForVoice.morningWeatherMaxMin.maxRain, weatherForecastForVoice.morningWeatherMaxMin.maxSnow, currentLocation));
            }
            if (afternoonWeather && !commonPartsAreComplete) {
                forecastToSay.append(R.string.tty_say_weather_forecast_afternoon);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.afternoonWeatherIds.mainWeatherId,
                        weatherForecastForVoice.afternoonWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
                if (weatherForecastForVoice.afternoonWeatherIds.warningWeatherId != null) {
                    forecastToSay.append(R.string.tty_say_weather_forecast_rarely);
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.afternoonWeatherIds.warningWeatherId,
                            weatherForecastForVoice.afternoonWeatherIds.warningWeatherDescriptionsFromOwm,
                            currentLocation.getLocaleAbbrev(),
                            getBaseContext()));
                }
                forecastToSay.append(sayRainSnow(weatherForecastForVoice.afternoonWeatherMaxMin.maxRain, weatherForecastForVoice.afternoonWeatherMaxMin.maxSnow, currentLocation));
            }
            if (eveningWeather && !commonPartsAreComplete) {
                forecastToSay.append(R.string.tty_say_weather_forecast_evening);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        weatherForecastForVoice.eveningWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
                if (weatherForecastForVoice.eveningWeatherIds.warningWeatherId != null) {
                    forecastToSay.append(R.string.tty_say_weather_forecast_rarely);
                    forecastToSay.append(" ");
                    forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.warningWeatherId,
                            weatherForecastForVoice.eveningWeatherIds.warningWeatherDescriptionsFromOwm,
                            currentLocation.getLocaleAbbrev(),
                            getBaseContext()));
                }
                forecastToSay.append(sayRainSnow(weatherForecastForVoice.eveningWeatherMaxMin.maxRain, weatherForecastForVoice.eveningWeatherMaxMin.maxSnow, currentLocation));
            }
            forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);

            if ( Math.round(weatherForecastForVoice.minTempForDay) == Math.round(weatherForecastForVoice.maxTempForDay)) {
                if (weatherForecastForVoice.minTempForDay >= 0) {
                    forecastToSay.append(getString(R.string.tty_say_temp_max,
                            TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, currentLocation.getLocale()),
                            AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), currentLocation.getLocale())));
                } else {
                    forecastToSay.append(getString(R.string.tty_say_temp_min,
                            TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, currentLocation.getLocale()),
                            AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), currentLocation.getLocale())));
                }
            } else {
                forecastToSay.append(getString(R.string.tty_say_temp_min_max,
                        TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.minTempForDay, currentLocation.getLocale()),
                        AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.minTempTime), currentLocation.getLocale()),
                        TemperatureUtil.getMeasuredTemperatureWithUnit(getBaseContext(), weatherForecastForVoice.maxTempForDay, currentLocation.getLocale()),
                        AppPreference.getLocalizedTime(getBaseContext(), new Date(weatherForecastForVoice.maxTempTime), currentLocation.getLocale())));
            }
            forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);
            WindWithUnit windWithUnit = AppPreference.getWindWithUnit(getBaseContext(),
                    (float)weatherForecastForVoice.maxWindForDay,
                    (float)weatherForecastForVoice.windDegreeForDay,
                    currentLocation.getLocale());
            forecastToSay.append(getString(R.string.tty_say_max_wind,
                    windWithUnit.getWindSpeed(0),
                    windWithUnit.getWindUnit(),
                    windWithUnit.getWindDirectionByVoice()));
            textToSay.add(forecastToSay.toString());
        }
        textToSay.add(TTS_END);
        sayWeather(textToSay);
    }

    private String sayRainSnow(double rain, double snow, Location location) {
        StringBuilder forecastToSay = new StringBuilder();
        if (rain > MIN_RAIN_SNOW_MM) {
            forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);
            forecastToSay.append(getString(R.string.tty_say_max_rain,
                    AppPreference.getFormatedRainOrSnow(getBaseContext(), rain, location.getLocale())));
        }
        if (snow > MIN_RAIN_SNOW_MM) {
            forecastToSay.append(TTS_DELAY_BETWEEN_ITEM);
            forecastToSay.append(getString(R.string.tty_say_max_rain,
                    AppPreference.getFormatedRainOrSnow(getBaseContext(), snow, location.getLocale())));
        }
        return forecastToSay.toString();
    }

    private double MIN_RAIN_SNOW_MM = 0.5;

    private String sayCommonWeatherForecastParts(ForecastUtil.WeatherForecastForVoice weatherForecastForVoice,
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
            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:nightWeatherIds:morningWeatherIds:" + weatherForecastForVoice.nightWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.morningWeatherIds.mainWeatherId);
            nightMorningAreSame = weatherForecastForVoice.nightWeatherIds.mainWeatherId == weatherForecastForVoice.morningWeatherIds.mainWeatherId;
            nightMorningWarningAreSame = weatherForecastForVoice.nightWeatherIds.warningWeatherId == weatherForecastForVoice.morningWeatherIds.warningWeatherId;

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
            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:morningWeatherIds:afternoonWeatherIds:" + weatherForecastForVoice.morningWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.afternoonWeatherIds.mainWeatherId);
            morningAfternoonAreSame = weatherForecastForVoice.morningWeatherIds.mainWeatherId == weatherForecastForVoice.afternoonWeatherIds.mainWeatherId;
            morningAfternoonWarningAreSame = weatherForecastForVoice.morningWeatherIds.warningWeatherId == weatherForecastForVoice.afternoonWeatherIds.warningWeatherId;

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
            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:morningWeatherIds:eveningWeatherIds:" + weatherForecastForVoice.morningWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.eveningWeatherIds.mainWeatherId);
            morningEveningAreSame = weatherForecastForVoice.morningWeatherIds.mainWeatherId == weatherForecastForVoice.eveningWeatherIds.mainWeatherId;
            morningEveningWarningAreSame = weatherForecastForVoice.morningWeatherIds.warningWeatherId == weatherForecastForVoice.eveningWeatherIds.warningWeatherId;

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
            appendLog(getBaseContext(), TAG,"sayCommonWeatherForecastParts:afternoonWeatherIds:eveningWeatherIds:" + weatherForecastForVoice.afternoonWeatherIds.mainWeatherId + ":" + weatherForecastForVoice.eveningWeatherIds.mainWeatherId);
            afternoonEveningAreSame = weatherForecastForVoice.afternoonWeatherIds.mainWeatherId == weatherForecastForVoice.eveningWeatherIds.mainWeatherId;
            afternoonEveningWarningAreSame = weatherForecastForVoice.afternoonWeatherIds.warningWeatherId == weatherForecastForVoice.eveningWeatherIds.warningWeatherId;

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

        if (nightMorningAreSame && morningAfternoonAreSame && morningEveningAreSame && afternoonEveningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm,
                    currentLocation.getLocaleAbbrev(),
                    getBaseContext()));
            if (nightMorningWarningAreSame && morningAfternoonWarningAreSame && morningEveningWarningAreSame && afternoonEveningWarningAreSame) {
                forecastToSay.append(R.string.tty_say_weather_forecast_rarely);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.warningWeatherId,
                        weatherForecastForVoice.morningWeatherIds.warningWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
            }
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, currentLocation));
            return forecastToSay.toString();
        }
        if (nightMorningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            if (morningAfternoonAreSame) {
                forecastToSay.append(R.string.tty_say_weather_forecast_night);
                forecastToSay.append(", ");
                forecastToSay.append(R.string.tty_say_weather_forecast_morning);
                forecastToSay.append(" ");
                forecastToSay.append(R.string.tty_say_weather_forecast_and);
                forecastToSay.append(" ");
                forecastToSay.append(R.string.tty_say_weather_forecast_afternoon);
                forecastToSay.append(" ");
            } else {
                forecastToSay.append(R.string.tty_say_weather_forecast_night);
                forecastToSay.append(" ");
                forecastToSay.append(R.string.tty_say_weather_forecast_and);
                forecastToSay.append(" ");
                forecastToSay.append(R.string.tty_say_weather_forecast_morning);
                forecastToSay.append(" ");
            }
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm,
                    currentLocation.getLocaleAbbrev(),
                    getBaseContext()));
        }
        if (morningAfternoonAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            if (!nightMorningAreSame) {
                forecastToSay.append(R.string.tty_say_weather_forecast_morning);
                forecastToSay.append(" ");
                forecastToSay.append(R.string.tty_say_weather_forecast_and);
                forecastToSay.append(" ");
                forecastToSay.append(R.string.tty_say_weather_forecast_afternoon);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                        weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
            }
            if (eveningWeather) {
                forecastToSay.append(R.string.tty_say_weather_forecast_evening);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        weatherForecastForVoice.eveningWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
            }
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, currentLocation));
            return forecastToSay.toString();
        } else if (morningEveningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            forecastToSay.append(R.string.tty_say_weather_forecast_morning);
            forecastToSay.append(" ");
            forecastToSay.append(R.string.tty_say_weather_forecast_and);
            forecastToSay.append(" ");
            forecastToSay.append(R.string.tty_say_weather_forecast_evening);
            forecastToSay.append(" ");
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm,
                    currentLocation.getLocaleAbbrev(),
                    getBaseContext()));
            if (afternoonWeather) {
                forecastToSay.append(R.string.tty_say_weather_forecast_afternoon);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        weatherForecastForVoice.eveningWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
            }
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, currentLocation));
            return forecastToSay.toString();
        } else if (afternoonEveningAreSame) {
            StringBuilder forecastToSay = null;
            forecastToSay = new StringBuilder();
            if (morningWeather) {
                forecastToSay.append(R.string.tty_say_weather_forecast_morning);
                forecastToSay.append(" ");
                forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.eveningWeatherIds.mainWeatherId,
                        weatherForecastForVoice.eveningWeatherIds.mainWeatherDescriptionsFromOwm,
                        currentLocation.getLocaleAbbrev(),
                        getBaseContext()));
            }
            forecastToSay.append(R.string.tty_say_weather_forecast_afternoon);
            forecastToSay.append(" ");
            forecastToSay.append(R.string.tty_say_weather_forecast_and);
            forecastToSay.append(" ");
            forecastToSay.append(R.string.tty_say_weather_forecast_evening);
            forecastToSay.append(" ");
            forecastToSay.append(Utils.getWeatherDescription(weatherForecastForVoice.morningWeatherIds.mainWeatherId,
                    weatherForecastForVoice.morningWeatherIds.mainWeatherDescriptionsFromOwm,
                    currentLocation.getLocaleAbbrev(),
                    getBaseContext()));
            forecastToSay.append(sayRainSnow(maxRain, maxSnow, currentLocation));
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
        TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                appendLog(getBaseContext(), TAG, "TextToSpeech initialized with status: " + status);
                if (status == TextToSpeech.SUCCESS) {
                    say(what);
                }
            }
        };
        tts = new TextToSpeech(getBaseContext(), onInitListener);
    }

    private Locale getLocaleForVoice() {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getBaseContext());
        String localeForVoiceId = voiceSettingParametersDbHelper.getGeneralStringParam(VoiceSettingParamType.VOICE_SETTING_VOICE_LANG.getVoiceSettingParamTypeId());
        if ((localeForVoiceId == null) || ("Default".equals(localeForVoiceId))) {
            return new Locale(PreferenceUtil.getLanguage(this));
        } else {
            return new Locale(localeForVoiceId);
        }
    }

    private boolean say(LinkedList<String> texts) {
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
            return true;
        } else {
            appendLog(getBaseContext(), TAG, "Locale " + locale.toString() + "is not available in TTS");
            return false;
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

    private class WeatherByVoiceMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            WeatherByVoiceRequestDataHolder weatherRequestDataHolder = (WeatherByVoiceRequestDataHolder) msg.obj;
            appendLog(getBaseContext(), TAG, "handleMessage:", msg.what, ":", weatherRequestDataHolder);
            appendLog(getBaseContext(),
                    TAG,
                    "weatherByVoiceMessages.size when adding new message = ", weatherByVoiceMessages);
            switch (msg.what) {
                case START_VOICE_WEATHER_UPDATED:
                    if (!weatherByVoiceMessages.contains(weatherRequestDataHolder)) {
                        weatherByVoiceMessages.add(weatherRequestDataHolder);
                    }
                    startVoiceCommand(weatherRequestDataHolder.getTimestamp());
                    break;
                case START_VOICE_WEATHER_ALL:
                    if (!weatherByVoiceMessages.contains(weatherRequestDataHolder)) {
                        weatherByVoiceMessages.add(weatherRequestDataHolder);
                    }
                    startAllLocationsVoiceCommand(weatherRequestDataHolder.getTimestamp(), weatherRequestDataHolder.getVoiceSettingsId());
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
