package org.thosp.yourlocalweather.service;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;

import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class BluetoothEventsReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothEventsReceiver";

    private Context context;
    private Long voiceSettingId;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if (context == null) {
                return;
            }
            sendMessageToWeatherByVoiceService(context);
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        appendLog(context, TAG, "Receiver started with intent: " + intent + " and action " + action);
        this.context = context;
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        appendLog(context, TAG, "onReceive:bluetoothDevice: " + bluetoothDevice);
        if (bluetoothDevice == null) {
            return;
        }
        switch (action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                btDeviceConnected(bluetoothDevice); break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
            case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                btDeviceDisConnected(bluetoothDevice); break;
        }
    }

    private void btDeviceConnected(BluetoothDevice bluetoothDevice) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> connectedBtDevices = sharedPreferences.getStringSet(Constants.CONNECTED_BT_DEVICES, new HashSet<String>());
        if (!connectedBtDevices.contains(bluetoothDevice.getAddress())) {
            connectedBtDevices.add(bluetoothDevice.getAddress());
            sharedPreferences.edit().putStringSet(Constants.CONNECTED_BT_DEVICES, connectedBtDevices).apply();
        }
        if (isBtTriggerEnabled(bluetoothDevice)) {
            timerHandler.postDelayed(timerRunnable, 15000);
        }
    }

    private void btDeviceDisConnected(BluetoothDevice bluetoothDevice) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> connectedBtDevices = sharedPreferences.getStringSet(Constants.CONNECTED_BT_DEVICES, new HashSet<String>());
        if (connectedBtDevices.contains(bluetoothDevice.getAddress())) {
            connectedBtDevices.remove(bluetoothDevice.getAddress());
            sharedPreferences.edit().putStringSet(Constants.CONNECTED_BT_DEVICES, connectedBtDevices).apply();
        }
    }

    private boolean isBtTriggerEnabled(BluetoothDevice bluetoothDevice) {
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(context);
        Map<Long, Long> voiceTriggetType = voiceSettingParametersDbHelper.getLongParam(
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId());
        appendLog(context, TAG, "isBtTriggerEnabled: " + voiceTriggetType);
        for (Long currentVoiceSettingId: voiceTriggetType.keySet()) {
            Long value = voiceTriggetType.get(currentVoiceSettingId);
            appendLog(context, TAG, "isBtTriggerEnabled:value: " + value);
            if (value == null) {
                continue;
            }
            if (value == 1) {
                Boolean allBtDevices = voiceSettingParametersDbHelper.getBooleanParam(
                        currentVoiceSettingId,
                        VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId());
                appendLog(context, TAG, "isBtTriggerEnabled:allBtDevices: " + allBtDevices);
                if ((allBtDevices != null) && allBtDevices) {
                    voiceSettingId = currentVoiceSettingId;
                    return true;
                }
                String enabledBtDevices = voiceSettingParametersDbHelper.getStringParam(
                        currentVoiceSettingId,
                        VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId());
                if (bluetoothDevice == null) {
                    return false;
                }
                if ((enabledBtDevices != null) && enabledBtDevices.contains(bluetoothDevice.getAddress())) {
                    voiceSettingId = currentVoiceSettingId;
                    return true;
                }
            }
        }
        return false;
    }

    private Messenger weatherByVoiceService;
    private Lock weatherByVoiceServiceLock = new ReentrantLock();
    private Queue<Message> weatherByvOiceUnsentMessages = new LinkedList<>();

    protected void sendMessageToWeatherByVoiceService(Context context) {
        weatherByVoiceServiceLock.lock();
        try {
            Message msg = Message.obtain(
                    null,
                    WeatherByVoiceService.START_VOICE_WEATHER_ALL,
                    new WeatherByVoiceRequestDataHolder(voiceSettingId)
            );
            if (checkIfWeatherByVoiceServiceIsNotBound(context)) {
                //appendLog(getBaseContext(), TAG, "WidgetIconService is still not bound");
                weatherByvOiceUnsentMessages.add(msg);
                return;
            }
            //appendLog(getBaseContext(), TAG, "sendMessageToService:");
            weatherByVoiceService.send(msg);
        } catch (RemoteException e) {
            appendLog(context, TAG, e.getMessage(), e);
        } finally {
            weatherByVoiceServiceLock.unlock();
        }
    }

    private boolean checkIfWeatherByVoiceServiceIsNotBound(Context context) {
        if (weatherByVoiceService != null) {
            return false;
        }
        try {
            bindWeatherByVoiceService(context);
        } catch (Exception ie) {
            appendLog(context, TAG, "currentWeatherServiceIsNotBound interrupted:", ie);
        }
        return (weatherByVoiceService == null);
    }

    private void bindWeatherByVoiceService(Context context) {
        context.getApplicationContext().bindService(
                new Intent(context.getApplicationContext(), WeatherByVoiceService.class),
                weatherByVoiceServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindWeatherByVoiceService(Context context) {
        if (weatherByVoiceService == null) {
            return;
        }
        context.getApplicationContext().unbindService(weatherByVoiceServiceConnection);
    }

    private ServiceConnection weatherByVoiceServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binderService) {
            weatherByVoiceService = new Messenger(binderService);
            weatherByVoiceServiceLock.lock();
            try {
                while (!weatherByvOiceUnsentMessages.isEmpty()) {
                    weatherByVoiceService.send(weatherByvOiceUnsentMessages.poll());
                }
            } catch (RemoteException e) {
                //appendLog(getBaseContext(), TAG, e.getMessage(), e);
            } finally {
                weatherByVoiceServiceLock.unlock();
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            weatherByVoiceService = null;
        }
    };
}
