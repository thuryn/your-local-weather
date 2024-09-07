package org.thosp.yourlocalweather.widget;

public enum WidgetSettingName {

    FORECAST_SETTINGS("forecastSettings", 1),
    GRAPH_SETTING("graphSetting", 2),
    DETAILS_SETTING("detailsSetting", 3),
    LOCATION_SETTINGS("locationSettings", 4),
    WIDGET_ACTION_SETTINGS("widgetActionSettings", 5);

    private final int settingNameId;
    private final String widgetSettingName;

    WidgetSettingName(String widgetSettingName, int settingNameId) {
        this.settingNameId = settingNameId;
        this.widgetSettingName = widgetSettingName;
    }

    public int getSettingNameId() {
        return settingNameId;
    }

    public String getWidgetSettingName() {
        return widgetSettingName;
    }
}
