package org.thosp.yourlocalweather_wearos.utils

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
}
