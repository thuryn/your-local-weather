package org.thosp.yourlocalweather.model;

import android.os.BaseBundle;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import java.util.Locale;

public class PersistableBundleBuilder {

    public static android.location.Location toLocation(PersistableBundle persistableBundle) {
        if (persistableBundle == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String provider = persistableBundle.getString("provider");
            android.location.Location location = new android.location.Location(provider);
            location.setLatitude(persistableBundle.getDouble("latitude"));
            location.setLongitude(persistableBundle.getDouble("longitude"));
            location.setAccuracy(new Double(persistableBundle.getDouble("accuracy")).floatValue());
            return location;
        } else {
            return null;
        }
    }

    public static PersistableBundle fromLocation(android.location.Location location) {
        if (location == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PersistableBundle persistableBundle = new PersistableBundle();
            persistableBundle.putDouble("latitude", location.getLatitude());
            persistableBundle.putDouble("latitude", location.getLatitude());
            persistableBundle.putDouble("accuracy", location.getAccuracy());
            persistableBundle.putString("provider", location.getProvider());
            return persistableBundle;
        } else {
            return null;
        }
    }

    public static android.location.Address toAddress(PersistableBundle persistableBundle) {
        if (persistableBundle == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String language = persistableBundle.getString("language");
            String country = persistableBundle.getString("country");
            String variant = persistableBundle.getString("variant");
            Locale addressLocale = new Locale(language, country, variant);
            android.location.Address address = new android.location.Address(addressLocale);
            address.setLocality(persistableBundle.getString("locality"));
            address.setSubLocality(persistableBundle.getString("subLocality"));
            address.setAdminArea(persistableBundle.getString("adminArea"));
            address.setSubAdminArea(persistableBundle.getString("subAdminArea"));
            address.setCountryName(persistableBundle.getString("countryName"));
            return address;
        } else {
            return null;
        }
    }

    public static PersistableBundle fromAddress(android.location.Address address) {
        if (address == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PersistableBundle persistableBundle = new PersistableBundle();
            persistableBundle.putString("country", address.getLocale().getCountry());
            persistableBundle.putString("language", address.getLocale().getLanguage());
            persistableBundle.putString("variant", address.getLocale().getVariant());
            persistableBundle.putString("locality", address.getLocality());
            persistableBundle.putString("subLocality", address.getSubLocality());
            persistableBundle.putString("adminArea", address.getAdminArea());
            persistableBundle.putString("subAdminArea", address.getSubAdminArea());
            persistableBundle.putString("countryName", address.getCountryName());
            return persistableBundle;
        } else {
            return null;
        }
    }
}
