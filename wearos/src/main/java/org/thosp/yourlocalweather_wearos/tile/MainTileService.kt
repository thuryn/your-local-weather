package org.thosp.yourlocalweather_wearos.tile

import android.content.Context
import android.graphics.Bitmap
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
import androidx.wear.protolayout.ResourceBuilders
import org.thosp.shared_resources.Utils
import org.thosp.yourlocalweather_wearos.utils.StringUtils
import androidx.wear.protolayout.DimensionBuilders.dp

data class DailyForecastTile(
    val dayOfYear: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val precipitation: Double,
    val weatherId: Int
)

class MainTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        Futures.immediateFuture(tile(requestParams, this))

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(resources(requestParams, this))
}

private fun resources(requestParams: ResourcesRequest, context: Context): Resources {
    val requestedVersion = requestParams.version

    val parts = requestedVersion.split("_")
    val iconText0 = parts.getOrNull(1) ?: "--"
    val iconText1 = parts.getOrNull(2) ?: "--"
    val bitmap0 = StringUtils.createWeatherBitmap(context, iconText0)
    val bitmap1 = StringUtils.createWeatherBitmap(context, iconText1)

    return Resources.Builder()
        .setVersion(requestedVersion)
        .addIdToImageMapping(
            "icon_0", // ID z Layoutu
            ResourceBuilders.ImageResource.Builder()
                .setInlineResource(bitmapToInlineImage(bitmap0))
                .build()
        )

        // Přidání DRUHÉHO obrázku
        .addIdToImageMapping(
            "icon_1", // ID z Layoutu
            ResourceBuilders.ImageResource.Builder()
                .setInlineResource(bitmapToInlineImage(bitmap1))
                .build()
        )
        .build()
}

private fun bitmapToInlineImage(bitmap: Bitmap): ResourceBuilders.InlineImageResource {
    val buffer = java.nio.ByteBuffer.allocate(bitmap.byteCount)
    bitmap.copyPixelsToBuffer(buffer)

    return ResourceBuilders.InlineImageResource.Builder()
        .setData(buffer.array())
        .setWidthPx(bitmap.width)
        .setHeightPx(bitmap.height)
        .setFormat(ResourceBuilders.IMAGE_FORMAT_ARGB_8888)
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
    var sunrise = 0L
    var sunset = 0L

    if (weatherDataJson != null) {
        try {
            val json = JSONObject(weatherDataJson)
            locationName = json.optString("locationName", "--")
            currentTemp = json.optDouble("currentTemperature", 0.0).roundToInt()
            apparentTemp = json.optDouble("apparentTemperature", 0.0).roundToInt()
            sunrise = json.optLong("sunrise", 0)
            sunset = json.optLong("sunset", 0)
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
                            precipitation = precipitation,
                            weatherId = forecastJson.optInt("weatherId", 0)
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

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val startingDay = if (currentHour < 20) currentDayOfYear else (currentDayOfYear + 1) % 365
    val filteredForecasts = dailyForecasts.filter { it.dayOfYear >= startingDay }
    val displayForecasts = filteredForecasts.take(2)

    var iconText0 = "--"
    var iconText1 = "--"
    if ((displayForecasts != null) && !displayForecasts.isEmpty()) {
        iconText0 = Utils.getStrIcon(context, displayForecasts[0].weatherId, 0, 0) //it's forecast
        iconText1 = Utils.getStrIcon(context, displayForecasts[1].weatherId, 0, 0) //it's forecast
    }

    val currentTileVersion = "v1_${iconText0}_${iconText1}"

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(currentTileVersion)
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
                                .addContent(createForecastRow(context, displayForecasts, tempUnit, sunrise, sunset))
                                .build()
                        )
                        .build()
                }
            )
        )
        .build()
}

private fun MaterialScope.createForecastRow(context: Context, displayForecasts: List<DailyForecastTile>, tempUnit: String, sunrise: Long, sunset: Long): LayoutElementBuilders.LayoutElement {
    val rowBuilder = LayoutElementBuilders.Row.Builder()
        .setWidth(DimensionBuilders.expand())
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)

    if (displayForecasts.isEmpty()) {
        return LayoutElementBuilders.Box.Builder().build()
    }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val labels = if (currentHour < 20) {
        listOf(context.getString(R.string.today), context.getString(R.string.tomorrow))
    } else {
        listOf(context.getString(R.string.tomorrow), context.getString(R.string.day_after_tomorrow))
    }

    for (i in displayForecasts.indices) {
        val forecast = displayForecasts[i]
        
        val labelText = if (i < labels.size) labels[i] else "--"

        val column = LayoutElementBuilders.Column.Builder()
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
        column.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(dp(1f))
                .build()
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
            column.addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setHeight(dp(1f))
                    .build()
            )

            val precipText = String.format(Locale.getDefault(), "💧 %.1f mm", forecast.precipitation)
            column.addContent(
                text(
                    text = precipText.layoutString,
                    maxLines = 1,
                    typography = Typography.BODY_SMALL
                )
            )
        }

        column.addContent(
            LayoutElementBuilders.Spacer.Builder()
                .setHeight(dp(3f))
                .build()
        )

        column.addContent(
            LayoutElementBuilders.Image.Builder()
                .setResourceId("icon_" + i)
                .setWidth(dp(24f))  // Nastavte požadovanou šířku
                .setHeight(dp(24f)) // Nastavte požadovanou výšku
                .build()
        )

        rowBuilder.addContent(column.build())
    }
    
    return rowBuilder.build()
}


@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
@Preview(device = WearDevices.RECT)
@Preview(device = WearDevices.SQUARE)
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

    return TilePreviewData(
        onTileResourceRequest = { request -> resources(request, context) },
        onTileRequest = { request -> tile(request, context) }
    )
}
