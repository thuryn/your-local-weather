package org.thosp.yourlocalweather;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatSpinner;

import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.util.ArrayList;
import java.util.Arrays;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;


public class MultiSelectionTriggerSpinner extends AppCompatSpinner implements DialogInterface.OnMultiChoiceClickListener {

    private static final String TAG = "MultiSelectionTriggerSpinner";

    ArrayList<MultiselectionItem> items = null;

    boolean[] selection = null;

    ArrayAdapter adapter;
    Long voiceSettingId;

    public MultiSelectionTriggerSpinner(Context context) {
        super(context);
        adapter = new ArrayAdapter(context,
                android.R.layout.simple_spinner_item);
        super.setAdapter(adapter);
    }

    public MultiSelectionTriggerSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        adapter = new ArrayAdapter(context,
                android.R.layout.simple_spinner_item);
        super.setAdapter(adapter);
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (selection != null && which >= 0 && which < selection.length) {
            selection[which] = isChecked;
            adapter.clear();
            adapter.add(buildSelectedItemString());
            writeCurrentSetting();
        }
    }

    private void writeCurrentSetting() {
        if (selection == null || items == null || voiceSettingId == null) {
            return;
        }
        StringBuilder selectedBtDevices = new StringBuilder();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i] && i < items.size()) {
                selectedBtDevices.append(items.get(i).getAddress());
                selectedBtDevices.append(",");
            }
        }
        String selectedBtDevicesString = selectedBtDevices.toString();
        appendLog(getContext(), TAG, "writeCurrentSetting: voiceSettingId=", voiceSettingId, ", selectedBtDevicesString=", selectedBtDevicesString);
        VoiceSettingParametersDbHelper voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(getContext());
        voiceSettingParametersDbHelper.saveStringParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId(),
                selectedBtDevicesString);
        appendLog(getContext(), TAG, "writeCurrentSetting saved");
    }

    @Override
    public boolean performClick() {
        if (items == null || items.isEmpty() || selection == null) {
            return false;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        String[] itemNames = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            itemNames[i] = items.get(i).getName();
        }
        builder.setMultiChoiceItems(itemNames, selection, this);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // Do nothing
            }
        });

        builder.show();

        return true;
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {

        throw new RuntimeException(
                "setAdapter is not supported by MultiSelectSpinner.");
    }

    public void setItems(ArrayList<MultiselectionItem> items) {
        this.items = items;
        if (this.items != null) {
            selection = new boolean[this.items.size()];
            Arrays.fill(selection, false);
        } else {
            selection = null;
        }
        adapter.clear();
        adapter.add("");
    }

    public void setSelection(ArrayList<MultiselectionItem> selection) {
        if (this.selection == null || items == null || selection == null) {
            return;
        }
        for (int i = 0; i < this.selection.length; i++) {
            this.selection[i] = false;
        }

        for (MultiselectionItem sel : selection) {
            if (sel == null || sel.getValue() == null) {
                continue;
            }
            for (int j = 0; j < items.size(); ++j) {
                if (items.get(j) != null && sel.getValue().equals(items.get(j).getValue())) {
                    this.selection[j] = true;
                }
            }
        }

        adapter.clear();
        adapter.add(buildSelectedItemString());
    }

    private String buildSelectedItemString() {
        if (items == null || selection == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean foundOne = false;

        for (int i = 0; i < items.size(); ++i) {
            if (i < selection.length && selection[i]) {
                if (foundOne) {
                    sb.append(", ");
                }
                foundOne = true;
                if (items.get(i) != null && items.get(i).getName() != null) {
                    sb.append(items.get(i).getName());
                }
            }
        }
        return sb.toString();
    }

    public ArrayList<MultiselectionItem> getSelectedItems() {
        ArrayList<MultiselectionItem> selectedItems = new ArrayList<>();
        if (items == null || selection == null) {
            return selectedItems;
        }
        for (int i = 0; i < items.size(); ++i) {
            if (i < selection.length && selection[i]) {
                selectedItems.add(items.get(i));
            }
        }
        return selectedItems;
    }

    public void setVoiceSettingId(Long voiceSettingId) {
        this.voiceSettingId = voiceSettingId;
    }
}
