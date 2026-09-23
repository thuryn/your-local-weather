package org.thosp.yourlocalweather.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.thosp.yourlocalweather.WidgetSettingsDialogue
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.LocationsDbHelper
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper
import org.thosp.yourlocalweather.utils.AppPreference
import org.thosp.yourlocalweather.utils.Constants
import org.thosp.yourlocalweather.utils.ForecastUtil

class ExtLocationWithForecastGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExtLocationWithForecastGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        if (action == Constants.ACTION_APPWIDGET_SETTINGS_OPENED) {
            val widgetId = intent.getIntExtra("widgetId", 0)
            val settingsName = intent.getStringExtra("settingName")
            val popUpIntent = Intent(context, WidgetSettingsDialogue::class.java).apply {
                putExtra("widgetId", widgetId)
                if (settingsName != null) {
                    putExtra("settings_option", settingsName)
                }
                putStringArrayListExtra(
                    "widget_action_places",
                    arrayListOf("action_city", "action_current_weather_icon", "action_forecast")
                )
            }
            popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(popUpIntent)
        } else if (action == Constants.ACTION_APPWIDGET_CHANGE_SETTINGS ||
            action == Constants.ACTION_FORCED_APPWIDGET_UPDATE ||
            action == Constants.ACTION_APPWIDGET_THEME_CHANGED ||
            action == Intent.ACTION_LOCALE_CHANGED
        ) {
            MainScope().launch {
                try {
                    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ExtLocationWithForecastGlanceWidget::class.java)
                    glanceIds.forEach { glanceId ->
                        glanceAppWidget.update(context, glanceId)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
}

class ExtLocationWithForecastGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val appWidgetId = GlanceAppWidgetManager(appContext).getAppWidgetId(id)

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

        val temperatureUnit = AppPreference.getTemperatureUnitFromPreferences(appContext)
        val windUnit = AppPreference.getWindUnitFromPreferences(appContext)
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
                GlanceHeaderBar(
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

                GlanceCurrentWeatherSection(
                    context = context,
                    currentLocation = currentLocation,
                    weatherRecord = weatherRecord,
                    temperatureUnit = temperatureUnit,
                    windUnit = windUnit,
                    textColor = textColor,
                    fontBasedIcons = fontBasedIcons
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

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
            }
        }
    }
}
