package org.thosp.yourlocalweather.utils;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.LocationManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.util.Log;

import org.thosp.yourlocalweather.SettingsActivity;
import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.service.Cell;
import org.thosp.yourlocalweather.service.WeatherRequestDataHolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class LogToFile {

    private static final String TAG = LogToFile.class.getName();

    private static final String TIME_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(TIME_DATE_PATTERN, new Locale("en"));

    public static String logFilePathname;
    public static Boolean logToFileEnabled;
    public static int logFileHoursOfLasting;
    private static Calendar logFileAtTheEndOfLive;
    private static Calendar nextCheckPreferencesCheck;

    public static void appendLogWithParams(Context context, String tag, String text, List<String> params) {
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        StringBuilder paramDescription = new StringBuilder();
        for (String param: params) {
            paramDescription.append(param);
            paramDescription.append(":");
        }
        appendLog(context, tag, text, paramDescription.toString());
    }

    public static void appendLogSensorsCheck(Context context,
                                             String tag,
                                             String reasonText,
                                             float currentLength,
                                             double countedLength,
                                             double countedAcc,
                                             float dT) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, reasonText, ", currentLength = ", String.format("%.8f", currentLength),
                ":counted length = ", String.format("%.8f", countedLength), ":countedAcc = ",
                String.format("%.8f", countedAcc),
                ", dT = ", String.format("%.8f", dT));
    }

    public static void appendLogSensorsEnd(Context context,
                                           String tag,
                                           float absCurrentLength,
                                           float currentLengthLowPassed,
                                           long nowInMillis,
                                           long lastUpdatedPosition,
                                           boolean nowIsBeforeTheLastUpdatedAndTimeSpan,
                                           boolean currentLengthIsUnderLimit,
                                           boolean nowIsBeforeTheLastUpdatedAndFastTimeSpan,
                                           boolean currentLengthIsUnderFastLimit,
                                           boolean autolocationForSensorEventAddressFound,
                                           boolean nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation,
                                           boolean currentLengthIsUnderNoLocationLimit) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, "end currentLength = ", String.format("%.8f", absCurrentLength),
                ", currentLengthLowPassed = ", String.format("%.8f", currentLengthLowPassed),
                ", lastUpdate=", String.valueOf(nowInMillis), ", lastUpdatePosition=", String.valueOf(lastUpdatedPosition),
                ", nowIsBeforeTheLastUpdatedAndTimeSpan=", String.valueOf(nowIsBeforeTheLastUpdatedAndTimeSpan),
                ", currentLengthIsUnderLimit=", String.valueOf(currentLengthIsUnderLimit),
                ", nowIsBeforeTheLastUpdatedAndFastTimeSpan=", String.valueOf(nowIsBeforeTheLastUpdatedAndFastTimeSpan),
                ", currentLengthIsUnderFastLimit=", String.valueOf(currentLengthIsUnderFastLimit),
                ", autolocationForSensorEventAddressFound=", String.valueOf(autolocationForSensorEventAddressFound),
                ", nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation=", String.valueOf(nowIsBeforeTheLastUpdatedAndTimeSpanNoLocation),
                ", currentLengthIsUnderNoLocationLimit=", String.valueOf(currentLengthIsUnderNoLocationLimit)
        );
    }

    public static void appendLog(Context context, String tag, String text1, int value1, String text2, int value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, boolean value1, String text2, boolean value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, Intent value1, String text2, Class value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, (value2 != null)? value2.toString() : "null");
    }


    public static void appendLog(Context context, String tag, String text1, long value1, String text2, Class value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, (value2 != null)? value2.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Sensor value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLogWakeupSources(Context context, String tag, String wakeupdown, List<Integer> wakeUpSources) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        StringBuilder wakeupSourcesList = new StringBuilder();
        wakeupSourcesList.append(wakeupdown);
        wakeupSourcesList.append(", wakeUpSources.size=");
        wakeupSourcesList.append(wakeUpSources.size());
        wakeupSourcesList.append(", WakeUp source list: ");
        for (Integer wakeupSource: wakeUpSources) {
            wakeupSourcesList.append(wakeupSource);
            wakeupSourcesList.append(",");
        }
        appendLog(context, tag, wakeupdown, "wakeUpSources:", wakeupSourcesList.toString());
    }

    public static void appendLog(Context context, String tag, String text1, SensorManager value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, PowerManager.WakeLock value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, PowerManager.WakeLock value1, String text2, boolean value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, int value1, String text2, long value2, String text3, long value3, String text4, long value4) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2), text3, String.valueOf(value3), text4, String.valueOf(value4));
    }

    public static void appendLog(Context context, String tag, String text1, int value1, int value2, int value3, int value4, int value5) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.format("%d|%s|%d|%d|%s", value1, value2, value3, value4, value5));
    }

    public static void appendLog(Context context, String tag, String text1, int value1, int value2, int value3, int value4, int value5, int value6) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.format("%d|%s|%d|%d|%s|%d", value1, value2, value3, value4, value5, value6));
    }

    public static void appendLog(Context context, String tag, String text1, PowerManager value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Messenger value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Intent value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Queue value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? String.valueOf(value1.size()) : "null");
    }

    public static void appendLog(Context context, String tag, String text1, List value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? (value1 + ":" + String.valueOf(value1.size())) : "null");
    }

    public static void appendLog(Context context, String tag, String text1, List value1, String text2, List value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? (value1 + ":" + String.valueOf(value1.size())) : "null", text2, (value2 != null)? (value2 + ":" + String.valueOf(value2.size())) : "null");
    }

    public static void appendLog(Context context, String tag, String text1, CellLocation value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Cell value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, double value1, String text2, double value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, double value1, String text2, double value2, String text3, String text4) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2), text3, text4);
    }

    public static void appendLog(Context context, String tag, String text1, Calendar value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? String.valueOf(value1.getTimeInMillis()) : "null");
    }

    public static void appendLog(Context context, String tag, String text1, int value1, String text2, Location value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, (value2 != null)? value2.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, NetworkInfo value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Network value1, String text2, boolean value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, android.location.Location value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, double value1, String text2, double value2, String text3, long value3, String text4, Address value4) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2), text3, String.valueOf(value3), text4, (value4 != null)? value4.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Address value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, android.location.Location value1, String text2, Address value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, (value2 != null)? value2.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, android.location.Location value1, String text2, double value2, String text3, double value3, String text4, String text5) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, String.valueOf(value2), text3, String.valueOf(value3), text4, text5);
    }

    public static void appendLog(Context context, String tag, String text1, int value1, String text2, WeatherRequestDataHolder value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, (value2 != null)? value2.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, long value1, String text2, long value2, String text3, long value3) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2), text3, String.valueOf(value3));
    }

    public static void appendLogLocale(Context context, String tag, String text1, String[] localeParts, String text2, String value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        StringBuilder s = new StringBuilder();
        for (String pa: localeParts) {
            s.append(pa);
            s.append(":");
        }
        appendLog(context, tag, text1, s.toString(), text2, value2);
    }

    public static void appendLogLastUpdateTime(Context context, String tag, String text1, WeatherForecastDbHelper.WeatherForecastRecord value1, String text2, long value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? String.valueOf(value1.getLastUpdatedTime()) : "null", text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, long value1, String text2, String text3) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, text3);
    }

    public static void appendLog(Context context, String tag, String text1, CurrentWeatherDbHelper.WeatherRecord value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, Location value1, String text2, String value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, Location value1, String text2, boolean value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null", text2, String.valueOf(value2));
    }

    public static void appendLog(Context context, String tag, String text1, Location value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, ServiceConnection value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, (value1 != null)? value1.toString() : "null");
    }

    public static void appendLog(Context context, String tag, String text1, boolean value1, String text2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2);
    }

    public static void appendLog(Context context, String tag, String text1, boolean value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1));
    }

    public static void appendLog(Context context, String tag, String text1, boolean value1, String text2, boolean value2, String text3, boolean value3) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2), text3, String.valueOf(value3));
    }

    public static void appendLog(Context context, String tag, String text1, boolean value1, String text2, boolean value2, String text3, boolean value3, String text4, boolean value4) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2), text3, String.valueOf(value3), text4, String.valueOf(value4));
    }

    public static void appendLog(Context context, String tag, String text1, boolean value1, String text2, long value2) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1), text2, String.valueOf(value2));
    }

    public static void appendLogWithDate(Context context, String tag, String text1, long value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, DATE_FORMATTER.format(new Date(value1)));
    }

    public static void appendLog(Context context, String tag, String text1, long value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1));
    }

    public static void appendLog(Context context, String tag, String text1, int value1) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        appendLog(context, tag, text1, String.valueOf(value1));
    }

    public static void appendLog(Context context, String tag, String... texts) {
        appendLog(context, tag, null, texts);
    }

    public static void appendLog(Context context, String tag, String text, Throwable throwable) {
        appendLog(context, tag, throwable, text);
    }

    public static void appendLog(Context context, String tag, Throwable throwable, String... texts) {
        checkPreferences(context);
        if (!logToFileEnabled || (logFilePathname == null)) {
            return;
        }
        File logFile = new File(logFilePathname);
        Date now = new Date();
        try {
            if (logFile.exists()) {
                if (logFileAtTheEndOfLive == null) {
                    boolean succeeded = initFileLogging(logFile);
                    if (!succeeded) {
                        createNewLogFile(logFile, now);
                    }
                } else if(Calendar.getInstance().after(logFileAtTheEndOfLive)) {
                    logFile.delete();
                    createNewLogFile(logFile, now);
                }
            } else {
                createNewLogFile(logFile, now);
            }
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(DATE_FORMATTER.format(now));
            buf.append(" ");
            buf.append(tag);
            buf.append(" - ");
            if (texts != null) {
                for (String text: texts) {
                    buf.append(text);
                }
            }
            if (throwable != null) {
                buf.append("\n");
                buf.append(throwable.getMessage());
                for (StackTraceElement ste: throwable.getStackTrace()) {
                    buf.newLine();
                    buf.append(ste.toString());
                }
            }
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private static boolean initFileLogging(File logFile) {
        char[] logFileDateCreatedBytes = new char[TIME_DATE_PATTERN.length()];
        Date logFileDateCreated;
        FileReader logFileReader = null;
        try {
            logFileReader = new FileReader(logFile);
            logFileReader.read(logFileDateCreatedBytes);
            logFileDateCreated = DATE_FORMATTER.parse(new String(logFileDateCreatedBytes));
        } catch (Exception e) {
            return false;
        } finally {
            if (logFileReader != null) {
                try {
                    logFileReader.close();
                } catch (IOException ex) {

                }
            }
        }
        initEndOfLive(logFileDateCreated);
        return true;
    }

    private static void initEndOfLive(Date logFileDateCreated) {
        logFileAtTheEndOfLive = Calendar.getInstance();
        logFileAtTheEndOfLive.setTime(logFileDateCreated);
        logFileAtTheEndOfLive.add(Calendar.HOUR_OF_DAY, logFileHoursOfLasting);
    }

    private static void createNewLogFile(File logFile, Date dateOfCreation) throws IOException {
        logFile.createNewFile();
        initEndOfLive(dateOfCreation);
    }

    private static void checkPreferences(Context context) {
        if (nextCheckPreferencesCheck != null) {
            Calendar now = Calendar.getInstance();
            if (nextCheckPreferencesCheck.after(now)) {
                return;
            }
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (logToFileEnabled == null) {
            logFilePathname = sharedPreferences.getString(SettingsActivity.KEY_DEBUG_FILE,"");
            logToFileEnabled = sharedPreferences.getBoolean(SettingsActivity.KEY_DEBUG_TO_FILE, false);
            logFileHoursOfLasting = Integer.valueOf(sharedPreferences.getString(SettingsActivity.KEY_DEBUG_FILE_LASTING_HOURS, "24"));
        }
        nextCheckPreferencesCheck = Calendar.getInstance();
        nextCheckPreferencesCheck.add(Calendar.MINUTE, 5);
    }
}
