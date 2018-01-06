package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.thosp.yourlocalweather.ConnectionDetector;
import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.R;
import org.thosp.yourlocalweather.WeatherJSONParser;
import org.thosp.yourlocalweather.WeatherRequest;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.Utils;
import org.json.JSONException;

import java.io.IOException;
import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NotificationService extends IntentService {

    private static final String TAG = "NotificationsService";

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectionDetector checkNetwork = new ConnectionDetector(this);
        if (!checkNetwork.isNetworkAvailableAndConnected()) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(Constants.APP_SETTINGS_NAME, 0);
        String latitude = preferences.getString(Constants.APP_SETTINGS_LATITUDE, "51.51");
        String longitude = preferences.getString(Constants.APP_SETTINGS_LONGITUDE, "-0.13");
        String locale = LanguageUtil.getLanguageName(PreferenceUtil.getLanguage(this));
        String units = AppPreference.getTemperatureUnit(this);

        Weather weather;
        try {
            String weatherRaw = new WeatherRequest().getItems(latitude, longitude, units, locale, getBaseContext());
            weather = WeatherJSONParser.getWeather(weatherRaw);

            AppPreference.saveLastUpdateTimeMillis(this);
            AppPreference.saveWeather(this, weather, "W");
            weatherNotification(weather);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error get weather", e);
        }
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, NotificationService.class);
    }

    public static void setNotificationServiceAlarm(Context context,
                                                   boolean isNotificationEnable) {
        Intent intent = NotificationService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent,
                                                               PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        String intervalPref = AppPreference.getInterval(context);
        long intervalMillis = Utils.intervalMillisForAlarm(intervalPref);
        if (isNotificationEnable) {

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                             SystemClock.elapsedRealtime() + intervalMillis,
                                             intervalMillis,
                                             pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void weatherNotification(Weather weather) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String temperatureScale = Utils.getTemperatureScale(this);
        String speedScale = Utils.getSpeedScale(this);

        String temperature = String.format(Locale.getDefault(), "%.1f",
                                           weather.temperature.getTemp());

        String wind = getString(R.string.wind_label, String.format(Locale.getDefault(),
                                                                   "%.1f",
                                                                   weather.wind.getSpeed()),
                                speedScale);
        String humidity = getString(R.string.humidity_label,
                                    String.valueOf(weather.currentCondition.getHumidity()),
                                    getString(R.string.percent_sign));
        String pressure = getString(R.string.pressure_label,
                                    String.format(Locale.getDefault(), "%.1f",
                                                  weather.currentCondition.getPressure()),
                                    getString(R.string.pressure_measurement));
        String cloudiness = getString(R.string.pressure_label,
                                      String.valueOf(weather.cloud.getClouds()),
                                      getString(R.string.percent_sign));

        StringBuilder notificationText = new StringBuilder(wind)
                .append("  ")
                .append(humidity)
                .append("  ")
                .append(pressure)
                .append("  ")
                .append(cloudiness);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(launchIntent)
                .setSmallIcon(R.drawable.small_icon)
                .setTicker(temperature
                                   + temperatureScale
                                   + "  "
                                   + weather.location.getCityName()
                                   + ", "
                                   + weather.location.getCountryCode())
                .setContentTitle(temperature
                                         + temperatureScale
                                         + "  "
                                         + Utils.getWeatherDescription(this, weather))
                .setContentText(notificationText)
                .setVibrate(isVibrateEnabled())
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, notification);
    }

    private long[] isVibrateEnabled() {
        if (!AppPreference.isVibrateEnabled(this)) {
            return null;
        }
        return new long[]{500, 500, 500};
    }
}
