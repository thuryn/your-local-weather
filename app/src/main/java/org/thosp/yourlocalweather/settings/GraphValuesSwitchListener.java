package org.thosp.yourlocalweather.settings;

import android.widget.CompoundButton;
import android.widget.Switch;

public class GraphValuesSwitchListener implements CompoundButton.OnCheckedChangeListener {

    boolean checked;
    Switch dependentSwitch;

    public GraphValuesSwitchListener(boolean initialValue, Switch dependentSwitch) {
        checked = initialValue;
        this.dependentSwitch = dependentSwitch;
    }

    public GraphValuesSwitchListener(boolean initialValue) {
        checked = initialValue;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        checked = isChecked;
        if (dependentSwitch != null) {
            if (isChecked) {
                dependentSwitch.setChecked(false);
                dependentSwitch.setEnabled(false);
            } else {
                dependentSwitch.setEnabled(true);
            }
        }
    }

    public boolean isChecked() {
        return checked;
    }
}
