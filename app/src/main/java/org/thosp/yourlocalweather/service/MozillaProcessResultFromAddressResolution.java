package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.SystemClock;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class MozillaProcessResultFromAddressResolution implements ProcessResultFromAddressResolution {

    public static final String TAG = "MozillaProcessResultFromAddressResolution";

    private Context context;
    private Intent sendIntent;

    public MozillaProcessResultFromAddressResolution(Context context, Intent sendIntent) {
        this.context = context;
        this.sendIntent = sendIntent;
    }

    public void processAddresses(List<Address> addresses) {
        appendLog(context, TAG, "processUpdateOfLocation:addresses:" + addresses);
        if ((addresses != null) && (addresses.size() > 0)) {
            sendIntent.putExtra("addresses", addresses.get(0));
        }
        appendLog(context, TAG, "processUpdateOfLocation:sendIntent:" + sendIntent);
        startBackgroundService(context, sendIntent);
    }

    private void startBackgroundService(Context context, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                pendingIntent);
    }
}
