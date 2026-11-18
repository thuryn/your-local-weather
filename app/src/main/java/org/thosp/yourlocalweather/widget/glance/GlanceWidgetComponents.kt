package org.thosp.yourlocalweather.widget.glance

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.state.GlanceStateDefinition
import androidx.datastore.core.DataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import org.thosp.yourlocalweather.GraphsActivity
import org.thosp.yourlocalweather.MainActivity
import org.thosp.yourlocalweather.R
import org.thosp.yourlocalweather.WeatherForecastActivity
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper
import org.thosp.yourlocalweather.utils.AppPreference
import org.thosp.yourlocalweather.utils.Constants
import org.thosp.yourlocalweather.utils.ForecastUtil
import org.thosp.yourlocalweather.utils.TemperatureUtil
import org.thosp.yourlocalweather.utils.Utils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun GlanceGlassPanel(
    modifier: GlanceModifier = GlanceModifier,
    isGlass: Boolean,
    content: @Composable () -> Unit
) {
    if (isGlass) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(ImageProvider(R.drawable.bg_glass_panel))
                .padding(6.dp)
        ) {
            content()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun GlanceHeaderBar(
    context: Context,
    appWidgetId: Int,
    currentLocation: Location?,
    weatherRecord: CurrentWeatherDbHelper.WeatherRecord?,
    weatherForecastRecord: WeatherForecastDbHelper.WeatherForecastRecord?,
    timeStyle: String,
    textColor: Color,
    headerColor: Color
) {
    val cityName = if (currentLocation != null) Utils.getCityAndCountry(context, currentLocation) else context.getString(R.string.location_not_found)
    val lastUpdate = if (currentLocation != null) Utils.getLastUpdateTime(context, weatherRecord, weatherForecastRecord, timeStyle, currentLocation) else ""

    val refreshBroadcastIntent = Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE).apply {
        setPackage(context.packageName)
        putExtra("widgetId", appWidgetId)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(headerColor)
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cityName,
            style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = lastUpdate,
            style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 12.sp),
            modifier = GlanceModifier.clickable(actionSendBroadcast(refreshBroadcastIntent))
        )
    }
}

@Composable
fun GlanceCurrentWeatherSection(
    context: Context,
    currentLocation: Location?,
    weatherRecord: CurrentWeatherDbHelper.WeatherRecord?,
    temperatureUnit: String,
    windUnit: String,
    textColor: Color,
    fontBasedIcons: Boolean
) {
    val weather = weatherRecord?.weather
    val mainIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val tempText = GlanceWeatherHelper.getTemperatureText(context, currentLocation, weatherRecord, temperatureUnit)
    val secondTempText = GlanceWeatherHelper.getSecondTemperatureText(context, currentLocation, weatherRecord, temperatureUnit)
    val windText = if (weather != null && currentLocation != null) GlanceWeatherHelper.getWindText(context, weather, windUnit, currentLocation.locale) else null
    val humidityText = if (weather != null) GlanceWeatherHelper.getHumidityText(context, weather) else null
    val description = GlanceWeatherHelper.getWeatherDescription(context, weather)

    // Icon
    val weatherIconProvider: ImageProvider = if (weatherRecord != null) {
        if (fontBasedIcons) {
            val bitmap = GlanceWeatherHelper.getWeatherIconBitmap(context, weatherRecord)
            if (bitmap != null) ImageProvider(bitmap) else ImageProvider(R.drawable.ic_weather_set_1_25)
        } else {
            ImageProvider(GlanceWeatherHelper.getWeatherResourceIcon(weatherRecord))
        }
    } else {
        ImageProvider(R.drawable.ic_weather_set_1_25)
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Temperature, Wind, Humidity
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tempText,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.clickable(actionStartActivity(mainIntent))
                )
                if (secondTempText != null) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = secondTempText,
                        style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 14.sp)
                    )
                }
            }
            if (windText != null) {
                Text(
                    text = windText,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 11.sp)
                )
            }
            if (humidityText != null) {
                Text(
                    text = humidityText,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 11.sp)
                )
            }
        }

        // Right Column: Description & Weather Icon
        Column(horizontalAlignment = Alignment.End) {
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
            }
            Image(
                provider = weatherIconProvider,
                contentDescription = "Weather Icon",
                modifier = GlanceModifier
                    .width(72.dp)
                    .height(72.dp)
                    .clickable(actionStartActivity(mainIntent))
            )
        }
    }
}

@Composable
fun GlanceResponsiveForecastSection(
    context: Context,
    weatherForecastRecord: WeatherForecastDbHelper.WeatherForecastRecord?,
    forecastDays: List<ForecastUtil.WeatherForecastPerDay>,
    currentLocation: Location,
    availableWidthDp: Float,
    temperatureUnit: String,
    textColor: Color,
    fontBasedIcons: Boolean,
    forecastDayAbbrev: Boolean,
    hoursForecast: Boolean
) {
    if (hoursForecast) {
        GlanceResponsiveHourlyForecastRow(
            context = context,
            weatherForecastRecord = weatherForecastRecord,
            currentLocation = currentLocation,
            availableWidthDp = availableWidthDp,
            temperatureUnit = temperatureUnit,
            textColor = textColor,
            fontBasedIcons = fontBasedIcons
        )
    } else if (forecastDays.isNotEmpty()) {
        GlanceResponsiveDailyForecastRow(
            context = context,
            forecastDays = forecastDays,
            availableWidthDp = availableWidthDp,
            locationLocale = currentLocation.locale,
            temperatureUnit = temperatureUnit,
            textColor = textColor,
            fontBasedIcons = fontBasedIcons,
            forecastDayAbbrev = forecastDayAbbrev
        )
    }
}

@Composable
fun GlanceResponsiveDailyForecastRow(
    context: Context,
    forecastDays: List<ForecastUtil.WeatherForecastPerDay>,
    availableWidthDp: Float,
    locationLocale: Locale,
    temperatureUnit: String,
    textColor: Color,
    fontBasedIcons: Boolean,
    forecastDayAbbrev: Boolean
) {
    val itemWidthDp = if (forecastDayAbbrev) 62f else 72f

    val maxCalculatedDays = (availableWidthDp / itemWidthDp).toInt().coerceIn(1, forecastDays.size)
    val visibleDays = forecastDays.take(maxCalculatedDays)

    val forecastIntent = Intent(context, WeatherForecastActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val pattern = if (forecastDayAbbrev) "EEE" else "EEEE"
    val sdfDayOfWeek = SimpleDateFormat(pattern, locationLocale)
    val tempUnitSymbol = TemperatureUtil.getTemperatureUnit(context, temperatureUnit)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(forecastIntent))
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (day in visibleDays) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, day.dayInYear)
                set(Calendar.YEAR, day.year)
            }
            val dayName = sdfDayOfWeek.format(cal.time)

            val minTempFormatted = Math.round(
                TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnit, day.weatherMaxMinForDay.minTemp)
            )
            val maxTempFormatted = Math.round(
                TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnit, day.weatherMaxMinForDay.maxTemp)
            )
            val tempRangeText = "$minTempFormatted/$maxTempFormatted$tempUnitSymbol"

            val dayIconProvider: ImageProvider = if (fontBasedIcons) {
                val bitmap = GlanceWeatherHelper.getForecastDayIconBitmap(context, day.weatherIds.mainWeatherId)
                if (bitmap != null) ImageProvider(bitmap) else ImageProvider(R.drawable.ic_weather_set_1_25)
            } else {
                ImageProvider(GlanceWeatherHelper.getForecastDayIconResId(day.weatherIds.mainWeatherId, day.weatherMaxMinForDay.maxTemp, day.weatherMaxMinForDay.maxWind))
            }

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayName,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Image(
                    provider = dayIconProvider,
                    contentDescription = dayName,
                    modifier = GlanceModifier
                        .width(42.dp)
                        .height(42.dp)
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = tempRangeText,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun GlanceResponsiveHourlyForecastRow(
    context: Context,
    weatherForecastRecord: WeatherForecastDbHelper.WeatherForecastRecord?,
    currentLocation: Location,
    availableWidthDp: Float,
    temperatureUnit: String,
    textColor: Color,
    fontBasedIcons: Boolean
) {
    val completeList = weatherForecastRecord?.completeWeatherForecast?.weatherForecastList ?: return
    val nowSec = System.currentTimeMillis() / 1000
    val futureList = completeList.filter { (it?.dateTime ?: 0) >= nowSec - 3600 }
    if (futureList.isEmpty()) return

    val itemWidthDp = 58f
    val maxCalculatedHours = (availableWidthDp / itemWidthDp).toInt().coerceIn(1, futureList.size)
    val visibleHours = futureList.take(maxCalculatedHours)

    val forecastIntent = Intent(context, WeatherForecastActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val tempUnitSymbol = TemperatureUtil.getTemperatureUnit(context, temperatureUnit)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(forecastIntent))
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (item in visibleHours) {
            val forecastDate = Date(item.dateTime * 1000)
            val hourText = AppPreference.getLocalizedHour(context, forecastDate, currentLocation.locale)
            val tempFormatted = Math.round(
                TemperatureUtil.getTemperatureInPreferredUnit(temperatureUnit, item.temperature)
            )
            val tempText = "$tempFormatted$tempUnitSymbol"

            val dayIconProvider: ImageProvider = if (fontBasedIcons) {
                val bitmap = GlanceWeatherHelper.getForecastDayIconBitmap(context, item.weatherId)
                if (bitmap != null) ImageProvider(bitmap) else ImageProvider(R.drawable.ic_weather_set_1_25)
            } else {
                ImageProvider(GlanceWeatherHelper.getForecastDayIconResId(item.weatherId, item.temperatureMax, item.windSpeed))
            }

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(horizontal = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = hourText,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Image(
                    provider = dayIconProvider,
                    contentDescription = hourText,
                    modifier = GlanceModifier
                        .width(42.dp)
                        .height(42.dp)
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = tempText,
                    style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

object RefreshableGlanceStateDefinition : GlanceStateDefinition<Long> {
    private val counter = AtomicLong(0L)
    private val dataStore = object : DataStore<Long> {
        override val data: Flow<Long>
            get() = flow { emit(counter.incrementAndGet()) }

        override suspend fun updateData(transform: suspend (t: Long) -> Long): Long {
            val current = counter.get()
            val updated = transform(current)
            counter.set(updated)
            return updated
        }
    }

    override fun getLocation(context: Context, fileKey: String): File =
        File(context.filesDir, "glance_refresh_$fileKey")

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Long> =
        dataStore
}

fun updateAllGlanceWidgets(context: Context) {
    val appContext = context.applicationContext
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
        try {
            val manager = GlanceAppWidgetManager(appContext)
            manager.getGlanceIds(ExtLocationWithForecastGraphGlanceWidget::class.java).forEach { ExtLocationWithForecastGraphGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(ExtLocationWithForecastGlanceWidget::class.java).forEach { ExtLocationWithForecastGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(ExtLocationWithGraphGlanceWidget::class.java).forEach { ExtLocationWithGraphGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(ExtLocationGlanceWidget::class.java).forEach { ExtLocationGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(WeatherForecastGlanceWidget::class.java).forEach { WeatherForecastGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(WeatherGraphGlanceWidget::class.java).forEach { WeatherGraphGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(LessGlanceWidget::class.java).forEach { LessGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(MoreGlanceWidget::class.java).forEach { MoreGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(CompactGlanceWidget::class.java).forEach { CompactGlanceWidget().update(appContext, it) }
            manager.getGlanceIds(TemperatureGlanceWidget::class.java).forEach { TemperatureGlanceWidget().update(appContext, it) }
        } catch (e: Exception) {
            // ignore
        }
    }
}
