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
            // 0: Jasno
            0 -> context.getString(if (isNight) R.string.wi_night_clear else R.string.wi_day_sunny)
            // 1: Skoro jasno (většinou jasno, sem tam malý mráček)
            1 -> context.getString(if (isNight) R.string.wi_night_alt_partly_cloudy else R.string.wi_day_sunny_overcast)
            // 2: Polojasno (střídání mraků a oblohy)
            2 -> context.getString(if (isNight) R.string.wi_night_alt_cloudy else R.string.wi_day_cloudy)
            // 3: Zataženo (plný mrak bez slunce/měsíce)
            3 -> context.getString(R.string.wi_cloudy)
            // 45, 48: Mlha a mrznoucí mlha
            45, 48 -> context.getString(if (isNight) R.string.wi_night_fog else R.string.wi_day_fog)
            // 51: Slabé mrholení (z mraků ještě prosvítá obloha)
            51 -> context.getString(if (isNight) R.string.wi_night_alt_sprinkle else R.string.wi_day_sprinkle)
            // 53, 55: Mírné až silné mrholení (obloha je už zcela zatažená)
            53, 55 -> context.getString(R.string.wi_sprinkle)
            // 56, 66: Slabé mrznoucí mrholení a slabý mrznoucí déšť (déšť se sněhem, prosvítá obloha)
            56, 66 -> context.getString(if (isNight) R.string.wi_night_alt_sleet else R.string.wi_day_sleet)
            // 57, 67: Silné mrznoucí srážky (zcela zataženo, plískanice)
            57, 67 -> context.getString(R.string.wi_sleet)
            // 61: Slabý trvalý déšť (prosvítající obloha s deštěm)
            61 -> context.getString(if (isNight) R.string.wi_night_alt_rain else R.string.wi_day_rain)
            // 63: Mírný trvalý déšť (zataženo)
            63 -> context.getString(R.string.wi_rain)
            // 65: Silný trvalý déšť (přidáme k dešti vítr pro dramatický efekt silného lijáku)
            65 -> context.getString(R.string.wi_rain_wind)
            // 71: Slabé sněžení
            71 -> context.getString(if (isNight) R.string.wi_night_alt_snow else R.string.wi_day_snow)
            // 73: Mírné sněžení (zataženo)
            73 -> context.getString(R.string.wi_snow)
            // 75: Silné sněžení (vánice - přidáme vítr ke sněhu)
            75 -> context.getString(R.string.wi_snow_wind)
            // 77: Sněhová zrna / poletující sníh (velmi drobný sníh, využijeme ikonu samotné chladné vločky)
            77 -> context.getString(R.string.wi_snowflake_cold)
            // 80, 81: Slabé až mírné dešťové přeháňky (rychlé střídání, vždy den/noc)
            80, 81 -> context.getString(if (isNight) R.string.wi_night_alt_showers else R.string.wi_day_showers)
            // 82: Silné přeháňky / průtrž mračen
            82 -> context.getString(R.string.wi_showers)
            // 85: Slabé sněhové přeháňky
            85 -> context.getString(if (isNight) R.string.wi_night_alt_snow else R.string.wi_day_snow)
            // 86: Silné sněhové přeháňky
            86 -> context.getString(R.string.wi_snow)
            // 95: Bouřka (slabá až mírná)
            95 -> context.getString(if (isNight) R.string.wi_night_alt_thunderstorm else R.string.wi_day_thunderstorm)
            // 96, 99: Bouřka s kroupami (kroupy padající z bouřkového mraku = sleet storm)
            96, 99 -> context.getString(if (isNight) R.string.wi_night_alt_sleet_storm else R.string.wi_day_sleet_storm)
            // Výchozí/Neznámý stav
            else -> context.getString(R.string.wi_na)
        }
    }
}