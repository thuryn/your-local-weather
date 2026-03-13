package org.thosp.yourlocalweather_wearos.complication

import org.json.JSONObject

class NoUnitComplicationService : AbstractTemperatureComplication() {
    override fun getUnit(json: JSONObject): String {
        return ""
    }

    override fun getTemperatureFontSize(tempText: String, canvasSize: Int): Float {
        if (tempText.length > 2) {
            return canvasSize / 2f
        } else {
            return canvasSize / 1.5f
        }
    }
}
