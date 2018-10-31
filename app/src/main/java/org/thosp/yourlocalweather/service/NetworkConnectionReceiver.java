package org.thosp.yourlocalweather.service;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;

import java.util.LinkedList;
import java.util.Queue;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

@TargetApi(Build.VERSION_CODES.M)
public class NetworkConnectionReceiver extends ConnectivityManager.NetworkCallback {

    private static final String TAG = "NetworkConnectionReceiver";

    private static Queue<String> screenOnOffUpdateServiceActions = new LinkedList<>();
    private ScreenOnOffUpdateService screenOnOffUpdateService;
    private Context context;

    public NetworkConnectionReceiver(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onAvailable(Network network) {
        super.onAvailable(network);
        appendLog(context, TAG, "onAvailable, network=" + network);
        if (networkIsOffline()) {
            return;
        }
        checkAndUpdateWeather();
    }

    private boolean networkIsOffline() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        appendLog(context, TAG, "networkIsOffline, networkInfo=" + networkInfo);
        if (networkInfo == null) {
            return true;
        }
        appendLog(context, TAG, "networkIsOffline, networkInfo.isConnectedOrConnecting()=" + networkInfo.isConnectedOrConnecting());
        return !networkInfo.isConnectedOrConnecting();
    }

    private void checkAndUpdateWeather() {
        appendLog(context, TAG, "checkAndUpdateWeather");
        if (screenOnOffUpdateService == null) {
            screenOnOffUpdateServiceActions.add("checkAndUpdateWeather");
            bindScreenOnOffService();
            return;
        }
        screenOnOffUpdateService.checkAndUpdateWeather();
    }

    private void bindScreenOnOffService() {
        Intent intent = new Intent(context, ScreenOnOffUpdateService.class);
        context.bindService(intent, screenOnOffUpdateServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindScreenOnOffService() {
        if (screenOnOffUpdateService == null) {
            return;
        }
        context.unbindService(screenOnOffUpdateServiceConnection);
    }

    private ServiceConnection screenOnOffUpdateServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder binder =
                    (ScreenOnOffUpdateService.ScreenOnOffUpdateServiceBinder) service;
            screenOnOffUpdateService = binder.getService();
            String bindedServiceActions;
            while ((bindedServiceActions = screenOnOffUpdateServiceActions.poll()) != null) {
                screenOnOffUpdateService.checkAndUpdateWeather();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            screenOnOffUpdateService = null;
        }
    };
}
