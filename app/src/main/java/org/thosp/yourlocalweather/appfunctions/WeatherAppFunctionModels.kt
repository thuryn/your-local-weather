package org.thosp.yourlocalweather.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Overview of weather app capabilities and user locations.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class WeatherCapabilitiesResult(
    /** Name of the weather provider application. */
    val providerName: String,
    /** List of saved user locations available in the app. */
    val savedLocations: List<String>,
    /** Temperature unit currently configured (e.g. Celsius, Fahrenheit). */
    val temperatureUnit: String,
    /** Description of supported weather data features (e.g. Current weather, Daily forecast, Hourly forecast). */
    val supportedFeatures: List<String>
)

/**
 * Current weather conditions for a location.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CurrentWeatherResult(
    /** Name of the location. */
    val locationName: String,
    /** Current temperature in the user's preferred unit. */
    val temperature: Double,
    /** Temperature unit label (e.g., "°C" or "°F"). */
    val temperatureUnit: String,
    /** Human readable description of weather condition (e.g., Clear sky, Rain, Thunderstorm). */
    val conditionDescription: String,
    /** Humidity percentage (0 to 100). */
    val humidity: Int,
    /** Atmospheric pressure in hPa. */
    val pressure: Double,
    /** Wind speed in m/s. */
    val windSpeed: Double,
    /** Wind direction angle in degrees (0 to 360). */
    val windDirection: Double,
    /** Cloudiness percentage (0 to 100). */
    val cloudiness: Int,
    /** Timestamp in epoch seconds when weather data was recorded. */
    val timestampEpochSeconds: Long
)

/**
 * Forecast item for a specific date and time.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ForecastItem(
    /** Timestamp of forecast point in epoch seconds. */
    val timestampEpochSeconds: Long,
    /** Temperature in user's preferred unit. */
    val temperature: Double,
    /** Minimum temperature in user's preferred unit. */
    val minTemperature: Double,
    /** Maximum temperature in user's preferred unit. */
    val maxTemperature: Double,
    /** Weather condition description. */
    val conditionDescription: String,
    /** Humidity percentage. */
    val humidity: Int,
    /** Wind speed in m/s. */
    val windSpeed: Double,
    /** Precipitation rain volume in mm if available. */
    val rainMm: Double,
    /** Precipitation snow volume in mm if available. */
    val snowMm: Double
)

/**
 * Weather forecast result containing daily or hourly entries.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class WeatherForecastResult(
    /** Name of the forecast location. */
    val locationName: String,
    /** Temperature unit label. */
    val temperatureUnit: String,
    /** List of forecast entries. */
    val forecastItems: List<ForecastItem>
)
