package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.location.Address;
import android.location.Location;

import java.util.List;

public interface ProcessResultFromAddressResolution {

    void processAddresses(Location location, List<Address> addresses);
    void processCanceledRequest(Context context);
}
