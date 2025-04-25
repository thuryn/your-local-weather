package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

public class DetailedWeatherForecastWithDetails extends DetailedWeatherForecast implements Parcelable {

    String weatherDescription;
    String dateTimeTxt;

    public DetailedWeatherForecastWithDetails() {
        super();
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        this.weatherDescription = weatherDescription;
    }

    public String getDateTimeTxt() {
        return dateTimeTxt;
    }

    public void setDateTimeTxt(String dateTimeTxt) {
        this.dateTimeTxt = dateTimeTxt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(dateTime);
        parcel.writeDouble(temperatureMin);
        parcel.writeDouble(temperatureMax);
        parcel.writeDouble(temperature);
        parcel.writeDouble(pressure);
        parcel.writeInt(humidity);
        parcel.writeDouble(windSpeed);
        parcel.writeDouble(windDegree);
        parcel.writeInt(cloudiness);
        parcel.writeDouble(rain);
        parcel.writeDouble(snow);
        parcel.writeInt(weatherId);
        parcel.writeString(weatherDescription);
        parcel.writeString(dateTimeTxt);
    }

    public static final Creator<DetailedWeatherForecastWithDetails> CREATOR
            = new Creator<DetailedWeatherForecastWithDetails>() {
        public DetailedWeatherForecastWithDetails createFromParcel(Parcel in) {
            return new DetailedWeatherForecastWithDetails(in);
        }

        public DetailedWeatherForecastWithDetails[] newArray(int size) {
            return new DetailedWeatherForecastWithDetails[size];
        }
    };

    private DetailedWeatherForecastWithDetails(Parcel in) {
        dateTime = in.readLong();
        temperatureMin = in.readDouble();
        temperatureMax = in.readDouble();
        temperature = in.readDouble();
        pressure = in.readDouble();
        humidity = in.readInt();
        windSpeed = in.readDouble();
        windDegree = in.readDouble();
        cloudiness = in.readInt();
        rain = in.readDouble();
        snow = in.readDouble();
        weatherId = in.readInt();
        weatherDescription = in.readString();
        dateTimeTxt = in.readString();
    }
}