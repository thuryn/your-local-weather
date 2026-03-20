package org.thosp.yourlocalweather_wearos.presentation

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import org.json.JSONObject
import org.thosp.yourlocalweather_wearos.R
import org.thosp.yourlocalweather_wearos.presentation.theme.YourlocalweatherTheme
import org.thosp.yourlocalweather_wearos.utils.StringUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.Button
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors
import org.thosp.shared_resources.Utils
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

data class DailyForecast(
    val dayOfYear: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val precipitation: Double,
    val weatherId: String
)

val weatherIcons = FontFamily(Font(R.font.weathericons))

@Composable
fun WearApp() {
    YourlocalweatherTheme {
        val context = LocalContext.current

        val prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)
        val weatherDataJson = prefs.getString("weather_data_json", null)

        var appStatus by remember { mutableStateOf(CompanionAppStatus.CHECKING) }

        if (weatherDataJson == null) {
            LaunchedEffect(Unit) {
                val capabilityClient = Wearable.getCapabilityClient(context)

                capabilityClient.getCapability(
                    "your_local_weather_mobile",
                    CapabilityClient.FILTER_ALL
                )
                    .addOnSuccessListener { capabilityInfo ->
                        if (capabilityInfo.nodes.isEmpty()) {
                            appStatus = CompanionAppStatus.MISSING
                        } else {
                            appStatus = CompanionAppStatus.INSTALLED
                        }
                    }
                    .addOnFailureListener {
                        appStatus = CompanionAppStatus.MISSING
                    }
            }

            DisposableEffect(Unit) {
                val capabilityClient = Wearable.getCapabilityClient(context)

                val listener = CapabilityClient.OnCapabilityChangedListener { capabilityInfo ->
                    if (capabilityInfo.nodes.isNotEmpty()) {
                        appStatus = CompanionAppStatus.INSTALLED
                    } else {
                        appStatus = CompanionAppStatus.MISSING
                    }
                }
                capabilityClient.addListener(listener, "your_local_weather_mobile")
                onDispose {
                    capabilityClient.removeListener(listener)
                }
            }
        } else {
            appStatus = CompanionAppStatus.INSTALLED
        }

        when (appStatus) {
            CompanionAppStatus.CHECKING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.wearos_main_activity_checking_connection),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            CompanionAppStatus.MISSING -> {
                val toastText = stringResource(R.string.wearos_main_activity_not_connected)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.wearos_main_activity_missing_connection_label),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(R.string.wearos_main_activity_missing_connection_info),
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            val nodeClient = Wearable.getNodeClient(context)
                            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                                if (nodes.isNotEmpty()) {
                                    openPlayStoreOnPhone(context, nodes.first().id)
                                } else {
                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(text =stringResource(R.string.activity_wearos_app_install_from_play))
                    }
                }
            }

            CompanionAppStatus.INSTALLED -> {
                var locationName by remember { mutableStateOf("--") }
                var currentTemp by remember { mutableStateOf(0) }
                var apparentTemp by remember { mutableStateOf(0) }
                var tempUnit by remember { mutableStateOf("°C") }
                var humidity by remember { mutableStateOf(0) }
                var weatherDescription by remember { mutableStateOf("--") }
                var sunrise by remember { mutableStateOf(0L) }
                var sunset by remember { mutableStateOf(0L) }
                var windSpeed by remember { mutableStateOf(0.0) }
                //var windDirection by remember { mutableStateOf(0.0) }
                var pressure by remember { mutableStateOf(0.0) }
                var cloudiness by remember { mutableStateOf(0) }
                var dailyForecasts by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }

                if (weatherDataJson != null) {
                    try {
                        val json = JSONObject(weatherDataJson)
                        locationName = json.optString("locationName", "--")
                        currentTemp = json.optDouble("currentTemperature", 0.0).roundToInt()
                        apparentTemp = json.optDouble("apparentTemperature", 0.0).roundToInt()
                        tempUnit = json.optString("temperatureUnit", "°C")
                        humidity = json.optInt("humidity", 0)
                        weatherDescription = json.optString("weatherDescription", "--")
                        sunrise = json.optLong("sunrise", 0)
                        sunset = json.optLong("sunset", 0)
                        windSpeed = json.optDouble("windSpeed", 0.0)
                        //windDirection = json.optDouble("windDegree", 0.0)
                        pressure = json.optDouble("pressure", 0.0)
                        cloudiness = json.optInt("cloudiness", 0)

                        val dailyForecastJson = json.optJSONArray("dailyForecast")
                        if (dailyForecastJson != null) {
                            val forecasts = mutableListOf<DailyForecast>()
                            for (i in 0 until dailyForecastJson.length()) {
                                val forecastJson = dailyForecastJson.getJSONObject(i)
                                val precipitation =
                                    forecastJson.optDouble("maxRain", 0.0) + forecastJson.optDouble(
                                        "maxSnow",
                                        0.0
                                    )
                                forecasts.add(
                                    DailyForecast(
                                        dayOfYear = forecastJson.getInt("dayOfYear"),
                                        minTemp = forecastJson.getDouble("minTemp").roundToInt(),
                                        maxTemp = forecastJson.getDouble("maxTemp").roundToInt(),
                                        precipitation = precipitation,
                                        weatherId = Utils.getStrIcon(
                                            context,
                                            forecastJson.optInt("weatherId", 0),
                                            0,
                                            0
                                        ) //it's forecast
                                    )
                                )
                            }
                            dailyForecasts = forecasts.sortedBy { it.dayOfYear }
                        }
                    } catch (e: Exception) {
                        Log.e("MainComplicationService", "Error parsing weather data", e)
                    }
                }

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val listState = rememberScalingLazyListState()
                val formattedLocation = StringUtils.formatLocationName(locationName)

                val cal = Calendar.getInstance()
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val dayAfterTomorrowDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

                val filteredForecasts = dailyForecasts.filter { forecast ->
                    !(currentHour >= 20 && forecast.dayOfYear == todayDayOfYear)
                }

                AppScaffold {
                    ScreenScaffold(
                        scrollState = listState
                    ) {
                        ScalingLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            state = listState,
                            contentPadding = PaddingValues(
                                top = 20.dp,
                                start = 10.dp,
                                end = 10.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = formattedLocation,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Text(
                                    text = "$currentTemp$tempUnit (~$apparentTemp$tempUnit)",
                                    style = MaterialTheme.typography.displaySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Text(
                                    text = weatherDescription,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconWithText(
                                        stringResource(R.string.icon_humidity),
                                        "$humidity%"
                                    )
                                    IconWithText(
                                        stringResource(R.string.icon_barometer),
                                        "${pressure.roundToInt()} hPa"
                                    )
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconWithText(
                                        stringResource(R.string.icon_wind),
                                        "${windSpeed.roundToInt()} m/s"
                                    )
                                    IconWithText(
                                        stringResource(R.string.icon_cloudiness),
                                        "$cloudiness%"
                                    )
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconWithText(
                                        stringResource(R.string.icon_sunrise),
                                        if (sunrise > 0) sdf.format(Date(sunrise * 1000)) else "--:--"
                                    )
                                    IconWithText(
                                        stringResource(R.string.icon_sunset),
                                        if (sunset > 0) sdf.format(Date(sunset * 1000)) else "--:--"
                                    )
                                }
                            }

                            if (filteredForecasts.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.label_activity_weather_forecast),
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                            }

                            items(filteredForecasts) { forecast ->
                                val calendar = Calendar.getInstance()
                                val currentYear = calendar.get(Calendar.YEAR)
                                calendar.set(Calendar.YEAR, currentYear)
                                calendar.set(Calendar.DAY_OF_YEAR, forecast.dayOfYear)

                                if (forecast.dayOfYear < Calendar.getInstance()
                                        .get(Calendar.DAY_OF_YEAR)
                                ) {
                                    calendar.add(Calendar.YEAR, 1)
                                }

                                val dateFormat =
                                    SimpleDateFormat("EEE, d. MMM", Locale.getDefault())

                                val dateText = when (forecast.dayOfYear) {
                                    todayDayOfYear -> stringResource(R.string.today)
                                    tomorrowDayOfYear -> stringResource(R.string.tomorrow)
                                    dayAfterTomorrowDayOfYear -> stringResource(R.string.day_after_tomorrow)
                                    else -> dateFormat.format(calendar.time)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "${forecast.minTemp}$tempUnit / ${forecast.maxTemp}$tempUnit",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (forecast.precipitation > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.wi_rain),
                                                fontFamily = weatherIcons,
                                                fontSize = 16.sp,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Text(
                                                text = String.format(
                                                    Locale.getDefault(),
                                                    "%.1f mm",
                                                    forecast.precipitation
                                                ),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    Text(
                                        text = forecast.weatherId,
                                        fontFamily = weatherIcons,
                                        fontSize = 24.sp, // Zde si nastavíte velikost ikony
                                        color = MaterialTheme.colorScheme.onSurface, // Barva ikony
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun openPlayStoreOnPhone(context: Context, phoneNodeId: String) {
    val intent = Intent(Intent.ACTION_VIEW)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setData("market://details?id=org.thosp.yourlocalweather".toUri())

    val executor = Executors.newSingleThreadExecutor()
    val remoteActivityHelper = RemoteActivityHelper(context, executor)

    remoteActivityHelper.startRemoteActivity(intent, phoneNodeId)
}

@Composable
fun IconWithText(icon: String, text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = icon,
            fontFamily = weatherIcons,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp()
}
