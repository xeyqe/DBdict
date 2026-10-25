package com.xeyqe.myapplication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.appbar.MaterialToolbar;
import com.ichi2.anki.api.AddContentApi;
import com.ichi2.anki.api.NoteInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AnkiSend extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";
    public static final String EXTRA_LANGUAGE = "com.xeyqe.myapplication.EXTRA_LANGUAGE";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TTS_ENGINE = "ttsEngine";
    public static final String TTS_LOCALE = "ttsLocale";
    public static final String TTS_VOICE = "ttsVoice";
    public static final String ANKI_DECK = "ankiDeck";
    private static final int INSTALL_TTS_REQUEST_CODE = 32;

    private final ExecutorService ankiWorker = Executors.newSingleThreadExecutor();
    private final List<Option<String>> engines = new ArrayList<>();
    private final List<Voice> voices = new ArrayList<>();
    private Tts speech;
    private AddContentApi api;
    private EditText front, back;
    private Spinner enginePicker, languagePicker, voicePicker, deckPicker;
    private SwitchMaterial includeAudio;
    private Button preview, send, retry, install, connect;
    private TextView speechStatus, voiceHint, deckStatus;
    private View progress;
    private String languageKey, preferredEngine, preferredLocale, preferredVoice, preferredDeck;
    private String currentEngine, defaultEngine, originalMeaning, initialMeaning;
    private Voice defaultVoice;
    private boolean speechReady, loading, busy, ankiGranted, destroyed;
    private long deckRequest;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_anki_send);
        WindowInsetsHelper.apply(this);
        speech = new Tts(this);
        api = new AddContentApi(this);
        front = findViewById(R.id.editTextFront);
        back = findViewById(R.id.editTextBack);
        enginePicker = findViewById(R.id.spinnerEngine);
        languagePicker = findViewById(R.id.spinnerLocale);
        voicePicker = findViewById(R.id.spinnerVoice);
        deckPicker = findViewById(R.id.spinnerDeck);
        includeAudio = findViewById(R.id.includeAudio);
        preview = findViewById(R.id.buTTS);
        send = findViewById(R.id.button);
        retry = findViewById(R.id.retrySpeech);
        install = findViewById(R.id.installVoices);
        connect = findViewById(R.id.connectAnki);
        progress = findViewById(R.id.ttsProgress);
        speechStatus = findViewById(R.id.speechStatus);
        voiceHint = findViewById(R.id.voiceHint);
        deckStatus = findViewById(R.id.deckStatus);
        MaterialToolbar toolbar = findViewById(R.id.ttsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        languageKey = value(getIntent().getStringExtra(EXTRA_LANGUAGE));
        originalMeaning = value(getIntent().getStringExtra(EXTRA_MEANING));
        initialMeaning = Html.fromHtml(originalMeaning, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        front.setText(Tts.withoutSound(value(getIntent().getStringExtra(EXTRA_WORD))));
        back.setText(initialMeaning);
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        preferredEngine = preferences.getString(languageKey + TTS_ENGINE, "");
        preferredLocale = preferences.getString(languageKey + TTS_LOCALE, "");
        preferredVoice = preferences.getString(languageKey + TTS_VOICE, "");
        preferredDeck = preferences.getString(languageKey + ANKI_DECK, "");
        includeAudio.setChecked(preferences.getBoolean(languageKey + "includeAudio", true));
        if (state != null) {
            preferredEngine = state.getString(TTS_ENGINE, preferredEngine);
            preferredLocale = state.getString(TTS_LOCALE, preferredLocale);
            preferredVoice = state.getString(TTS_VOICE, preferredVoice);
            preferredDeck = state.getString(ANKI_DECK, preferredDeck);
            includeAudio.setChecked(state.getBoolean("includeAudio", includeAudio.isChecked()));
        }
        TextWatcher contentChanged = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderControls(); }
            @Override public void afterTextChanged(Editable text) { }
        };
        front.addTextChangedListener(contentChanged);
        back.addTextChangedListener(contentChanged);
        includeAudio.setOnCheckedChangeListener((button, checked) -> renderControls());
        preview.setOnClickListener(v -> {
            if (!speech.speak(Tts.withoutSound(front.getText().toString()))) {
                speechStatus.setText(R.string.tts_preview_failed);
            }
        });
        retry.setOnClickListener(v -> discoverEngines());
        install.setOnClickListener(v -> installVoices());
        connect.setOnClickListener(v -> {
            if (AddContentApi.getAnkiDroidPackageName(this) == null) {
                toast(R.string.anki_not_installed);
            } else PermissionChecker.checkAnkiApiPermission(this);
        });
        send.setOnClickListener(v -> addFlashcard());
        discoverEngines();
        refreshDecks();
        PermissionChecker.checkAnkiApiPermission(this);
    }

    private void discoverEngines() {
        engines.clear();
        Set<String> found = new HashSet<>();
        for (ResolveInfo info : getPackageManager().queryIntentServices(
                new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)) {
            String name = info.serviceInfo.packageName;
            if (found.add(name)) engines.add(new Option<>(name, info.loadLabel(getPackageManager()).toString(), "", name));
        }
        engines.sort(Comparator.comparing(option -> option.title.toLowerCase(Locale.ROOT)));
        defaultEngine = Settings.Secure.getString(getContentResolver(), Settings.Secure.TTS_DEFAULT_SYNTH);
        if (indexOf(engines, defaultEngine) < 0) defaultEngine = engines.isEmpty() ? null : engines.get(0).id;
        if (engines.isEmpty()) {
            currentEngine = null;
            loading = false;
            speechReady = false;
            speechStatus.setText(R.string.tts_no_engines);
            retry.setVisibility(View.VISIBLE);
            renderControls();
            return;
        }
        String selected = indexOf(engines, preferredEngine) >= 0 ? preferredEngine : defaultEngine;
        bind(enginePicker, engines, selected, option -> {
            if (!option.id.equals(currentEngine)) startEngine(option.id, true);
        });
        startEngine(selected, true);
    }

    private void startEngine(String engine, boolean allowFallback) {
        currentEngine = engine;
        preferredEngine = engine;
        loading = true;
        speechReady = false;
        languagePicker.setAdapter(null);
        voicePicker.setAdapter(null);
        voices.clear();
        speechStatus.setText(R.string.tts_loading);
        retry.setVisibility(View.GONE);
        renderControls();
        speech.initializeTTS(engine, new Tts.InitializationCallback() {
            @Override public void onReady(Set<Voice> available, Voice defaultChoice) {
                if (destroyed) return;
                loading = false;
                defaultVoice = defaultChoice;
                for (Voice voice : available) {
                    Set<String> features = voice.getFeatures();
                    if (features == null || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) voices.add(voice);
                }
                voices.sort(Comparator.comparing(Voice::isNetworkConnectionRequired)
                        .thenComparing(Comparator.comparingInt(Voice::getQuality).reversed())
                        .thenComparing(Voice::getName));
                if (voices.isEmpty()) {
                    speechStatus.setText(R.string.tts_no_voices);
                    retry.setVisibility(View.VISIBLE);
                    renderControls();
                } else fillLanguages();
            }
            @Override public void onFailure() {
                if (destroyed) return;
                if (allowFallback && defaultEngine != null && !defaultEngine.equals(engine)) {
                    startEngine(defaultEngine, false);
                    enginePicker.setSelection(indexOf(engines, defaultEngine));
                    return;
                }
                loading = false;
                speechReady = false;
                speechStatus.setText(R.string.tts_failed);
                retry.setVisibility(View.VISIBLE);
                renderControls();
            }
        });
    }

    private void fillLanguages() {
        List<Option<Locale>> languages = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Voice voice : voices) {
            Locale locale = voice.getLocale();
            String tag = locale.toLanguageTag();
            if (seen.add(tag)) languages.add(new Option<>(tag, locale.getDisplayName(), "", locale));
        }
        languages.sort(Comparator.comparing(option -> option.title.toLowerCase(Locale.ROOT)));
        String selected = preferredLocale;
        if (indexOf(languages, selected) < 0) {
            // Migrate preferences that previously stored a translated language name.
            selected = defaultVoice == null ? Locale.getDefault().toLanguageTag() : defaultVoice.getLocale().toLanguageTag();
            for (Option<Locale> option : languages) {
                if (option.value.getDisplayLanguage().equals(preferredLocale)) { selected = option.id; break; }
            }
        }
        bind(languagePicker, languages, selected, option -> {
            preferredLocale = option.id;
            fillVoices(option.id);
        });
    }

    private void fillVoices(String locale) {
        List<Option<Voice>> choices = new ArrayList<>();
        for (Voice voice : voices) {
            if (!voice.getLocale().toLanguageTag().equals(locale)) continue;
            String quality = getString(voice.getQuality() >= Voice.QUALITY_HIGH ? R.string.voice_quality_high : R.string.voice_quality_standard);
            String mode = getString(voice.isNetworkConnectionRequired() ? R.string.voice_online : R.string.voice_offline);
            choices.add(new Option<>(voice.getName(), mode + " · " + quality, voice.getName(), voice));
        }
        String selected = preferredVoice;
        if (indexOf(choices, selected) < 0 && defaultVoice != null) selected = defaultVoice.getName();
        bind(voicePicker, choices, selected, option -> {
            preferredVoice = option.id;
            speechReady = speech.setVoice(option.value);
            speechStatus.setText(speechReady ? R.string.tts_ready : R.string.tts_voice_failed);
            voiceHint.setText(option.value.isNetworkConnectionRequired() ? R.string.voice_network_hint : R.string.voice_offline_hint);
            retry.setVisibility(speechReady ? View.GONE : View.VISIBLE);
            renderControls();
        });
    }

    private void refreshDecks() {
        long request = ++deckRequest;
        ankiGranted = PermissionChecker.doIHaveAnkiApiPermission(this);
        connect.setVisibility(ankiGranted ? View.GONE : View.VISIBLE);
        deckStatus.setText(ankiGranted ? R.string.anki_loading : R.string.anki_connect_hint);
        deckPicker.setAdapter(null);
        renderControls();
        if (!ankiGranted) return;
        ankiWorker.execute(() -> {
            List<Option<Long>> decks = new ArrayList<>();
            try {
                Map<Long, String> values = api.getDeckList();
                if (values != null) for (Map.Entry<Long, String> entry : values.entrySet()) {
                    decks.add(new Option<>(entry.getValue(), entry.getValue(), "", entry.getKey()));
                }
                decks.sort(Comparator.comparing(option -> option.title.toLowerCase(Locale.ROOT)));
            } catch (RuntimeException ignored) { }
            runOnUiThread(() -> {
                if (destroyed || request != deckRequest) return;
                deckStatus.setText(decks.isEmpty() ? R.string.anki_no_decks : R.string.anki_deck_hint);
                bind(deckPicker, decks, preferredDeck, option -> { preferredDeck = option.id; renderControls(); });
                renderControls();
            });
        });
    }

    private void renderControls() {
        boolean content = !Tts.withoutSound(front.getText().toString()).isEmpty()
                && !back.getText().toString().trim().isEmpty();
        boolean wantsAudio = includeAudio.isChecked() && ankiGranted;
        preview.setEnabled(!busy && speechReady && !front.getText().toString().trim().isEmpty());
        send.setEnabled(!busy && content && (!wantsAudio || speechReady)
                && (!ankiGranted || deckPicker.getSelectedItem() != null));
        send.setText(busy ? R.string.anki_sending : ankiGranted ? R.string.anki_add : R.string.anki_share);
        enginePicker.setEnabled(!busy && !engines.isEmpty());
        languagePicker.setEnabled(!busy && !loading && languagePicker.getCount() > 0);
        voicePicker.setEnabled(!busy && !loading && voicePicker.getCount() > 0);
        deckPicker.setEnabled(!busy && ankiGranted && deckPicker.getCount() > 0);
        includeAudio.setEnabled(!busy && ankiGranted);
        retry.setEnabled(!busy);
        install.setEnabled(!busy && currentEngine != null);
        connect.setEnabled(!busy);
        front.setEnabled(!busy);
        back.setEnabled(!busy);
        progress.setVisibility(loading || busy ? View.VISIBLE : View.GONE);
    }

    private void addFlashcard() {
        String word = Tts.withoutSound(front.getText().toString());
        String editedMeaning = back.getText().toString().trim();
        String meaning = editedMeaning.equals(initialMeaning) ? originalMeaning
                : Html.escapeHtml(editedMeaning).replace("\n", "<br>");
        savePreferences();
        if (!ankiGranted) {
            Intent share = new ShareCompat.IntentBuilder(this).setType("text/plain")
                    .setSubject(word).setText(meaning).getIntent();
            try { startActivity(Intent.createChooser(share, getString(R.string.anki_share))); }
            catch (ActivityNotFoundException error) { toast(R.string.anki_share_failed); }
            return;
        }
        @SuppressWarnings("unchecked") Option<Long> deck = (Option<Long>) deckPicker.getSelectedItem();
        if (deck == null || word.isEmpty()) return;
        busy = true;
        renderControls();
        if (includeAudio.isChecked()) {
            String mediaName = speech.audioMediaName(word);
            speechStatus.setText(R.string.tts_encoding);
            speech.createMediaFile(word, file -> {
                if (file == null) completeSend(R.string.audio_export_failed);
                else ankiWorker.execute(() -> importAndAdd(file, mediaName, word, meaning, deck.value));
            });
        } else ankiWorker.execute(() -> importAndAdd(null, null, word, meaning, deck.value));
    }

    private void importAndAdd(File audio, String name, String word, String meaning, long deck) {
        Uri uri = null;
        try {
            if (audio != null) {
                String host = AddContentApi.getAnkiDroidPackageName(this);
                if (host == null) throw new IllegalStateException();
                uri = FileProvider.getUriForFile(this, getPackageName() + ".files", audio);
                grantUriPermission(host, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String media = api.addMediaFromUri(uri, name, "audio");
                if (media == null) throw new IllegalStateException();
                word += " " + media;
            }
            Long model = null;
            Map<Long, String> models = api.getModelList();
            if (models != null) for (Map.Entry<Long, String> entry : models.entrySet()) {
                if (entry.getValue().equalsIgnoreCase("basic")) { model = entry.getKey(); break; }
            }
            if (model == null) model = api.addNewBasicModel("basic");
            if (model == null) throw new IllegalStateException();
            List<NoteInfo> duplicates = api.findDuplicateNotes(model, word);
            if (duplicates != null) for (NoteInfo note : duplicates) {
                if (note.getFields().length > 1 && meaning.equals(note.getFields()[1])) {
                    completeSend(R.string.anki_duplicate);
                    return;
                }
            }
            completeSend(api.addNote(model, deck, new String[]{word, meaning}, null) == null
                    ? R.string.anki_failed : R.string.anki_added);
        } catch (RuntimeException error) {
            completeSend(R.string.anki_failed);
        } finally {
            if (uri != null) revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (audio != null) audio.delete();
        }
    }

    private void completeSend(int message) {
        runOnUiThread(() -> {
            if (destroyed) return;
            busy = false;
            speechStatus.setText(speechReady ? R.string.tts_ready : R.string.tts_failed);
            renderControls();
            toast(message);
        });
    }

    private void installVoices() {
        try {
            startActivityForResult(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    .setPackage(currentEngine), INSTALL_TTS_REQUEST_CODE);
        } catch (ActivityNotFoundException error) { toast(R.string.tts_install_unavailable); }
    }

    @Override protected void onActivityResult(int request, int result, @Nullable Intent data) {
        super.onActivityResult(request, result, data);
        if (request == INSTALL_TTS_REQUEST_CODE && currentEngine != null) startEngine(currentEngine, true);
    }

    @Override public void onRequestPermissionsResult(int request, @NonNull String[] permissions, @NonNull int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == 0) refreshDecks();
    }

    private void savePreferences() {
        getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit()
                .putString(languageKey + TTS_ENGINE, value(preferredEngine))
                .putString(languageKey + TTS_LOCALE, value(preferredLocale))
                .putString(languageKey + TTS_VOICE, value(preferredVoice))
                .putString(languageKey + ANKI_DECK, value(preferredDeck))
                .putBoolean(languageKey + "includeAudio", includeAudio.isChecked()).apply();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putString(TTS_ENGINE, preferredEngine);
        state.putString(TTS_LOCALE, preferredLocale);
        state.putString(TTS_VOICE, preferredVoice);
        state.putString(ANKI_DECK, preferredDeck);
        state.putBoolean("includeAudio", includeAudio.isChecked());
        super.onSaveInstanceState(state);
    }

    @Override protected void onStop() { savePreferences(); super.onStop(); }
    @Override protected void onDestroy() {
        destroyed = true;
        speech.destroyTTS();
        ankiWorker.shutdown(); // Let an already submitted Anki import finish and clean up its file.
        super.onDestroy();
    }

    private void toast(int message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private static String value(String value) { return value == null ? "" : value; }
    private static <T> int indexOf(List<Option<T>> options, String id) {
        for (int i = 0; i < options.size(); i++) if (options.get(i).id.equals(id)) return i;
        return -1;
    }
    private <T> void bind(Spinner spinner, List<Option<T>> options, String selected, Consumer<Option<T>> change) {
        spinner.setOnItemSelectedListener(null);
        ArrayAdapter<Option<T>> adapter = new ArrayAdapter<Option<T>>(this, R.layout.tts_option, android.R.id.text1, options) {
            @NonNull @Override public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                return row(position, view, parent);
            }
            @Override public View getDropDownView(int position, @Nullable View view, @NonNull ViewGroup parent) {
                return row(position, view, parent);
            }
            private View row(int position, View view, ViewGroup parent) {
                if (view == null) view = getLayoutInflater().inflate(R.layout.tts_option, parent, false);
                Option<T> option = getItem(position);
                ((TextView) view.findViewById(android.R.id.text1)).setText(option.title);
                TextView detail = view.findViewById(android.R.id.text2);
                detail.setText(option.detail);
                detail.setVisibility(option.detail.isEmpty() ? View.GONE : View.VISIBLE);
                return view;
            }
        };
        spinner.setAdapter(adapter);
        if (options.isEmpty()) return;
        int position = Math.max(0, indexOf(options, selected));
        spinner.setSelection(position);
        change.accept(options.get(position));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private String current = options.get(position).id;
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                Option<T> option = options.get(index);
                if (!current.equals(option.id)) { current = option.id; change.accept(option); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
    private static final class Option<T> {
        final String id, title, detail;
        final T value;
        Option(String id, String title, String detail, T value) {
            this.id = id; this.title = title; this.detail = detail; this.value = value;
        }
        @NonNull @Override public String toString() { return title; }
    }
}
