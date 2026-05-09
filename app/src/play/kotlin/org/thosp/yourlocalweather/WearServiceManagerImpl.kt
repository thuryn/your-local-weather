package org.thosp.yourlocalweather

import android.content.Context
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject
import org.thosp.yourlocalweather.model.CompleteWeatherForecast
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.Weather
import org.thosp.yourlocalweather.utils.AppPreference
import org.thosp.yourlocalweather.utils.ForecastUtil
import org.thosp.yourlocalweather.utils.LogToFile.appendLog
import org.thosp.yourlocalweather.utils.TemperatureUtil
import org.thosp.yourlocalweather.utils.Utils
import java.nio.charset.StandardCharsets
import java.util.Calendar

class WearServiceManagerImpl(private val context: Context) : WearServiceManager {

    private var TAG = "WearServiceManagerImpl"

    override fun sendUpdate2Wearables(
        location: Location,
        weather: Weather,
        completeWeatherForecast: CompleteWeatherForecast?
    ) {
        appendLog(
            context,
            TAG, "Sending messages to all wearables"
        )
        val messageClient = Wearable.getMessageClient(context)

        try {
            val weatherJson = JSONObject()
            weatherJson.put("locationName", Utils.getCityAndCountry(context, location))
            weatherJson.put("currentTemperature", weather.getTemperature().toDouble())
            weatherJson.put(
                "apparentTemperature", TemperatureUtil.getApparentTemperature(
                    weather.getTemperature().toDouble(),
                    weather.getHumidity(),
                    weather.getWindSpeed().toDouble(),
                    weather.getClouds(),
                    location.getLatitude(),
                    System.currentTimeMillis()
                ).toDouble()
            )
            weatherJson.put(
                "temperatureUnit",
                TemperatureUtil.getTemperatureUnit(
                    context,
                    AppPreference.getTemperatureUnitFromPreferences(context)
                )
            )
            weatherJson.put("humidity", weather.getHumidity())
            weatherJson.put("weatherDescription", Utils.getWeatherDescription(context, weather))
            weatherJson.put("sunrise", weather.getSunrise())
            weatherJson.put("sunset", weather.getSunset())
            weatherJson.put("windSpeed", weather.getWindSpeed().toDouble())
            weatherJson.put("windDegree", weather.getWindDirection().toDouble())
            weatherJson.put("pressure", weather.getPressure().toDouble())
            weatherJson.put("cloudiness", weather.getClouds())
            weatherJson.put("weatherId", weather.getWeatherId())

            val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

            val dailyForecast = JSONArray()
            val weatherList = ForecastUtil.createWeatherList(completeWeatherForecast, true)

            for (entry in weatherList.entries) {
                val dayOfYear: Int = entry.key!!
                val weatherMaxMinForDay = ForecastUtil.calculateWeatherMaxMinForDay(entry.value)
                val weatherIdsForDay = ForecastUtil.getWeatherIdForDay(
                    context,
                    entry.value,
                    weatherMaxMinForDay
                )

                val forecastJson = JSONObject()
                forecastJson.put("dayOfYear", dayOfYear)
                forecastJson.put("minTemp", weatherMaxMinForDay!!.minTemp)
                forecastJson.put("maxTemp", weatherMaxMinForDay.maxTemp)
                forecastJson.put("maxWind", weatherMaxMinForDay.maxWind)
                forecastJson.put("maxPressure", weatherMaxMinForDay.maxPressure)
                forecastJson.put("maxRain", weatherMaxMinForDay.maxRain)
                forecastJson.put("maxSnow", weatherMaxMinForDay.maxSnow)
                forecastJson.put("maxHumidity", weatherMaxMinForDay.maxHumidity)
                forecastJson.put("weatherId", weatherIdsForDay.mainWeatherId)
                forecastJson.put(
                    "weatherDescription",
                    weatherIdsForDay.mainWeatherDescriptionsFromOwm
                )
                dailyForecast.put(forecastJson)
                appendLog(
                    context,
                    TAG,
                    "Added forecast for wearos with day of year ",
                    (dayOfYear.toString() + ", " + dayOfYear % 365),
                    " and main weather id:",
                    weatherIdsForDay.mainWeatherId.toString()
                )
            }
            weatherJson.put("dailyForecast", dailyForecast)

            Wearable.getNodeClient(context).getConnectedNodes()
                .addOnSuccessListener(OnSuccessListener { nodes: MutableList<Node>? ->
                    for (node in nodes!!) {
                        appendLog(
                            context,
                            TAG,
                            "Sending messages to the wearable ",
                            node.getId()
                        )
                        messageClient.sendMessage(
                            node.getId(),
                            "/weather_update",
                            weatherJson.toString().toByteArray(StandardCharsets.UTF_8)
                        )
                            .addOnSuccessListener(OnSuccessListener { messageId: Int? ->
                                appendLog(
                                    context,
                                    TAG,
                                    "Message with ID " + messageId + " has been successfully sent!"
                                )
                            })
                            .addOnFailureListener(OnFailureListener { e: Exception? ->
                                appendLog(
                                    context,
                                    TAG,
                                    "Sending message to wearable id " + node.getId() + " failed",
                                    e
                                )
                            })
                    }
                })
            appendLog(
                context,
                TAG, "Sending messages to all wearables finished"
            )
        } catch (e: Exception) {
            appendLog(
                context,
                TAG,
                e
            )
        }
    }

}