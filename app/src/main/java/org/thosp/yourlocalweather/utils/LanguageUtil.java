package org.thosp.yourlocalweather.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import java.util.Locale;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LanguageUtil {

    private static final String TAG = LanguageUtil.class.getSimpleName();

    public static Context setLanguage(final Context context, String locale) {
        Locale sLocale;
        if (TextUtils.isEmpty(locale) || "default".equals(locale)) {
            sLocale = Locale.getDefault();
        } else {
            String[] localeParts = locale.split("-");
            StringBuilder s = new StringBuilder();
            for (String pa: localeParts) {
                s.append(pa);
                s.append(":");
            }
            LogToFile.appendLog(context, "LanguageUtil", "locale.split(\"_-\"):" + s.toString() + ":locale:" + locale);
            if (localeParts.length > 1) {
                sLocale = new Locale(localeParts[0], localeParts[1]);
            } else {
                sLocale = new Locale(locale);
            }
        }

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale.setDefault(sLocale);
        Context newContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(sLocale);
            newContext = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = sLocale;
        }
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return newContext;
    }

    public static void forceChangeLanguage(Activity activity) {
        Intent intent = activity.getIntent();
        if (intent == null) {
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 15);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static String getLanguageName(String locale) {
        if (TextUtils.isEmpty(locale)) {
            locale = Locale.getDefault().toString();
        }
        if (locale.contains("-")) {
            return locale.split("-")[0];
        }
        return locale;
    }
}
