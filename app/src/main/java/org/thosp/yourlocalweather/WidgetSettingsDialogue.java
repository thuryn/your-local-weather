package org.thosp.yourlocalweather;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.ListView;

import org.osmdroid.config.Configuration;
import org.thosp.yourlocalweather.model.WidgetSettingsDbHelper;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.GraphUtils;

import java.util.HashSet;
import java.util.Set;

public class WidgetSettingsDialogue extends Activity {

    private Set<Integer> combinedGraphValues = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        String settingOption = getIntent().getStringExtra("settings_option");

        switch (settingOption) {
            case "graphSetting": createGraphSettingDialog(getIntent().getIntExtra("widgetId", 0));
        }
    }

    private void createGraphSettingDialog(final int widgetId) {
        final Set<Integer> mSelectedItems = new HashSet<>();

        combinedGraphValues = GraphUtils.getCombinedGraphValuesFromSettings(this, widgetId);

        boolean[] checkedItems = new boolean[4];
        for (Integer visibleColumn: combinedGraphValues) {
            mSelectedItems.add(visibleColumn);
            checkedItems[visibleColumn] = true;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.forecast_settings_combined_values)
                .setMultiChoiceItems(R.array.pref_combined_graph_values, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                ListView dialogListView = ((AlertDialog) dialog).getListView();
                                if (isChecked) {
                                    mSelectedItems.add(which);
                                    if (which == 2) {
                                        if (mSelectedItems.contains(3)) {
                                            mSelectedItems.remove(3);
                                        }
                                        dialogListView.getChildAt(3).setEnabled(false);
                                        dialogListView.getChildAt(3).setClickable(true);
                                    } else if (which == 3) {
                                        if (mSelectedItems.contains(2)) {
                                            mSelectedItems.remove(2);
                                        }
                                        dialogListView.getChildAt(2).setEnabled(false);
                                        dialogListView.getChildAt(2).setClickable(true);
                                    }
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                    if ((which == 2) || (which == 3)) {
                                        dialogListView.getChildAt(3).setEnabled(true);
                                        dialogListView.getChildAt(3).setClickable(false);
                                        dialogListView.getChildAt(2).setEnabled(true);
                                        dialogListView.getChildAt(2).setClickable(false);
                                    }
                                }
                            }
                        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        combinedGraphValues = new HashSet<>();
                        for (Integer selectedItem: mSelectedItems) {
                            combinedGraphValues.add(selectedItem);
                        }
                        WidgetSettingsDbHelper widgetSettingsDbHelper = WidgetSettingsDbHelper.getInstance(WidgetSettingsDialogue.this);
                        StringBuilder valuesToStore = new StringBuilder();
                        for (int selectedValue: combinedGraphValues) {
                            valuesToStore.append(selectedValue);
                            valuesToStore.append(",");
                        }
                        widgetSettingsDbHelper.saveParamString(widgetId, "combinedGraphValues", valuesToStore.toString());
                        GraphUtils.invalidateGraph();
                        Intent refreshWidgetIntent = new Intent(Constants.ACTION_APPWIDGET_CHANGE_GRAPH_SCALE);
                        refreshWidgetIntent.setPackage("org.thosp.yourlocalweather");
                        sendBroadcast(refreshWidgetIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
