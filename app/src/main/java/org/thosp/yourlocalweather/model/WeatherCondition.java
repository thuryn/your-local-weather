package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.thosp.yourlocalweather.WmoCodes;

public class WeatherCondition implements Parcelable {

    private final Integer weatherId;
    private final String icon;
    private final String description;

    public WeatherCondition(WmoCodes wmoCodes) {
        this.weatherId = wmoCodes.getId();
        this.description = wmoCodes.getDescription();
        this.icon = wmoCodes.getIconId();
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
