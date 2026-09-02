package org.thosp.yourlocalweather_wearos

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.thosp.yourlocalweather_wearos.complication.MainComplicationService
import org.thosp.yourlocalweather_wearos.complication.NoUnitComplicationService
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

            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isInteractive) {
                try {
                    val wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "YourLocalWeather:ComplicationUpdate"
                    )
                    wakeLock.acquire(1000)
                    wakeLock.release()
                } catch (e: Exception) {
                    Log.e("WeatherSync", "Error waking up display", e)
                }
            }

            // Update Complication
            val complicationComponentName = ComponentName(this, MainComplicationService::class.java)
            ComplicationDataSourceUpdateRequester.create(applicationContext, complicationComponentName).requestUpdateAll()

            val complicationNoUnitComponentName = ComponentName(this, NoUnitComplicationService::class.java)
            ComplicationDataSourceUpdateRequester.create(applicationContext, complicationNoUnitComponentName).requestUpdateAll()

            // Update Tile
            TileService.getUpdater(this).requestUpdate(MainTileService::class.java)

        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}