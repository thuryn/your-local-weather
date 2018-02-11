package org.thosp.yourlocalweather.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;

public class PreferenceUtil {

    public enum Theme {
        light,
        dark,
    }

    public static SharedPreferences getDefaultSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String getLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_LANGUAGE, Locale.getDefault().getLanguage());
    }

    public static Theme getTheme(Context context) {
        return Theme.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_THEME, "light"));
    }
}
