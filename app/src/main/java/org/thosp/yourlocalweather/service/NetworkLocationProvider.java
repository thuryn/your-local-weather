package org.thosp.yourlocalweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
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

    public enum NetworkLocationProviderActions {
        START_LOCATION_UPDATE, LOCATION_UPDATE_CELLS_ONLY
    }

    private final IBinder binder = new NetworkLocationProviderBinder();

    boolean resolveAddress;

    public TelephonyManager mTelephonyManager;
    WifiManager.WifiLock mWifiLock;
    private WifiManager wifiManager;
    private volatile boolean scanning;
    private volatile Calendar nextScanningAllowedFrom;
    private volatile PendingIntent intentToCancel;
    private volatile Integer jobId;
    private AlarmManager alarmManager;

    private WifiScanCallback mWifiScanResults = new WifiScanCallback() {

        @Override
        public void onWifiResultsAvailable() {
            appendLog(getBaseContext(), TAG, "Wifi results are available now:" + scanning);
            if (!scanning) {
                return;
            }
            nextScanningAllowedFrom = null;
            scanning = false;
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                if (jobId != null) {
                    JobScheduler jobScheduler = getSystemService(JobScheduler.class);
                    jobScheduler.cancel(jobId);
                }
            } else {
                if (intentToCancel != null) {
                    alarmManager.cancel(intentToCancel);
                }
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
        return binder;
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

        if (null != intent.getAction()) switch (intent.getAction()) {
            case "org.openbmap.unifiedNlp.LOCATION_UPDATE_CELLS_ONLY":
                startLocationUpdateCellsOnly();
                return ret;
            default:
                break;
        }
        return START_STICKY;
    }

    public void startLocationUpdateCellsOnly() {
        appendLog(getBaseContext(), TAG, "LOCATION_UPDATE_CELLS_ONLY:nextScanningAllowedFrom:" + ((nextScanningAllowedFrom != null) ? nextScanningAllowedFrom.getTimeInMillis() : "null"));
        if (nextScanningAllowedFrom == null) {
            return;
        }
        nextScanningAllowedFrom = null;
        scanning = false;
        getLocationFromWifisAndCells(null);
    }

    public void startLocationUpdate(Location inputLocation, boolean resolveAddress) {
        this.resolveAddress = resolveAddress;
        if (nextScanningAllowedFrom != null) {
            Calendar now = Calendar.getInstance();
            if (now.before(nextScanningAllowedFrom)) {
                return;
            }
        }
        if (inputLocation != null) {
            MozillaLocationService.getInstance(getBaseContext()).processUpdateOfLocation(getBaseContext(), inputLocation, resolveAddress);
        } else {
            sendUpdateToLocationBackends();
        }
        return;
    }

    private void sendUpdateToLocationBackends() {
        appendLog(getBaseContext(), TAG, "update():nextScanningAllowedFrom:" + ((nextScanningAllowedFrom != null)?nextScanningAllowedFrom.getTimeInMillis():"null"));
        if(nextScanningAllowedFrom == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                scanning = wifiManager.startScan();
            } else {
                scanning = true;
            }
            if (scanning) {
                nextScanningAllowedFrom = Calendar.getInstance();
                nextScanningAllowedFrom.add(Calendar.MINUTE, 15);
            }
        }
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
            ComponentName serviceComponent = new ComponentName(this, NetworkLocationCellsOnlyJob.class);
            JobInfo.Builder builder = new JobInfo.Builder(NetworkLocationCellsOnlyJob.JOB_ID, serviceComponent);
            builder.setMinimumLatency(8000); // wait at least
            builder.setOverrideDeadline(10000); // maximum delay
            JobInfo jobInfo = builder.build();
            jobId= jobInfo.getId();
            JobScheduler jobScheduler = getSystemService(JobScheduler.class);
            jobScheduler.schedule(jobInfo);
        } else {
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
        }
        appendLog(getBaseContext(), TAG, "update():cells only task scheduled");
    }

    private void getLocationFromWifisAndCells(List<ScanResult> scans) {
        appendLog(getBaseContext(), TAG, "getLocationFromWifisAndCells(), scans=" + ((scans != null)?scans.size():"null"));
        MozillaLocationService.getInstance(getBaseContext()).getLocationFromCellsAndWifis(getBaseContext(),
                                                                          LocationNetworkSourcesService.getInstance().getCells(getBaseContext(),
                                                                          mTelephonyManager),
                                                                          scans,
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

    public class NetworkLocationProviderBinder extends Binder {
        NetworkLocationProvider getService() {
            return NetworkLocationProvider.this;
        }
    }
}
