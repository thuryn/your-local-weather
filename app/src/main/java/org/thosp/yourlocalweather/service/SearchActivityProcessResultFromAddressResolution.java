package org.thosp.yourlocalweather.service;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class SearchActivityProcessResultFromAddressResolution implements ProcessResultFromAddressResolution {

    public static final String TAG = "SearchActivityProcessResultFromAddressResolution";

    private final Context context;
    private final Intent sendIntent;
    private ProgressDialog mProgressDialog;

    public SearchActivityProcessResultFromAddressResolution(Context context,
                                                            Intent sendIntent,
                                                            ProgressDialog mProgressDialog) {
        this.context = context;
        this.sendIntent = sendIntent;
        this.mProgressDialog = mProgressDialog;
    }

    public void processAddresses(Location location, List<Address> addresses) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        appendLog(context, TAG, "processUpdateOfLocation:addresses:", addresses);
        if ((addresses != null) && (!addresses.isEmpty())) {
            sendIntent.putExtra("addresses", addresses.get(0));
        }
        appendLog(context, TAG, "processUpdateOfLocation:sendIntent:", sendIntent);
        context.sendBroadcast(sendIntent);
    }

    @Override
    public void processCanceledRequest(Context context) {
        processAddresses(null, null);
    }
}
