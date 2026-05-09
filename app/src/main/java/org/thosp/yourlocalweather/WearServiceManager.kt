package org.thosp.yourlocalweather

import org.thosp.yourlocalweather.model.CompleteWeatherForecast
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.Weather

interface WearServiceManager {
    fun sendUpdate2Wearables(
        location: Location,
        weather: Weather,
        completeWeatherForecast: CompleteWeatherForecast?
    )
}