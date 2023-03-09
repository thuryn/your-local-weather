package org.thosp.yourlocalweather.utils;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.thosp.yourlocalweather.R;

public class PreferenceUtil {

    public enum Theme {
        system,
        light,
        dark,
    }

    public static SharedPreferences getDefaultSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Theme getThemeFromPreferences(Context context) {
        return Theme.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_THEME, "system"));
    }

    public static Theme getTheme(Context context) {
        Theme theme = getThemeFromPreferences(context);
        if (theme.equals(Theme.system)) {
            if (getIsOsDarkTheme(context)) {
                theme = Theme.dark;
            } else {
                theme = Theme.light;
            }
        }
        return theme;
    }

    public static AppPreference.GraphGridColors getGraphGridColor(Context context) {
        switch (getTheme(context)) {
            case dark:
                return new AppPreference.GraphGridColors(Color.WHITE, Color.GRAY);
            case light:
                return new AppPreference.GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
            default:
                return new AppPreference.GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
        }
    }

    public static int getTextColor(Context context) {
        switch (getTheme(context)) {
            case dark:
                return Color.WHITE;
            case light:
                return Color.BLACK;
            default:
                return ContextCompat.getColor(context, R.color.widget_transparentTheme_textColorPrimary);
        }
    }

    public static int getNotificationTextColor(Context context) {
        if (getIsOsDarkTheme(context)) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }

    public static int getBackgroundColor(Context context) {
        switch (getTheme(context)) {
            case dark:
                return ContextCompat.getColor(context,
                        R.color.widget_darkTheme_colorBackground);
            case light:
                return Color.WHITE;
            default:
                return ContextCompat.getColor(context,
                        R.color.widget_transparentTheme_colorBackground);
        }
    }
    public static boolean getIsOsDarkTheme(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}
