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
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.thosp.yourlocalweather.MainActivity
import org.thosp.yourlocalweather.R
import org.thosp.yourlocalweather.WidgetSettingsDialogue
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper
import org.thosp.yourlocalweather.model.Location
import org.thosp.yourlocalweather.model.LocationsDbHelper
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper
import org.thosp.yourlocalweather.utils.AppPreference
import org.thosp.yourlocalweather.utils.Constants
import org.thosp.yourlocalweather.utils.Utils

class LessGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LessGlanceWidget()

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
                    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(LessGlanceWidget::class.java)
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

class LessGlanceWidget : GlanceAppWidget() {

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
            val loc0 = locationsDbHelper.getLocationByOrderId(0)
            if (loc0 != null && !loc0.isEnabled) locationsDbHelper.getLocationByOrderId(1) else loc0
        }

        val weatherRecord = currentLocation?.let { currentWeatherDbHelper.getWeather(it.id) }

        val temperatureUnit = AppPreference.getTemperatureUnitFromPreferences(appContext)
        val textColorInt = AppPreference.getWidgetTextColor(appContext)
        val backgroundColorInt = AppPreference.getWidgetBackgroundColor(appContext)
        val fontBasedIcons = "weather_icon_set_fontbased" == AppPreference.getIconSet(appContext)

        val textColor = Color(textColorInt)
        val backgroundColor = Color(backgroundColorInt)

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = appContext,
                    appWidgetId = appWidgetId,
                    currentLocation = currentLocation,
                    weatherRecord = weatherRecord,
                    temperatureUnit = temperatureUnit,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    fontBasedIcons = fontBasedIcons
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
        temperatureUnit: String,
        textColor: Color,
        backgroundColor: Color,
        fontBasedIcons: Boolean
    ) {
        val cityName = if (currentLocation != null) Utils.getCityAndCountry(context, currentLocation) else context.getString(R.string.location_not_found)
        val tempText = GlanceWeatherHelper.getTemperatureText(context, currentLocation, weatherRecord, temperatureUnit)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

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

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundColor)
                .cornerRadius(12.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cityName,
                        style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(actionStartActivity(mainIntent))
                    )
                    Text(
                        text = tempText,
                        style = TextStyle(color = ColorProvider(day = textColor, night = textColor), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        modifier = GlanceModifier.clickable(actionStartActivity(mainIntent))
                    )
                }
                Image(
                    provider = weatherIconProvider,
                    contentDescription = "Weather Icon",
                    modifier = GlanceModifier
                        .width(36.dp)
                        .height(36.dp)
                        .clickable(actionStartActivity(mainIntent))
                )
            }
        }
    }
}
