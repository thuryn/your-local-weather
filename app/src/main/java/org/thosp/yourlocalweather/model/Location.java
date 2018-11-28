package org.thosp.yourlocalweather.model;

import android.location.Address;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.Locale;

public class Location implements Parcelable {

    private long id;
    private Address address;
    private double longitude;
    private double latitude;
    private int orderId;
    private String localeAbbrev;
    private String nickname;
    private float accuracy;
    private long lastLocationUpdate;
    private String locationSource;
    private boolean addressFound;
    private boolean enabled;
    private Locale locale;

    public Location(long id,
                    int orderId,
                    String nickname,
                    String localeAbbrev,
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
        if (localeAbbrev == null) {
            this.locale = Locale.getDefault();
            this.localeAbbrev = this.locale.getLanguage();
        } else {
            this.localeAbbrev = localeAbbrev;
            this.locale = new Locale(localeAbbrev);
        }
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

    public Locale getLocale() {
        return locale;
    }

    public String getLocaleAbbrev() {
        return localeAbbrev;
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
        parcel.writeString(localeAbbrev);
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
        localeAbbrev = in.readString();
        nickname = in.readString();
        accuracy = in.readFloat();
        locationSource = in.readString();
        lastLocationUpdate = in.readLong();
        address = in.readParcelable(Address.class.getClassLoader());
        locale = new Locale(localeAbbrev);
    }

    public Location(PersistableBundle persistentBundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            id = persistentBundle.getLong("id");
            latitude = persistentBundle.getDouble("latitude");
            longitude = persistentBundle.getDouble("longitude");
            orderId = persistentBundle.getInt("orderId");
            localeAbbrev = persistentBundle.getString("locale");
            locale = new Locale(localeAbbrev);
            nickname = persistentBundle.getString("nickname");;
            accuracy = new Double(persistentBundle.getDouble("accuracy")).floatValue();
            locationSource = persistentBundle.getString("locationSource");
            lastLocationUpdate = persistentBundle.getLong("lastLocationUpdate");
            address = PersistableBundleBuilder.toAddress(persistentBundle.getPersistableBundle("address"));
        }
    }

    public PersistableBundle getPersistableBundle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PersistableBundle persistableBundle = new PersistableBundle();
            persistableBundle.putLong("id", id);
            persistableBundle.putDouble("latitude", latitude);
            persistableBundle.putDouble("longitude", longitude);
            persistableBundle.putInt("orderId", orderId);
            persistableBundle.putString("locale", localeAbbrev);
            persistableBundle.putString("nickname", nickname);
            persistableBundle.putDouble("accuracy", new Double(accuracy));
            persistableBundle.putString("locationSource", locationSource);
            persistableBundle.putLong("lastLocationUpdate", lastLocationUpdate);
            persistableBundle.putPersistableBundle("address", PersistableBundleBuilder.fromAddress(address));
            return persistableBundle;
        } else {
            return null;
        }
    }
}
