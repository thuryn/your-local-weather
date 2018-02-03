package org.thosp.yourlocalweather.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DetailedWeatherForecast implements Serializable {

    private long dateTime;
    private double temperatureMin;
    private double temperatureMax;
    private double temperature;
    private double pressure;
    private int humidity;
    private double windSpeed;
    private double windDegree;
    private int cloudiness;
    private double rain;
    private double snow;
    private List<WeatherCondition> weatherConditions = new ArrayList<>();

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getWindDegree() {
        return windDegree;
    }

    public void setWindDegree(double windDegree) {
        this.windDegree = windDegree;
    }

    public int getCloudiness() {
        return cloudiness;
    }

    public void setCloudiness(int cloudiness) {
        this.cloudiness = cloudiness;
    }

    public double getRain() {
        return rain;
    }

    public void setRain(double rain) {
        this.rain = rain;
    }

    public double getSnow() {
        return snow;
    }

    public void setSnow(double snow) {
        this.snow = snow;
    }

    public double getTemperatureMin() {
        return temperatureMin;
    }

    public void setTemperatureMin(double temperatureMin) {
        this.temperatureMin = temperatureMin;
    }

    public double getTemperatureMax() {
        return temperatureMax;
    }

    public void setTemperatureMax(double temperatureMax) {
        this.temperatureMax = temperatureMax;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void addWeatherCondition(String icon, String description) {
        weatherConditions.add(new WeatherCondition(icon, description));
    }

    public WeatherCondition getFirstWeatherCondition() {
        if (weatherConditions.isEmpty()) {
            return null;
        }
        return weatherConditions.get(0);
    }

    public class WeatherCondition {
        private String icon;
        private String description;

        public WeatherCondition(String icon, String description) {
            this.icon = icon;
            this.description = description;
        }

        public String getIcon() {
            return icon;
        }

        public String getDescription() {
            return description;
        }
    }
}