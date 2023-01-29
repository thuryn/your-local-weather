package org.thosp.yourlocalweather;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionDetector {

    private static final String TAG = "ConnectionDetector";

    private static final int WAITS_FOR_RESULT = 100; //1 second

    private final Context mContext;

    public ConnectionDetector(Context context) {
        mContext = context;
    }

    private volatile Boolean result;

    public synchronized boolean isNetworkAvailableAndConnected() {
        Thread connectionThread = new Thread(connectionRunnable);
        connectionThread.start();
        int counter = WAITS_FOR_RESULT;
        try {
            while ((counter > 0) && (result == null)) {
                Thread.sleep(10);
                counter--;
            }
        } catch (InterruptedException e) {
            return false;
        } finally {
            connectionThread.interrupt();
            if (result !=null) {
                boolean ret = result;
                result = null;
                return ret;
            } else {
                return false;
            }
        }
    }

    Runnable connectionRunnable = new Runnable() {

        @Override
        public void run() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo = null;
            try {
                networkInfo = connectivityManager.getActiveNetworkInfo();
            } catch (Exception e) {
                appendLog(mContext, TAG, e);
            }
            result = (networkInfo != null) && networkInfo.isConnected();
        }
    };
}
