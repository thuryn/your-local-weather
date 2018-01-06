package org.thosp.yourlocalweather.service;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import org.microg.nlp.api.CellBackendHelper;

import java.util.ArrayList;
import java.util.List;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class LocationNetworkSourcesService {

    public static final String TAG = "LocationNetworkSourcesService";

    private static LocationNetworkSourcesService instance;

    private LocationNetworkSourcesService() {
    }

    public static LocationNetworkSourcesService getInstance() {
        if (instance == null) {
            instance = new LocationNetworkSourcesService();
        }
        return instance;
    }

    public List<CellBackendHelper.Cell> getCells(Context context,
                                                 TelephonyManager mTelephonyManager) {
        List<CellBackendHelper.Cell> cells = new ArrayList<>();

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

        CellLocation cellLocation = mTelephonyManager.getCellLocation();

        appendLog(context, TAG, "getCells():cellLocation:" + cellLocation);

        if (cellLocation != null) {
            if (cellLocation instanceof GsmCellLocation) {
                CellBackendHelper.Cell cell = new CellBackendHelper.Cell(getCellType(mTelephonyManager.getNetworkType()), mcc, mnc, ((GsmCellLocation) cellLocation).getLac(), ((GsmCellLocation) cellLocation).getCid(),
                        ((GsmCellLocation) cellLocation).getPsc(), 0);
                appendLog(context, TAG, "GsmCellLocation for cell:" + cell);
                cells.add(cell);
            } else if (cellLocation instanceof CdmaCellLocation) {
                appendLog(context, TAG, "getCells():cellLocation - CdmaCellLocation: Using CDMA cells for NLP is not yet implemented");
                appendLog(context, TAG, "CdmaCellLocation: Using CDMA cells for NLP is not yet implemented");
            } else {
                appendLog(context, TAG, "getCells():cellLocation - Got a CellLocation of an unknown class");
                appendLog(context, TAG, "Got a CellLocation of an unknown class");
            }
        } else {
            appendLog(context, TAG, "getCellLocation returned null");
        }

        List<NeighboringCellInfo> neighboringCells = mTelephonyManager.getNeighboringCellInfo();
        appendLog(context, TAG, "getCells():neighboringCells:" + neighboringCells);
        if (neighboringCells != null) {
            appendLog(context, TAG, "getCells():neighboringCells.size:" + neighboringCells.size());
            appendLog(context, TAG, "getNeighboringCellInfo found " + neighboringCells.size() + " cells");
        } else {
            appendLog(context, TAG, "getNeighboringCellInfo returned null");
        }

        if (neighboringCells != null) {
            for (NeighboringCellInfo c : neighboringCells) {
                CellBackendHelper.Cell cell = new CellBackendHelper.Cell(getCellType(c.getNetworkType()), mcc, mnc, c.getLac(), c.getCid(), c.getPsc(), c.getRssi());
                appendLog(context, TAG, "GsmCellLocation for cell:" + cell);
                cells.add(cell);
            }
        }

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            List<CellInfo> cellsRawList = mTelephonyManager.getAllCellInfo();
            appendLog(context, TAG, "getCells():getAllCellInfo:cellsRawList:" + cellsRawList);
            if (cellsRawList != null) {
                appendLog(context, TAG, "getCells():cellsRawList.size:" + cellsRawList.size());
                Log.d(TAG, "getAllCellInfo found " + cellsRawList.size() + " cells");
            } else {
                Log.d(TAG, "getAllCellInfo returned null");
            }

            if ((cellsRawList != null) && !cellsRawList.isEmpty()) {
                processCellInfoList(cellsRawList, cells);
            }
        } else {
            Log.d(TAG, "getAllCellInfo is not available (requires API 17)");
        }*/

        appendLog(context, TAG, "getCells():return cells.size: " + cells.size());
        return cells;
    }

    /* private void processCellInfoList(List<CellInfo> cellInfoList, List<Cell> cells) {
        for (CellInfo c : cellInfoList) {
            Cell cell = new Cell(getCellType(c.getNetworkType()));
            if (c instanceof CellInfoGsm) {
                //Log.v(TAG, "GSM cell found");
                cell.cellId = ((CellInfoGsm) c).getCellIdentity().getCid();
                cell.area = ((CellInfoGsm) c).getCellIdentity().getLac();
                cell.mcc = ((CellInfoGsm)c).getCellIdentity().getMcc();
                cell.mnc = String.valueOf(((CellInfoGsm)c).getCellIdentity().getMnc());
                cell.technology = TECHNOLOGY_MAP().get(mTelephonyManager.getNetworkType());
                appendLog(context, TAG, String.format("CellInfoGsm for %d|%s|%d|%d|%s", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology));
                Log.d(TAG, String.format("CellInfoGsm for %d|%s|%d|%d|%s", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology));
            } else if (c instanceof CellInfoCdma) {
                    object.put("cellId", ((CellInfoCdma)s).getCellIdentity().getBasestationId());
                    object.put("locationAreaCode", ((CellInfoCdma)s).getCellIdentity().getLac());
                    object.put("mobileCountryCode", ((CellInfoCdma)s).getCellIdentity().get());
                    object.put("mobileNetworkCode", ((CellInfoCdma)s).getCellIdentity().getMnc());
                appendLog(context, TAG, ":Using of CDMA cells for NLP not yet implemented");
                Log.wtf(TAG, "Using of CDMA cells for NLP not yet implemented");
            } else if (c instanceof CellInfoLte) {
                //Log.v(TAG, "LTE cell found");
                cell.cellId = ((CellInfoLte) c).getCellIdentity().getCi();
                cell.area = ((CellInfoLte) c).getCellIdentity().getTac();
                cell.mcc = ((CellInfoLte)c).getCellIdentity().getMcc();
                cell.mnc = String.valueOf(((CellInfoLte)c).getCellIdentity().getMnc());
                cell.technology = TECHNOLOGY_MAP().get(mTelephonyManager.getNetworkType());
                appendLog(context, TAG, String.format("CellInfoLte for %d|%s|%d|%d|%s|%d", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology, ((CellInfoLte)c).getCellIdentity().getPci()));
                Log.d(TAG, String.format("CellInfoLte for %d|%s|%d|%d|%s|%d", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology, ((CellInfoLte)c).getCellIdentity().getPci()));
            } else if (c instanceof CellInfoWcdma) {
                //Log.v(TAG, "CellInfoWcdma cell found");
                cell.cellId = ((CellInfoWcdma) c).getCellIdentity().getCid();
                cell.area = ((CellInfoWcdma) c).getCellIdentity().getLac();
                cell.mcc = ((CellInfoWcdma)c).getCellIdentity().getMcc();
                cell.mnc = String.valueOf(((CellInfoWcdma)c).getCellIdentity().getMnc());
                cell.technology = TECHNOLOGY_MAP().get(mTelephonyManager.getNetworkType());
                appendLog(context, TAG, String.format("CellInfoWcdma for %d|%s|%d|%d|%s|%d", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology, ((CellInfoWcdma) c).getCellIdentity().getPsc()));
                Log.d(TAG, String.format("CellInfoWcdma for %d|%s|%d|%d|%s|%d", cell.mcc, cell.mnc, cell.area, cell.cellId, cell.technology, ((CellInfoWcdma) c).getCellIdentity().getPsc()));
            } else {
                appendLog(context, TAG, "CellInfo of unexpected type: " + c);
            }
            cells.add(cell);
        }
    }*/

    private CellBackendHelper.Cell.CellType getCellType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return CellBackendHelper.Cell.CellType.GSM;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return CellBackendHelper.Cell.CellType.UMTS;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return CellBackendHelper.Cell.CellType.LTE;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return CellBackendHelper.Cell.CellType.CDMA;
        }
        return null;
    }
}
