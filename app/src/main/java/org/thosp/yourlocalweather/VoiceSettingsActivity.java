package org.thosp.yourlocalweather;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.thosp.yourlocalweather.model.CurrentWeatherDbHelper;
import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.model.WeatherForecastDbHelper;
import org.thosp.yourlocalweather.utils.ApiKeys;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.TimeUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VoiceSettingsActivity extends BaseActivity {

    public static final String TAG = "SearchActivity";

    private VoiceSettingsAdapter voiceSettingsAdapter;
    private RecyclerView recyclerView;
    private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
    private Locale applicationLocale;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        applicationLocale = new Locale(PreferenceUtil.getLanguage(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        setContentView(R.layout.activity_voice_settings);

        setupActionBar();
        setupRecyclerView();
        voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        voiceSettingsAdapter = new VoiceSettingsAdapter(voiceSettingParametersDbHelper.getAllSettingIds());
        recyclerView.setAdapter(voiceSettingsAdapter);
    }

    public synchronized void addVoiceSetting(View view) {
        long newVoiceSettingId;
        if ((voiceSettingsAdapter.voiceSettingIds == null) || voiceSettingsAdapter.voiceSettingIds.isEmpty()) {
            newVoiceSettingId = 1;
        } else {
            newVoiceSettingId = Collections.max(voiceSettingsAdapter.voiceSettingIds) + 1;
        }
        voiceSettingParametersDbHelper.saveLongParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId(),
                0);
        voiceSettingParametersDbHelper.saveBooleanParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_LOCATIONS.getVoiceSettingParamTypeId(),
                true);
        voiceSettingParametersDbHelper.saveLongParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_VOICE_DEVICES.getVoiceSettingParamTypeId(),
                7);
        voiceSettingParametersDbHelper.saveBooleanParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_ENABLED_WHEN_BT_DEVICES.getVoiceSettingParamTypeId(),
                true);
        voiceSettingParametersDbHelper.saveLongParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_PARTS_TO_SAY.getVoiceSettingParamTypeId(),
                341);
        voiceSettingParametersDbHelper.saveLongParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId(),
                127);
        voiceSettingParametersDbHelper.saveBooleanParam(
                newVoiceSettingId,
                VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId(),
                true);
        moveToAddVoiceSettingsActivity(newVoiceSettingId);
    }

    private void moveToAddVoiceSettingsActivity(long newVoiceSettingId) {
        Intent intent = new Intent(VoiceSettingsActivity.this, AddVoiceSettingActivity.class);
        intent.putExtra("voiceSettingId", newVoiceSettingId);
        startActivity(intent);
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.voice_setting_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(VoiceSettingsActivity.this));

        final VoiceSettingsSwipeController swipeController = new VoiceSettingsSwipeController(new LocationsSwipeControllerActions() {
            @Override
            public void onRightClicked(int position) {
                deleteVoiceSetting(position);
            }

            @Override
            public void onLeftClicked(int position) {
                moveToAddVoiceSettingsActivity(voiceSettingsAdapter.voiceSettingIds.get(position));
            }
        }, this);

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);

        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });

        itemTouchhelper.attachToRecyclerView(recyclerView);
    }

    private void deleteVoiceSetting(int position) {
        Long voiceSettingId = voiceSettingsAdapter.voiceSettingIds.get(position);
        voiceSettingParametersDbHelper.deleteAllSettings(voiceSettingId);
        voiceSettingsAdapter.voiceSettingIds.remove(position);
        voiceSettingsAdapter.notifyItemRemoved(position);
        voiceSettingsAdapter.notifyItemRangeChanged(position, voiceSettingsAdapter.getItemCount());
        List<Long> voiceSettingIds = voiceSettingParametersDbHelper.getAllSettingIds();
        voiceSettingsAdapter = new VoiceSettingsAdapter(voiceSettingIds);
        recyclerView.setAdapter(voiceSettingsAdapter);
        TimeUtils.setupAlarmForVoice(getBaseContext());
    }

    @Override
    protected void updateUI() {
    }

    public class VoiceSettingHolder extends RecyclerView.ViewHolder {

        private Long voiceSettingId;
        private TextView voiceSettingIdView;
        private TextView voiceSettingTypeView;
        private TextView voiceSettingAddInfo1View;
        private TextView voiceSettingAddInfo2View;

        VoiceSettingHolder(View itemView) {
            super(itemView);
            voiceSettingIdView = itemView.findViewById(R.id.voice_setting_id);
            voiceSettingTypeView = itemView.findViewById(R.id.voice_setting_type);
            voiceSettingAddInfo1View = itemView.findViewById(R.id.voice_setting_add_info_1);
            voiceSettingAddInfo2View = itemView.findViewById(R.id.voice_setting_add_info_2);

        }

        void bindVoiceSetting(Long voiceSettingId) {
            this.voiceSettingId = voiceSettingId;
            if (voiceSettingId == null) {
                return;
            }
            Long triggerType = voiceSettingParametersDbHelper.getLongParam(
                    voiceSettingId,
                    VoiceSettingParamType.VOICE_SETTING_TRIGGER_TYPE.getVoiceSettingParamTypeId());
            voiceSettingIdView.setText(getString(R.string.pref_title_tts_trigger_type_label));
            if (triggerType != null) {
                String triggerTypeName = "";
                String addInfo1 = "";
                String addInfo2 = "";
                if (triggerType == 0) {
                    triggerTypeName = getString(R.string.voice_setting_trigger_on_weather_update);
                } else if (triggerType == 1) {
                    triggerTypeName = getString(R.string.voice_setting_trigger_when_bt_connected);

                    Boolean enabledVoiceDevices = voiceSettingParametersDbHelper.getBooleanParam(
                            voiceSettingId,
                            VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId());
                    if ((enabledVoiceDevices != null) && enabledVoiceDevices) {
                        addInfo1 = getString(R.string.pref_title_tts_bt_all);
                    } else {
                        addInfo1 = voiceSettingParametersDbHelper.getStringParam(
                                voiceSettingId,
                                VoiceSettingParamType.VOICE_SETTING_TRIGGER_ENABLED_BT_DEVICES.getVoiceSettingParamTypeId());
                    }

                } else if (triggerType == 2) {
                    triggerTypeName = getString(R.string.voice_setting_trigger_at_time);
                    Long storedHourMinute = voiceSettingParametersDbHelper.getLongParam(
                            voiceSettingId,
                            VoiceSettingParamType.VOICE_SETTING_TIME_TO_START.getVoiceSettingParamTypeId());

                    int hour;
                    int minute;
                    if (storedHourMinute != null) {
                        int hourMinute = storedHourMinute.intValue();
                        hour = hourMinute / 100;
                        minute = hourMinute - (hour * 100);
                        final Calendar c = Calendar.getInstance();
                        c.set(Calendar.HOUR_OF_DAY, hour);
                        c.set(Calendar.MINUTE, minute);
                        addInfo1 = AppPreference.getLocalizedTime(getBaseContext(), c.getTime(), applicationLocale);
                    }
                    Calendar calendar = Calendar.getInstance();
                    Long daysOfWeek = voiceSettingParametersDbHelper.getLongParam(
                            voiceSettingId,
                            VoiceSettingParamType.VOICE_SETTING_TRIGGER_DAY_IN_WEEK.getVoiceSettingParamTypeId());
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", applicationLocale);
                    if (daysOfWeek != null) {
                        StringBuilder enabledDays = new StringBuilder();
                        boolean isFirst = false;
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 6)) {
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                            isFirst = true;
                        }
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 5)) {
                            if (isFirst) {
                                enabledDays.append(", ");
                            }
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                            isFirst = true;
                        }
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 4)) {
                            if (isFirst) {
                                enabledDays.append(", ");
                            }
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                            isFirst = true;
                        }
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 3)) {
                            if (isFirst) {
                                enabledDays.append(", ");
                            }
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                            isFirst = true;
                        }
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 2)) {
                            if (isFirst) {
                                enabledDays.append(", ");
                            }
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                            isFirst = true;
                        }
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 1)) {
                            if (isFirst) {
                                enabledDays.append(", ");
                            }
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                            isFirst = true;
                        }
                        if (TimeUtils.isCurrentSettingIndex(daysOfWeek, 0)) {
                            if (isFirst) {
                                enabledDays.append(", ");
                            }
                            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                            enabledDays.append(simpleDateFormat.format(calendar.getTime()));
                        }
                        addInfo2 = enabledDays.toString();
                    }
                }
                voiceSettingTypeView.setText(triggerTypeName);
                voiceSettingAddInfo1View.setText(addInfo1);
                voiceSettingAddInfo2View.setText(addInfo2);
            }

        }

        public Long getVoiceSettingId() {
            return voiceSettingId;
        }
    }

    private class VoiceSettingsAdapter extends RecyclerView.Adapter<VoiceSettingHolder> {

        private List<Long> voiceSettingIds;

        VoiceSettingsAdapter(List<Long> voiceSettingIds) {
            this.voiceSettingIds = voiceSettingIds;
        }

        @Override
        public int getItemCount() {
            if (voiceSettingIds != null)
                return voiceSettingIds.size();

            return 0;
        }

        @Override
        public void onBindViewHolder(VoiceSettingHolder locationHolder, int position) {
            locationHolder.bindVoiceSetting(voiceSettingIds.get(position));
        }

        @Override
        public VoiceSettingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(VoiceSettingsActivity.this);
            View v = inflater.inflate(R.layout.voice_setting_item, parent, false);
            return new VoiceSettingHolder(v);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

