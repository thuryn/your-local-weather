package org.thosp.yourlocalweather.widget;

import org.thosp.yourlocalweather.GraphsActivity;
import org.thosp.yourlocalweather.MainActivity;
import org.thosp.yourlocalweather.WeatherForecastActivity;

public enum WidgetActions {
    MAIN_SCREEN(1, MainActivity.class),
    FORECAST_SCREEN(2, WeatherForecastActivity.class),
    GRAPHS_SCREEN(3, GraphsActivity.class),
    LOCATION_SWITCH(4);

    private final long id;
    private Class activityClass;

    WidgetActions(long id, Class activityClass) {
        this.activityClass = activityClass;
        this.id = id;
    }

    WidgetActions(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public int getComboSelection() {
        return (int) (id - 1);
    }

    public Class getActivityClass() {
        return activityClass;
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
