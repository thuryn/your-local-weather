package org.thosp.yourlocalweather_wearos.tile

import android.content.Context
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale
import org.json.JSONObject
import androidx.wear.protolayout.material3.MaterialScope
import org.thosp.yourlocalweather_wearos.R
import kotlin.math.roundToInt
import androidx.core.content.edit

private const val RESOURCES_VERSION = "0"

data class DailyForecastTile(
    val dayOfYear: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val precipitation: Double
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
                    val precipitation = forecastJson.optDouble("maxRain", 0.0) + forecastJson.optDouble("maxSnow", 0.0)
                    forecasts.add(
                        DailyForecastTile(
                            dayOfYear = forecastJson.getInt("dayOfYear"),
                            minTemp = forecastJson.getDouble("minTemp").roundToInt(),
                            maxTemp = forecastJson.getDouble("maxTemp").roundToInt(),
                            precipitation = precipitation
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
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(DimensionBuilders.expand())
                        .setHeight(DimensionBuilders.expand())
                        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setStart(DimensionBuilders.dp(12f))
                                        .setEnd(DimensionBuilders.dp(12f))
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .setWidth(DimensionBuilders.expand())
                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
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
                                .addContent(
                                    LayoutElementBuilders.Spacer.Builder()
                                        .setHeight(DimensionBuilders.dp(8f))
                                        .build()
                                )
                                .addContent(createForecastRow(context, dailyForecasts, tempUnit))
                                .build()
                        )
                        .build()
                }
            )
        )
        .build()
}

private fun MaterialScope.createForecastRow(context: Context, forecasts: List<DailyForecastTile>, tempUnit: String): LayoutElementBuilders.LayoutElement {
    val rowBuilder = LayoutElementBuilders.Row.Builder()
        .setWidth(DimensionBuilders.expand())
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

    if (forecasts.isEmpty()) {
        return LayoutElementBuilders.Box.Builder().build()
    }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    
    // Zde je oprava - na Tile chceme ukázat dnešek a zítřek POUZE pokud je dřív než např. 20:00, 
    // ale nesmíme se spoléhat na fixní list forecast.take(2), protože json může mít data 
    // posunutá a první forecast nemusí nutně být ten dnešní. 
    // Místo toho z listu vyfiltrujeme dny začínající aktuálním dnem.
    val currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    
    // Vybereme jen dny od dneška (nebo od zítřka, pokud je už pozdě večer)
    val startingDay = if (currentHour < 20) currentDayOfYear else (currentDayOfYear + 1) % 365
    
    val filteredForecasts = forecasts.filter { it.dayOfYear >= startingDay }
    val displayForecasts = filteredForecasts.take(2)

    val labels = if (currentHour < 20) {
        listOf(context.getString(R.string.today), context.getString(R.string.tomorrow))
    } else {
        listOf(context.getString(R.string.tomorrow), context.getString(R.string.day_after_tomorrow))
    }

    // Pro bezpečné roztlačení úplně napříč Tile odebereme veškeré prázdné mezery a boxy, 
    // které způsobovaly kolaps.
    // Místo toho nastavíme samotným sloupcům weight(1f), čímž se Row rovnoměrně rozdělí 
    // napůl a každá půlka vycentruje svůj obsah.
    for (i in displayForecasts.indices) {
        val forecast = displayForecasts[i]
        
        val labelText = if (i < labels.size) labels[i] else "--"

        val column = LayoutElementBuilders.Column.Builder()
            // ZMĚNA: Váha přímo na Column. Jeden column tak dostane přesně polovinu (nebo 1/3 atd.) obrazovky.
            .setWidth(DimensionBuilders.weight(1f))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            
        // Den v týdnu (Zkratka)
        column.addContent(
            text(
                text = labelText.layoutString,
                maxLines = 1,
                alignment = LayoutElementBuilders.TEXT_ALIGN_CENTER,
                typography = Typography.BODY_SMALL
            )
        )
        // Teploty s předanou jednotkou z telefonu
        column.addContent(
            text(
                text = "${forecast.minTemp}$tempUnit / ${forecast.maxTemp}$tempUnit".layoutString,
                maxLines = 1,
                typography = Typography.BODY_SMALL
            )
        )

        // Srážky
        if (forecast.precipitation > 0) {
            val precipText = String.format(Locale.getDefault(), "💧 %.1f mm", forecast.precipitation)
            column.addContent(
                text(
                    text = precipText.layoutString,
                    maxLines = 1,
                    typography = Typography.BODY_SMALL
                )
            )
        }

        rowBuilder.addContent(column.build())
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
        val forecastJson = JSONObject()
        forecastJson.put("dayOfYear", calendar.get(Calendar.DAY_OF_YEAR))
        forecastJson.put("minTemp", 15 + i)
        forecastJson.put("maxTemp", 25 + i)
        // Přidáme náhodné srážky pro preview
        forecastJson.put("maxRain", if (i == 0) 2.5 else 0.0) 
        forecastJson.put("maxSnow", 0.0)
        dailyForecastArray.put(forecastJson)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    mockJson.put("dailyForecast", dailyForecastArray)


    val prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
    prefs.edit { putString("weather_data_json", mockJson.toString()) }

    return TilePreviewData(::resources) {
        tile(it, context)
    }
}
