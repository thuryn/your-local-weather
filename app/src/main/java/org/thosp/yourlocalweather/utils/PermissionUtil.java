package org.thosp.yourlocalweather.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLogWithParams;

public class PermissionUtil {

    private static final String TAG_CHECK_PERMISSIONS_AND_SETTINGS = "PermissionUtil:checkPermissionsAndSettings";

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
        if ("location_geocoder_system".equals(geocoder) || "location_geocoder_local".equals(geocoder)) {
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

    public static boolean checkPermissionsAndSettings(Context context) {
        String locationUpdateStrategy = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.KEY_PREF_LOCATION_UPDATE_STRATEGY, "update_location_full");
        if ("update_location_none".equals(locationUpdateStrategy)) {
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "locationUpdateStrategy is set to update_location_none, return false");
            return false;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "isGPSEnabled=" + isGPSEnabled + ", isNetworkEnabled=" + isNetworkEnabled);
        if (!isGPSEnabled && !isNetworkEnabled) {
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "isGPSEnabled and isNetworkEnabled is not set, returning false");
            return false;
        } else {
            String geocoder = AppPreference.getLocationGeocoderSource(context);
            List<String> permissions = new ArrayList<>();
            if (AppPreference.isGpsEnabledByPreferences(context) &&
                    isGPSEnabled &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (isNetworkEnabled) {
                if ("location_geocoder_local".equals(geocoder) && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.READ_PHONE_STATE);
                } else if ("location_geocoder_system".equals(geocoder) && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "permissions is empty = " + permissions.isEmpty());
            if (permissions.isEmpty()) {
                appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "permissions is empty, returning true");
                return true;
            } else {
                appendLogWithParams(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "permissions is not empty, returning false, permissions = ", permissions);
                return false;
            }
        }
    }
}
