package org.thosp.yourlocalweather.widget.glance

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import org.thosp.yourlocalweather.GraphsActivity
import org.thosp.yourlocalweather.MainActivity
import org.thosp.yourlocalweather.R
import org.thosp.yourlocalweather.WeatherForecastActivity
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.LocationsDbHelper
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper
import org.thosp.yourlocalweather.utils.AppPreference
import org.thosp.yourlocalweather.utils.Constants
import org.thosp.yourlocalweather.utils.ForecastUtil
import org.thosp.yourlocalweather.utils.GraphUtils
import org.thosp.yourlocalweather.utils.TemperatureUtil
import org.thosp.yourlocalweather.utils.Utils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExtLocationWithForecastGraphGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val appWidgetId = GlanceAppWidgetManager(appContext).getAppWidgetId(id)

        // Load data from DBs and preferences
        val widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(appContext)
        val locationsDbHelper = LocationsDbHelper.getInstance(appContext)
        val currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(appContext)
        val weatherForecastDbHelper = WeatherForecastDbHelper.getInstance(appContext)

        val locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId")
        val currentLocation: Location? = if (locationId != null) {
            locationsDbHelper.getLocationById(locationId)
        } else {
            val loc0 = locationsDbHelper.getLocationByOrderId(0)
            if (loc0 != null && !loc0.isEnabled) locationsDbHelper.getLocationByOrderId(1) else loc0
        }

        val weatherRecord = currentLocation?.let { currentWeatherDbHelper.getWeather(it.id) }
        val weatherForecastRecord = currentLocation?.let { weatherForecastDbHelper.getWeatherForecast(it.id) }

        // Preferences
        val temperatureUnit = AppPreference.getTemperatureUnitFromPreferences(appContext)
        val pressureUnit = AppPreference.getPressureUnitFromPreferences(appContext)
        val windUnit = AppPreference.getWindUnitFromPreferences(appContext)
        val rainSnowUnit = AppPreference.getRainSnowUnitFromPreferences(appContext)
        val timeStyle = AppPreference.getTimeStylePreference(appContext)
        val textColorInt = AppPreference.getWidgetTextColor(appContext)
        val backgroundColorInt = AppPreference.getWidgetBackgroundColor(appContext)
        val headerColorInt = AppPreference.getWindowHeaderBackgroundColorId(appContext)
        val fontBasedIcons = "weather_icon_set_fontbased" == AppPreference.getIconSet(appContext)
        val forecastDayAbbrev = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "forecast_day_abbrev") ?: false
        val hoursForecast = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "hoursForecast") ?: false

        val textColor = Color(textColorInt)
        val backgroundColor = Color(backgroundColorInt)
        val headerColor = Color(headerColorInt)

        // Graph Bitmap Generation
        val showLegend = widgetSettingsDbHelper.getParamBoolean(appWidgetId, "combinedGraphShowLegend")
        val combinedGraphValuesFromPreferences = AppPreference.getCombinedGraphValues(appContext)
        val combinedGraphValuesFromSettings = GraphUtils.getCombinedGraphValuesFromSettings(
            combinedGraphValuesFromPreferences,
            widgetSettingsDbHelper,
            appWidgetId
        )
        val gridColor = AppPreference.getWidgetGraphGridColor(appContext)
        val nativeScaled = AppPreference.isWidgetGraphNativeScaled(appContext)

        var graphBitmap: Bitmap? = null
        if (currentLocation != null && weatherForecastRecord?.completeWeatherForecast != null) {
            try {
                graphBitmap = GraphUtils.getCombinedChart(
                    appContext,
                    appWidgetId,
                    0.4f,
                    weatherForecastRecord.completeWeatherForecast.weatherForecastList,
                    currentLocation.id,
                    currentLocation.locale,
                    showLegend,
                    combinedGraphValuesFromSettings,
                    textColorInt,
                    backgroundColorInt,
                    gridColor,
                    temperatureUnit,
                    pressureUnit,
                    rainSnowUnit,
                    nativeScaled,
                    windUnit
                )
            } catch (e: Exception) {
                // Ignore graph errors
            }
        }

        // Daily Forecast Calculation
        val forecastDays = if (currentLocation != null && weatherForecastRecord != null) {
            ForecastUtil.calculateWeatherForDays(appContext, weatherForecastRecord)
                ?.sortedBy { it.dayIndex } ?: emptyList()
        } else {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = appContext,
                    appWidgetId = appWidgetId,
                    currentLocation = currentLocation,
                    weatherRecord = weatherRecord,
                    weatherForecastRecord = weatherForecastRecord,
                    forecastDays = forecastDays,
                    graphBitmap = graphBitmap,
                    temperatureUnit = temperatureUnit,
                    windUnit = windUnit,
                    timeStyle = timeStyle,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    headerColor = headerColor,
                    fontBasedIcons = fontBasedIcons,
                    forecastDayAbbrev = forecastDayAbbrev,
                    hoursForecast = hoursForecast
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        appWidgetId: Int,
        currentLocation: Location?,
        weatherRecord: CurrentWeatherDbHelper.WeatherRecord?,
        weatherForecastRecord: WeatherForecastDbHelper.WeatherForecastRecord?,
        forecastDays: List<ForecastUtil.WeatherForecastPerDay>,
        graphBitmap: Bitmap?,
        temperatureUnit: String,
        windUnit: String,
        timeStyle: String,
        textColor: Color,
        backgroundColor: Color,
        headerColor: Color,
        fontBasedIcons: Boolean,
        forecastDayAbbrev: Boolean,
        hoursForecast: Boolean
    ) {
        val size = LocalSize.current
        val availableWidth = size.width

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .cornerRadius(16.dp)
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                // 1. Header Bar (City Name & Last Update)
                HeaderBar(
                    context = context,
                    appWidgetId = appWidgetId,
                    currentLocation = currentLocation,
                    weatherRecord = weatherRecord,
                    weatherForecastRecord = weatherForecastRecord,
                    timeStyle = timeStyle,
                    textColor = textColor,
                    headerColor = headerColor
                )

                Spacer(modifier = GlanceModifier.height(6.dp))

                // 2. Current Weather Info (Temp, Description, Wind, Humidity, Icon)
                CurrentWeatherSection(
                    context = context,
                    currentLocation = currentLocation,
                    weatherRecord = weatherRecord,
                    temperatureUnit = temperatureUnit,
                    windUnit = windUnit,
                    textColor = textColor,
                    fontBasedIcons = fontBasedIcons
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                // 3. RESPONSIVE Forecast Row (Days vs Hours)
                if (currentLocation != null) {
                    GlanceResponsiveForecastSection(
                        context = context,
                        weatherForecastRecord = weatherForecastRecord,
                        forecastDays = forecastDays,
                        currentLocation = currentLocation,
                        availableWidthDp = availableWidth.value,
                        temperatureUnit = temperatureUnit,
                        textColor = textColor,
                        fontBasedIcons = fontBasedIcons,
                        forecastDayAbbrev = forecastDayAbbrev,
                        hoursForecast = hoursForecast
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // 4. Combined Weather Graph
                if (graphBitmap != null) {
                    val graphIntent = Intent(context, GraphsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    Image(
                        provider = ImageProvider(graphBitmap),
                        contentDescription = "Weather Graph",
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .clickable(actionStartActivity(graphIntent))
                    )
                }
            }
        }
    }

    @Composable
    private fun HeaderBar(
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
    private fun CurrentWeatherSection(
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
                        style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 12.sp)
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
}
