package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.location.Address;
import android.location.Location;

import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class MozillaProcessResultFromAddressResolution implements ProcessResultFromAddressResolution {

    public static final String TAG = "MozillaProcessResultFromAddressResolution";

    private final Context context;
    private final Location location;
    private final MozillaLocationService mozillaLocationService;

    public MozillaProcessResultFromAddressResolution(Context context, Location location, MozillaLocationService mozillaLocationService) {
        this.context = context;
        this.location = location;
        this.mozillaLocationService = mozillaLocationService;
    }

    public void processAddresses(Location location, List<Address> addresses) {
        appendLog(context, TAG, "processUpdateOfLocation:addresses:", addresses);
        Address resolvedAddress = null;
        if ((addresses != null) && (addresses.size() > 0)) {
            resolvedAddress = addresses.get(0);
        }
        appendLog(context, TAG, "processUpdateOfLocation:location:", location, ", address=", resolvedAddress);
        mozillaLocationService.reportNewLocation(location, resolvedAddress);
    }

    @Override
    public void processCanceledRequest() {
        mozillaLocationService.reportCanceledRequestForNewLocation();
    }
}
