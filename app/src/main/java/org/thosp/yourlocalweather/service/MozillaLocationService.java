package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.microg.address.Formatter;
import org.microg.nlp.api.CellBackendHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.os.Build.VERSION.RELEASE;
import static org.thosp.yourlocalweather.BuildConfig.VERSION_NAME;
import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class MozillaLocationService {

    public static final String TAG = "MozillaLocationService";

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
    private Thread thread;
    private boolean running = false;
    private boolean replay = false;
    private String lastRequest = null;
    private Location lastResponse = null;

    public synchronized void getLocationFromCellsAndWifis(final Context context,
                                                          List<Cell> cells,
                                                          List<ScanResult> wiFis,
                                                          final String destinationPackageName,
                                                          final boolean resolveAddress) {
        appendLog(context, TAG, "getLocationFromCellsAndWifis:wifi=" + ((wiFis != null)?wiFis.size():"null") +
                    ", cells=" + ((cells != null)?cells.size():"null"));
        if (thread != null) return;
        if ((cells == null || cells.isEmpty()) && (wiFis == null || wiFis.size() < 2)) {
            processUpdateOfLocation(context, null, destinationPackageName, false);
            return;
        }
        try {
            final String request = createRequest(cells, wiFis);
            if (request.equals(lastRequest)) {
                if (replay) {
                    appendLog(context, TAG, "No data changes, replaying location " + lastResponse);
                    lastResponse = create(PROVIDER, lastResponse.getLatitude(), lastResponse.getLongitude(), lastResponse.getAccuracy());
                    processUpdateOfLocation(context, lastResponse, destinationPackageName, resolveAddress);
                }
                return;
            }
            replay = false;
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection conn = null;
                    Location response = null;
                    try {
                        conn = (HttpURLConnection) new URL(String.format(SERVICE_URL, API_KEY)).openConnection();
                        conn.setDoOutput(true);
                        conn.setDoInput(true);
                        //appendLog(context, TAG, "request: " + request);
                        conn.getOutputStream().write(request.getBytes());
                        String r = new String(readStreamToEnd(conn.getInputStream()));
                        appendLog(context, TAG, "response: " + r);
                        JSONObject responseJson = new JSONObject(r);
                        double lat = responseJson.getJSONObject("location").getDouble("lat");
                        double lon = responseJson.getJSONObject("location").getDouble("lng");
                        double acc = responseJson.getDouble("accuracy");
                        response = create(PROVIDER, lat, lon, (float) acc);
                        processUpdateOfLocation(context, response, destinationPackageName, resolveAddress);
                    } catch (IOException | JSONException e) {
                        if (conn != null) {
                            InputStream is = conn.getErrorStream();
                            if (is != null) {
                                try {
                                    String error = new String(readStreamToEnd(is));
                                    appendLog(context, TAG, "Error: " + error);
                                } catch (Exception ignored) {
                                }
                            }
                        }
                        appendLog(context, TAG, e.toString());
                        processUpdateOfLocation(context, null, destinationPackageName, resolveAddress);
                    }

                    lastRequest = request;
                    lastResponse = response;
                    thread = null;
                }
            });
            thread.start();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void processUpdateOfLocation(final Context context,
                                         Location location,
                                         String destinationPackageName,
                                         boolean resolveAddress) {
        /*appendLog(getBaseContext(), TAG, "processUpdateOfLocation:location:" + location + ":destinationPackageName:" + destinationPackageName + ":" + locationListener);
        if ((destinationPackageName == null) || ("".equals(destinationPackageName))) {
            if (locationListener != null) {
                locationListener.onLocationChanged(location);
            }
            return;
        }*/
        Intent sendIntent = new Intent("android.intent.action.LOCATION_UPDATE");
        sendIntent.setPackage(destinationPackageName);
        sendIntent.putExtra("location", location);
        appendLog(context, TAG, "processUpdateOfLocation:resolveAddress:" + resolveAddress);
        if (resolveAddress && (location != null)) {
            appendLog(context, TAG, "processUpdateOfLocation:location:" + location.getLatitude() + ", " + location.getLongitude() + ", " + Locale.getDefault().getLanguage());
            List<Address> addresses = NominatimLocationService.getInstance().getFromLocation(context, location.getLatitude(), location.getLongitude(), 1, Locale.getDefault().getLanguage());
            appendLog(context, TAG, "processUpdateOfLocation:addresses:" + addresses);
            if ((addresses != null) && (addresses.size() > 0)) {
                sendIntent.putExtra("addresses", addresses.get(0));
            }
        }
        appendLog(context, TAG, "processUpdateOfLocation:sendIntent:" + sendIntent);
        context.startService(sendIntent);
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

    private static byte[] readStreamToEnd(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (is != null) {
            byte[] buff = new byte[1024];
            while (true) {
                int nb = is.read(buff);
                if (nb < 0) {
                    break;
                }
                bos.write(buff, 0, nb);
            }
            is.close();
        }
        return bos.toByteArray();
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
