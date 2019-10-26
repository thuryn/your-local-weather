package org.thosp.yourlocalweather.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class TimeUtils {

    private static final String TAG = "TimeUtils";

    public static void setupAlarmForVoice(Context context) {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(context);
        Map<Long, Long> voiceTimeSettings = voiceSettingParametersDbHelper.getLongParam(
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId());

        Map<Long, Long> nextAlarms = new HashMap<>();

        appendLog(context,
                TAG,
                "voiceTimeSettings.size = ", voiceTimeSettings.size());

        for (Long voiceSettingId: voiceTimeSettings.keySet()) {
            appendLog(context,
                    TAG,
                    "voiceSettingId = ", voiceSettingId);
            appendLog(context,
                    TAG,
                    "voiceSettingId.triggerType = ", voiceTimeSettings.get(voiceSettingId));
            if (voiceTimeSettings.get(voiceSettingId) != 2) {
                continue;
            }
            Long nextAlarmForVoiceSetting = setupAlarmForVoiceForVoiceSetting(context, voiceSettingId, voiceSettingParametersDbHelper);
            if (nextAlarmForVoiceSetting == null) {
                continue;
            }
            appendLog(context,
                    TAG,
                    "nextAlarmForVoiceSetting = ", nextAlarmForVoiceSetting);
            nextAlarms.put(nextAlarmForVoiceSetting, voiceSettingId);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (nextAlarms.isEmpty()) {
            alarmManager.cancel(TimeUtils.getPendingIntentForVoice(context, 0l));
            return;
        }

        Set<Long> nextAlarmsList = new TreeSet<>(nextAlarms.keySet());
        long nextTime = nextAlarmsList.iterator().next();

        appendLog(context,
                TAG,
                "nextTime = ", nextTime, ", settingsId = ", nextAlarms.get(nextTime));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    nextTime,
                    getPendingIntentForVoice(context, nextAlarms.get(nextTime)));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                    nextTime,
                    getPendingIntentForVoice(context, nextAlarms.get(nextTime)));
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP,
                    nextTime,
                    getPendingIntentForVoice(context, nextAlarms.get(nextTime)));
        }
    }

    public static Long setupAlarmForVoiceForVoiceSetting(Context context, Long voiceSettingId, VoiceSettingParametersDbHelper voiceSettingParametersDbHelper) {
        Long storedHourMinute = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId());

        if (storedHourMinute == null) {
            return null;
        }

        int hourMinute = storedHourMinute.intValue();
        int hour = hourMinute / 100;
        int minute = hourMinute - (hour * 100);

        final Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        now.add(Calendar.MINUTE, 1);

        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (now.after(c)) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        Long enabledDaysOfWeek = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId());

        if (enabledDaysOfWeek == null) {
            return null;
        }

        for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
            int currentDayOfWeek = c.get(Calendar.DAY_OF_WEEK);

            if (currentDayOfWeek == Calendar.MONDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 6)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
            if (currentDayOfWeek == Calendar.TUESDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 5)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
            if (currentDayOfWeek == Calendar.WEDNESDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 4)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
            if (currentDayOfWeek == Calendar.THURSDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 3)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
            if (currentDayOfWeek == Calendar.FRIDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 2)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
            if (currentDayOfWeek == Calendar.SATURDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 1)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
            if (currentDayOfWeek == Calendar.SUNDAY) {
                if (isCurrentSettingIndex(enabledDaysOfWeek, 0)) {
                    break;
                } else {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    continue;
                }
            }
        }
        return c.getTimeInMillis();
    }

    public static PendingIntent getPendingIntentForVoice(Context context, Long voiceSettingId) {
        Intent sendIntent = new Intent("android.intent.action.SAY_WEATHER");
        sendIntent.setPackage("org.thosp.yourlocalweather");
        sendIntent.putExtra("voiceSettingId", voiceSettingId);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, sendIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pendingIntent;
    }

    public static boolean isCurrentSettingIndex(long inputIntSetting, int index) {
        long bitOperatorParam = getTwoPower(index);
        return ((inputIntSetting & bitOperatorParam) == bitOperatorParam);
    }

    public static long getTwoPower(int y) {
        if (y == 0) {
            return 1;
        }
        long result = 2;
        for (int i = 0; i < y; i++) {
            result *= 2;
        }
        return result;
    }
}
