package org.thosp.yourlocalweather.widget.glance

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.thosp.yourlocalweather.WidgetSettingsDialogue
import org.thosp.yourlocalweather.utils.Constants

class ExtLocationWithForecastGraphGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ExtLocationWithForecastGraphGlanceWidget()

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
                    arrayListOf("action_city", "action_current_weather_icon", "action_forecast", "action_graph")
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
                    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(ExtLocationWithForecastGraphGlanceWidget::class.java)
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
