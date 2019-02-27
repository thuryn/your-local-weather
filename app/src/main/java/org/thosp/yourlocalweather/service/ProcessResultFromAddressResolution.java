package org.thosp.yourlocalweather.service;

import android.location.Address;

import java.util.List;

public interface ProcessResultFromAddressResolution {

    void processAddresses(List<Address> addresses);
    void processCanceledRequest();
}
