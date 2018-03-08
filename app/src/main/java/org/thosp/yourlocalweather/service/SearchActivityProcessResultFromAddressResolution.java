package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class SearchActivityProcessResultFromAddressResolution implements ProcessResultFromAddressResolution {

    public static final String TAG = "SearchActivityProcessResultFromAddressResolution";

    private Context context;
    private Intent sendIntent;

    public SearchActivityProcessResultFromAddressResolution(Context context, Intent sendIntent) {
        this.context = context;
        this.sendIntent = sendIntent;
    }

    public void processAddresses(List<Address> addresses) {
        appendLog(context, TAG, "processUpdateOfLocation:addresses:" + addresses);
        if ((addresses != null) && (addresses.size() > 0)) {
            sendIntent.putExtra("addresses", addresses.get(0));
        }
        appendLog(context, TAG, "processUpdateOfLocation:sendIntent:" + sendIntent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(sendIntent);
    }
}
