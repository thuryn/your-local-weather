package org.thosp.yourlocalweather.model;

import android.location.Address;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.Locale;

public class LicenseKey implements Parcelable {

    private long id;
    private String requestUri;
    private String initialLicense;
    private String token;
    private Long lastCallTimeInMs;

    public LicenseKey(long id,
                      String requestUri,
                      String initialLicense,
                      String token,
                      Long lastCallTimeInMs) {
        this.id = id;
        this.requestUri = requestUri;
        this.initialLicense = initialLicense;
        this.token = token;
        this.lastCallTimeInMs = lastCallTimeInMs;
    }

    public Long getId() {
        return id;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getInitialLicense() {
        return initialLicense;
    }

    public void setInitialLicense(String initialLicense) {
        this.initialLicense = initialLicense;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getLastCallTimeInMs() {
        return lastCallTimeInMs;
    }

    public void setLastCallTimeInMs(Long lastCallTimeInMs) {
        this.lastCallTimeInMs = lastCallTimeInMs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(requestUri);
        parcel.writeString(initialLicense);
        parcel.writeString(token);
        parcel.writeLong(lastCallTimeInMs);
    }

    public static final Creator<LicenseKey> CREATOR
            = new Creator<LicenseKey>() {
        public LicenseKey createFromParcel(Parcel in) {
            return new LicenseKey(in);
        }

        public LicenseKey[] newArray(int size) {
            return new LicenseKey[size];
        }
    };

    private LicenseKey(Parcel in) {
        id = in.readLong();
        requestUri = in.readString();
        initialLicense = in.readString();
        token = in.readString();
        lastCallTimeInMs = in.readLong();
    }

    public LicenseKey(PersistableBundle persistentBundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            id = persistentBundle.getLong("id");
            requestUri = persistentBundle.getString("requestUri");
            initialLicense = persistentBundle.getString("initialLicense");
            token = persistentBundle.getString("token");
            lastCallTimeInMs = persistentBundle.getLong("lastCallTimeInMs");
        }
    }

    public PersistableBundle getPersistableBundle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PersistableBundle persistableBundle = new PersistableBundle();
            persistableBundle.putLong("id", id);
            persistableBundle.putString("requestUri", requestUri);
            persistableBundle.putString("initialLicense", initialLicense);
            persistableBundle.putString("token", token);
            persistableBundle.putLong("lastCallTimeInMs", lastCallTimeInMs);
            return persistableBundle;
        } else {
            return null;
        }
    }
}
