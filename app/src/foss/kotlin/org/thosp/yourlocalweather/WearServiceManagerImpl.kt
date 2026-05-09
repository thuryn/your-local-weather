package org.thosp.yourlocalweather

import android.content.Context
import org.thosp.yourlocalweather.model.CompleteWeatherForecast
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.Weather

class WearServiceManagerImpl(private val context: Context) : WearServiceManager {

    override fun sendUpdate2Wearables(
        location: Location,
        weather: Weather,
        completeWeatherForecast: CompleteWeatherForecast?
    ) {
    }
}