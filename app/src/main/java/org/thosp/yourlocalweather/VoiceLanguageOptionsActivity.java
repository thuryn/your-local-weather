package org.thosp.yourlocalweather;

import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NavUtils;

import org.thosp.yourlocalweather.databinding.ActivityVoiceLanguageOptionsBinding;
import org.thosp.yourlocalweather.model.VoiceSettingParametersDbHelper;
import org.thosp.yourlocalweather.utils.AppPreference;
import org.thosp.yourlocalweather.utils.VoiceSettingParamType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.thosp.yourlocalweather.utils.LogToFile.appendLog;

public class VoiceLanguageOptionsActivity extends BaseActivity {

    public static final String TAG = "VoiceLanguageOptionsActivity";

    // 1. Deklarujeme automaticky vygenerovanou třídu bindingu
    private ActivityVoiceLanguageOptionsBinding binding;

    private VoiceSettingParametersDbHelper voiceSettingParametersDbHelper;
    private Locale applicationLocale;
    private Set<Locale> ttsAvailableLanguages;
    private TextToSpeech tts;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            recreateTts();
            ttsAvailableLanguages = tts.getAvailableLanguages();
            populateLanguageOptionsSpinner();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        applicationLocale = new Locale(AppPreference.getInstance().getLanguage(this));

        // 2. Inicializujeme binding objekt a nastavíme root view
        binding = ActivityVoiceLanguageOptionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupActionBar();

        // 3. Přistupujeme k View prvkům bezpečně napřímo
        binding.prefTitleTtsLangAppLocaleId.setText(getString(R.string.pref_title_tts_lang_app_locale) + " " + applicationLocale.getDisplayName());
        Linkify.addLinks(binding.prefTitleTtsAppGeneralInfoId, Linkify.WEB_URLS);

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
            binding.voiceLanguageOptionsTtsDoesNotSupportLang.setVisibility(View.VISIBLE);
            binding.voiceLanguageOptionsTtsDoesNotSupportLang.setText(getString(R.string.pref_title_tts_presence));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.ttsLanguages.setAdapter(adapter);
        if (selection != null) {
            binding.ttsLanguages.setSelection(selection);
        }
        binding.ttsLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        recreateTts();
    }

    private void recreateTts() {
        TextToSpeech.OnInitListener onInitListener = status -> {
            appendLog(getBaseContext(), TAG, "TextToSpeech initialized with status: " + status);
            if ((tts != null) && (status == TextToSpeech.SUCCESS)) {
                prepareTtsLanguages();
            }
        };
        tts = new TextToSpeech(getBaseContext(), onInitListener);
    }

    private void prepareTtsLanguages() {
        ttsAvailableLanguages = tts.getAvailableLanguages();
        if (ttsAvailableLanguages.isEmpty()) {
            timerHandler.postDelayed(timerRunnable, 1000);
            return;
        }
        populateLanguageOptionsSpinner();
    }

    private void setupActionBar() {
        setSupportActionBar(binding.voiceSettingLangOptToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}