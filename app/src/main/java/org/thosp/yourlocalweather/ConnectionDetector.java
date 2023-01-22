package org.thosp.yourlocalweather;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionDetector {

    private static final String TAG = "ConnectionDetector";

    private static final int WAITS_FOR_RESULT = 30; //1.5 seconds

    private final Context mContext;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ConnectionDetector(Context context) {
        mContext = context;
    }

    public synchronized boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        Future<Boolean> resultWithTimeout = getNetworkStatusWithTimeout(connectivityManager);
        int waitCounter = WAITS_FOR_RESULT;
        while((waitCounter > 0) && !resultWithTimeout.isDone()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
            }
            waitCounter--;
        }
        try {
            if (resultWithTimeout.isDone() && (resultWithTimeout.get() != null)) {
                return resultWithTimeout.get();
            } else {
                resultWithTimeout.cancel(true);
            }
        } catch (ExecutionException ee) {
            return false;
        } catch (InterruptedException ie) {
            return false;
        }

        return false;
    }

    public Future<Boolean> getNetworkStatusWithTimeout(ConnectivityManager connectivityManager) {
        return executor.submit(() -> {
            NetworkInfo networkInfo = null;
            try {
                networkInfo = connectivityManager.getActiveNetworkInfo();
            } catch (Exception e) {
                appendLog(mContext, TAG, e);
            }
            return (networkInfo != null) && networkInfo.isConnected();
        });
    }
}
