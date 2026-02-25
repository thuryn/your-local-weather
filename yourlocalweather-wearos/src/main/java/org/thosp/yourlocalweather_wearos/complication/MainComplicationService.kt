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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.util.Log
import org.json.JSONObject
import org.thosp.yourlocalweather_wearos.presentation.MainActivity
import java.util.Locale


/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(type, "20°C", 20f, null)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = applicationContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val weatherDataJson = prefs.getString("weather_data_json", null)

        var tempText = "--°"
        var tempValue = 0f

        if (weatherDataJson != null) {
            try {
                val json = JSONObject(weatherDataJson)
                tempValue = json.getDouble("currentTemperature").toFloat()
                val unit = json.getString("temperatureUnit")
                tempText = "${Math.round(tempValue)}$unit"
            } catch (e: Exception) {
                Log.e("MainComplicationService", "Error parsing weather data", e)
            }
        } else if (prefs.contains("current_temp")) {
            // Fallback to read old data for a seamless transition.
            // This will be removed by WeatherListenerService on the first new update.
            tempText = prefs.getString("current_temp", "--°") ?: "--°"
            try {
                // The old format was a string like "20°C". We need the float for Ranged complication.
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
        return createComplicationData(request.complicationType, tempText, tempValue, tapIntent)
    }

    private fun createComplicationData(type: ComplicationType, tempText: String, tempValue: Float, tapIntent: PendingIntent?): ComplicationData =
        when (type) {
            ComplicationType.SHORT_TEXT -> {
                createShortTextComplicationData(tempText, "Temperature", createTextIcon(tempText), tapIntent)
            }

            ComplicationType.LONG_TEXT -> {
                val complicationValue = PlainComplicationText.Builder(tempText).build();
                val complicationDesc = PlainComplicationText.Builder("Temperature").build();
                val builder = LongTextComplicationData.Builder(complicationValue, complicationDesc)
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }

            ComplicationType.RANGED_VALUE -> {
                val complicationValue = PlainComplicationText.Builder(tempText).build();
                val complicationDesc = PlainComplicationText.Builder("Temperature").build();
                val builder = RangedValueComplicationData.Builder(
                    value = tempValue,
                    min = -20f,
                    max = 60f,
                    contentDescription = complicationDesc
                )
                builder.setText(complicationValue)
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }

            ComplicationType.MONOCHROMATIC_IMAGE -> {
                val complicationDesc = PlainComplicationText.Builder("Temperature").build();
                val dynamicIcon = createTextIcon(tempText)
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
                val dynamicIcon = createTextIcon(tempText)
                val builder = SmallImageComplicationData.Builder(
                    SmallImage.Builder(dynamicIcon, SmallImageType.ICON).build(),
                    complicationDesc
                )
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }
            else -> createComplicationData(ComplicationType.SHORT_TEXT, "--", 0f, tapIntent)
        }

    private fun createTextIcon(text: String): Icon {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = size / 2.5f // Velikost písma (upravte dělitel, pokud je text moc velký/malý)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Tučné je na hodinkách čitelnější
            isAntiAlias = true // Aby hrany nebylyubaté
        }

        val xPos = (canvas.width / 2).toFloat()
        val yPos = (canvas.height / 2 - (paint.descent() + paint.ascent()) / 2)

        canvas.drawText(text, xPos, yPos, paint)
        return Icon.createWithBitmap(bitmap)
    }

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
