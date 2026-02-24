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
import org.thosp.yourlocalweather_wearos.presentation.MainActivity


/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return createComplicationData(type, "20°C", null)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prefs = applicationContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val currentTemperature = prefs.getString("current_temp", "--°C") ?: "--°C"
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val tapIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
       return createComplicationData(request.complicationType, currentTemperature, tapIntent)
    }

    private fun createComplicationData(type: ComplicationType, currentTemperature: String, tapIntent: PendingIntent?): ComplicationData =
        when (type) {
            ComplicationType.SHORT_TEXT -> {
                createComplicationData(currentTemperature, "Temperature", createTextIcon(currentTemperature), tapIntent)
            }

            ComplicationType.LONG_TEXT -> {
                val complicationValue = PlainComplicationText.Builder(currentTemperature).build();
                val complicationDesc = PlainComplicationText.Builder("Temperature").build();
                val builder = LongTextComplicationData.Builder(complicationValue, complicationDesc)
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }

            ComplicationType.RANGED_VALUE -> {
                val complicationValue = PlainComplicationText.Builder(currentTemperature).build();
                val complicationDesc = PlainComplicationText.Builder("Temperature").build();
                var tempValue = 0f
                try {
                    tempValue =
                        currentTemperature.substring(0, currentTemperature.length - 2).toFloat()
                } catch (e: Exception) {
                    Log.e("YourLocalWeather complication", "Error parsing temperature value", e)
                }
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
                val dynamicIcon = createTextIcon(currentTemperature)
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
                val dynamicIcon = createTextIcon(currentTemperature)
                val builder = SmallImageComplicationData.Builder(
                    SmallImage.Builder(dynamicIcon, SmallImageType.ICON).build(),
                    complicationDesc
                )
                if (tapIntent != null) {
                    builder.setTapAction(tapIntent)
                }
                builder.build()
            }
            else -> createComplicationData("-20", "Temperature", createTextIcon("-20"), tapIntent)
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

    private fun createComplicationData(text: String, contentDescription: String, icon: Icon, tapIntent: PendingIntent?): ComplicationData {
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