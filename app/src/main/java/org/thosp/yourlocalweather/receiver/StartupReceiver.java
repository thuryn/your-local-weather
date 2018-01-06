package org.thosp.yourlocalweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.thosp.yourlocalweather.service.NotificationService;
import org.thosp.yourlocalweather.utils.AppPreference;

public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isNotificationEnabled = AppPreference.isNotificationEnabled(context);
        NotificationService.setNotificationServiceAlarm(context, isNotificationEnabled);
    }
}
