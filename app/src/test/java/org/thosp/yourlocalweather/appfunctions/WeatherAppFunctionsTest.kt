package org.thosp.yourlocalweather.appfunctions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherAppFunctionsTest {

    @Test
    fun testWeatherCapabilitiesResultDataStructure() {
        val capabilities = WeatherCapabilitiesResult(
            providerName = "Your Local Weather",
            savedLocations = listOf("Prague", "Brno"),
            temperatureUnit = "°C",
            supportedFeatures = listOf("Current Weather", "Weather Forecast"),
        )

        assertEquals("Your Local Weather", capabilities.providerName)
        assertEquals(2, capabilities.savedLocations.size)
        assertEquals("°C", capabilities.temperatureUnit)
        assertEquals(2, capabilities.supportedFeatures.size)
    }

    @Test
    fun testCurrentWeatherResultDataStructure() {
        val result = CurrentWeatherResult(
            locationName = "Prague",
            temperature = 21.5,
            temperatureUnit = "°C",
            conditionDescription = "Partly cloudy",
            humidity = 65,
            pressure = 1013.25,
            windSpeed = 3.5,
            windDirection = 180.0,
            cloudiness = 40,
            timestampEpochSeconds = 1700000000L,
        )

        assertEquals("Prague", result.locationName)
        assertEquals(21.5, result.temperature, 0.01)
        assertEquals("°C", result.temperatureUnit)
        assertEquals("Partly cloudy", result.conditionDescription)
        assertEquals(65, result.humidity)
        assertEquals(1013.25, result.pressure, 0.01)
        assertEquals(3.5, result.windSpeed, 0.01)
        assertEquals(180.0, result.windDirection, 0.01)
        assertEquals(40, result.cloudiness)
        assertEquals(1700000000L, result.timestampEpochSeconds)
    }

    @Test
    fun testWeatherForecastResultDataStructure() {
        val forecastItem = ForecastItem(
            timestampEpochSeconds = 1700003600L,
            temperature = 22.0,
            minTemperature = 18.0,
            maxTemperature = 25.0,
            conditionDescription = "Clear sky",
            humidity = 50,
            windSpeed = 2.1,
            rainMm = 0.0,
            snowMm = 0.0,
        )

        val result = WeatherForecastResult(
            locationName = "Prague",
            temperatureUnit = "°C",
            forecastItems = listOf(forecastItem),
        )

        assertEquals("Prague", result.locationName)
        assertEquals(1, result.forecastItems.size)
        val item = result.forecastItems.first()
        assertEquals("Clear sky", item.conditionDescription)
        assertEquals(22.0, item.temperature, 0.01)
    }
}
