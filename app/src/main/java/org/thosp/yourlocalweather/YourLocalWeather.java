package org.thosp.yourlocalweather;

import android.app.Activity;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
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

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class YourLocalWeather extends Application {

    private static final String TAG = "YourLocalWeather";

    private static Theme sTheme = Theme.light;

    @Override
    public void onCreate() {
        super.onCreate();
        appendLog(this, TAG,"Default locale:", Resources.getSystem().getConfiguration().locale.getLanguage());
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constants.PREF_OS_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage())
                .apply();
        LanguageUtil.setLanguage(this, PreferenceUtil.getLanguage(this));

        sTheme = PreferenceUtil.getTheme(this);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
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
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constants.PREF_OS_LANGUAGE, Resources.getSystem().getConfiguration().locale.getLanguage())
                .apply();
        LanguageUtil.setLanguage(this, PreferenceUtil.getLanguage(this));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageUtil.setLanguage(base, PreferenceUtil.getLanguage(base)));
    }

    public void reloadTheme() {
        sTheme = PreferenceUtil.getTheme(this);
    }

    public void applyTheme(Activity activity) {
        activity.setTheme(getThemeResId());
    }

    public static int getThemeResId() {
        switch (sTheme) {
            case dark:
                return R.style.AppThemeDark;
            case light:
            default:
                return R.style.AppThemeLight;
        }
    }
}
