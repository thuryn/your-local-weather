package org.thosp.yourlocalweather;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thosp.yourlocalweather.licence.LicenseNotValidException;
import org.thosp.yourlocalweather.licence.TooEarlyUpdateException;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Weather;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WeatherJSONParser {

    private static final String TAG = "WeatherJSONParser";

    private static final SimpleDateFormat inputDateTimes = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    public static JSONParseResult parseServerResult(String serverResult) throws JSONException,
                                                                                LicenseNotValidException,
                                                                                TooEarlyUpdateException {
        JSONObject serverResponse = new JSONObject(serverResult);
        String result = serverResponse.getString("result");
        switch (result) {
            case "TOO_EARLY_UPDATE": throw new TooEarlyUpdateException();
            case "OK": return new JSONParseResult(serverResponse.getString("token"), serverResponse.getString("owm"));
        }
        throw new LicenseNotValidException("Result is not OK. Result = " + result);
    }

    public static Weather getWeather(JSONObject weatherData, String locale) throws JSONException, ParseException {
        Weather weather = new Weather();
        if (weatherData.has("current")) {
            JSONObject current = weatherData.getJSONObject("current");
            if (current.has("weather_code")) {
                weather.setWeatherId(current.getInt("weather_code"));
            }
            if (current.has("temperature_2m")) {
                weather.setTemperature(Float.parseFloat(current.getString("temperature_2m")));
            }
            if (current.has("relative_humidity_2m")) {
                weather.setHumidity(current.getInt("relative_humidity_2m"));
            }
            if (current.has("pressure_msl")) { //or surface_pressure
                weather.setPressure(Float.parseFloat(current.getString("pressure_msl")));
            }
            if (current.has("wind_speed_10m")) {
                weather.setWindSpeed(Float.parseFloat(current.getString("wind_speed_10m")));
            }
            if (current.has("wind_direction_10m")) {
                weather.setWindDirection(Float.parseFloat(current.getString("wind_direction_10m")));
            }
            if (current.has("cloud_cover")) {
                weather.setClouds(current.getInt("cloud_cover"));
            }
        }

        if (weatherData.has("daily")) {
            JSONObject daily = weatherData.getJSONObject("daily");

            JSONArray sunrises = daily.getJSONArray("sunrise");
            String firstSunrise = sunrises.getString(0);
            weather.setSunrise(inputDateTimes.parse(firstSunrise).getTime()/1000);

            JSONArray sunsets = daily.getJSONArray("sunset");
            String firstSunset = sunsets.getString(0);
            weather.setSunset(inputDateTimes.parse(firstSunset).getTime()/1000);
        }

        if (weatherData.has("longitude")) {
            weather.setLon(Float.parseFloat(weatherData.getString("longitude")));
        }
        if (weatherData.has("latitude")) {
            weather.setLat(Float.parseFloat(weatherData.getString("latitude")));
        }

        return weather;
    }

    public static CompleteWeatherForecast getWeatherForecast(Context context, JSONObject weatherForecastResponse) throws JSONException, ParseException {
        CompleteWeatherForecast completeWeatherForecast = new CompleteWeatherForecast();

        JSONObject hourly = weatherForecastResponse.getJSONObject("hourly");
        JSONArray timeList = hourly.getJSONArray("time");
        JSONArray temperatureList = hourly.getJSONArray("temperature_2m");
        JSONArray humidityList = hourly.getJSONArray("relative_humidity_2m");
        JSONArray rainList = hourly.getJSONArray("rain");
        JSONArray snowfallList = hourly.getJSONArray("snowfall");
        JSONArray weatherList = hourly.getJSONArray("weather_code");
        JSONArray pressureList = hourly.getJSONArray("pressure_msl");
        JSONArray cloudList = hourly.getJSONArray("cloud_cover");
        JSONArray windSpeedList = hourly.getJSONArray("wind_speed_10m");
        JSONArray windDirectionList = hourly.getJSONArray("wind_direction_10m");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yyyy");

        for (int weatherForecastCounter = 0; weatherForecastCounter < timeList.length(); weatherForecastCounter++) {
            DetailedWeatherForecast weatherForecast = new DetailedWeatherForecast();

            Date forecastDateTime = inputDateTimes.parse(timeList.getString(weatherForecastCounter));
            weatherForecast.setDateTime(forecastDateTime.getTime()/1000);
            appendLog(context, TAG, "weatherForecast.time:", sdf.format(forecastDateTime));
            Double temperature = getDoubleValue(temperatureList, weatherForecastCounter);
            weatherForecast.setTemperatureMin(temperature);
            weatherForecast.setTemperatureMax(temperature);
            weatherForecast.setTemperature(temperature);

            weatherForecast.setPressure(getDoubleValue(pressureList, weatherForecastCounter));
            weatherForecast.setHumidity(getIntegerValue(humidityList, weatherForecastCounter));
            weatherForecast.setWindSpeed(getDoubleValue(windSpeedList, weatherForecastCounter) / 3.6);
            weatherForecast.setWindDegree(getDoubleValue(windDirectionList, weatherForecastCounter));
            weatherForecast.setCloudiness(getIntegerValue(cloudList, weatherForecastCounter));
            weatherForecast.setRain(getDoubleValue(rainList, weatherForecastCounter));
            weatherForecast.setSnow(10 * getDoubleValue(snowfallList, weatherForecastCounter));
            weatherForecast.setWeatherId(getIntegerValue(weatherList, weatherForecastCounter));

            completeWeatherForecast.addDetailedWeatherForecast(weatherForecast);
        }
        return completeWeatherForecast;
    }

    private static double getDoubleValue(JSONArray inputArray, int index) throws JSONException {
        String valueTxt = inputArray.getString(index);
        if ("null".equals(valueTxt)) {
            return 0;
        } else {
            return Double.valueOf(valueTxt);
        }
    }

    private static int getIntegerValue(JSONArray inputArray, int index) throws JSONException {
        String valueTxt = inputArray.getString(index);
        if ("null".equals(valueTxt)) {
            return 0;
        } else {
            return Integer.valueOf(valueTxt);
        }
    }
    public static class JSONParseResult {
        String token;
        String owmResponse;

        public JSONParseResult(String token, String owmResponse) {
            this.token = token;
            this.owmResponse = owmResponse;
        }

        public String getToken() {
            return token;
        }

        public String getOwmResponse() {
            return owmResponse;
        }
    }
}
