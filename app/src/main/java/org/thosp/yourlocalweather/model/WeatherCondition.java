package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

public class WeatherCondition implements Parcelable {

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
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
        icon = in.readString();
        description = in.readString();
    }
}
