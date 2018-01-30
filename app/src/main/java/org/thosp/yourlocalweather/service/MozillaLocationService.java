package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class MozillaLocationService {

    public static final String TAG = "MozillaLocationService";

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static MozillaLocationService instance;

    private MozillaLocationService() {
    }

    public static MozillaLocationService getInstance() {
        if (instance == null) {
            instance = new MozillaLocationService();
        }
        return instance;
    }

    private static final String SERVICE_URL = "https://location.services.mozilla.com/v1/geolocate?key=%s";
    private static final String API_KEY = "3693d51230c04a34af807fbefd1caebb";
    private static final String PROVIDER = "ichnaea";

    public synchronized void getLocationFromCellsAndWifis(final Context context,
                                                          List<Cell> cells,
                                                          List<ScanResult> wiFis,
                                                          final String destinationPackageName,
                                                          final boolean resolveAddress) {
        appendLog(context, TAG, "getLocationFromCellsAndWifis:wifi=" + ((wiFis != null)?wiFis.size():"null") +
                    ", cells=" + ((cells != null)?cells.size():"null"));
        if ((cells == null || cells.isEmpty()) && (wiFis == null || wiFis.size() < 2)) {
            processUpdateOfLocation(context, null, destinationPackageName, false);
            return;
        }
        try {
            final String request = createRequest(cells, wiFis);
            final StringEntity entity = new StringEntity(request);
            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    client.post(context,
                                String.format(SERVICE_URL, API_KEY),
                                entity,
                                "application/json",
                                new AsyncHttpResponseHandler() {

                        @Override
                        public void onStart() {
                            // called before request is started
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] httpResponse) {
                            Location response = null;
                            try {
                                String result = new String(httpResponse);
                                appendLog(context, TAG, "response: " + result);
                                JSONObject responseJson = new JSONObject(result);
                                double lat = responseJson.getJSONObject("location").getDouble("lat");
                                double lon = responseJson.getJSONObject("location").getDouble("lng");
                                double acc = responseJson.getDouble("accuracy");
                                response = create(PROVIDER, lat, lon, (float) acc);
                                processUpdateOfLocation(context, response, destinationPackageName, resolveAddress);
                            } catch (JSONException e) {
                                appendLog(context, TAG, e.toString());
                                processUpdateOfLocation(context, null, destinationPackageName, resolveAddress);
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            appendLog(context, TAG, "onFailure:" + statusCode);
                            processUpdateOfLocation(context, null, destinationPackageName, resolveAddress);
                        }

                        @Override
                        public void onRetry(int retryNo) {
                            // called when request is retried
                        }
                    });
                }
            };
            mainHandler.post(myRunnable);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void processUpdateOfLocation(final Context context,
                                         Location location,
                                         String destinationPackageName,
                                         boolean resolveAddress) {
        Intent sendIntent = new Intent("android.intent.action.LOCATION_UPDATE");
        sendIntent.setPackage(destinationPackageName);
        sendIntent.putExtra("location", location);
        appendLog(context, TAG, "processUpdateOfLocation:resolveAddress:" + resolveAddress);
        if (resolveAddress && (location != null)) {
            appendLog(context, TAG, "processUpdateOfLocation:location:" + location.getLatitude() + ", " + location.getLongitude() + ", " + Locale.getDefault().getLanguage());
            NominatimLocationService.getInstance().getFromLocation(context, location.getLatitude(), location.getLongitude(), 1, Locale.getDefault().getLanguage(), new ProcessResultFromAddressResolution(context, sendIntent));
            return;
        }
        appendLog(context, TAG, "processUpdateOfLocation:sendIntent:" + sendIntent);
        context.startService(sendIntent);
    }

    public class ProcessResultFromAddressResolution {

        private Context context;
        private Intent sendIntent;

        public ProcessResultFromAddressResolution(Context context, Intent sendIntent) {
            this.context = context;
            this.sendIntent = sendIntent;
        }

        public void processAddresses(List<Address> addresses) {
            appendLog(context, TAG, "processUpdateOfLocation:addresses:" + addresses);
            if ((addresses != null) && (addresses.size() > 0)) {
                sendIntent.putExtra("addresses", addresses.get(0));
            }
            appendLog(context, TAG, "processUpdateOfLocation:sendIntent:" + sendIntent);
            context.startService(sendIntent);
        }
    }

    private static String createRequest(List<Cell> cells, List<ScanResult> wiFis) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray cellTowers = new JSONArray();

        if (cells != null) {
            String lastType = null;
            for (Cell cell : cells) {
                String networkType = getRadioType(cell);
                if (lastType != null && lastType.equals(networkType)) {
                    // We can't contribute if different cell types are mixed.
                    jsonObject.put("radioType", null);
                } else {
                    jsonObject.put("radioType", getRadioType(cell));
                }
                lastType = networkType;
                JSONObject cellTower = new JSONObject();
                cellTower.put("radioType", getRadioType(cell));
                cellTower.put("mobileCountryCode", cell.mcc);
                cellTower.put("mobileNetworkCode", cell.mnc);
                cellTower.put("locationAreaCode", cell.area);
                cellTower.put("cellId", cell.cellId);
                cellTower.put("signalStrength", cell.signal);
                if (cell.psc != -1)
                    cellTower.put("psc", cell.psc);
                cellTower.put("asu", calculateAsu(networkType, cell.signal));
                cellTowers.put(cellTower);
            }
        }
        JSONArray wifiAccessPoints = new JSONArray();
        if (wiFis != null) {
            for (ScanResult wiFi : wiFis) {
                JSONObject wifiAccessPoint = new JSONObject();
                wifiAccessPoint.put("macAddress", wiFi.BSSID);
                //wifiAccessPoint.put("age", age);
                if (wiFi.frequency != -1) wifiAccessPoint.put("channel", convertFrequencyToChannel(wiFi.frequency));
                if (wiFi.frequency != -1)
                    wifiAccessPoint.put("frequency", wiFi.frequency);
                wifiAccessPoint.put("signalStrength", wiFi.level);
                //wifiAccessPoint.put("signalToNoiseRatio", signalToNoiseRatio);
                wifiAccessPoints.put(wifiAccessPoint);
            }
        }
        jsonObject.put("cellTowers", cellTowers);
        jsonObject.put("wifiAccessPoints", wifiAccessPoints);
        jsonObject.put("fallbacks", new JSONObject().put("lacf", true).put("ipf", false));
        return jsonObject.toString();
    }

    /**
     * see https://mozilla-ichnaea.readthedocs.org/en/latest/cell.html
     */
    @SuppressWarnings("MagicNumber")
    private static int calculateAsu(String networkType, int signal) {
        switch (networkType) {
            case "gsm":
                return Math.max(0, Math.min(31, (signal + 113) / 2));
            case "wcdma":
                return Math.max(-5, Math.max(91, signal + 116));
            case "lte":
                return Math.max(0, Math.min(95, signal + 140));
            case "cdma":
                if (signal >= -75) {
                    return 16;
                }
                if (signal >= -82) {
                    return 8;
                }
                if (signal >= -90) {
                    return 4;
                }
                if (signal >= -95) {
                    return 2;
                }
                if (signal >= -100) {
                    return 1;
                }
                return 0;
        }
        return 0;
    }

    private static String getRadioType(Cell cell) {
        switch (cell.technology) {
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "wcdma";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "lte";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "cdma";
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            default:
                return "gsm";
        }
    }


    public static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    public Location create(String source, double latitude, double longitude, float accuracy) {
        Location location = new Location(source);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        return location;
    }
}
