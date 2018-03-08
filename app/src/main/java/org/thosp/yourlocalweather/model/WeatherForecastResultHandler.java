package org.thosp.yourlocalweather.model;

public interface WeatherForecastResultHandler {
    void processResources(CompleteWeatherForecast completeWeatherForecast, long lastUpdate);
    void processError(Exception e);
}
