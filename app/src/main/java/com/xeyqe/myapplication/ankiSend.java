package com.xeyqe.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ichi2.anki.api.AddContentApi;
import com.ichi2.anki.api.NoteInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ankiSend extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";
    public static final String EXTRA_LANGUAGE = "com.xeyqe.myapplication.EXTRA_LANGUAGE";

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TTS_ENGINE = "ttsEngine";
    public static final String TTS_LOCALE = "ttsLocale";
    public static final String TTS_VOICE = "ttsVoice";
    public static final String ANKI_DECK = "ankiDeck";
    public static final String ANKI_PATH = "ankiPath";

    private String loadedEngine;
    private String loadedLocale;
    private String loadedVoice;
    private String loadedDeck;
    private String loadedPath;

    private EditText editTextFront;
    private EditText editTextBack;
    private EditText editTextPath;
    private Button button;
    private Button buTTS;
    private Context context = GlobalApplication.getAppContext();
    private AddContentApi api = new AddContentApi(context);
    private String meaning;
    private String lang;
    private File file;
    private TextToSpeech mTTS;

    private Spinner spinnerEngine;
    private Spinner spinnerLocale;
    private Spinner spinnerVoice;
    private Spinner spinnerDeck;

    private HashMap<String,Voice> map;

    private ArrayAdapter<String> adapterEngines;
    private ArrayAdapter<String> adapterLocale;
    private ArrayAdapter<String> adapterVoice;
    private ArrayAdapter<String> adapterDeck;

    private boolean canLoadEngine;
    private boolean canLoadLocale;
    private boolean canLoadVoice;
    private boolean canLoadDeck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anki_send);

        canLoadEngine = true;
        canLoadLocale = true;
        canLoadVoice = true;
        canLoadDeck = true;

        editTextFront = findViewById(R.id.editTextFront);
        editTextBack = findViewById(R.id.editTextBack);
        editTextPath = findViewById(R.id.editTextPath);

        button = findViewById(R.id.button);
        buTTS = findViewById(R.id.buTTS);
        spinnerEngine = findViewById(R.id.spinnerEngine);
        spinnerLocale = findViewById(R.id.spinnerLocale);
        spinnerVoice = findViewById(R.id.spinnerVoice);
        spinnerDeck = findViewById(R.id.spinnerDeck);

        map = new HashMap<>();

        Intent intent = getIntent();

        meaning = intent.getStringExtra(EXTRA_MEANING);
        lang = intent.getStringExtra(EXTRA_LANGUAGE);
        editTextFront.setText(intent.getStringExtra(EXTRA_WORD));
        editTextBack.setText(Html.fromHtml(meaning));

        editTextBack.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    editTextBack.setText(meaning);

                } else {
                    meaning = editTextBack.getText().toString();
                    editTextBack.setText(Html.fromHtml(meaning));

                }
            }
        });

        editTextPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                loadedPath = s.toString();
                checkIfPathExists(s.toString());
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String front = editTextFront.getText().toString();

                List<NoteInfo> duplicates = null;
                if (permissionChecker.doIHaveAnkiApiPermission(ankiSend.this)) {
                    long id = getIdOfBasic() != null ? getIdOfBasic() : api.addNewBasicModel("basic");
                    duplicates = api.findDuplicateNotes(id, front);
                }

                String path = loadedPath;
                if (loadedPath.length() > 0 && !loadedPath.substring(loadedPath.length() - 1).equals(File.separator)) {
                    path += File.separator;
                }
                String externalPath = Environment.getExternalStorageDirectory().getPath() +
                        File.separator;

                if (checkIfPathExists(path)) {
                    if (duplicates != null && duplicates.size() > 0) {
                        for (NoteInfo info : duplicates) {
                            if (info.getFields()[1].equals(meaning)) {
                                Toast.makeText(ankiSend.this, "Already exists", Toast.LENGTH_LONG).show();
                                saveSharedPreferences();
                                break;
                            } else {
                                createMediaFile(externalPath + path);
                                sendNote();
                                saveSharedPreferences();
                            }
                        }
                    } else {
                        createMediaFile(externalPath + path);
                        sendNote();
                        saveSharedPreferences();
                    }
                } else
                    Toast.makeText(ankiSend.this, "Path if not valid!", Toast.LENGTH_LONG).show();
            }
        });

        buTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionChecker.doIHaveWritePermission(ankiSend.this)) {
                    speak();
                } else
                    permissionChecker.checkWritePermission(ankiSend.this);
            }
        });

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    buTTS.setEnabled(true);
                    spinnerEngineFill();
                }
            }
        });

        permissionChecker.checkAnkiApiPermission(ankiSend.this);
        if (permissionChecker.doIHaveAnkiApiPermission(ankiSend.this)) {
            spinnerDeck.setEnabled(true);
            spinnerDeckFill();
        }

        loadSharedPreferences();
    }

    private boolean checkIfPathExists(String path) {
        String externalPath = Environment.getExternalStorageDirectory() + File.separator;
        File f = new File( externalPath + path);
        if (f.isDirectory() && new File(f.getParent()+File.separator + "collection.anki2").exists() &&
                !path.substring(0,1).equals(File.separator)) {
            editTextPath.setBackgroundColor(Color.BLACK);
            return true;
        } else {
            editTextPath.setBackgroundColor(Color.RED);
            return false;
        }
    }

    private void spinnerDeckFill() {
        List<String> decks = new ArrayList<>();
        Map<Long, String> deckList = api.getDeckList();
        for (Map.Entry<Long, String> entry : deckList.entrySet()) {
            decks.add(entry.getValue());
        }

        adapterDeck = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, decks);

        spinnerDeck.setAdapter(adapterDeck);
        spinnerDeck.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (canLoadDeck) {
                    if (!loadedDeck.equals("")) {
                        int pos = adapterDeck.getPosition(loadedDeck);
                        spinnerDeck.setSelection(pos);
                    }
                    canLoadDeck = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    private void initializeTTS(String ttsEngine) {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    mTTS.setSpeechRate(1);
                    buTTS.setEnabled(true);
                    if (spinnerEngine.getSelectedItem() == null) {
                        spinnerEngineFill();
                    } else {
                        spinnerLocaleFill();
                    }
                }
            }
        }, ttsEngine);
    }

    private void createMediaFile(String path) {
        String word = editTextFront.getText().toString();
        Pattern pattern = Pattern.compile("\\[sound:(.*?)\\]");
        Matcher matcher = pattern.matcher(word);
        if (matcher.find()) {
            try {
                String text = word.replaceAll(" \\[sound:.*\\.wav\\]", "");

                file = new File(path+matcher.group(1));
                if (permissionChecker.doIHaveWritePermission(ankiSend.this)) {

                    file.createNewFile();
                    if (file.exists()) {
                        mTTS.synthesizeToFile(text, null, new File(path + matcher.group(1)), null);
                    }
                } else
                    editTextFront.setText(text);
            } catch (IOException e){
                Log.e("NWFILE", e.toString());
            }
        }
    }

    private void spinnerEngineFill() {
        List<String> engines = new ArrayList<>();
        for (TextToSpeech.EngineInfo name : mTTS.getEngines()) {
            engines.add(name.name);
        }

        adapterEngines = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, engines);
        spinnerEngine.setAdapter(adapterEngines);

        spinnerEngine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (canLoadEngine) {
                    if (!loadedEngine.equals("")) {
                        int pos = adapterEngines.getPosition(loadedEngine);
                        spinnerEngine.setSelection(pos);
                    }
                    canLoadEngine = false;
                }
                initializeTTS(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void spinnerLocaleFill() {

        List<String> locales = new ArrayList<>();

        if (mTTS.getAvailableLanguages()!=null) {
            for (Locale locale : mTTS.getAvailableLanguages()) {
                locales.add(locale.toString());
            }
        }

        Collections.sort(locales);

        adapterLocale = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locales);
        spinnerLocale.setAdapter(adapterLocale);

        spinnerLocale.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (canLoadLocale) {
                    if (!loadedLocale.equals("")) {
                        int pos = adapterLocale.getPosition(loadedLocale);
                        spinnerLocale.setSelection(pos);
                    }
                    canLoadLocale = false;
                }
                    if (mTTS.getVoices() != null)
                        spinnerVoiceFill();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void spinnerVoiceFill() {
        List<String> voices = new ArrayList<>();
        String localeString = spinnerLocale.getSelectedItem().toString();
        Locale locale = new Locale(localeString.split("_")[0],localeString.split("_")[1]);

        for (Voice voice : mTTS.getVoices()) {
            if (!voice.getFeatures().contains("notInstalled") && !voice.isNetworkConnectionRequired() &&
                voice.getLocale().getISO3Country().equals(locale.getISO3Country())) {

                map.put(voice.getName(), voice);
                voices.add(voice.getName());
            }
        }

        Collections.sort(voices);

        adapterVoice = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voices);
        spinnerVoice.setAdapter(adapterVoice);

        spinnerVoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (canLoadVoice) {
                    if (!loadedVoice.equals("")) {
                        int pos = adapterVoice.getPosition(loadedVoice);
                        spinnerVoice.setSelection(pos);
                    }
                    canLoadVoice = false;
                }
                    mTTS.setVoice(map.get(parent.getItemAtPosition(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void speak() {
        String text = editTextFront.getText().toString().replaceAll(" \\[sound:.*\\]", "");
        String filename = text.replaceAll(" ", "_") + ".wav";

        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        editTextFront.setText(text + " [sound:" + mTTS.getVoice().getName() + "_" + filename + "]");
    }

    private void sendNote() {
        String word = editTextFront.getText().toString();
        if (permissionChecker.doIHaveAnkiApiPermission(ankiSend.this)) {
            long deckId = getIdOfDeck() != null ? getIdOfDeck() :
                    api.addNewDeck(spinnerDeck.getSelectedItem().toString());
            long modelId = getIdOfBasic() != null ? getIdOfBasic() :
                    api.addNewBasicModel("basic");
            Long err = api.addNote(modelId, deckId, new String[] {word, meaning}, null);
            if (err != null)
                Toast.makeText(ankiSend.this, "Successufuly added", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(ankiSend.this, "error", Toast.LENGTH_LONG).show();
        } else {
            Intent shareIntent = ShareCompat.IntentBuilder.from(ankiSend.this)
                    .setType("text/plain")
                    .setSubject(word)
                    .setText(meaning)
                    .getIntent();
            if (shareIntent.resolveActivity(ankiSend.this.getPackageManager()) != null) {
                ankiSend.this.startActivity(shareIntent);
            }
        }
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        loadedEngine = sharedPreferences.getString(lang+TTS_ENGINE, "");
        loadedLocale = sharedPreferences.getString(lang+TTS_LOCALE, "");
        loadedVoice = sharedPreferences.getString(lang+TTS_VOICE, "");
        loadedDeck = sharedPreferences.getString(lang+ANKI_DECK, "");
        loadedPath = sharedPreferences.getString(lang+ANKI_PATH, "AnkiDroid/collection.media/");

        editTextPath.setText(loadedPath);
    }

    private void saveSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(lang+TTS_ENGINE, spinnerEngine.getSelectedItem().toString());
        editor.putString(lang+TTS_LOCALE, spinnerLocale.getSelectedItem().toString());
        editor.putString(lang+TTS_VOICE, spinnerVoice.getSelectedItem().toString());
        editor.putString(lang+ANKI_DECK, spinnerDeck.getSelectedItem().toString());
        if (checkIfPathExists(loadedPath))
            editor.putString(lang+ANKI_PATH, editTextPath.getText().toString());

        editor.apply();
    }

    private Long getIdOfBasic() {
        Map<Long, String> modelList = api.getModelList();
        for (Map.Entry<Long, String> entry : modelList.entrySet()) {
            if (entry.getValue().equals("basic")) {
                return entry.getKey();
            }
        }
        return null;
    }


    private Long getIdOfDeck() {
        Map<Long, String> deckList = api.getDeckList();
        if (deckList != null) {
            for (Map.Entry<Long, String> entry : deckList.entrySet()) {
                if (entry.getValue().equals(spinnerDeck.getSelectedItem().toString())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public static String getCollectionPath() {
        return new File(getDefaultAnkiDroidDirectory(), "collection.anki2").getParent() +
                File.separator + "collection.media" + File.separator;
    }

    public static String getDefaultAnkiDroidDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "AnkiDroid").getAbsolutePath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                spinnerDeck.setEnabled(true);
                spinnerDeckFill();

            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();
    }
}
