package org.thosp.yourlocalweather.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.thosp.yourlocalweather.R
import org.thosp.yourlocalweather.WmoCodes
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.LocationsDbHelper
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper
import org.thosp.yourlocalweather.utils.AppPreference

/**
 * Base AppFunctionService entry point for Android 16/17 AI agents.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "WeatherAppFunctionService",
    appFunctionXmlFileName = "weather_app_functions"
)
abstract class BaseWeatherAppFunctionService : AppFunctionService() {

    /**
     * Get an overview of this weather provider's capabilities, supported metrics,
     * temperature units, and currently saved locations.
     *
     * @return [WeatherCapabilitiesResult] containing app capabilities and locations.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getCapabilities(): WeatherCapabilitiesResult = withContext(Dispatchers.IO) {
        val locationsDbHelper = LocationsDbHelper.getInstance(this@BaseWeatherAppFunctionService)
        val locations = locationsDbHelper.allRows ?: emptyList()
        val locationNames = locations.map { getLocationDisplayName(it) }.distinct()

        val tempUnit = AppPreference.getTemperatureUnitFromPreferences(this@BaseWeatherAppFunctionService) ?: "°C"

        WeatherCapabilitiesResult(
            providerName = getString(R.string.app_name),
            savedLocations = locationNames,
            temperatureUnit = tempUnit,
            supportedFeatures = listOf(
                "Current Weather (temperature, condition, humidity, pressure, wind)",
                "Weather Forecast (daily/hourly predictions, min/max temperatures, rain/snow)",
                "Multiple Saved Locations Support",
                "Automated & GPS Location Support",
            ),
        )
    }

    /**
     * Get current weather conditions for a specified location or the active default location.
     *
     * @param locationName Optional name of the target location. If null or empty, active location is used.
     * @return [CurrentWeatherResult] containing current weather conditions and metrics.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getCurrentWeather(
        locationName: String? = null,
    ): CurrentWeatherResult = withContext(Dispatchers.IO) {
        val location = resolveLocation(locationName)
        val currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(this@BaseWeatherAppFunctionService)
        val record = currentWeatherDbHelper.getWeather(location.id)

        val weather = record?.weather
        val tempUnit = AppPreference.getTemperatureUnitFromPreferences(this@BaseWeatherAppFunctionService) ?: "°C"
        val rawTemp = weather?.temperature?.toDouble() ?: 0.0
        val conditionDesc = getConditionDescription(weather?.weatherId)

        CurrentWeatherResult(
            locationName = getLocationDisplayName(location),
            temperature = rawTemp,
            temperatureUnit = tempUnit,
            conditionDescription = conditionDesc,
            humidity = weather?.humidity ?: 0,
            pressure = weather?.pressure?.toDouble() ?: 0.0,
            windSpeed = weather?.windSpeed?.toDouble() ?: 0.0,
            windDirection = weather?.windDirection?.toDouble() ?: 0.0,
            cloudiness = weather?.clouds ?: 0,
            timestampEpochSeconds = if (record != null && record.lastUpdatedTime > 0) {
                record.lastUpdatedTime / 1000
            } else {
                System.currentTimeMillis() / 1000
            },
        )
    }

    /**
     * Get weather forecast entries for a specified location or default location.
     *
     * @param locationName Optional target location name. If null or empty, active location is used.
     * @param days Optional number of forecast days to retrieve (1 to 14, default 5).
     * @return [WeatherForecastResult] containing detailed forecast items.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getWeatherForecast(
        locationName: String? = null,
        days: Int? = 5,
    ): WeatherForecastResult = withContext(Dispatchers.IO) {
        val location = resolveLocation(locationName)
        val weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(this@BaseWeatherAppFunctionService)
        val record = weatherForecastDbHelper.getWeatherForecast(location.id)

        val tempUnit = AppPreference.getTemperatureUnitFromPreferences(this@BaseWeatherAppFunctionService) ?: "°C"
        val forecastList = record?.completeWeatherForecast?.weatherForecastList ?: emptyList()

        val numDays = (days ?: 5).coerceIn(1, 14)
        val maxItems = numDays * 8 // 8 intervals of 3 hours per day

        val items = forecastList.take(maxItems).map { df ->
            ForecastItem(
                timestampEpochSeconds = df.dateTime,
                temperature = df.temperature,
                minTemperature = df.temperatureMin,
                maxTemperature = df.temperatureMax,
                conditionDescription = getConditionDescription(df.weatherId),
                humidity = df.humidity,
                windSpeed = df.windSpeed,
                rainMm = df.rain,
                snowMm = df.snow,
            )
        }

        WeatherForecastResult(
            locationName = getLocationDisplayName(location),
            temperatureUnit = tempUnit,
            forecastItems = items,
        )
    }

    private fun resolveLocation(locationName: String?): Location {
        val locationsDbHelper = LocationsDbHelper.getInstance(this)
        val allLocations = locationsDbHelper.allRows ?: emptyList()

        if (!locationName.isNullOrBlank()) {
            val matched = allLocations.firstOrNull { loc ->
                val name = getLocationDisplayName(loc)
                name.equals(locationName, ignoreCase = true) || name.contains(locationName, ignoreCase = true)
            }
            if (matched != null) return matched
        }

        val activeLocationId = AppPreference.getCurrentLocationId(this)
        val activeLoc = locationsDbHelper.getLocationById(activeLocationId)
        if (activeLoc != null) return activeLoc

        return locationsDbHelper.getLocationByOrderId(0)
            ?: locationsDbHelper.getLocationByOrderId(1)
            ?: allLocations.firstOrNull()
            ?: Location(0, 0, "Current Location", "en", 0.0, 0.0, 0f, "", 0L, false, true, null)
    }

    private fun getLocationDisplayName(location: Location): String {
        return try {
            val address = location.address
            if (address != null && !address.locality.isNullOrBlank()) {
                address.locality
            } else if (address != null && !address.featureName.isNullOrBlank()) {
                address.featureName
            } else if (!location.nickname.isNullOrBlank()) {
                location.nickname
            } else {
                "Location #${location.id}"
            }
        } catch (e: Exception) {
            "Location #${location.id}"
        }
    }

    private fun getConditionDescription(weatherId: Int?): String {
        if (weatherId == null) return "Unknown"
        val wmoCode = WmoCodes.getById(weatherId)
        return wmoCode?.description ?: "Weather Code $weatherId"
    }
}
