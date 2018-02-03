package org.thosp.yourlocalweather.model;

import java.util.List;

public interface WeatherForecastResultHandler {
    void processResources(List<DetailedWeatherForecast> weatherForecastList);
    void processError(Exception e);
}
