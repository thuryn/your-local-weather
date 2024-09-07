package org.thosp.yourlocalweather.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;

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

    public static boolean noPermissionGranted(Context context) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if ((autoLocation == null) || !autoLocation.isEnabled()) {
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS,
                    "locationUpdateStrategy is set to update_location_none, return false");
            return false;
        }
        List<String> permissions = getAllPermissions(context);
        return (permissions != null) && (permissions.size() > 0);
    }

    private static List<String> getAllPermissions(Context context) {
        List<String> permissions = new ArrayList<>();

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        String geocoder = AppPreference.getLocationGeocoderSource(context);
        if (AppPreference.isGpsEnabledByPreferences(context) &&
                isGPSEnabled &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        /*if ("location_geocoder_local".equals(geocoder) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else if ("location_geocoder_system".equals(geocoder) && */
        if (isNetworkEnabled &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return permissions;
    }

    public static boolean checkPermissionsAndSettings(Context context) {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(context);
        Location autoLocation = locationsDbHelper.getLocationByOrderId(0);
        if (!autoLocation.isEnabled()) {
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS,
                    "locationUpdateStrategy is set to update_location_none, return false");
            return false;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        String geocoder = AppPreference.getLocationGeocoderSource(context);
        boolean gpsNotSet = AppPreference.isGpsEnabledByPreferences(context) && !isGPSEnabled;
        boolean networkNotSet = "location_geocoder_system".equals(geocoder) && !isNetworkEnabled;

        appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS,
                "isGPSEnabled=", isGPSEnabled, ", isNetworkEnabled=", isNetworkEnabled);
        if (gpsNotSet && networkNotSet) {
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "isGPSEnabled and isNetworkEnabled are not set, returning false");
            return false;
        } else {
            boolean preciseOrCoarseLocation = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "permissions are empty = ", preciseOrCoarseLocation);
            if (preciseOrCoarseLocation) {
                appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "permissions are empty, returning true");
                return true;
            } else {
                appendLog(context, TAG_CHECK_PERMISSIONS_AND_SETTINGS, "permissions are not empty, returning false, permissions = ", preciseOrCoarseLocation);
                return false;
            }
        }
    }
}
