package org.thosp.yourlocalweather.model;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;

public class Location implements Parcelable {

    private long id;
    private Address address;
    private double longitude;
    private double latitude;
    private int orderId;
    private String locale;
    private String nickname;
    private float accuracy;
    private long lastLocationUpdate;
    private String locationSource;
    private boolean addressFound;
    private boolean enabled;

    public Location(long id,
                    int orderId,
                    String nickname,
                    String locale,
                    double longitude,
                    double latitude,
                    float accuracy,
                    String locationSource,
                    long lastLocationUpdate,
                    boolean addressFound,
                    boolean enabled,
                    Address address) {
        this.id = id;
        this.address = address;
        this.orderId = orderId;
        this.nickname = nickname;
        this.locale = locale;
        this.longitude = longitude;
        this.latitude = latitude;
        this.accuracy = accuracy;
        this.locationSource = locationSource;
        this.lastLocationUpdate = lastLocationUpdate;
        this.addressFound = addressFound;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getLocale() {
        return locale;
    }

    public String getNickname() {
        return nickname;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public long getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public boolean isAddressFound() {
        return addressFound;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeInt(orderId);
        parcel.writeString(locale);
        parcel.writeString(nickname);
        parcel.writeFloat(accuracy);
        parcel.writeString(locationSource);
        parcel.writeLong(lastLocationUpdate);
        parcel.writeParcelable(address, 0);
    }

    public static final Parcelable.Creator<Location> CREATOR
            = new Parcelable.Creator<Location>() {
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    private Location(Parcel in) {
        id = in.readLong();
        latitude = in.readDouble();
        longitude = in.readDouble();
        orderId = in.readInt();
        locale = in.readString();
        nickname = in.readString();
        accuracy = in.readFloat();
        locationSource = in.readString();
        lastLocationUpdate = in.readLong();
        address = in.readParcelable(Address.class.getClassLoader());
    }
}
