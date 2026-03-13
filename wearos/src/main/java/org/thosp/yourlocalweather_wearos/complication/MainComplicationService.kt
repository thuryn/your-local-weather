package org.thosp.yourlocalweather_wearos.complication

import org.json.JSONObject

class MainComplicationService : AbstractTemperatureComplication() {
    override fun getUnit(json: JSONObject): String {
        return json.getString("temperatureUnit")
    }

    override fun getTemperatureFontSize(tempText: String, canvasSize: Int): Float {
        if (tempText.length > 4) {
            return canvasSize / 2.5f
        } else {
            return canvasSize / 2f
        }
    }
}
