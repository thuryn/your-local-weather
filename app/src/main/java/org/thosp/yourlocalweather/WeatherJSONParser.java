package org.thosp.yourlocalweather;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thosp.yourlocalweather.model.CompleteWeatherForecast;
import org.thosp.yourlocalweather.model.DetailedWeatherForecast;
import org.thosp.yourlocalweather.model.Weather;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastResultHandler;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherJSONParser {

    private static final String TAG = "WeatherJSONParser";

    public static Weather getWeather(String data) throws JSONException {
        Weather weather = new Weather();

        JSONObject weatherData = new JSONObject(data);
        JSONArray weatherArray = weatherData.getJSONArray("weather");

        for (int weatherCounter = 0; weatherCounter < weatherArray.length(); weatherCounter++) {
            JSONObject weatherObj = weatherArray.getJSONObject(weatherCounter);
            Integer weatherId = null;
            String weatherDescription = null;
            String weatherIconId = null;
            if (weatherObj.has("description")) {
                weatherDescription = weatherObj.getString("description");
            }
            if (weatherObj.has("icon")) {
                weatherIconId = weatherObj.getString("icon");
            }
            if (weatherObj.has("id")) {
                weatherId = weatherObj.getInt("id");
            }
            weather.addCurrentWeather(weatherId, weatherDescription, weatherIconId);
        }

        JSONObject mainObj = weatherData.getJSONObject("main");
        if (mainObj.has("temp")) {
            weather.setTemperature(Float.parseFloat(mainObj.getString("temp")));
        }
        if (mainObj.has("pressure")) {
            weather.setPressure(Float.parseFloat(mainObj.getString("pressure")));
        }
        if (mainObj.has("humidity")) {
            weather.setHumidity(mainObj.getInt("humidity"));
        }

        JSONObject windObj = weatherData.getJSONObject("wind");
        if (windObj.has("speed")) {
            weather.setWindSpeed(Float.parseFloat(windObj.getString("speed")));
        }
        if (windObj.has("deg")) {
            weather.setWindDirection(Float.parseFloat(windObj.getString("deg")));
        }

        JSONObject cloudsObj = weatherData.getJSONObject("clouds");
        if (cloudsObj.has("all")) {
            weather.setClouds(cloudsObj.getInt("all"));
        }

        JSONObject sysObj = weatherData.getJSONObject("sys");

        weather.setSunrise(sysObj.getLong("sunrise"));
        weather.setSunset(sysObj.getLong("sunset"));

        JSONObject coordObj = weatherData.getJSONObject("coord");
        if (coordObj.has("lon")) {
            weather.setLon(Float.parseFloat(coordObj.getString("lon")));
        }
        if (coordObj.has("lat")) {
            weather.setLat(Float.parseFloat(coordObj.getString("lat")));
        }

        return weather;
    }

    public static CompleteWeatherForecast getWeatherForecast(Context context,
                                            long locationId,
                                            String weatherForecastResponseTxt) throws JSONException {
        CompleteWeatherForecast completeWeatherForecast = new CompleteWeatherForecast();
        JSONObject weatherForecastResponse = new JSONObject(weatherForecastResponseTxt);
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
            completeWeatherForecast.addDetailedWeatherForecast(weatherForecast);
        }
        return completeWeatherForecast;
    }
}
