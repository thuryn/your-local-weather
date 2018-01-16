package org.thosp.yourlocalweather.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.thosp.yourlocalweather.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        if ((permissions != null) || (permissions.size() < 2)) {
            return true;
        }
        return false;
    }

    public static List<String> getAllPermissions(Context context) {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        return permissions;
    }

    public static boolean checkAllPermissions(Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Constants.KEY_PREF_WIDGET_UPDATE_LOCATION, false)) {
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
