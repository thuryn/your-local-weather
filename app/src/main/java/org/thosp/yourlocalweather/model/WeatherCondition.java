package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

public class WeatherCondition implements Parcelable {

    private Integer weatherId;
    private String icon;
    private String description;

    public WeatherCondition(Integer weatherId, String icon, String description) {
        this.weatherId = weatherId;
        this.icon = icon;
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public Integer getWeatherId() {
        return weatherId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(weatherId);
        parcel.writeString(icon);
        parcel.writeString(description);
    }

    public static final Parcelable.Creator<WeatherCondition> CREATOR
            = new Parcelable.Creator<WeatherCondition>() {
        public WeatherCondition createFromParcel(Parcel in) {
            return new WeatherCondition(in);
        }

        public WeatherCondition[] newArray(int size) {
            return new WeatherCondition[size];
        }
    };

    private WeatherCondition(Parcel in) {
        weatherId = in.readInt();
        icon = in.readString();
        description = in.readString();
    }
}
