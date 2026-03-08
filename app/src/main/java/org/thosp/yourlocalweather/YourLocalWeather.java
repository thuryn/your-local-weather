package org.thosp.yourlocalweather;

import android.app.Activity;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.service.StartAutoLocationJob;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.LanguageUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.PreferenceUtil.Theme;
import org.thosp.yourlocalweather.utils.WidgetUtils;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.thosp.yourlocalweather.utils.PreferenceUtil.Theme.*;

public class YourLocalWeather extends Application {

    private static final String TAG = "YourLocalWeather";

    public static ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public void onCreate() {
        super.onCreate();
        appendLog(this, TAG,"Default locale:", Resources.getSystem().getConfiguration().locale.getLanguage());

        /*StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().build());*/

        applyTheme(this);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constants.PREF_OS_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage())
                .apply();
        AppPreference appPreference = AppPreference.getInstance();
        appPreference.clearLanguage();
        LanguageUtil.setLanguage(this, appPreference.getLanguage(this));

        executor.submit(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                JobScheduler jobScheduler = getSystemService(JobScheduler.class);
                appendLog(this, TAG, "scheduleStart at YourLocalWeather");
                AppPreference.setLastSensorServicesCheckTimeInMs(this, 0);
                jobScheduler.cancelAll();
                ComponentName serviceComponent = new ComponentName(this, StartAutoLocationJob.class);
                JobInfo.Builder builder = new JobInfo.Builder(StartAutoLocationJob.JOB_ID, serviceComponent);
                builder.setMinimumLatency(1000); // wait at least
                builder.setOverrideDeadline(3 * 1000); // maximum delay
                jobScheduler.schedule(builder.build());
            }
            WidgetUtils.updateWidgets(getBaseContext());
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constants.PREF_OS_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage())
                .apply();
        AppPreference appPreference = AppPreference.getInstance();
        appPreference.clearLanguage();
        LanguageUtil.setLanguage(this, appPreference.getLanguage(this));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageUtil.setLanguage(base, AppPreference.getInstance().getLanguage(base)));
    }

    public static void applyTheme(Context context) {
        Theme theme = PreferenceUtil.getThemeFromPreferences(context);
        switch (theme) {
            case light:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case dark:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case system:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
