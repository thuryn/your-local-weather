package org.thosp.yourlocalweather_wearos.tile

import android.content.Context
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import androidx.wear.protolayout.material3.MaterialScope
import kotlin.math.roundToInt

private const val RESOURCES_VERSION = "0"

data class DailyForecastTile(
    val dayOfYear: Int,
    val minTemp: Int,
    val maxTemp: Int
)

class MainTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        Futures.immediateFuture(tile(requestParams, this))

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(resources(requestParams))
}

private fun resources(requestParams: ResourcesRequest): Resources {
    return Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): TileBuilders.Tile {
    val prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
    val weatherDataJson = prefs.getString("weather_data_json", null)

    var locationName = "--"
    var currentTemp = 0
    var apparentTemp = 0
    var tempUnit = "°C"
    var weatherDescription = "--"
    var dailyForecasts = emptyList<DailyForecastTile>()

    if (weatherDataJson != null) {
        try {
            val json = JSONObject(weatherDataJson)
            locationName = json.optString("locationName", "--")
            currentTemp = json.optDouble("currentTemperature", 0.0).roundToInt()
            apparentTemp = json.optDouble("apparentTemperature", 0.0).roundToInt()
            tempUnit = json.optString("temperatureUnit", "°C")
            weatherDescription = json.optString("weatherDescription", "--")

            val dailyForecastJson = json.optJSONArray("dailyForecast")
            if (dailyForecastJson != null) {
                val forecasts = mutableListOf<DailyForecastTile>()
                for (i in 0 until dailyForecastJson.length()) {
                    val forecastJson = dailyForecastJson.getJSONObject(i)
                    forecasts.add(
                        DailyForecastTile(
                            dayOfYear = forecastJson.getInt("dayOfYear"),
                            minTemp = forecastJson.getDouble("minTemp").roundToInt(),
                            maxTemp = forecastJson.getDouble("maxTemp").roundToInt()
                        )
                    )
                }
                dailyForecasts = forecasts.sortedBy { it.dayOfYear }
            }
        } catch (e: Exception) {
            // Log or handle error
        }
    }

    val city = locationName.split(',').firstOrNull()?.trim() ?: locationName

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(
            TimelineBuilders.Timeline.fromLayoutElement(
                materialScope(context, requestParams.deviceConfiguration) {
                    primaryLayout(
                        mainSlot = {
                            LayoutElementBuilders.Column.Builder()
                                .addContent(text(city.layoutString, typography = Typography.TITLE_LARGE))
                                .addContent(
                                    text(
                                        text = "$currentTemp$tempUnit (~$apparentTemp$tempUnit)".layoutString,
                                        typography = Typography.BODY_LARGE
                                    )
                                )
                                .addContent(
                                    text(
                                        text = weatherDescription.layoutString,
                                        typography = Typography.BODY_MEDIUM
                                    )
                                )
                                .addContent(createForecastRow(dailyForecasts.take(2), tempUnit))
                                .build()
                        }
                    )
                }
            )
        )
        .build()
}

private fun MaterialScope.createForecastRow(forecasts: List<DailyForecastTile>, tempUnit: String): LayoutElementBuilders.LayoutElement {
    val rowBuilder = LayoutElementBuilders.Row.Builder()
        .setWidth(DimensionBuilders.expand())
    val dateFormat = SimpleDateFormat("d.M.", Locale.getDefault())

    if (forecasts.isEmpty()) {
        return LayoutElementBuilders.Box.Builder().build()
    }

    for (forecast in forecasts) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.DAY_OF_YEAR, forecast.dayOfYear)

        if (forecast.dayOfYear < Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
            calendar.add(Calendar.YEAR, 1)
        }
        val dateText = dateFormat.format(calendar.time)

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.weight(1f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(text(dateText.layoutString, typography = Typography.BODY_SMALL))
            .addContent(text("${forecast.minTemp}${tempUnit}/${forecast.maxTemp}${tempUnit}".layoutString, typography = Typography.BODY_SMALL))
            .build()
        rowBuilder.addContent(column)
    }
    return rowBuilder.build()
}


@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context): TilePreviewData {
    val mockJson = JSONObject()
    mockJson.put("locationName", "Praha 8, Karlin, Czech Republic")
    mockJson.put("currentTemperature", 22)
    mockJson.put("apparentTemperature", 25)
    mockJson.put("temperatureUnit", "°C")
    mockJson.put("weatherDescription", "Polojasno")

    val dailyForecastArray = JSONArray()
    val calendar = Calendar.getInstance()
    for (i in 0..2) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val forecastJson = JSONObject()
        forecastJson.put("dayOfYear", calendar.get(Calendar.DAY_OF_YEAR))
        forecastJson.put("minTemp", 15 + i)
        forecastJson.put("maxTemp", 25 + i)
        dailyForecastArray.put(forecastJson)
    }
    mockJson.put("dailyForecast", dailyForecastArray)


    val prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
    prefs.edit().putString("weather_data_json", mockJson.toString()).apply()

    return TilePreviewData(::resources) {
        tile(it, context)
    }
}
