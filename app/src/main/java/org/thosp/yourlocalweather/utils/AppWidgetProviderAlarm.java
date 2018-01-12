package org.thosp.yourlocalweather.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import org.thosp.yourlocalweather.service.LocationUpdateService;
import org.thosp.yourlocalweather.widget.LessWidgetProvider;
import org.thosp.yourlocalweather.widget.MoreWidgetProvider;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AppWidgetProviderAlarm {

    private static final String TAG = "AppWidgetProviderAlarm";

    private static final long START_SENSORS_CHECK_PERIOD = 3600000; //1 hour

    private Context mContext;
    private Class<?> mCls;

    public AppWidgetProviderAlarm(Context context, Class<?> cls) {
        this.mContext = context;
        this.mCls = cls;
    }

    public void setAlarm() {
        String updatePeriodStr = AppPreference.getWidgetUpdatePeriod(mContext);
        long updatePeriodMills = Utils.intervalMillisForAlarm(updatePeriodStr);
        appendLog(mContext, TAG, "setAlarm:" + updatePeriodStr);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        if ("0".equals(updatePeriodStr)) {
            sendSensorStartIntent(mCls);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + START_SENSORS_CHECK_PERIOD,
                    START_SENSORS_CHECK_PERIOD,
                    getPendingSensorStartIntent(mCls));
        } else {
            sendSensorStopIntent(mCls);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + updatePeriodMills,
                    updatePeriodMills,
                    getPendingIntent(mCls));
        }
    }

    public void cancelAlarm() {
        appendLog(mContext, TAG, "cancelAlarm");
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(mCls));
        getPendingIntent(mCls).cancel();
    }

    private void sendSensorStartIntent(Class<?> cls) {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent = fillAndSendSensorEvent(sendIntent, cls);
        mContext.startService(sendIntent);
        appendLog(mContext, TAG, "sendIntent:" + sendIntent);
    }

    private void sendSensorStopIntent(Class<?> cls) {
        Intent sendIntent = new Intent("android.intent.action.STOP_SENSOR_BASED_UPDATES");
        sendIntent = fillAndSendSensorEvent(sendIntent, cls);
        mContext.startService(sendIntent);
        appendLog(mContext, TAG, "sendIntent:" + sendIntent);
    }

    private PendingIntent getPendingSensorStartIntent(Class<?> cls) {
        Intent sendIntent = new Intent("android.intent.action.START_SENSOR_BASED_UPDATES");
        sendIntent = fillAndSendSensorEvent(sendIntent, cls);
        return PendingIntent.getService(mContext,
                0,
                sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Intent fillAndSendSensorEvent(Intent sendIntent, Class<?> cls) {
        sendIntent.setPackage("org.thosp.yourlocalweather");
        if (cls.getCanonicalName().equals(LessWidgetProvider.class.getCanonicalName())) {
            sendIntent.putExtra("updateSource", "LESS_WIDGET");
        } else if (cls.getCanonicalName().equals(MoreWidgetProvider.class.getCanonicalName())) {
            sendIntent.putExtra("updateSource", "MORE_WIDGET");
        } else {
            sendIntent.putExtra("updateSource", "EXT_LOC_WIDGET");
        }
        return sendIntent;
    }

    private PendingIntent getPendingIntent(Class<?> cls) {
        if(AppPreference.isUpdateLocationEnabled(mContext, "")) {
            Intent intent = new Intent("android.intent.action.START_LOCATION_AND_WEATHER_UPDATE");
            intent.setPackage("org.thosp.yourlocalweather");
            if (cls.getCanonicalName().equals(LessWidgetProvider.class.getCanonicalName())) {
                intent.putExtra("updateSource", "LESS_WIDGET");
            } else if (cls.getCanonicalName().equals(MoreWidgetProvider.class.getCanonicalName())) {
                intent.putExtra("updateSource", "MORE_WIDGET");
            } else {
                intent.putExtra("updateSource", "EXT_LOC_WIDGET");
            }
            return PendingIntent.getService(mContext,
                                            0,
                                            intent,
                                            PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            Intent intent = new Intent(mContext, cls);
            intent.setAction(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
            return PendingIntent.getBroadcast(mContext,
                                              0,
                                              intent,
                                              PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public boolean isAlarmOff() {
        Intent intent = new Intent(mContext, mCls);
        intent.setAction(Constants.ACTION_FORCED_APPWIDGET_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext,
                                                                 0,
                                                                 intent,
                                                                 PendingIntent.FLAG_NO_CREATE);
        return pendingIntent == null;
    }
}
