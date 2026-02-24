package org.thosp.yourlocalweather_wearos

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.thosp.yourlocalweather_wearos.complication.MainComplicationService

class WeatherListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Zkontrolujeme, jestli je to opravdu zpráva pro nás (stejná adresa jako v mobilu)
        if (messageEvent.path == "/weather_update") {

            // 1. Rozbalíme data z mobilu
            val newTemp = String(messageEvent.data)
            Log.d("WeatherSync", "Z mobilu přiletěla teplota: $newTemp")

            // 2. Uložíme ji do paměti hodinek (SharedPreferences)
            // Aby si ji za zlomek vteřiny mohla přečíst naše Komplikace
            val prefs = applicationContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("current_temp", newTemp).apply()

            // 3. Vynutíme překreslení ciferníku (Tohle je TEN krok z minula!)
            // Tím říkáme: "Haló, ciferníku, mám v paměti nová data, zavolej moji ComplicationService!"
            val componentName = ComponentName(this, MainComplicationService::class.java)
            val request = ComplicationDataSourceUpdateRequester.create(applicationContext, componentName)
            request.requestUpdateAll()

        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}