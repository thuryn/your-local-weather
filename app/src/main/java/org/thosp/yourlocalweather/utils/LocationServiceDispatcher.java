package org.thosp.yourlocalweather.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import org.thosp.yourlocalweather.service.LocationUpdateService;

public class LocationServiceDispatcher {

    private static final String TAG = "LocationServiceDispatcher";

    /**
     * Ověří, zda má aplikace oprávnění k poloze A ZÁROVEŇ je v eligible stavu
     * (tj. je v popředí, nebo má uděleno ACCESS_BACKGROUND_LOCATION).
     */
    public static boolean canStartLocationFgsNow(Context context) {
        int fine = PermissionChecker.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = PermissionChecker.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION);

        return fine == PermissionChecker.PERMISSION_GRANTED
                || coarse == PermissionChecker.PERMISSION_GRANTED;
    }

    /**
     * Bezpečně spustí LocationUpdateService s akcí START_LOCATION_AND_WEATHER_UPDATE.
     * Vrací true, pokud byla služba spuštěna, jinak false (volající může spustit
     * fallback aktualizaci počasí pro poslední známé souřadnice).
     */
    public static boolean startLocationAndWeatherUpdate(Context context, Intent intent) {
        intent.setAction("org.thosp.yourlocalweather.action.START_LOCATION_AND_WEATHER_UPDATE");
        intent.setPackage(context.getPackageName());
        intent.setClass(context, LocationUpdateService.class);

        if (!canStartLocationFgsNow(context)) {
            // Aplikace buď vůbec nemá oprávnění k poloze, nebo běží na pozadí
            // pouze s oprávněním "Při používání aplikace" (PERMISSION_DENIED_APP_OP).
            return false;
        }

        try {
            ContextCompat.startForegroundService(context, intent);
            return true;
        } catch (Exception e) {
            // Ošetření ForegroundServiceStartNotAllowedException (Android 12+) a SecurityException
            return false;
        }
    }
}