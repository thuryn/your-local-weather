package org.thosp.yourlocalweather.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.thosp.yourlocalweather.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class PermissionUtil {

    public static boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllPermissionsGranted(Context context) {
        List<String> permissions = getAllPermissions(context);
        if ((permissions != null) && (permissions.size() > 0)) {
            return true;
        }
        return false;
    }

    public static List<String> getAllPermissions(Context context) {
        List<String> permissions = new ArrayList<>();

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        boolean gsmWifiBasedNetworkLocationProvider = false;
        String geocoder = AppPreference.getLocationGeocoderSource(context);
        if ("location_geocoder_unifiednlp".equals(geocoder) || "location_geocoder_local".equals(geocoder)) {
            gsmWifiBasedNetworkLocationProvider = true;
        }

        if (AppPreference.isGpsEnabledByPreferences(context) &&
                isGPSEnabled &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (isNetworkEnabled) {
            if (gsmWifiBasedNetworkLocationProvider && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        return permissions;
    }

    public static boolean checkAllPermissions(Context context) {
        String locationUpdateStrategy = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_STRATEGY, "update_location_full");
        if ("update_location_none".equals(locationUpdateStrategy)) {
            return false;
        }
        List<String> permissions = getAllPermissions(context);
        if (!permissions.isEmpty()) {
            if (!(context instanceof Activity)) {
                return false;
            }
            ActivityCompat.requestPermissions((Activity) context,
                    permissions.toArray(new String[permissions.size()]),
                    123);
            return false;
        } else {
            return true;
        }
    }
}
