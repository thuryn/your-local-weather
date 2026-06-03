package org.thosp.yourlocalweather;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import org.thosp.yourlocalweather.databinding.ActivityAddVoiceSettingBinding;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.TimeUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AddVoiceSettingActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    // 1. Deklarujeme instanční proměnnou bindingu
    private ActivityAddVoiceSettingBinding binding;

    private Long voiceSettingId;
    private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
    private Locale applicationLocale;
    private String timeStylePreference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // 2. Inicializujeme View Binding
        binding = ActivityAddVoiceSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActionBar();
        YourLocalWeather.executor.submit(() -> {
            voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(this);
            applicationLocale = new Locale(AppPreference.getInstance().getLanguage(this));
            timeStylePreference = AppPreference.getTimeStylePreference(this);

            updateItemsFromDb();
            populateTriggerType();
        });
    }

    @Override
    protected void updateUI() {
    }

    private boolean checkExistenceAndBtPermissions() {
        return (Utils.getBluetoothAdapter(getBaseContext()) != null) &&
                ContextCompat.checkSelfPermission(AddVoiceSettingActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateItemsFromDb() {
        voiceSettingId = getIntent().getLongExtra("voiceSettingId", 0);
        setTextTime();
        populateDayOfWeeks();
        populateTtsDeviceEnabled();
        populateTtsSeySetting();
        populateTextes();

        // Předáváme přímo konkrétní View komponenty namísto původních int ID
        populateTriggerBtDevices(binding.triggerBtWhenDevices, binding.triggerBtAllDevices, VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES);
        populateBtDevices(binding.btWhenDevices, binding.btAllDevices, VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES);
        populateLocations();
    }

    private void populateTextes() {
        setTextHandler(binding.ttsSayGreetingCustomTextMorning, VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_MORNING);
        setTextHandler(binding.ttsSayGreetingCustomTextDay, VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_DAY);
        setTextHandler(binding.ttsSayGreetingCustomTextEvening, VoiceSettingParamType.VOICE_SETTING_GREETING_CUSTOM_EVENING);
        setTextHandler(binding.ttsSayLocationCustomText, VoiceSettingParamType.VOICE_SETTING_LOCATION_CUSTOM);
        setTextHandler(binding.ttsSayWeatherDescriptionCustomText, VoiceSettingParamType.VOICE_SETTING_WEATHER_DESCRIPTION_CUSTOM);
        setTextHandler(binding.ttsSayTemperatureCustomText, VoiceSettingParamType.VOICE_SETTING_TEMPERATURE_CUSTOM);
        setTextHandler(binding.ttsSayWindCustomText, VoiceSettingParamType.VOICE_SETTING_WIND_CUSTOM);
    }

    private void setTextHandler(EditText editText, final VoiceSettingParamType paramType) {
        String originalValue = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                paramType.getVoiceSettingParamTypeId());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.setText(originalValue, TextView.BufferType.EDITABLE);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable s) {}

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.length() != 0)
                            voiceSettingParametersDbHelper.saveStringParam(
                                    voiceSettingId,
                                    paramType.getVoiceSettingParamTypeId(),
                                    s.toString());
                    }
                });
            }
        });
    }

    private void populateTriggerBtDevices(MultiSelectionTriggerSpinner btDevicesSpinner, CheckBox allBtCheckbox, VoiceSettingParamType voiceSettingParamType) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btDevicesSpinner.setVoiceSettingId(voiceSettingId);

                if (!checkExistenceAndBtPermissions()) {
                    btDevicesSpinner.setVisibility(View.GONE);
                    allBtCheckbox.setVisibility(View.GONE);
                } else {
                    btDevicesSpinner.setVisibility(View.VISIBLE);
                    allBtCheckbox.setVisibility(View.VISIBLE);
                }
            }
        });
        BluetoothAdapter bluetoothAdapter = Utils.getBluetoothAdapter(getBaseContext());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
                    allBtCheckbox.setChecked(true);
                    binding.triggerBtWhenDevices.setVisibility(View.GONE);
                } else {
                    binding.triggerBtWhenDevices.setVisibility(View.VISIBLE);
                }

                if (enabledBtDevices != null) {
                    Collections.addAll(selectedItems, enabledBtDevices.split(","));
                }

                if (ContextCompat.checkSelfPermission(AddVoiceSettingActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                for(BluetoothDevice bluetoothDevice: bluetoothDeviceSet) {
                    String currentDeviceName = bluetoothDevice.getName();
                    String currentDeviceAddress = bluetoothDevice.getAddress();
                    MultiselectionItem multiselectionItem;
                    if (selectedItems.contains(currentDeviceAddress)) {
                        multiselectionItem = new MultiselectionItem(currentDeviceName, currentDeviceAddress, true);
                        selection.add(multiselectionItem);
                    } else {
                        multiselectionItem = new MultiselectionItem(currentDeviceName, currentDeviceAddress,false);
                    }
                    items.add(multiselectionItem);
                }
                btDevicesSpinner.setItems(items);
                btDevicesSpinner.setSelection(selection);
            }
        });
    }

    private void populateBtDevices(MultiSelectionSpinner btDevicesSpinner, CheckBox allBtCheckbox, VoiceSettingParamType voiceSettingParamType) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btDevicesSpinner.setVoiceSettingId(voiceSettingId);

                if (!checkExistenceAndBtPermissions()) {
                    btDevicesSpinner.setVisibility(View.GONE);
                    allBtCheckbox.setVisibility(View.GONE);
                    binding.ttsBtDevicePanel.setVisibility(View.GONE);
                } else {
                    btDevicesSpinner.setVisibility(View.VISIBLE);
                    allBtCheckbox.setVisibility(View.VISIBLE);
                    binding.ttsBtDevicePanel.setVisibility(View.VISIBLE);
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        BluetoothAdapter bluetoothAdapter = Utils.getBluetoothAdapter(getBaseContext());
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
                    allBtCheckbox.setChecked(true);
                    binding.btWhenDevices.setVisibility(View.GONE);
                } else {
                    binding.btWhenDevices.setVisibility(View.VISIBLE);
                }

                if (enabledBtDevices != null) {
                    Collections.addAll(selectedItems, enabledBtDevices.split(","));
                }

                if (ContextCompat.checkSelfPermission(AddVoiceSettingActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                for (BluetoothDevice bluetoothDevice : bluetoothDeviceSet) {
                    String currentDeviceName = bluetoothDevice.getName();
                    String currentDeviceAddress = bluetoothDevice.getAddress();
                    MultiselectionItem multiselectionItem;
                    if (selectedItems.contains(currentDeviceAddress)) {
                        multiselectionItem = new MultiselectionItem(currentDeviceName, currentDeviceAddress, true);
                        selection.add(multiselectionItem);
                    } else {
                        multiselectionItem = new MultiselectionItem(currentDeviceName, currentDeviceAddress, false);
                    }
                    items.add(multiselectionItem);
                }
                btDevicesSpinner.setItems(items);
                btDevicesSpinner.setSelection(selection);
            }
        });
    }

    public void onAllBtDevicesButtonClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        voiceSettingParametersDbHelper.saveBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES.getVoiceSettingParamTypeId(),
                checked);
        if (checked) {
            binding.btWhenDevices.setVisibility(View.GONE);
        } else {
            binding.btWhenDevices.setVisibility(View.VISIBLE);
        }
    }

    public void onTriggerAllBtDevicesButtonClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        voiceSettingParametersDbHelper.saveBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId(),
                checked);
        if (checked) {
            binding.triggerBtWhenDevices.setVisibility(View.GONE);
        } else {
            binding.triggerBtWhenDevices.setVisibility(View.VISIBLE);
        }
    }

    public void onAllLocationsButtonClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        voiceSettingParametersDbHelper.saveBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId(),
                checked);
        if (checked) {
            binding.ttsSettingLocations.setVisibility(View.GONE);
        } else {
            binding.ttsSettingLocations.setVisibility(View.VISIBLE);
        }
    }

    private void populateTtsDeviceEnabled() {
        Long enabledVoiceDevices = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId());
        if (enabledVoiceDevices == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices, 2)) {
                    binding.ttsToSpeakerEnabled.setChecked(true);
                }
                if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices, 1)) {
                    binding.ttsWhenWiredEnabled.setChecked(true);
                }
                if (TimeUtils.isCurrentSettingIndex(enabledVoiceDevices, 0)) {
                    binding.ttsWhenBtEnabled.setChecked(true);
                }
            }
        });
    }

    private void populateDayOfWeeks() {
        Long daysOfWeek = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId());
        if (daysOfWeek == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", applicationLocale);

                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                binding.voiceTriggerMon.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 6)) {
                    binding.voiceTriggerMon.setChecked(true);
                }
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                binding.voiceTriggerTue.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 5)) {
                    binding.voiceTriggerTue.setChecked(true);
                }
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                binding.voiceTriggerWed.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 4)) {
                    binding.voiceTriggerWed.setChecked(true);
                }
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                binding.voiceTriggerThu.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 3)) {
                    binding.voiceTriggerThu.setChecked(true);
                }
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                binding.voiceTriggerFri.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 2)) {
                    binding.voiceTriggerFri.setChecked(true);
                }
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                binding.voiceTriggerSat.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 1)) {
                    binding.voiceTriggerSat.setChecked(true);
                }
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                binding.voiceTriggerSun.setText(simpleDateFormat.format(calendar.getTime()));
                if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 0)) {
                    binding.voiceTriggerSun.setChecked(true);
                }
            }
        });
    }

    private void setTextTime() {
        Long storedHourMinute = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId());
        String timeStylePreference = AppPreference.getTimeStylePreference(getBaseContext());

        if (storedHourMinute == null) {
            binding.buttonVoiceSettingTime.setText(getString(R.string.pref_title_tts_time));
            return;
        }

        int hourMinute = storedHourMinute.intValue();
        int hour = hourMinute / 100;
        int minute = hourMinute - (hour * 100);

        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);

        binding.buttonVoiceSettingTime.setText(AppPreference.getLocalizedTime(this, c.getTime(), timeStylePreference, applicationLocale));
    }

    private void setupActionBar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void showTimePickerDialog(View v) {
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setVoiceSettingId(voiceSettingId);
        newFragment.setVoiceSettingParametersDbHelper(voiceSettingParametersDbHelper);
        newFragment.setApplicationLocale(applicationLocale);
        newFragment.setTimeStylePreference(timeStylePreference);
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        private Long voiceSettingId;
        private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
        private Locale applicationLocale;
        private String timeStylePreference;

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
            long storedHourMinute = hourOfDay* 100L +minute;
            voiceSettingParametersDbHelper.saveLongParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId(),
                    storedHourMinute);
            setNewTextTime(hourOfDay, minute, timeStylePreference);
        }

        private void setNewTextTime(int hourOfDay, int minute, String timeStylePreference) {
            final Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);

            if (getActivity() instanceof AddVoiceSettingActivity) {
                AddVoiceSettingActivity act = (AddVoiceSettingActivity) getActivity();
                act.binding.buttonVoiceSettingTime.setText(AppPreference.getLocalizedTime(getContext(), c.getTime(), timeStylePreference, applicationLocale));
                prepareNextTime(new java.lang.ref.WeakReference<>(act), voiceSettingId, timeStylePreference, applicationLocale, voiceSettingParametersDbHelper);
            }
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

        public void setTimeStylePreference(String timeStylePreference) {
            this.timeStylePreference = timeStylePreference;
        }
    }

    public void populateTriggerType() {
        boolean btNotPresentOrEnabled = checkExistenceAndBtPermissions();

        Long currentTriggerId = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId());

        runOnUiThread(() -> {
            ArrayAdapter<CharSequence> adapter;
            if (!btNotPresentOrEnabled) {
                adapter = ArrayAdapter.createFromResource(this,
                        R.array.voice_trigger_type_wo_bt, android.R.layout.simple_spinner_item);
            } else {
                adapter = ArrayAdapter.createFromResource(this,
                        R.array.voice_trigger_type, android.R.layout.simple_spinner_item);
            }
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.triggerType.setAdapter(adapter);

            if (currentTriggerId != null) {
                int currentTriggerIdInt = currentTriggerId.intValue();
                if (!btNotPresentOrEnabled && (currentTriggerIdInt == 2)) {
                    currentTriggerIdInt = 1;
                }
                binding.triggerType.setSelection(currentTriggerIdInt);
                triggerTypeChanged(currentTriggerIdInt);
            }
            binding.triggerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int positionToSave = position;
                    if (!btNotPresentOrEnabled && (position == 1)) {
                        positionToSave++;
                    }
                    voiceSettingParametersDbHelper.saveLongParam(
                            voiceSettingId,
                            VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId(),
                            positionToSave);

                    triggerTypeChanged(positionToSave);
                    if (positionToSave == 2) {
                        prepareNextTime(new java.lang.ref.WeakReference<>(AddVoiceSettingActivity.this), voiceSettingId, timeStylePreference, applicationLocale, voiceSettingParametersDbHelper);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });
    }

    private void triggerTypeChanged(int currentTriggerId) {
        if (currentTriggerId != 2) {
            binding.buttonVoiceSettingTime.setVisibility(View.GONE);
            binding.prefTitleTtsTriggerDaysPanel.setVisibility(View.GONE);
        } else {
            binding.buttonVoiceSettingTime.setVisibility(View.VISIBLE);
            binding.prefTitleTtsTriggerDaysPanel.setVisibility(View.VISIBLE);
        }
        if (currentTriggerId != 1) {
            binding.prefTitleTtsBtTriggerPanel.setVisibility(View.GONE);
            binding.enabledDevicesPanel.setVisibility(View.VISIBLE);
        } else {
            binding.prefTitleTtsBtTriggerPanel.setVisibility(View.VISIBLE);
            binding.enabledDevicesPanel.setVisibility(View.GONE);
        }

        final java.lang.ref.WeakReference<Activity> activityRef = new java.lang.ref.WeakReference<>(this);
        final Long settingId = this.voiceSettingId;
        final String timeStyle = this.timeStylePreference;
        final Locale appLocale = this.applicationLocale;
        final VoiceSettingParametersDbHelper dbHelper = this.voiceSettingParametersDbHelper;

        YourLocalWeather.executor.submit(() -> {
            prepareNextTime(activityRef, settingId, timeStyle, appLocale, dbHelper);
        });
    }

    private void populateLocations() {
        LocationsDbHelper locationsDbHelper = LocationsDbHelper.getInstance(this);
        List<Location> currentLocations = locationsDbHelper.getAllRows();

        ArrayList<MultiselectionLocationItem> items = new ArrayList<>();
        ArrayList<MultiselectionLocationItem> selection = new ArrayList<>();
        ArrayList<String> selectedItems = new ArrayList<>();

        String enabledBtDevices = voiceSettingParametersDbHelper.getStringParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());
        Boolean enabledVoiceDevices = voiceSettingParametersDbHelper.getBooleanParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.ttsSettingLocations.setVoiceSettingId(voiceSettingId);
                if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
                    binding.ttsSettingAllLocations.setChecked(true);
                    binding.ttsSettingLocations.setVisibility(View.GONE);
                } else {
                    binding.ttsSettingLocations.setVisibility(View.VISIBLE);
                }

                if (enabledBtDevices != null) {
                    Collections.addAll(selectedItems, enabledBtDevices.split(","));
                }

                for (Location location : currentLocations) {
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
                binding.ttsSettingLocations.setItems(items);
                binding.ttsSettingLocations.setSelection(selection);
            }
        });
    }

    public void onTtsDeviceEnabledButtonClicked(View view) {
        Long enabledVoiceDevices = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId());
        if (enabledVoiceDevices == null) {
            enabledVoiceDevices = 0L;
        }
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.tts_to_speaker_enabled:
                if (checked) { enabledVoiceDevices += TimeUtils.getTwoPower(2); }
                else { enabledVoiceDevices -= TimeUtils.getTwoPower(2); }
                break;
            case R.id.tts_when_wired_enabled:
                if (checked) { enabledVoiceDevices += TimeUtils.getTwoPower(1); }
                else { enabledVoiceDevices -= TimeUtils.getTwoPower(1); }
                break;
            case R.id.tts_when_bt_enabled:
                if (checked) {
                    enabledVoiceDevices += TimeUtils.getTwoPower(0);
                    binding.ttsWhenBtEnabledPanel.setVisibility(View.VISIBLE);
                    binding.btAllDevices.setVisibility(View.VISIBLE);
                } else {
                    enabledVoiceDevices -= TimeUtils.getTwoPower(0);
                    binding.ttsWhenBtEnabledPanel.setVisibility(View.GONE);
                    binding.btAllDevices.setVisibility(View.GONE);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 0)) {
                    binding.ttsSayGreetingEnabled.setChecked(true);
                    binding.ttsSayGreetingCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayGreetingCustom.setVisibility(View.VISIBLE);
                } else {
                    binding.ttsSayGreetingCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayGreetingCustom.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 1)) {
                    binding.ttsSayGreetingCustom.setChecked(true);
                    binding.ttsSayGreetingCustomTextMorning.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayGreetingCustomTextMorning.getText())) {
                        binding.ttsSayGreetingCustomTextMorning.setText(getString(R.string.tts_say_greeting_morning));
                    }
                    binding.ttsSayGreetingCustomTextDay.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayGreetingCustomTextDay.getText())) {
                        binding.ttsSayGreetingCustomTextDay.setText(getString(R.string.tts_say_greeting_day));
                    }
                    binding.ttsSayGreetingCustomTextEvening.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayGreetingCustomTextEvening.getText())) {
                        binding.ttsSayGreetingCustomTextEvening.setText(getString(R.string.tts_say_greeting_evening));
                    }
                } else {
                    binding.ttsSayGreetingCustomTextMorning.setVisibility(View.GONE);
                    binding.ttsSayGreetingCustomTextDay.setVisibility(View.GONE);
                    binding.ttsSayGreetingCustomTextEvening.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 2)) {
                    binding.ttsSayLocationEnabled.setChecked(true);
                    binding.ttsSayLocationCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayLocationCustom.setVisibility(View.VISIBLE);
                    binding.ttsSayWeatherDescriptionPanel.setVisibility(View.GONE);
                    binding.ttsSayWeatherDescriptionCustomPanel.setVisibility(View.GONE);
                } else {
                    binding.ttsSayLocationCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayLocationCustom.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 3)) {
                    binding.ttsSayLocationCustom.setChecked(true);
                    binding.ttsSayLocationCustomText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayLocationCustomText.getText())) {
                        binding.ttsSayLocationCustomText.setText(getString(R.string.tts_say_current_weather_with_location));
                    }
                } else {
                    binding.ttsSayLocationCustomText.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 4)) {
                    binding.ttsSayWeatherDescriptionEnabled.setChecked(true);
                    binding.ttsSayWeatherDescriptionCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayWeatherDescriptionCustom.setVisibility(View.VISIBLE);
                    binding.ttsSayLocationEnabledPanel.setVisibility(View.GONE);
                    binding.ttsSayLocationCustomPanel.setVisibility(View.GONE);
                } else {
                    binding.ttsSayWeatherDescriptionCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayWeatherDescriptionCustom.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 5)) {
                    binding.ttsSayWeatherDescriptionCustom.setChecked(true);
                    binding.ttsSayWeatherDescriptionCustomText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayWeatherDescriptionCustomText.getText())) {
                        binding.ttsSayWeatherDescriptionCustomText.setText(getString(R.string.tts_say_current_weather));
                    }
                } else {
                    binding.ttsSayWeatherDescriptionCustomText.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 6)) {
                    binding.ttsSayTemperatureEnabled.setChecked(true);
                    binding.ttsSayTemperatureCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayTemperatureCustom.setVisibility(View.VISIBLE);
                } else {
                    binding.ttsSayTemperatureCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayTemperatureCustom.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 7)) {
                    binding.ttsSayTemperatureCustom.setChecked(true);
                    binding.ttsSayTemperatureCustomText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayTemperatureCustomText.getText())) {
                        binding.ttsSayTemperatureCustomText.setText(getString(R.string.tty_say_temperature));
                    }
                } else {
                    binding.ttsSayTemperatureCustomText.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 8)) {
                    binding.ttsSayWindEnabled.setChecked(true);
                    binding.ttsSayWindCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayWindCustom.setVisibility(View.VISIBLE);
                } else {
                    binding.ttsSayWindCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayWindCustom.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 9)) {
                    binding.ttsSayWindCustom.setChecked(true);
                    binding.ttsSayWindCustomText.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayWindCustomText.getText())) {
                        binding.ttsSayWindCustomText.setText(getString(R.string.tty_say_wind));
                    }
                } else {
                    binding.ttsSayWindCustomText.setVisibility(View.GONE);
                }
                if (TimeUtils.isCurrentSettingIndex(partsToSay, 10)) {
                    binding.ttsSayForecastEnabled.setChecked(true);
                }
            }
        });
    }

    public void onTtsSeySettingButtonClicked(View view) {
        Long partsToSay = voiceSettingParametersDbHelper.getLongParam(
                voiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_PARTS_TO_SAY.getVoiceSettingParamTypeId());
        if (partsToSay == null) {
            partsToSay = 0L;
        }
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.tts_say_greeting_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(0);
                    binding.ttsSayGreetingCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayGreetingCustom.setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(0);
                    binding.ttsSayGreetingCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayGreetingCustom.setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_greeting_custom:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(1);
                    binding.ttsSayGreetingCustomTextMorning.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayGreetingCustomTextMorning.getText())) {
                        binding.ttsSayGreetingCustomTextMorning.setText(getString(R.string.tts_say_greeting_morning));
                    }
                    binding.ttsSayGreetingCustomTextDay.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayGreetingCustomTextDay.getText())) {
                        binding.ttsSayGreetingCustomTextDay.setText(getString(R.string.tts_say_greeting_day));
                    }
                    binding.ttsSayGreetingCustomTextEvening.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(binding.ttsSayGreetingCustomTextEvening.getText())) {
                        binding.ttsSayGreetingCustomTextEvening.setText(getString(R.string.tts_say_greeting_evening));
                    }
                    binding.ttsSayGreetingMorningOriginalText.setTextColor(Color.GRAY);
                    binding.ttsSayGreetingDayOriginalText.setTextColor(Color.GRAY);
                    binding.ttsSayGreetingEveningOriginalText.setTextColor(Color.GRAY);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(1);
                    binding.ttsSayGreetingCustomTextMorning.setVisibility(View.GONE);
                    binding.ttsSayGreetingCustomTextDay.setVisibility(View.GONE);
                    binding.ttsSayGreetingCustomTextEvening.setVisibility(View.GONE);
                    binding.ttsSayGreetingMorningOriginalText.setTextColor(Color.BLACK);
                    binding.ttsSayGreetingDayOriginalText.setTextColor(Color.BLACK);
                    binding.ttsSayGreetingEveningOriginalText.setTextColor(Color.BLACK);
                }
                break;
            case R.id.tts_say_location_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(2);
                    binding.ttsSayLocationCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayLocationCustom.setVisibility(View.VISIBLE);
                    binding.ttsSayWeatherDescriptionPanel.setVisibility(View.GONE);
                    binding.ttsSayWeatherDescriptionCustomPanel.setVisibility(View.GONE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(2);
                    binding.ttsSayLocationCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayLocationCustom.setVisibility(View.GONE);
                    binding.ttsSayWeatherDescriptionPanel.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.tts_say_location_custom:
                partsToSay = enableAndFillCustomText(
                        3,
                        checked,
                        partsToSay,
                        binding.ttsSayLocationCustomText,
                        R.string.tts_say_current_weather_with_location,
                        binding.ttsSayLocationOriginalText);
                break;
            case R.id.tts_say_weather_description_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(4);
                    binding.ttsSayWeatherDescriptionCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayWeatherDescriptionCustom.setVisibility(View.VISIBLE);
                    binding.ttsSayLocationEnabledPanel.setVisibility(View.GONE);
                    binding.ttsSayLocationCustomPanel.setVisibility(View.GONE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(4);
                    binding.ttsSayWeatherDescriptionCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayWeatherDescriptionCustom.setVisibility(View.GONE);
                    binding.ttsSayLocationEnabledPanel.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.tts_say_weather_description_custom:
                partsToSay = enableAndFillCustomText(
                        5,
                        checked,
                        partsToSay,
                        binding.ttsSayWeatherDescriptionCustomText,
                        R.string.tts_say_current_weather,
                        binding.ttsSayWeatherDescriptionOriginalText);
                break;
            case R.id.tts_say_temperature_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(6);
                    binding.ttsSayTemperatureCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayTemperatureCustom.setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(6);
                    binding.ttsSayTemperatureCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayTemperatureCustom.setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_temperature_custom:
                partsToSay = enableAndFillCustomText(
                        7,
                        checked,
                        partsToSay,
                        binding.ttsSayTemperatureCustomText,
                        R.string.tty_say_temperature,
                        binding.ttsSayTemperatureOriginalText);
                break;
            case R.id.tts_say_wind_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(8);
                    binding.ttsSayWindCustomPanel.setVisibility(View.VISIBLE);
                    binding.ttsSayWindCustom.setVisibility(View.VISIBLE);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(8);
                    binding.ttsSayWindCustomPanel.setVisibility(View.GONE);
                    binding.ttsSayWindCustom.setVisibility(View.GONE);
                }
                break;
            case R.id.tts_say_wind_custom:
                partsToSay = enableAndFillCustomText(
                        9,
                        checked,
                        partsToSay,
                        binding.ttsSayWindCustomText,
                        R.string.tty_say_wind,
                        binding.ttsSayWindOriginalText);
                break;
            case R.id.tts_say_forecast_enabled:
                if (checked) {
                    partsToSay += TimeUtils.getTwoPower(10);
                } else {
                    partsToSay -= TimeUtils.getTwoPower(10);
                }
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
                                         EditText customText,
                                         int defaultSayText,
                                         TextView originalText) {
        if (checked) {
            partsToSay += TimeUtils.getTwoPower(index);
            customText.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(customText.getText())) {
                customText.setText(getString(defaultSayText));
            }
            originalText.setTextColor(Color.GRAY);
        } else {
            partsToSay -= TimeUtils.getTwoPower(index);
            customText.setVisibility(View.GONE);
            originalText.setTextColor(Color.BLACK);
        }
        return partsToSay;
    }

    private static void prepareNextTime(java.lang.ref.WeakReference<Activity> activityRef, Long voiceSettingId, String timeStylePreference, Locale applicationLocale, VoiceSettingParametersDbHelper voiceSettingParametersDbHelper) {
        Activity context = activityRef.get();
        if (!(context instanceof AddVoiceSettingActivity)) {
            return;
        }
        AddVoiceSettingActivity act = (AddVoiceSettingActivity) context;
        if (act.isFinishing() || act.isDestroyed()) {
            return;
        }

        TimeUtils.setupAlarmForVoice(act);
        Calendar c = Calendar.getInstance();
        Long nextTimeDate = TimeUtils.setupAlarmForVoiceForVoiceSetting(act, voiceSettingId, voiceSettingParametersDbHelper);

        if (nextTimeDate != null) {
            c.setTimeInMillis(nextTimeDate);
            String formattedText = " (-> " + AppPreference.getLocalizedDateTime(act, c.getTime(), false, timeStylePreference, applicationLocale) + ")";

            act.runOnUiThread(() -> {
                if (act.binding != null && !act.isFinishing() && !act.isDestroyed()) {
                    if (act.binding.voiceSettingNextTime != null) {
                        act.binding.voiceSettingNextTime.setText(formattedText);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}