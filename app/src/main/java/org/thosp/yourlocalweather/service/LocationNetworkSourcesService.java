package org.thosp.yourlocalweather.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import java.util.ArrayList;
import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LocationNetworkSourcesService {

    public static final String TAG = "LocationNetworkSourcesService";

    private static LocationNetworkSourcesService instance;

    private LocationNetworkSourcesService() {
    }

    public synchronized static LocationNetworkSourcesService getInstance() {
        if (instance == null) {
            instance = new LocationNetworkSourcesService();
        }
        return instance;
    }

    public List<Cell> getCells(Context context, TelephonyManager mTelephonyManager) {
        List<Cell> cells = new ArrayList<>();

        String operator = mTelephonyManager.getNetworkOperator();
        int mnc;
        int mcc;

        // getNetworkOperator() may return empty string, probably due to dropped connection
        if (operator != null && operator.length() > 3) {
            mcc = Integer.valueOf(operator.substring(0, 3));
            mnc = Integer.valueOf(operator.substring(3));
        } else {
            appendLog(context, TAG, "Error retrieving network operator, skipping cell");
            mcc = 0;
            mnc = 0;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        CellLocation cellLocation = null;
        try {
            cellLocation = mTelephonyManager.getCellLocation();
        } catch (SecurityException securityException) {
            appendLog(context, TAG, "SecurityException when getCellLocation is called ", securityException);
        }

        appendLog(context, TAG, "getCells():cellLocation:" + cellLocation);

        if (cellLocation != null) {
            if (cellLocation instanceof GsmCellLocation) {
                Cell cell = new Cell();
                cell.cellId = ((GsmCellLocation) cellLocation).getCid();
                cell.area = ((GsmCellLocation) cellLocation).getLac();
                cell.psc = ((GsmCellLocation) cellLocation).getPsc();
                cell.mcc = mcc;
                cell.mnc = mnc;
                cell.technology = mTelephonyManager.getNetworkType();
                appendLog(context, TAG, "GsmCellLocation for cell:" + cell);
                cells.add(cell);
            } else if (cellLocation instanceof CdmaCellLocation) {
                appendLog(context, TAG, "getCells():cellLocation - CdmaCellLocation: Using CDMA cells for NLP is not yet implemented");
            } else {
                appendLog(context, TAG, "getCells():cellLocation - Got a CellLocation of an unknown class");
            }
        } else {
            appendLog(context, TAG, "getCellLocation returned null");
        }

        List<NeighboringCellInfo> neighboringCells = null;
        try {
            neighboringCells = mTelephonyManager.getNeighboringCellInfo();
        } catch (SecurityException securityException) {
            appendLog(context, TAG, "SecurityException when getCellLocation is called ", securityException);
        }
        appendLog(context, TAG, "getCells():neighboringCells:" + neighboringCells);
        if (neighboringCells != null) {
            appendLog(context, TAG, "getCells():neighboringCells.size:" + neighboringCells.size());
            appendLog(context, TAG, "getNeighboringCellInfo found " + neighboringCells.size() + " cells");
        } else {
            appendLog(context, TAG, "getNeighboringCellInfo returned null");
        }

        if (neighboringCells != null) {
            for (NeighboringCellInfo c : neighboringCells) {
                Cell cell = new Cell();
                cell.cellId = c.getCid();
                cell.area = c.getLac();
                cell.mcc = mcc;
                cell.mnc = mnc;
                cell.psc = c.getPsc();
                cell.signal = c.getRssi();
                cell.technology = c.getNetworkType();
                appendLog(context, TAG, "GsmCellLocation for cell:" + cell);
                cells.add(cell);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            List<CellInfo> cellsRawList = null;
            try {
                cellsRawList = mTelephonyManager.getAllCellInfo();
            } catch (SecurityException securityException) {
                appendLog(context, TAG, "SecurityException when getCellLocation is called ", securityException);
            }
            appendLog(context, TAG, "getCells():getAllCellInfo:cellsRawList:" + cellsRawList);
            if (cellsRawList != null) {
                appendLog(context, TAG, "getCells():cellsRawList.size:" + cellsRawList.size());
                appendLog(context, TAG, "getAllCellInfo found " + cellsRawList.size() + " cells");
            } else {
                appendLog(context, TAG, "getAllCellInfo returned null");
            }

            if ((cellsRawList != null) && !cellsRawList.isEmpty()) {
                processCellInfoList(context, mTelephonyManager, cellsRawList, cells);
            }
        } else {
            appendLog(context, TAG, "getAllCellInfo is not available (requires API 17)");
        }

        appendLog(context, TAG, "getCells():return cells.size: " + cells.size());
        return cells;
    }

    private void processCellInfoList(Context context,
                                     TelephonyManager mTelephonyManager,
                                     List<CellInfo> cellInfoList,
                                     List<Cell> cells) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return;
        }
        for (CellInfo c : cellInfoList) {
            Cell cell = new Cell();
            if (c instanceof CellInfoGsm) {
                //Log.v(TAG, "GSM cell found");
                cell.cellId = ((CellInfoGsm) c).getCellIdentity().getCid();
                cell.area = ((CellInfoGsm) c).getCellIdentity().getLac();
                cell.mcc = ((CellInfoGsm)c).getCellIdentity().getMcc();
                cell.mnc = ((CellInfoGsm)c).getCellIdentity().getMnc();
                cell.psc = ((CellInfoGsm)c).getCellIdentity().getPsc();
                cell.technology = mTelephonyManager.getNetworkType();
                appendLog(context, TAG, String.format("CellInfoGsm for %d|%s|%d|%d|%s", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology));
            } else if (c instanceof CellInfoCdma) {
                /* object.put("cellId", ((CellInfoCdma)s).getCellIdentity().getBasestationId());
                    object.put("locationAreaCode", ((CellInfoCdma)s).getCellIdentity().getLac());
                    object.put("mobileCountryCode", ((CellInfoCdma)s).getCellIdentity().get());
                    object.put("mobileNetworkCode", ((CellInfoCdma)s).getCellIdentity().getMnc());*/
                appendLog(context, TAG, ":Using of CDMA cells for NLP not yet implemented");
            } else if (c instanceof CellInfoLte) {
                //Log.v(TAG, "LTE cell found");
                cell.cellId = ((CellInfoLte) c).getCellIdentity().getCi();
                cell.area = ((CellInfoLte) c).getCellIdentity().getTac();
                cell.mcc = ((CellInfoLte)c).getCellIdentity().getMcc();
                cell.mnc = ((CellInfoLte)c).getCellIdentity().getMnc();
                cell.technology = mTelephonyManager.getNetworkType();
                appendLog(context, TAG, String.format("CellInfoLte for %d|%s|%d|%d|%s|%d", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology, ((CellInfoLte)c).getCellIdentity().getPci()));
            } else if (c instanceof CellInfoWcdma) {
                //Log.v(TAG, "CellInfoWcdma cell found");
                cell.cellId = ((CellInfoWcdma) c).getCellIdentity().getCid();
                cell.area = ((CellInfoWcdma) c).getCellIdentity().getLac();
                cell.mcc = ((CellInfoWcdma)c).getCellIdentity().getMcc();
                cell.mnc = ((CellInfoWcdma)c).getCellIdentity().getMnc();
                cell.psc = ((CellInfoWcdma)c).getCellIdentity().getPsc();
                cell.technology = mTelephonyManager.getNetworkType();
                appendLog(context, TAG, String.format("CellInfoWcdma for %d|%s|%d|%d|%s|%d", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology, ((CellInfoWcdma) c).getCellIdentity().getPsc()));
            } else {
                appendLog(context, TAG, "CellInfo of unexpected type: " + c);
            }
            cells.add(cell);
        }
    }
}
