package org.thosp.yourlocalweather;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;

import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil.Theme;

public class YourLocalWeather extends Application {

    private static final String TAG = "YourLocalWeather";

    private static Theme sTheme = Theme.light;

    @Override
    public void onCreate() {
        super.onCreate();
        LanguageUtil.setLanguage(this, PreferenceUtil.getLanguage(this));

        sTheme = PreferenceUtil.getTheme(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LanguageUtil.setLanguage(this, PreferenceUtil.getLanguage(this));
    }

    public void reloadTheme() {
        sTheme = PreferenceUtil.getTheme(this);
    }

    public void applyTheme(Activity activity) {
        activity.setTheme(getThemeResId());
    }

    public static int getThemeResId() {
        switch (sTheme) {
            case light:
                return R.style.AppThemeLight;
            case dark:
                return R.style.AppThemeDark;
            default:
                return R.style.AppThemeLight;
        }
    }
}
