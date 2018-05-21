package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;

import org.thosp.yourlocalweather.model.LocationsDbHelper;

import java.util.Calendar;
import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class NetworkLocationProvider extends Service {

    public static final String TAG = "NetworkLocationProvider";

    String destinationPackageName;
    boolean resolveAddress;

    public TelephonyManager mTelephonyManager;
    WifiManager.WifiLock mWifiLock;
    private WifiManager wifiManager;
    private volatile boolean scanning;
    private volatile Calendar nextScanningAllowedFrom;
    private volatile PendingIntent intentToCancel;
    private AlarmManager alarmManager;
    private org.thosp.yourlocalweather.model.Location currentLocation;

    private WifiScanCallback mWifiScanResults = new WifiScanCallback() {

        @Override
        public void onWifiResultsAvailable() {
            appendLog(getBaseContext(), TAG, "Wifi results are available now");
            if (!scanning) {
                return;
            }
            nextScanningAllowedFrom = null;
            scanning = false;
            if (intentToCancel != null) {
                intentToCancel.cancel();
                alarmManager.cancel(intentToCancel);
            }
            List<ScanResult> scans = null;
            try {
                appendLog(getBaseContext(), TAG, "Wifi results are available now - going to get wifi results");
                scans = wifiManager.getScanResults();
            } catch (Throwable exception) {
                appendLog(getBaseContext(), TAG, "Exception occured getting wifi results:", exception);
            }
            if (scans == null) {
                appendLog(getBaseContext(), TAG, "WifiManager.getScanResults returned null");
            }
            getLocationFromWifisAndCells(scans);
        }
    };

    /**
     * Receives location updates as well as wifi scan result updates
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                mWifiScanResults.onWifiResultsAvailable();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTelephonyManager = ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE));
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "SCAN_LOCK");
        if(!mWifiLock.isHeld()){
            mWifiLock.acquire();
        }
        registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return ret;
        }

        Location inputLocation = null;

        if (null != intent.getAction()) switch (intent.getAction()) {
            case "org.openbmap.unifiedNlp.LOCATION_UPDATE_CELLS_ONLY":
                appendLog(getBaseContext(), TAG, "LOCATION_UPDATE_CELLS_ONLY:nextScanningAllowedFrom:" + ((nextScanningAllowedFrom != null)?nextScanningAllowedFrom.getTimeInMillis():"null"));
                if (nextScanningAllowedFrom == null) {
                    return ret;
                }
                nextScanningAllowedFrom = null;
                scanning = false;
                getLocationFromWifisAndCells(null);
                return ret;
            case "android.intent.action.START_LOCATION_UPDATE":
                if (intent.getExtras() != null) {
                    destinationPackageName = intent.getExtras().getString("destinationPackageName");
                    resolveAddress = intent.getExtras().getBoolean("resolveAddress");
                    inputLocation = intent.getExtras().getParcelable("inputLocation");
                    LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
                    currentLocation = locationsDbHelper.getLocationById(intent.getExtras().getLong("locationId"));
                }
                break;
            default:
                break;
        }

        appendLog(getBaseContext(), TAG, "onStartCommand:" + intent);
        appendLog(getBaseContext(), TAG, "onStartCommand:inputLocation:" + inputLocation);
        appendLog(getBaseContext(), TAG, "onStartCommand:destinationPackageName:" + destinationPackageName);
        if (nextScanningAllowedFrom != null) {
            Calendar now = Calendar.getInstance();
            if (now.before(nextScanningAllowedFrom)) {
                return ret;
            }
        }
        if (inputLocation != null) {
            MozillaLocationService.getInstance().processUpdateOfLocation(getBaseContext(), inputLocation, destinationPackageName, resolveAddress);
        } else {
            sendUpdateToLocationBackends();
        }
        return START_STICKY;
    }

    private void sendUpdateToLocationBackends() {
        appendLog(getBaseContext(), TAG, "update():nextScanningAllowedFrom:" + ((nextScanningAllowedFrom != null)?nextScanningAllowedFrom.getTimeInMillis():"null"));
        if(nextScanningAllowedFrom == null) {
            scanning = wifiManager.startScan();
            if (scanning) {
                nextScanningAllowedFrom = Calendar.getInstance();
                nextScanningAllowedFrom.add(Calendar.MINUTE, 15);
            }
        }
        intentToCancel = getIntentToGetCellsOnly();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 8000,
                    intentToCancel);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 8000,
                    intentToCancel);
        }
        appendLog(getBaseContext(), TAG, "update():alarm set");
    }

    private void getLocationFromWifisAndCells(List<ScanResult> scans) {
        appendLog(getBaseContext(), TAG, "getLocationFromWifisAndCells(), scans=" + ((scans != null)?scans.size():"null"));
        MozillaLocationService.getInstance().getLocationFromCellsAndWifis(getBaseContext(),
                                                                          currentLocation,
                                                                          LocationNetworkSourcesService.getInstance().getCells(getBaseContext(),
                                                                          mTelephonyManager),
                                                                          scans,
                                                                          destinationPackageName,
                                                                          resolveAddress);
    }

    private PendingIntent getIntentToGetCellsOnly() {
        Intent intent = new Intent(getBaseContext(), NetworkLocationProvider.class);
        intent.setAction("org.openbmap.unifiedNlp.LOCATION_UPDATE_CELLS_ONLY");
        return PendingIntent.getService(getBaseContext(),
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
