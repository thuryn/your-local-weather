package org.thosp.yourlocalweather_wearos

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.thosp.yourlocalweather_wearos.complication.MainComplicationService
import org.thosp.yourlocalweather_wearos.tile.MainTileService
import androidx.wear.tiles.TileService

class WeatherListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/weather_update") {

            val weatherDataJson = String(messageEvent.data)
            Log.d("WeatherSync", "Received weather data: $weatherDataJson")

            val prefs = applicationContext.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString("weather_data_json", weatherDataJson)

            if (prefs.contains("current_temp")) {
                editor.remove("current_temp")
            }

            editor.apply()

            // Update Complication
            val complicationComponentName = ComponentName(this, MainComplicationService::class.java)
            val complicationRequest = ComplicationDataSourceUpdateRequester.create(applicationContext, complicationComponentName)
            complicationRequest.requestUpdateAll()

            // Update Tile
            TileService.getUpdater(this).requestUpdate(MainTileService::class.java)

        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}