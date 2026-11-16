package org.thosp.yourlocalweather.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.thosp.yourlocalweather.MainActivity
import org.thosp.yourlocalweather.WidgetSettingsDialogue
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.LocationsDbHelper
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper
import org.thosp.yourlocalweather.utils.AppPreference
import org.thosp.yourlocalweather.utils.Constants

class TemperatureGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TemperatureGlanceWidget()

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
                    arrayListOf("action_city", "action_current_weather_icon")
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
                    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(TemperatureGlanceWidget::class.java)
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

class TemperatureGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val appWidgetId = GlanceAppWidgetManager(appContext).getAppWidgetId(id)

        val widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(appContext)
        val locationsDbHelper = LocationsDbHelper.getInstance(appContext)
        val currentWeatherDbHelper = CurrentWeatherDbHelper.getInstance(appContext)

        val locationId = widgetSettingsDbHelper.getParamLong(appWidgetId, "locationId")
        val currentLocation: Location? = if (locationId != null) {
            locationsDbHelper.getLocationById(locationId)
        } else {
            locationsDbHelper.getLocationWithFallback(0)
        }

        val weatherRecord = currentLocation?.let { currentWeatherDbHelper.getWeather(it.id) }
        val temperatureUnit = AppPreference.getTemperatureUnitFromPreferences(appContext)
        val textColorInt = AppPreference.getWidgetTextColor(appContext)
        val backgroundColorInt = AppPreference.getWidgetBackgroundColor(appContext)

        val textColor = Color(textColorInt)
        val backgroundColor = Color(backgroundColorInt)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = appContext,
                    currentLocation = currentLocation,
                    weatherRecord = weatherRecord,
                    temperatureUnit = temperatureUnit,
                    textColor = textColor,
                    backgroundColor = backgroundColor
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        currentLocation: Location?,
        weatherRecord: CurrentWeatherDbHelper.WeatherRecord?,
        temperatureUnit: String,
        textColor: Color,
        backgroundColor: Color
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val tempText = GlanceWeatherHelper.getTemperatureText(context, currentLocation, weatherRecord, temperatureUnit)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(mainIntent)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tempText,
                style = TextStyle(
                    color = ColorProvider(day = textColor, night = textColor),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(4.dp)
            )
        }
    }
}
