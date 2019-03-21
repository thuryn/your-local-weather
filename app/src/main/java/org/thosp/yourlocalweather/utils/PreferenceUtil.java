package org.thosp.yourlocalweather.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.LocaleListCompat;

import org.thosp.yourlocalweather.R;

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
        String language = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage());
        if ("default".equals(language)) {
            language = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_OS_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage());
        }
        return language;
    }

    public static Theme getTheme(Context context) {
        return Theme.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_THEME, "light"));
    }

    public static AppPreference.GraphGridColors getGraphGridColor(Context context) {
        Theme theme = getTheme(context);
        if (null == theme) {
            return new AppPreference.GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
        } else switch (theme) {
            case dark:
                return new AppPreference.GraphGridColors(Color.WHITE, Color.GRAY);
            case light:
                return new AppPreference.GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
            default:
                return new AppPreference.GraphGridColors(Color.parseColor("#333333"), Color.LTGRAY);
        }
    }

    public static int getTextColor(Context context) {
        Theme theme = getTheme(context);
        if (null == theme) {
            return ContextCompat.getColor(context, R.color.widget_transparentTheme_textColorPrimary);
        } else switch (theme) {
            case dark:
                return Color.WHITE;
            case light:
                return Color.BLACK;
            default:
                return ContextCompat.getColor(context, R.color.widget_transparentTheme_textColorPrimary);
        }
    }

    public static int getBackgroundColor(Context context) {
        Theme theme = getTheme(context);
        if (null == theme) {
            return ContextCompat.getColor(context,
                    R.color.widget_transparentTheme_colorBackground);
        } else switch (theme) {
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
}
