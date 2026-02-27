package org.thosp.yourlocalweather_wearos.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import org.json.JSONObject
import org.thosp.yourlocalweather_wearos.R
import org.thosp.yourlocalweather_wearos.presentation.MainActivity
import java.util.Calendar
import androidx.core.graphics.createBitmap


/**
 * Skeleton for complication data source that returns short text.
 */
abstract class AbstractTemperatureComplication : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(type, "20°C", 20f, getString(R.string.icon_clear_sky_day), null)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = applicationContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val weatherDataJson = prefs.getString("weather_data_json", null)

        var tempText = "--°"
        var tempValue = 0f
        var iconText = getString(R.string.icon_weather_default)

        if (weatherDataJson != null) {
            try {
                val json = JSONObject(weatherDataJson)
                tempValue = json.getDouble("currentTemperature").toFloat()
                val unit = getUnit(json)
                tempText = "${Math.round(tempValue)}$unit"

                val currentDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val startingDay = if (currentHour < 20) currentDayOfYear else (currentDayOfYear + 1) % 365

                val dailyForecastJson = json.optJSONArray("dailyForecast")
                if (dailyForecastJson != null) {
                    var weatherId = 0
                    for (i in 0 until dailyForecastJson.length()) {
                        val forecastJson = dailyForecastJson.getJSONObject(i)
                        if (forecastJson.getInt("dayOfYear") == startingDay) {
                            weatherId = forecastJson.optInt("weatherId", 0)
                            break
                        }
                    }
                    if (weatherId == 0 && dailyForecastJson.length() > 0) {
                        weatherId = dailyForecastJson.getJSONObject(0).optInt("weatherId", 0)
                    }
                    iconText = getStrIcon(applicationContext, weatherId)
                }

            } catch (e: Exception) {
                Log.e("MainComplicationService", "Error parsing weather data", e)
            }
        } else if (prefs.contains("current_temp")) {
            // Fallback to read old data for a seamless transition.
            tempText = prefs.getString("current_temp", "--°") ?: "--°"
            try {
                val tempString = tempText.filter { it.isDigit() || it == '-' || it == '.' }
                if (tempString.isNotEmpty()) {
                    tempValue = tempString.toFloat()
                }
            } catch (e: NumberFormatException) {
                Log.w("Complication", "Could not parse old temperature value: $tempText", e)
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val tapIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return createComplicationData(request.complicationType, tempText, tempValue, iconText, tapIntent)
    }

    private fun getStrIcon(context: Context, weatherId: Int): String {
        if (weatherId == 0) return context.getString(R.string.icon_clear_sky_day)
        return when (weatherId) {
            0 -> context.getString(R.string.icon_clear_sky_day)
            1 -> context.getString(R.string.icon_few_clouds_day)
            2 -> context.getString(R.string.icon_scattered_clouds)
            3 -> context.getString(R.string.icon_broken_clouds)
            51, 61, 56, 66, 80 -> context.getString(R.string.icon_shower_rain)
            53, 55, 57, 63, 65, 67, 81, 82 -> context.getString(R.string.icon_rain_day)
            96, 95, 99 -> context.getString(R.string.icon_thunderstorm)
            71, 73, 75, 77, 85, 86 -> context.getString(R.string.icon_snow)
            45, 48 -> context.getString(R.string.icon_mist)
            else -> context.getString(R.string.icon_weather_default)
        }
    }

    abstract fun getUnit(json: JSONObject): String

    private fun createComplicationData(type: ComplicationType, tempText: String, tempValue: Float, iconText: String, tapIntent: PendingIntent?): ComplicationData =
        when (type) {
            ComplicationType.SHORT_TEXT -> {
                createShortTextComplicationData(tempText, "Temperature", createWeatherIcon(iconText), tapIntent)
            }

            ComplicationType.LONG_TEXT -> {
                val complicationValue = PlainComplicationText.Builder(tempText).build()
                val complicationDesc = PlainComplicationText.Builder("Temperature").build()
                val builder = LongTextComplicationData.Builder(complicationValue, complicationDesc)
                builder.setMonochromaticImage(MonochromaticImage.Builder(createWeatherIcon(iconText)).build())
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }

            ComplicationType.RANGED_VALUE -> {
                val complicationValue = PlainComplicationText.Builder(tempText).build()
                val complicationDesc = PlainComplicationText.Builder("Temperature").build()
                val builder = RangedValueComplicationData.Builder(
                    value = tempValue,
                    min = -20f,
                    max = 60f,
                    contentDescription = complicationDesc
                )
                builder.setText(complicationValue)
                builder.setMonochromaticImage(MonochromaticImage.Builder(createWeatherIcon(iconText)).build())
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                val complicationDesc = PlainComplicationText.Builder("Temperature").build()
                // Tady pouzijeme novou kombinovanou ikonu
                val dynamicIcon = createCombinedIcon(iconText, tempText)
                val builder = MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(dynamicIcon).build(),
                    complicationDesc
                )
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }

            ComplicationType.SMALL_IMAGE -> {
                val complicationDesc = PlainComplicationText.Builder("Temperature").build()
                // I tady pouzijeme novou kombinovanou ikonu
                val dynamicIcon = createCombinedIcon(iconText, tempText)
                val builder = SmallImageComplicationData.Builder(
                    SmallImage.Builder(dynamicIcon, SmallImageType.ICON).build(),
                    complicationDesc
                )
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }
            else -> createComplicationData(ComplicationType.SHORT_TEXT, "--", 0f, getString(R.string.icon_weather_default), tapIntent)
        }

    // Puvodni metoda - pro komplikace, ktere text vykresluji samy (SHORT_TEXT, RANGED_VALUE)
    private fun createWeatherIcon(text: String): Icon {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val weatherFont = ResourcesCompat.getFont(applicationContext, R.font.weathericons)

        val paint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = size / 1.5f
            typeface = weatherFont
            isAntiAlias = true
        }

        val xPos = (canvas.width / 2).toFloat()
        // Posunuto lehce nahoru, jak jste si přál minule pro bezne ikony
        val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2) - (size / 8)

        canvas.drawText(text, xPos, yPos, paint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun createCombinedIcon(iconText: String, tempText: String): Icon {
        val size = 256
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val xPos = (canvas.width / 2).toFloat()

        // Nastavení fontu pro ikonu počasí
        val weatherFont = ResourcesCompat.getFont(applicationContext, R.font.weathericons)
        val iconPaint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = size / 3f
            typeface = weatherFont
            isAntiAlias = true
        }

        // Nastavení fontu pro text teploty
        val textPaint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = getTemperatureFontSize(tempText, size)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // 1. Zjistíme PŘESNÝ viditelný ořez ikony (ignoruje vnitřní padding písma)
        val iconBounds = android.graphics.Rect()
        iconPaint.getTextBounds(iconText, 0, iconText.length, iconBounds)

        // Ikona bude doslova narvaná úplně nahoře (přidal jsem jen jemný 5% padding)
        val iconYPos = -iconBounds.top.toFloat()
        canvas.drawText(iconText, xPos, iconYPos, iconPaint)

        // 2. Zjistíme PŘESNÝ viditelný ořez teploty
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(tempText, 0, tempText.length, textBounds)

        // Teplota bude doslova narvaná úplně dole u dna.
        val bottomPadding = size * 0.05f
        val textYPos = size.toFloat() - textBounds.bottom.toFloat() - bottomPadding
        canvas.drawText(tempText, xPos, textYPos, textPaint)

        return Icon.createWithBitmap(bitmap)
    }

    abstract fun getTemperatureFontSize(tempText: String, canvasSize: Int): Float

    private fun createShortTextComplicationData(text: String, contentDescription: String, icon: Icon, tapIntent: PendingIntent?): ComplicationData {
        val builder = ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        )
        builder.setMonochromaticImage(MonochromaticImage.Builder(icon).build())
        if (tapIntent != null) {
            builder.setTapAction(tapIntent)
        }
        return builder.build()
    }
}
