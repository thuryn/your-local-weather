package org.thosp.yourlocalweather.service;

import androidx.annotation.NonNull;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.Weather;

import java.io.Serializable;

public class WeatherByVoiceRequestDataHolder implements Serializable {
    private final long timeNow;
    private final Weather weather;
    private final Location location;
    private final long timestamp;

    public WeatherByVoiceRequestDataHolder(Location location, Weather weather, long timeNow) {
        this.location = location;
        this.weather = weather;
        this.timeNow = timeNow;
        this.timestamp = System.currentTimeMillis();
    }

    public Weather getWeather() {
        return weather;
    }

    public Location getLocation() {
        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof WeatherByVoiceRequestDataHolder)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WeatherByVoiceRequestDataHolder objToCompareTo = (WeatherByVoiceRequestDataHolder) obj;
        return (this.timeNow == objToCompareTo.timeNow) &&
               (((this.location == null) && (objToCompareTo.getLocation() == null)) ||
                ((this.weather != null) && (this.weather.equals(objToCompareTo.getWeather())))
        );
    }

    @Override
    public int hashCode() {
        return location.getOrderId();
    }
    
    @NonNull
    @Override
    public String toString() {
        return "WeatherByVoiceRequestDataHolder:location=" + location + ", weather="
                + weather + ", timeNow=" + timeNow;
    }
}
