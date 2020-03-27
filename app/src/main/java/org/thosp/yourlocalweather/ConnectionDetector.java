package org.thosp.yourlocalweather;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class ConnectionDetector {

    private static final String TAG = "ConnectionDetector";

    private final Context mContext;

    public ConnectionDetector(Context context) {
        mContext = context;
    }

    public boolean isNetworkAvailableAndConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = null;
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (Exception e) {
            appendLog(mContext, TAG, e);
        }
        return (networkInfo != null) && networkInfo.isConnected();
    }
}
