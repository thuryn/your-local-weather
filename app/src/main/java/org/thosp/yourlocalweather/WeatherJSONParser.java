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
import org.thosp.yourlocalweather.utils.OWMLanguages;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class WeatherJSONParser {

    private static final String TAG = "WeatherJSONParser";

    public static Weather getWeather(Context context, String data, String locale) throws JSONException {
        Weather weather = new Weather();

        JSONObject weatherData = new JSONObject(data);
        JSONArray weatherArray = weatherData.getJSONArray("weather");

        for (int weatherCounter = 0; weatherCounter < weatherArray.length(); weatherCounter++) {
            JSONObject weatherObj = weatherArray.getJSONObject(weatherCounter);
            Integer weatherId = null;
            String weatherDescription = null;
            String weatherIconId = null;
            if (weatherObj.has("icon")) {
                weatherIconId = weatherObj.getString("icon");
            }
            if (weatherObj.has("id")) {
                weatherId = 200; //weatherObj.getInt("id");
            }
            if (weatherObj.has("description")) {
                if (OWMLanguages.isLanguageSupportedByOWM(locale)) {
                    weatherDescription = weatherObj.getString("description");
                } else {
                    weatherDescription = context.getString(getWeatherDescriptionResourceId(weatherId));
                }
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
                weatherForecast.addWeatherCondition(weatherCondition.getInt("id"),
                                                    weatherCondition.getString("icon"),
                                                    weatherCondition.getString("description"));
            }
            completeWeatherForecast.addDetailedWeatherForecast(weatherForecast);
        }
        return completeWeatherForecast;
    }

    private static int getWeatherDescriptionResourceId(int weatherId) {
        switch (weatherId) {
            case 200: return R.string.weather_condition_description_200;
            case 201: return R.string.weather_condition_description_201;
            case 202: return R.string.weather_condition_description_202;
            case 210: return R.string.weather_condition_description_210;
            case 211: return R.string.weather_condition_description_211;
            case 212: return R.string.weather_condition_description_212;
            case 221: return R.string.weather_condition_description_221;
            case 230: return R.string.weather_condition_description_230;
            case 231: return R.string.weather_condition_description_231;
            case 232: return R.string.weather_condition_description_232;
            case 300: return R.string.weather_condition_description_300;
            case 301: return R.string.weather_condition_description_301;
            case 302: return R.string.weather_condition_description_302;
            case 310: return R.string.weather_condition_description_310;
            case 311: return R.string.weather_condition_description_311;
            case 312: return R.string.weather_condition_description_312;
            case 313: return R.string.weather_condition_description_313;
            case 314: return R.string.weather_condition_description_314;
            case 321: return R.string.weather_condition_description_321;
            case 500: return R.string.weather_condition_description_500;
            case 501: return R.string.weather_condition_description_501;
            case 502: return R.string.weather_condition_description_502;
            case 503: return R.string.weather_condition_description_503;
            case 504: return R.string.weather_condition_description_504;
            case 511: return R.string.weather_condition_description_511;
            case 520: return R.string.weather_condition_description_520;
            case 521: return R.string.weather_condition_description_521;
            case 522: return R.string.weather_condition_description_522;
            case 531: return R.string.weather_condition_description_531;
            case 600: return R.string.weather_condition_description_600;
            case 601: return R.string.weather_condition_description_601;
            case 602: return R.string.weather_condition_description_602;
            case 611: return R.string.weather_condition_description_611;
            case 612: return R.string.weather_condition_description_612;
            case 615: return R.string.weather_condition_description_615;
            case 616: return R.string.weather_condition_description_616;
            case 620: return R.string.weather_condition_description_620;
            case 621: return R.string.weather_condition_description_621;
            case 622: return R.string.weather_condition_description_622;
            case 701: return R.string.weather_condition_description_701;
            case 711: return R.string.weather_condition_description_711;
            case 721: return R.string.weather_condition_description_721;
            case 731: return R.string.weather_condition_description_731;
            case 741: return R.string.weather_condition_description_741;
            case 751: return R.string.weather_condition_description_751;
            case 761: return R.string.weather_condition_description_761;
            case 762: return R.string.weather_condition_description_762;
            case 771: return R.string.weather_condition_description_771;
            case 781: return R.string.weather_condition_description_781;
            case 800: return R.string.weather_condition_description_800;
            case 801: return R.string.weather_condition_description_801;
            case 802: return R.string.weather_condition_description_802;
            case 803: return R.string.weather_condition_description_803;
            case 804: return R.string.weather_condition_description_804;
            default: return R.string.weather_condition_description_none;
        }
    }
}
