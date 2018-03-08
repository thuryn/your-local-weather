package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Weather implements Parcelable {

    private float temperature;
    private float lon;
    private float lat;
    private float windSpeed;
    private float windDirection;
    private float pressure;
    private int humidity;
    private int clouds;
    private long sunrise;
    private long sunset;
    private List<CurrentWeather> currentWeathers = new ArrayList<>();

    public Weather() {
        super();
    }

    public void addCurrentWeather(Integer id, String description, String iconId) {
        currentWeathers.add(new CurrentWeather(id, description, iconId));
    }

    public List<CurrentWeather> getCurrentWeathers() {
        return currentWeathers;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(float windSpeed) {
        this.windSpeed = windSpeed;
    }

    public float getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(float windDirection) {
        this.windDirection = windDirection;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getClouds() {
        return clouds;
    }

    public void setClouds(int clouds) {
        this.clouds = clouds;
    }

    public long getSunrise() {
        return sunrise;
    }

    public void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public long getSunset() {
        return sunset;
    }

    public void setSunset(long sunset) {
        this.sunset = sunset;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(temperature);
        parcel.writeFloat(lon);
        parcel.writeFloat(lat);
        parcel.writeFloat(windSpeed);
        parcel.writeFloat(windDirection);
        parcel.writeFloat(pressure);
        parcel.writeInt(humidity);
        parcel.writeInt(clouds);
        parcel.writeLong(sunrise);
        parcel.writeLong(sunset);
        parcel.writeTypedList(currentWeathers);
    }

    public static final Parcelable.Creator<Weather> CREATOR
            = new Parcelable.Creator<Weather>() {
        public Weather createFromParcel(Parcel in) {
            return new Weather(in);
        }

        public Weather[] newArray(int size) {
            return new Weather[size];
        }
    };

    private Weather(Parcel in) {
        temperature = in.readFloat();
        lon = in.readFloat();
        lat = in.readFloat();
        windSpeed = in.readFloat();
        windDirection = in.readFloat();
        pressure = in.readFloat();
        humidity = in.readInt();
        clouds = in.readInt();
        sunrise = in.readLong();
        sunset = in.readLong();
        in.readTypedList(currentWeathers, CurrentWeather.CREATOR);
    }
}
