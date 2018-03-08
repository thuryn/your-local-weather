package org.thosp.yourlocalweather.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CurrentWeather implements Parcelable {

    private String mDescription;
    private String mIdIcon;
    private Integer mWeatherId;

    public CurrentWeather(Integer id, String description, String iconId) {
        this.mWeatherId = id;
        this.mDescription = description;
        this.mIdIcon = iconId;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description.substring(0, 1).toUpperCase() + description.substring(1);
    }

    public String getIdIcon() {
        return mIdIcon;
    }

    public void setIdIcon(String idIcon) {
        mIdIcon = idIcon;
    }

    public Integer getWeatherId() {
        return mWeatherId;
    }

    public void setWeatherId(Integer mWeatherId) {
        this.mWeatherId = mWeatherId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mWeatherId);
        parcel.writeString(mDescription);
        parcel.writeString(mIdIcon);
    }

    public static final Parcelable.Creator<CurrentWeather> CREATOR
            = new Parcelable.Creator<CurrentWeather>() {
        public CurrentWeather createFromParcel(Parcel in) {
            return new CurrentWeather(in);
        }

        public CurrentWeather[] newArray(int size) {
            return new CurrentWeather[size];
        }
    };

    private CurrentWeather(Parcel in) {
        mWeatherId = in.readInt();
        mDescription = in.readString();
        mIdIcon = in.readString();
    }
}
