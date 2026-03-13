package org.thosp.yourlocalweather_wearos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import org.thosp.yourlocalweather_wearos.R

object StringUtils {
    fun formatLocationName(location: String): String {
        // Normalize spaces around commas and then split into words.
        // This keeps a comma attached to the word before it.
        val words = location.replace(",", ", ").split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return "--"

        val lines = mutableListOf<String>()
        val limits = listOf(10, 20, 30) // Character limits for line 1, 2, 3
        var currentLine = ""
        var wordIndex = 0
        var lineIndex = 0

        while (wordIndex < words.size) {
            if (lineIndex >= limits.size) {
                // If we are past the defined limits, append the rest to the last line.
                val restOfWords = words.subList(wordIndex, words.size).joinToString(" ")
                if (currentLine.isNotEmpty()) {
                    currentLine += " $restOfWords"
                } else {
                    currentLine = restOfWords
                }
                wordIndex = words.size // Exit loop
            } else {
                val word = words[wordIndex]
                val limit = limits[lineIndex]

                if (currentLine.isEmpty()) {
                    // Start a new line.
                    if (word.length > limit) {
                        // Word is longer than the limit, it gets its own line.
                        lines.add(word)
                        lineIndex++
                        wordIndex++
                    } else {
                        currentLine = word
                        wordIndex++
                    }
                } else if (currentLine.length + 1 + word.length <= limit) {
                    // Add word to current line if it fits.
                    currentLine += " $word"
                    wordIndex++
                } else {
                    // Current line is full, commit and start a new line.
                    lines.add(currentLine)
                    currentLine = ""
                    lineIndex++
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines.joinToString("\n")
    }

    fun createWeatherIcon(context: Context, text: String): Icon {
        return Icon.createWithBitmap(createWeatherBitmap(context, text))
    }

    fun createWeatherBitmap(context: Context, text: String): Bitmap {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val weatherFont = ResourcesCompat.getFont(context, R.font.weathericons)

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
        return bitmap
    }
}
