package org.thosp.yourlocalweather.settings;

import android.widget.CompoundButton;
import android.widget.Switch;

public class CurrentWeatherDetailSwitchListener implements CompoundButton.OnCheckedChangeListener {

    boolean checked;
    Switch[] dependentSwitches;
    int switchIndex;
    int numberOfAvailableDetailsInWidget;

    public CurrentWeatherDetailSwitchListener(boolean initialValue, Switch[] dependentSwitches, int switchIndex, int numberOfAvailableDetailsInWidget) {
        checked = initialValue;
        this.dependentSwitches = dependentSwitches;
        this.switchIndex = switchIndex;
        this.numberOfAvailableDetailsInWidget = numberOfAvailableDetailsInWidget;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        checked = isChecked;
        int switchedCounter = 0;
        for (Switch dependentSwitch: dependentSwitches) {
          if (dependentSwitch.isChecked()) {
            switchedCounter++;
          }
        }
        if (switchedCounter >= numberOfAvailableDetailsInWidget) {
          for (Switch dependentSwitch: dependentSwitches) {
            if (!dependentSwitch.isChecked()) {
              dependentSwitch.setEnabled(false);
            }
          }
        } else {
          for (Switch dependentSwitch: dependentSwitches) {
            dependentSwitch.setEnabled(true);
          }
        }
    }

    public boolean isChecked() {
        return checked;
    }
}
