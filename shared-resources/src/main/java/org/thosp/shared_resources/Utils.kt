package org.thosp.shared_resources

import android.content.Context
import java.util.Calendar

object Utils {

    @JvmStatic
    fun getStrIcon(context: Context, weatherId: Int?, sunrise: Long, sunset: Long): String {
        // Pokud nemáme data, vrátíme neutrální "N/A" (na) ikonu
        if (weatherId == null) return context.getString(R.string.wi_na)

        val currentTimeInMilis = Calendar.getInstance().timeInMillis
        val isNight = if ((sunrise > 0) && (sunset > 0)) {
            (currentTimeInMilis < (sunrise * 1000)) || (currentTimeInMilis > (sunset * 1000))
        } else {
            false
        }

        return when (weatherId) {
            // Jasno
            0 -> context.getString(if (isNight) R.string.wi_night_clear else R.string.wi_day_sunny)
            // Skoro jasno
            1 -> context.getString(if (isNight) R.string.wi_night_partly_cloudy else R.string.wi_day_cloudy)
            // Polojasno
            2 -> context.getString(if (isNight) R.string.wi_night_alt_cloudy else R.string.wi_day_cloudy)
            // Zataženo
            3 -> context.getString(R.string.wi_cloudy)
            // Mlha
            45, 48 -> context.getString(if (isNight) R.string.wi_night_fog else R.string.wi_day_fog)
            // Mrholení
            51, 53, 55 -> context.getString(if (isNight) R.string.wi_night_sprinkle else R.string.wi_day_sprinkle)
            // Mrznoucí srážky (déšť se sněhem / plískanice)
            56, 57, 66, 67 -> context.getString(if (isNight) R.string.wi_night_alt_sleet else R.string.wi_day_sleet)
            // Trvalý déšť (zde stačí neutrální, protože u trvalého deště slunce/měsíc moc nesvítí)
            61, 63, 65 -> context.getString(R.string.wi_rain)
            // Trvalé sněžení
            71, 73, 75, 77 -> context.getString(R.string.wi_snow)
            // Dešťové přeháňky (střídání mraků, deště a oblohy)
            80, 81, 82 -> context.getString(if (isNight) R.string.wi_night_alt_showers else R.string.wi_day_showers)
            // Sněhové přeháňky
            85, 86 -> context.getString(if (isNight) R.string.wi_night_alt_snow else R.string.wi_day_snow)
            // Běžná bouřka
            95 -> context.getString(if (isNight) R.string.wi_night_alt_thunderstorm else R.string.wi_day_thunderstorm)
            // Bouřka s kroupami
            96, 99 -> context.getString(if (isNight) R.string.wi_night_alt_hail else R.string.wi_day_hail)
            // Výchozí/Neznámý stav
            else -> context.getString(R.string.wi_na)
        }
    }
}