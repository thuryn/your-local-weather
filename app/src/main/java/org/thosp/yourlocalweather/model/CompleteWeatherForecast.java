package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class CompleteWeatherForecast implements Parcelable {

    List<DetailedWeatherForecast> mWeatherForecastList = new ArrayList<>();

    public CompleteWeatherForecast() {
    }

    public void addDetailedWeatherForecast(DetailedWeatherForecast detailedWeatherForecast) {
        mWeatherForecastList.add(detailedWeatherForecast);
    }

    public List<DetailedWeatherForecast> getWeatherForecastList() {
        return mWeatherForecastList;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(mWeatherForecastList);
    }

    public static final Parcelable.Creator<CompleteWeatherForecast> CREATOR
            = new Parcelable.Creator<CompleteWeatherForecast>() {
        public CompleteWeatherForecast createFromParcel(Parcel in) {
            return new CompleteWeatherForecast(in);
        }

        public CompleteWeatherForecast[] newArray(int size) {
            return new CompleteWeatherForecast[size];
        }
    };

    private CompleteWeatherForecast(Parcel in) {
        in.readTypedList(mWeatherForecastList, DetailedWeatherForecast.CREATOR);
    }
}
