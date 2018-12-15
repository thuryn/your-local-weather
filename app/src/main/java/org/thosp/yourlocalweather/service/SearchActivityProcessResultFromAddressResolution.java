package org.thosp.yourlocalweather.service;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class SearchActivityProcessResultFromAddressResolution implements ProcessResultFromAddressResolution {

    public static final String TAG = "SearchActivityProcessResultFromAddressResolution";

    private Context context;
    private Intent sendIntent;
    private ProgressDialog mProgressDialog;

    public SearchActivityProcessResultFromAddressResolution(Context context,
                                                            Intent sendIntent,
                                                            ProgressDialog mProgressDialog) {
        this.context = context;
        this.sendIntent = sendIntent;
        this.mProgressDialog = mProgressDialog;
    }

    public void processAddresses(List<Address> addresses) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        appendLog(context, TAG, "processUpdateOfLocation:addresses:", addresses);
        if ((addresses != null) && (addresses.size() > 0)) {
            sendIntent.putExtra("addresses", addresses.get(0));
        }
        appendLog(context, TAG, "processUpdateOfLocation:sendIntent:", sendIntent);
        context.sendBroadcast(sendIntent);
    }
}
