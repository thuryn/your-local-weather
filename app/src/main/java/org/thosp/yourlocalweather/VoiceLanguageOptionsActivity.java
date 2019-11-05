package org.thosp.yourlocalweather;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import org.thosp.yourlocalweather.model.Location;
import org.thosp.yourlocalweather.model.LocationsDbHelper;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.PreferenceUtil;
import org.thosp.yourlocalweather.utils.TimeUtils;
import org.thosp.yourlocalweather.utils.Utils;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class VoiceLanguageOptionsActivity extends BaseActivity {

    public static final String TAG = "VoiceLanguageOptionsActivity";

    private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
    private Locale applicationLocale;
    private Set<Locale> ttsAvailableLanguages;
    private TextToSpeech tts;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ((YourLocalWeather) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        applicationLocale = new Locale(PreferenceUtil.getLanguage(this));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        setContentView(R.layout.activity_voice_language_options);

        setupActionBar();
        TextView appLang = findViewById(R.id.pref_title_tts_lang_app_locale_id);
        appLang.setText(getString(R.string.pref_title_tts_lang_app_locale) + " " + applicationLocale.getDisplayName());
        TextView ttsAppGeneralInfo = findViewById(R.id.pref_title_tts_app_general_info_id);
        Linkify.addLinks(ttsAppGeneralInfo, Linkify.WEB_URLS);
        voiceSettingParametersDbHelper = VoiceSettingParametersDbHelper.getInstance(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        initTts();
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

    @Override
    protected void updateUI() {
    }

    private void populateLanguageOptionsSpinner() {

        String localeForVoiceId = voiceSettingParametersDbHelper.getGeneralStringParam(VoiceSettingParamType.VOICE_SETTING_VOICE_LANG.getVoiceSettingParamTypeId());
        Spinner ttsLanguageOptionsSpinner = findViewById(R.id.tts_languages);
        Integer selection = 0;
        String[] spinnerArray =  new String[ttsAvailableLanguages.size() + 1];
        final Map<Integer,String> spinnerMap = new HashMap<>();
        spinnerMap.put(0, "Default");
        spinnerArray[0] = getString(R.string.pref_title_tts_lang_app_default);
        if (localeForVoiceId == null) {
           selection = 0;
        }
        int i = 1;
        boolean supportedLanguage = false;
        for(Locale locale: ttsAvailableLanguages) {
            if (locale.getLanguage().equals(localeForVoiceId)) {
                selection = i;
            }
            if (locale.getISO3Language().equals(applicationLocale.getISO3Language())) {
                supportedLanguage = true;
            }
            spinnerMap.put(i, locale.getLanguage());
            spinnerArray[i] = locale.getDisplayName();
            i++;
        }

        if (!supportedLanguage) {
            TextView langNotSupported = findViewById(R.id.voice_language_options_tts_does_not_support_lang);
            langNotSupported.setVisibility(View.VISIBLE);
            langNotSupported.setText(getString(R.string.pref_title_tts_presence));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ttsLanguageOptionsSpinner.setAdapter(adapter);
        if (selection != null) {
            ttsLanguageOptionsSpinner.setSelection(selection);
        }
        ttsLanguageOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                voiceSettingParametersDbHelper.saveGeneralStringParam(
                        VoiceSettingParamType.VOICE_SETTING_VOICE_LANG.getVoiceSettingParamTypeId(),
                        spinnerMap.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initTts() {
        if (tts != null) {
            prepareTtsLanguages();
            return;
        }
        TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                appendLog(getBaseContext(), TAG, "TextToSpeech initialized with status: " + status);
                if ((tts != null) && (status == TextToSpeech.SUCCESS)) {
                    prepareTtsLanguages();
                }
            }
        };
        tts = new TextToSpeech(getBaseContext(), onInitListener);
    }

    private void prepareTtsLanguages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsAvailableLanguages = tts.getAvailableLanguages();
        } else {
            ttsAvailableLanguages = new HashSet<>();
            Locale[] locales = Locale.getAvailableLocales();
            for (Locale loc : locales) {
                if (!loc.toString().toLowerCase().contains("os")
                        && tts.isLanguageAvailable(loc) >= 0) {
                    ttsAvailableLanguages.add(loc);
                }
            }
        }
        populateLanguageOptionsSpinner();
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.voice_setting_lang_opt_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}

