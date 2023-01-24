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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionDetector {

    private static final String TAG = "ConnectionDetector";

    private static final int WAITS_FOR_RESULT = 1; //1 second

    private final Context mContext;

    public ConnectionDetector(Context context) {
        mContext = context;
    }

    public synchronized boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> resultWithTimeout = getNetworkStatusWithTimeout(executor, connectivityManager);

        try {
            return resultWithTimeout.get(WAITS_FOR_RESULT, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    public Future<Boolean> getNetworkStatusWithTimeout(ExecutorService executor, ConnectivityManager connectivityManager) {
        if (executor == null) {
            return new Future<Boolean>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Boolean get() throws ExecutionException, InterruptedException {
                    return false;
                }

                @Override
                public Boolean get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
                    return false;
                }
            };
        }
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
