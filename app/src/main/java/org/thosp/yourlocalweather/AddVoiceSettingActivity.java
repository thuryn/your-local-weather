package org.thosp.yourlocalweather;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import org.osmdroid.config.Configuration;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.VoiceSettingParameterContract;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.Constants;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.TimeUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class AddVoiceSettingActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    private Long voiceSettingId;
    private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
    private Locale applicationLocale;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());
        Configuration.getInstance().setUserAgentValue(String.format("YourLocalWeather/%s (Linux; Android %s)",
                BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE));

        setContentView(R.layout.activity_add_voice_setting);
        setupActionBar();

        voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(this);
        applicationLocale = new Locale(PreferenceUtil.getLanguage(this));

        updateItemsFromDb();
        populateTriggerType();
    }

    @Override
    protected void updateUI() {
    }

    private void updateItemsFromDb() {
        voiceSettingId = getIntent().getLongExtra("voiceSettingId", 0);
        setTextTime();
        populateDayOfWeeks();
        populateTtsDeviceEnabled();
        populateTtsSeySetting();
        populateTextes();
        populateTriggerBtDevices(R.id.trigger_bt_when_devices, R.id.trigger_bt_all_devices, VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES);
        populateBtDevices(R.id.bt_when_devices, R.id.bt_all_devices, VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES);
        populateLocations();
    }

    private void populateTextes() {
        setTextHandler(R.id.tts_say_greeting_custom_text_morning, VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_MORNING);
        setTextHandler(R.id.tts_say_greeting_custom_text_day, VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_DAY);
        setTextHandler(R.id.tts_say_greeting_custom_text_evening, VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_EVENING);
        setTextHandler(R.id.tts_say_location_custom_text, VoiceSettingParamType.VOICE_SETTING_LOCATION_CUSTOM);
        setTextHandler(R.id.tts_say_weather_description_custom_text, VoiceSettingParamType.VOICE_SETTING_WEATHER_DESCRIPTION_CUSTOM);
        setTextHandler(R.id.tts_say_temperature_custom_text, VoiceSettingParamType.VOICE_SETTING_TEMPERATURE_CUSTOM);
        setTextHandler(R.id.tts_say_wind_custom_text, VoiceSettingParamType.VOICE_SETTING_WIND_CUSTOM);
    }

    private void setTextHandler(int textViewId, final VoiceSettingParamType paramType) {
        EditText textView = (EditText)findViewById(textViewId);

        String originalValue = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                paramType.getVoiceSettingParamTypeId());

        textView.setText(originalValue, TextView.BufferType.EDITABLE);
        textView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0)
                    voiceSettingParametersDbHelper.saveStringParam(
                            voiceSettingId,
                            paramType.getVoiceSettingParamTypeId(),
                            s.toString());
                }
            }
        );
    }

    private void populateTriggerBtDevices(int spinnerViewId, int checkBoxViewId, VoiceSettingParamType voiceSettingParamType) {
        MultiSelectionTriggerSpinner btDevicesSpinner = findViewById(spinnerViewId);
        btDevicesSpinner.setVoiceSettingId(voiceSettingId);

        BluetoothAdapter bluetoothAdapter = Utils.getBluetoothAdapter(getBaseContext());

        if (bluetoothAdapter == null) {
            return;
        }

        Set<BluetoothDevice> bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();

        ArrayList<MultiselectionItem> items = new ArrayList<>();
        ArrayList<MultiselectionItem> selection = new ArrayList<>();
        ArrayList<String> selectedItems = new ArrayList<>();

        String enabledBtDevices = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                voiceSettingParamType.getVoiceSettingParamTypeId());
        Boolean enabledVoiceDevices = voiceSettingParametersDbHelper.getBooleanParam(
                voiceSettingId,
                voiceSettingParamType.getVoiceSettingParamTypeId());
        if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
            ((CheckBox) findViewById(checkBoxViewId)).setChecked(true);
            findViewById(R.id.trigger_bt_when_devices).setVisibility(View.GONE);
        } else {
            findViewById(R.id.trigger_bt_when_devices).setVisibility(View.VISIBLE);
        }

        if (enabledBtDevices != null) {
            for (String btDeviceName: enabledBtDevices.split(",")) {
                selectedItems.add(btDeviceName);
            }
        }

        for(BluetoothDevice bluetoothDevice: bluetoothDeviceSet) {
            String currentDeviceName = bluetoothDevice.getName();
            MultiselectionItem multiselectionItem;
            if (selectedItems.contains(currentDeviceName)) {
                multiselectionItem = new MultiselectionItem(currentDeviceName, true);
                selection.add(multiselectionItem);
            } else {
                multiselectionItem = new MultiselectionItem(currentDeviceName, false);
            }
            items.add(multiselectionItem);
        }
        btDevicesSpinner.setItems(items);
        btDevicesSpinner.setSelection(selection);
    }

    private void populateBtDevices(int spinnerViewId, int checkBoxViewId, VoiceSettingParamType voiceSettingParamType) {
        MultiSelectionSpinner btDevicesSpinner = findViewById(spinnerViewId);
        btDevicesSpinner.setVoiceSettingId(voiceSettingId);

        BluetoothAdapter bluetoothAdapter = Utils.getBluetoothAdapter(getBaseContext());

        if (bluetoothAdapter == null) {
            return;
        }

        Set<BluetoothDevice> bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();

        ArrayList<MultiselectionItem> items = new ArrayList<>();
        ArrayList<MultiselectionItem> selection = new ArrayList<>();
        ArrayList<String> selectedItems = new ArrayList<>();

        String enabledBtDevices = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                voiceSettingParamType.getVoiceSettingParamTypeId());
        Boolean enabledVoiceDevices = voiceSettingParametersDbHelper.getBooleanParam(
                voiceSettingId,
                voiceSettingParamType.getVoiceSettingParamTypeId());
        if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
            ((CheckBox) findViewById(checkBoxViewId)).setChecked(true);
            findViewById(R.id.bt_when_devices).setVisibility(View.GONE);
        } else {
            findViewById(R.id.bt_when_devices).setVisibility(View.VISIBLE);
        }

        if (enabledBtDevices != null) {
            for (String btDeviceName: enabledBtDevices.split(",")) {
                selectedItems.add(btDeviceName);
            }
        }

        for(BluetoothDevice bluetoothDevice: bluetoothDeviceSet) {
            String currentDeviceName = bluetoothDevice.getName();
            MultiselectionItem multiselectionItem;
            if (selectedItems.contains(currentDeviceName)) {
                multiselectionItem = new MultiselectionItem(currentDeviceName, true);
                selection.add(multiselectionItem);
            } else {
                multiselectionItem = new MultiselectionItem(currentDeviceName, false);
            }
            items.add(multiselectionItem);
        }
        btDevicesSpinner.setItems(items);
        btDevicesSpinner.setSelection(selection);
    }

    public void onAllBtDevicesButtonClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        voiceSettingParametersDbHelper.saveBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES.getVoiceSettingParamTypeId(),
                checked);
        if (checked) {
            findViewById(R.id.bt_when_devices).setVisibility(View.GONE);
        } else {
            findViewById(R.id.bt_when_devices).setVisibility(View.VISIBLE);
        }
    }

    public void onTriggerAllBtDevicesButtonClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        voiceSettingParametersDbHelper.saveBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId(),
                checked);
        if (checked) {
            findViewById(R.id.trigger_bt_when_devices).setVisibility(View.GONE);
        } else {
            findViewById(R.id.trigger_bt_when_devices).setVisibility(View.VISIBLE);
        }
    }

    public void onAllLocationsButtonClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        voiceSettingParametersDbHelper.saveBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId(),
                checked);
        if (checked) {
            findViewById(R.id.tts_setting_locations).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tts_setting_locations).setVisibility(View.VISIBLE);
        }
    }

    private void populateTtsDeviceEnabled() {
        Long enabledVoiceDevices = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId());
        if (enabledVoiceDevices == null) {
            return;
        }
        if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices, 2)) {
            ((CheckBox) findViewById(R.id.tts_to_speaker_enabled)).setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices, 1)) {
            ((CheckBox) findViewById(R.id.tts_when_wired_enabled)).setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices, 0)) {
            ((CheckBox) findViewById(R.id.tts_when_bt_enabled)).setChecked(true);
        }
    }

    private void populateDayOfWeeks() {
        Long daysOfWeek = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId());
        if (daysOfWeek == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", applicationLocale);
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 6)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_mon);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 5)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_tue);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 4)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_wed);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 3)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_thu);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 2)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_fri);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 1)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_sat);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 0)) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            CheckBox triggerCheckBox = findViewById(R.id.voice_trigger_sun);
            triggerCheckBox.setText(simpleDateFormat.format(calendar.getTime()));
            triggerCheckBox.setChecked(true);
        }
    }

    private void setTextTime() {
        Long storedHourMinute = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId());

        Button voiceSettingButton = (Button) findViewById(R.id.button_voice_setting_time);

        if (storedHourMinute == null) {
            voiceSettingButton.setText(getString(R.string.pref_title_tts_time));
            return;
        }

        int hourMinute = storedHourMinute.intValue();
        int hour = hourMinute / 100;
        int minute = hourMinute - (hour * 100);

        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);

        voiceSettingButton.setText(AppPreference.getLocalizedTime(this, c.getTime(), applicationLocale));
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void showTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setVoiceSettingId(voiceSettingId);
        newFragment.setVoiceSettingParametersDbHelper(voiceSettingParametersDbHelper);
        newFragment.setApplicationLocale(applicationLocale);
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private Long voiceSettingId;
        private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
        private Locale applicationLocale;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Long storedHourMinute = voiceSettingParametersDbHelper.getLongParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId());

            int hour;
            int minute;
            if (storedHourMinute == null) {
                final Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            } else {
                int hourMinute = storedHourMinute.intValue();
                hour = hourMinute / 100;
                minute = hourMinute - (hour * 100);
            }
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            long storedHourMinute = hourOfDay*100+minute;
            voiceSettingParametersDbHelper.saveLongParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId(),
                    storedHourMinute);
            setNewTextTime(hourOfDay, minute);
        }

        private void setNewTextTime(int hourOfDay, int minute) {
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);

            Button voiceSettingButton = (Button) getActivity().findViewById(R.id.button_voice_setting_time);
            voiceSettingButton.setText(AppPreference.getLocalizedTime(getContext(), c.getTime(), applicationLocale));
            TimeUtils.setupAlarmForVoice(getActivity());
        }

        public void setVoiceSettingId(Long voiceSettingId) {
            this.voiceSettingId = voiceSettingId;
        }

        public void setVoiceSettingParametersDbHelper(VoiceSettingParametersDbHelper voiceSettingParametersDbHelper) {
            this.voiceSettingParametersDbHelper = voiceSettingParametersDbHelper;
        }

        public void setApplicationLocale(Locale applicationLocale) {
            this.applicationLocale = applicationLocale;
        }
    }

    public void populateTriggerType() {
        Spinner spinner = (Spinner) findViewById(R.id.trigger_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.voice_trigger_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        Long currentTriggerId = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId());
        if (currentTriggerId != null) {
            int currentTriggerIdInt = currentTriggerId.intValue();
            spinner.setSelection(currentTriggerIdInt);
            triggerTypeChanged(currentTriggerIdInt);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                voiceSettingParametersDbHelper.saveLongParam(
                        voiceSettingId,
                        VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId(),
                        position);

                triggerTypeChanged(position);
                if (position == 2) {
                    TimeUtils.setupAlarmForVoice(getBaseContext());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void triggerTypeChanged(int currentTriggerId) {
        if (currentTriggerId != 2) {
            findViewById(R.id.button_voice_setting_time).setVisibility(View.GONE);
            findViewById(R.id.pref_title_tts_trigger_days_panel).setVisibility(View.GONE);
        } else {
            findViewById(R.id.button_voice_setting_time).setVisibility(View.VISIBLE);
            findViewById(R.id.pref_title_tts_trigger_days_panel).setVisibility(View.VISIBLE);
        }
        if (currentTriggerId != 1) {
            findViewById(R.id.pref_title_tts_bt_trigger_panel).setVisibility(View.GONE);
            findViewById(R.id.enabled_devices_panel).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.pref_title_tts_bt_trigger_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.enabled_devices_panel).setVisibility(View.GONE);
        }
    }

    private void populateLocations() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        List<Location> currentLocations = locationsDbHelper.getAllRows();
        MultiSelectionLocationSpinner btDevicesSpinner = findViewById(R.id.tts_setting_locations);
        btDevicesSpinner.setVoiceSettingId(voiceSettingId);

        ArrayList<MultiselectionLocationItem> items = new ArrayList<>();
        ArrayList<MultiselectionLocationItem> selection = new ArrayList<>();
        ArrayList<String> selectedItems = new ArrayList<>();

        String enabledBtDevices = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
        Boolean enabledVoiceDevices = voiceSettingParametersDbHelper.getBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
        if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
            ((CheckBox) findViewById(R.id.tts_setting_all_locations)).setChecked(true);
            findViewById(R.id.tts_setting_locations).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tts_setting_locations).setVisibility(View.VISIBLE);
        }

        if (enabledBtDevices != null) {
            for (String btDeviceName: enabledBtDevices.split(",")) {
                selectedItems.add(btDeviceName);
            }
        }

        for(Location location: currentLocations) {
            String locationCityForVoice = Utils.getLocationForVoiceFromAddress(location.getAddress());
            MultiselectionLocationItem multiselectionItem;
            if (selectedItems.contains(location.getId().toString())) {
                multiselectionItem = new MultiselectionLocationItem(location.getId(), locationCityForVoice, true);
                selection.add(multiselectionItem);
            } else {
                multiselectionItem = new MultiselectionLocationItem(location.getId(), locationCityForVoice, false);
            }
            items.add(multiselectionItem);
        }
        btDevicesSpinner.setItems(items);
        btDevicesSpinner.setSelection(selection);
    }

    public void onTtsDeviceEnabledButtonClicked(View view) {

        Long enabledVoiceDevices = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId());
        if (enabledVoiceDevices == null) {
            enabledVoiceDevices = 0l;
        }
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.tts_to_speaker_enabled:
                if (checked) {
                    enabledVoiceDevices += TimeUtils.getTwoPower(2);
                } else {
                    enabledVoiceDevices -= TimeUtils.getTwoPower(2);
                }
                break;
            case R.id.tts_when_wired_enabled:
                if (checked) {
                    enabledVoiceDevices += TimeUtils.getTwoPower(1);
                } else {
                    enabledVoiceDevices -= TimeUtils.getTwoPower(1);
                }
                break;
            case R.id.tts_when_bt_enabled:
                if (checked) {
                    enabledVoiceDevices += TimeUtils.getTwoPower(0);
                    findViewById(R.id.tts_when_bt_enabled_panel).setVisibility(View.VISIBLE);
                    findViewById(R.id.bt_all_devices).setVisibility(View.VISIBLE);
                } else {
                    enabledVoiceDevices -= TimeUtils.getTwoPower(0);
                    findViewById(R.id.tts_when_bt_enabled_panel).setVisibility(View.GONE);
                    findViewById(R.id.bt_all_devices).setVisibility(View.GONE);
                }
                break;
        }
        voiceSettingParametersDbHelper.saveLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId(),
                enabledVoiceDevices);
    }

    private void populateTtsSeySetting() {
        Long partsToSay = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_PARTS_TO_SAY.getVoiceSettingParamTypeId());
        if (partsToSay == null) {
            return;
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 0)) {
            ((CheckBox) findViewById(R.id.tts_say_greeting_enabled)).setChecked(true);
            findViewById(R.id.tts_say_greeting_custom_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.tts_say_greeting_custom).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tts_say_greeting_custom_panel).setVisibility(View.GONE);
            findViewById(R.id.tts_say_greeting_custom).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 1)) {
            ((CheckBox) findViewById(R.id.tts_say_greeting_custom)).setChecked(true);
            EditText customText = findViewById(R.id.tts_say_greeting_custom_text_morning);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tts_say_greeting_morning));
            }
            customText = findViewById(R.id.tts_say_greeting_custom_text_day);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tts_say_greeting_day));
            }
            customText = findViewById(R.id.tts_say_greeting_custom_text_evening);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tts_say_greeting_evening));
            }
        } else {
            findViewById(R.id.tts_say_greeting_custom_text_morning).setVisibility(View.GONE);
            findViewById(R.id.tts_say_greeting_custom_text_day).setVisibility(View.GONE);
            findViewById(R.id.tts_say_greeting_custom_text_evening).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 2)) {
            ((CheckBox) findViewById(R.id.tts_say_location_enabled)).setChecked(true);
            findViewById(R.id.tts_say_location_custom_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.tts_say_location_custom).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tts_say_location_custom_panel).setVisibility(View.GONE);
            findViewById(R.id.tts_say_location_custom).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 3)) {
            ((CheckBox) findViewById(R.id.tts_say_location_custom)).setChecked(true);
            EditText customText = findViewById(R.id.tts_say_location_custom_text);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tts_say_current_weather_with_location));
            }
        } else {
            findViewById(R.id.tts_say_location_custom_text).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 4)) {
            ((CheckBox) findViewById(R.id.tts_say_weather_description_enabled)).setChecked(true);
            findViewById(R.id.tts_say_weather_description_custom_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.tts_say_weather_description_custom).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tts_say_weather_description_custom_panel).setVisibility(View.GONE);
            findViewById(R.id.tts_say_weather_description_custom).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 5)) {
            ((CheckBox) findViewById(R.id.tts_say_weather_description_custom)).setChecked(true);
            EditText customText = findViewById(R.id.tts_say_weather_description_custom_text);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tts_say_current_weather));
            }
        } else {
            findViewById(R.id.tts_say_weather_description_custom_text).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 6)) {
            ((CheckBox) findViewById(R.id.tts_say_temperature_enabled)).setChecked(true);
            findViewById(R.id.tts_say_temperature_custom_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.tts_say_temperature_custom).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tts_say_temperature_custom_panel).setVisibility(View.GONE);
            findViewById(R.id.tts_say_temperature_custom).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 7)) {
            ((CheckBox) findViewById(R.id.tts_say_temperature_custom)).setChecked(true);
            EditText customText = findViewById(R.id.tts_say_temperature_custom_text);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tty_say_temperature));
            }
        } else {
            findViewById(R.id.tts_say_temperature_custom_text).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 8)) {
            ((CheckBox) findViewById(R.id.tts_say_wind_enabled)).setChecked(true);
            findViewById(R.id.tts_say_wind_custom_panel).setVisibility(View.VISIBLE);
            findViewById(R.id.tts_say_wind_custom).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.tts_say_wind_custom_panel).setVisibility(View.GONE);
            findViewById(R.id.tts_say_wind_custom).setVisibility(View.GONE);
        }
        if (TimeUtils.isCurrentSettingIndex(partsToSay, 9)) {
            ((CheckBox) findViewById(R.id.tts_say_wind_custom)).setChecked(true);
            EditText customText = findViewById(R.id.tts_say_wind_custom_text);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(R.string.tty_say_wind));
            }
        } else {
            findViewById(R.id.tts_say_wind_custom_text).setVisibility(View.GONE);
        }
    }

    public void onTtsSeySettingButtonClicked(View view) {
        Long partsToSay = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_PARTS_TO_SAY.getVoiceSettingParamTypeId());
        if (partsToSay == null) {
            partsToSay = 0l;
        }
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.tts_say_greeting_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(0);
                    findViewById(R.id.tts_say_greeting_custom_panel).setVisibility(View.VISIBLE);
                    findViewById(R.id.tts_say_greeting_custom).setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(0);
                    findViewById(R.id.tts_say_greeting_custom_panel).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_greeting_custom).setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_greeting_custom:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(1);
                    EditText customText = findViewById(R.id.tts_say_greeting_custom_text_morning);
                    customText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(customText.getText())) {
                        customText.setText(getString(R.string.tts_say_greeting_morning));
                    }
                    customText = findViewById(R.id.tts_say_greeting_custom_text_day);
                    customText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(customText.getText())) {
                        customText.setText(getString(R.string.tts_say_greeting_day));
                    }
                    customText = findViewById(R.id.tts_say_greeting_custom_text_evening);
                    customText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(customText.getText())) {
                        customText.setText(getString(R.string.tts_say_greeting_evening));
                    }
                    TextView originalText = findViewById(R.id.tts_say_greeting_morning_original_text);
                    originalText.setTextColor(Color.GRAY);
                    originalText = findViewById(R.id.tts_say_greeting_day_original_text);
                    originalText.setTextColor(Color.GRAY);
                    originalText = findViewById(R.id.tts_say_greeting_evening_original_text);
                    originalText.setTextColor(Color.GRAY);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(1);
                    findViewById(R.id.tts_say_greeting_custom_text_morning).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_greeting_custom_text_day).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_greeting_custom_text_evening).setVisibility(View.GONE);
                    TextView originalText = findViewById(R.id.tts_say_greeting_morning_original_text);
                    originalText.setTextColor(Color.BLACK);
                    originalText = findViewById(R.id.tts_say_greeting_day_original_text);
                    originalText.setTextColor(Color.BLACK);
                    originalText = findViewById(R.id.tts_say_greeting_evening_original_text);
                    originalText.setTextColor(Color.BLACK);
                }
                break;
            case R.id.tts_say_location_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(2);
                    findViewById(R.id.tts_say_location_custom_panel).setVisibility(View.VISIBLE);
                    findViewById(R.id.tts_say_location_custom).setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(2);
                    findViewById(R.id.tts_say_location_custom_panel).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_location_custom).setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_location_custom:
                partsToSay = enableAndFillCustomText(
                        3,
                        checked,
                        partsToSay,
                        R.id.tts_say_location_custom_text,
                        R.string.tts_say_current_weather_with_location,
                        R.id.tts_say_location_original_text);
                break;
            case R.id.tts_say_weather_description_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(4);
                    findViewById(R.id.tts_say_weather_description_custom_panel).setVisibility(View.VISIBLE);
                    findViewById(R.id.tts_say_weather_description_custom).setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(4);
                    findViewById(R.id.tts_say_weather_description_custom_panel).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_weather_description_custom).setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_weather_description_custom:
                partsToSay = enableAndFillCustomText(
                        5,
                        checked,
                        partsToSay,
                        R.id.tts_say_weather_description_custom_text,
                        R.string.tts_say_current_weather,
                        R.id.tts_say_weather_description_original_text);
                break;
            case R.id.tts_say_temperature_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(6);
                    findViewById(R.id.tts_say_temperature_custom_panel).setVisibility(View.VISIBLE);
                    findViewById(R.id.tts_say_temperature_custom).setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(6);
                    findViewById(R.id.tts_say_temperature_custom_panel).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_temperature_custom).setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_temperature_custom:
                partsToSay = enableAndFillCustomText(
                        7,
                        checked,
                        partsToSay,
                        R.id.tts_say_temperature_custom_text,
                        R.string.tty_say_temperature,
                        R.id.tts_say_temperature_original_text);
                break;
            case R.id.tts_say_wind_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(8);
                    findViewById(R.id.tts_say_wind_custom_panel).setVisibility(View.VISIBLE);
                    findViewById(R.id.tts_say_wind_custom).setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(8);
                    findViewById(R.id.tts_say_wind_custom_panel).setVisibility(View.GONE);
                    findViewById(R.id.tts_say_wind_custom).setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_wind_custom:
                partsToSay = enableAndFillCustomText(
                        9,
                        checked,
                        partsToSay,
                        R.id.tts_say_wind_custom_text,
                        R.string.tty_say_wind,
                        R.id.tts_say_wind_original_text);
                break;
        }
        voiceSettingParametersDbHelper.saveLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_PARTS_TO_SAY.getVoiceSettingParamTypeId(),
                partsToSay);
    }

    private Long enableAndFillCustomText(int index,
                                         boolean checked,
                                         Long partsToSay,
                                         int editTextId,
                                         int defaultSayText,
                                         int originalTextId) {
        if (checked) {
            partsToSay += TimeUtils.getTwoPower(index);
            EditText customText = findViewById(editTextId);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(defaultSayText));
            }
            TextView originalText = findViewById(originalTextId);
            originalText.setTextColor(Color.GRAY);
        } else {
            partsToSay -= TimeUtils.getTwoPower(index);
            findViewById(editTextId).setVisibility(View.GONE);
            TextView originalText = findViewById(originalTextId);
            originalText.setTextColor(Color.BLACK);
        }
        return partsToSay;
    }

    public void onRadioButtonClicked(View view) {
        Long daysOfWeek = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId());
        if (daysOfWeek == null) {
            daysOfWeek = 0l;
        }
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.voice_trigger_mon:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(6);
                } else {
                    daysOfWeek += TimeUtils.getTwoPower(6);
                }
                break;
            case R.id.voice_trigger_tue:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(5);
                } else {
                    daysOfWeek -= TimeUtils.getTwoPower(5);
                }
                break;
            case R.id.voice_trigger_wed:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(4);
                } else {
                    daysOfWeek -= TimeUtils.getTwoPower(4);
                }                break;
            case R.id.voice_trigger_thu:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(3);
                } else {
                    daysOfWeek -= TimeUtils.getTwoPower(3);
                }
                break;
            case R.id.voice_trigger_fri:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(2);
                } else {
                    daysOfWeek -= TimeUtils.getTwoPower(2);
                }
                break;
            case R.id.voice_trigger_sat:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(1);
                } else {
                    daysOfWeek -= TimeUtils.getTwoPower(1);
                }
                break;
            case R.id.voice_trigger_sun:
                if (checked) {
                    daysOfWeek += TimeUtils.getTwoPower(0);
                } else {
                    daysOfWeek -= TimeUtils.getTwoPower(0);
                }
                break;
        }
        voiceSettingParametersDbHelper.saveLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId(),
                daysOfWeek);
        TimeUtils.setupAlarmForVoice(getBaseContext());
    }
}
