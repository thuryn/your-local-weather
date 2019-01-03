package org.thosp.yourlocalweather.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.thosp.yourlocalweather.MainActivity;

public enum WidgetActions {
    MAIN_SCREEN(1), FORECAST_SCREEN(2), GRAPHS_SCREEN(3), LOCATION_SWITCH(4);

    private long id;

    WidgetActions(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public int getComboSelection() {
        return (int) (id - 1);
    }

    public static WidgetActions getByComboSelection(int selection) {
        switch (selection) {
            case 0: return MAIN_SCREEN;
            case 1: return FORECAST_SCREEN;
            case 2: return GRAPHS_SCREEN;
            case 3: return LOCATION_SWITCH;
        }
        return MAIN_SCREEN;
    }

    public static WidgetActions getById(Long id, String settingName) {
        if (id == null) {
            switch (settingName) {
                case "action_city": return LOCATION_SWITCH;
                case "action_current_weather_icon": return MAIN_SCREEN;
                case "action_forecast": return FORECAST_SCREEN;
                case "action_graph": return GRAPHS_SCREEN;
            }
            return MAIN_SCREEN;
        }
        if (id == 1) {
            return MAIN_SCREEN;
        } else if (id == 2) {
            return FORECAST_SCREEN;
        } else if (id == 3) {
            return GRAPHS_SCREEN;
        } else if (id == 4) {
            return LOCATION_SWITCH;
        } else {
            return MAIN_SCREEN;
        }
    }
}
