package org.thosp.yourlocalweather;

import org.thosp.yourlocalweather.model.CitySearch;
import org.thosp.yourlocalweather.model.Weather;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            weather.temperature.setTemp(Float.parseFloat(mainObj.getString("temp")));
        }
        if (mainObj.has("pressure")) {
            weather.currentCondition.setPressure(Float.parseFloat(mainObj.getString("pressure")));
        }
        if (mainObj.has("humidity")) {
            weather.currentCondition.setHumidity(mainObj.getInt("humidity"));
        }

        JSONObject windObj = weatherData.getJSONObject("wind");
        if (windObj.has("speed")) {
            weather.wind.setSpeed(Float.parseFloat(windObj.getString("speed")));
        }
        if (windObj.has("deg")) {
            weather.wind.setDirection(Float.parseFloat(windObj.getString("deg")));
        }

        JSONObject cloudsObj = weatherData.getJSONObject("clouds");
        if (cloudsObj.has("all")) {
            weather.cloud.setClouds(cloudsObj.getInt("all"));
        }

        JSONObject sysObj = weatherData.getJSONObject("sys");

        weather.sys.setSunrise(sysObj.getLong("sunrise"));
        weather.sys.setSunset(sysObj.getLong("sunset"));

        JSONObject coordObj = weatherData.getJSONObject("coord");
        if (coordObj.has("lon")) {
            weather.coord.setLon(Float.parseFloat(coordObj.getString("lon")));
        }
        if (coordObj.has("lat")) {
            weather.coord.setLat(Float.parseFloat(coordObj.getString("lat")));
        }

        return weather;
    }
}
