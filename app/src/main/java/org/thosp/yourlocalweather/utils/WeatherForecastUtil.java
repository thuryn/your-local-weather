package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.WeatherForecastResultHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.Utils.getWeatherForecastUrl;

public class WeatherForecastUtil {

    private static final String TAG = "WeatherForecastUtil";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void getWeather(final Context context,
                                  final WeatherForecastResultHandler weatherForecastResultHandler) {
        SharedPreferences pref = context.getSharedPreferences(Constants.APP_SETTINGS_NAME, 0);
        String latitude = pref.getString(Constants.APP_SETTINGS_LATITUDE, "51.51");
        String longitude = pref.getString(Constants.APP_SETTINGS_LONGITUDE, "-0.13");
        String locale = LanguageUtil.getLanguageName(PreferenceUtil.getLanguage(context));
        try {
            final URL url = getWeatherForecastUrl(Constants.WEATHER_FORECAST_ENDPOINT, latitude, longitude, "metric", locale);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    client.get(url.toString(), null, new JsonHttpResponseHandler() {

                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject weatherForecastResponse) {
                            parseWeatherForecast(weatherForecastResponse, weatherForecastResultHandler);
                            AppPreference.saveLastForecastUpdateTimeMillis(context);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            super.onFailure(statusCode, headers, throwable, errorResponse);
                            appendLog(context, TAG, "onFailure:" + statusCode);
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    });
                }
            };
            mainHandler.post(myRunnable);
        } catch (MalformedURLException mue) {
            appendLog(context, TAG, "MalformedURLException:" + mue);
            return;
        }
    }

    private static void parseWeatherForecast(JSONObject weatherForecastResponse,
                                             WeatherForecastResultHandler weatherForecastResultHandler) {
        try {
            List<DetailedWeatherForecast> mWeatherForecastList = new ArrayList<>();
            JSONArray weatherForecastList = weatherForecastResponse.getJSONArray("list");
            for (int weatherForecastCounter = 0; weatherForecastCounter < weatherForecastList.length(); weatherForecastCounter++) {
                DetailedWeatherForecast weatherForecast = new DetailedWeatherForecast();
                JSONObject weatherForecastCase = weatherForecastList.getJSONObject(weatherForecastCounter);
                weatherForecast.setDateTime(weatherForecastCase.getLong("dt"));
                JSONObject weatherForecastCaseMain = weatherForecastCase.getJSONObject("main");
                weatherForecast.setPressure(weatherForecastCaseMain.getDouble("pressure"));
                weatherForecast.setHumidity(weatherForecastCaseMain.getInt("humidity"));
                JSONObject weatherForecastCaseWind = weatherForecastCase.getJSONObject("wind");
                weatherForecast.setWindSpeed(weatherForecastCaseWind.getDouble("speed"));
                weatherForecast.setWindDegree(weatherForecastCaseWind.getDouble("deg"));
                JSONObject weatherForecastCaseClouds = weatherForecastCase.getJSONObject("clouds");
                weatherForecast.setCloudiness(weatherForecastCaseClouds.getInt("all"));

                if (weatherForecastCase.has("rain")) {
                    JSONObject rain = weatherForecastCase.getJSONObject("rain");
                    if (rain.has("3h")) {
                        weatherForecast.setRain(rain.getDouble("3h"));
                    }
                } else {
                    weatherForecast.setRain(0);
                }
                if (weatherForecastCase.has("snow")) {
                    JSONObject snow = weatherForecastCase.getJSONObject("snow");
                    if (snow.has("3h")) {
                        weatherForecast.setSnow(snow.getDouble("3h"));
                    }
                } else {
                    weatherForecast.setSnow(0);
                }
                weatherForecast.setTemperatureMin(weatherForecastCaseMain.getDouble("temp_min"));
                weatherForecast.setTemperatureMax(weatherForecastCaseMain.getDouble("temp_max"));
                weatherForecast.setTemperature(weatherForecastCaseMain.getDouble("temp"));
                JSONArray weatherConditionList = weatherForecastCase.getJSONArray("weather");
                for (int weatherConditionCounter = 0; weatherConditionCounter < weatherConditionList.length(); weatherConditionCounter++) {
                    JSONObject weatherCondition = weatherConditionList.getJSONObject(weatherConditionCounter);
                    weatherForecast.addWeatherCondition(weatherCondition.getString("icon"), weatherCondition.getString("description"));
                }

                mWeatherForecastList.add(weatherForecast);
            }
            weatherForecastResultHandler.processResources(mWeatherForecastList);
        } catch (JSONException e) {
            weatherForecastResultHandler.processError(e);
        }
    }
}
