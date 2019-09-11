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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class AnkiSend extends AppCompatActivity {
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
    private String language;

    private EditText editTextFront;
    private EditText editTextBack;
    private EditText editTextPath;
    private Button button;
    private Button buTTS;
    private Context context = GlobalApplication.getAppContext();
    private AddContentApi api = new AddContentApi(context);
    private String meaning;
    private String lang;

    private Spinner spinnerEngine;
    private Spinner spinnerLocale;
    private Spinner spinnerVoice;
    private Spinner spinnerDeck;

    private HashMap<String,List<Voice>> map;
    private HashMap<String,Voice> mapVoiceName_Voice;

    private ArrayAdapter<String> adapterEngines;
    private ArrayAdapter<String> adapterLocale;
    private ArrayAdapter<String> adapterVoice;
    private ArrayAdapter<String> adapterDeck;

    private boolean canLoadEngine;
    private boolean canLoadLocale;
    private boolean canLoadVoice;
    private boolean canLoadDeck;

    private Tts mTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anki_send);

        mTTS = new Tts(AnkiSend.this);

        canLoadEngine = true;
        canLoadLocale = true;
        canLoadVoice = true;
        canLoadDeck = true;

        language = "";

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
        mapVoiceName_Voice = new HashMap<>();

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
                if (PermissionChecker.doIHaveAnkiApiPermission(AnkiSend.this)) {
                    long id = getIdOfBasic() != null ? getIdOfBasic() : api.addNewBasicModel("basic");
                    duplicates = api.findDuplicateNotes(id, front);
                }

                String path = loadedPath;
                if (loadedPath.length() > 0 && !loadedPath.substring(loadedPath.length() - 1).equals(File.separator)) {
                    path += File.separator;
                }
                String externalPath = Environment.getExternalStorageDirectory().getPath() +
                        File.separator;
                String text = editTextFront.getText().toString();

                if (checkIfPathExists(path)) {
                    if (duplicates != null && duplicates.size() > 0) {
                        for (NoteInfo info : duplicates) {
                            if (info.getFields()[1].equals(meaning)) {
                                Toast.makeText(AnkiSend.this, "Already exists", Toast.LENGTH_LONG).show();
                                saveSharedPreferences();
                                break;
                            } else {
                                mTTS.createMediaFile(externalPath + path, text);
                                sendNote();
                                saveSharedPreferences();
                            }
                        }
                    } else {
                        mTTS.createMediaFile(externalPath + path, text);
                        sendNote();
                        saveSharedPreferences();
                    }
                } else
                    Toast.makeText(AnkiSend.this, "Path if not valid!", Toast.LENGTH_LONG).show();
            }
        });

        buTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PermissionChecker.doIHaveWritePermission(AnkiSend.this)) {
                    if (spinnerVoice.getAdapter() != null) {
                        String text = editTextFront.getText().toString().replaceAll(" \\[sound:.*\\]", "");
                        mTTS.speak(text);
                    } else
                        Toast.makeText(AnkiSend.this, "Come on! Just give me some voice, please.",
                                Toast.LENGTH_LONG).show();
                } else
                    PermissionChecker.checkWritePermission(AnkiSend.this);
            }
        });

        mTTS.initializeTTS("com.google.android.tts", new Callable<Void>() {
            public Void call() {
                mCallback();
                return null;
            }
        });

        PermissionChecker.checkAnkiApiPermission(AnkiSend.this);
        if (PermissionChecker.doIHaveAnkiApiPermission(AnkiSend.this)) {
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

    private void spinnerEngineFill() {
        List<String> engines = new ArrayList<>();
        for (TextToSpeech.EngineInfo name : mTTS.listOfEngines()) {
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
                mTTS.initializeTTS(parent.getItemAtPosition(position).toString(), new Callable<Void>() {
                    public Void call() {
                        mCallback();
                        return null;
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void spinnerLocaleFill() {

        List<String> languages = new ArrayList<>();
        map.clear();
        mapVoiceName_Voice.clear();

        for (Voice voice : mTTS.setOfVoices()) {
            if (!voice.isNetworkConnectionRequired() && !voice.getFeatures().contains("notInstalled")) {
                String language = voice.getLocale().getDisplayLanguage();

                List<Voice> list;
                if (map.containsKey(language)) {
                    list = map.get(language);
                } else {
                    list = new ArrayList<>();
                }

                list.add(voice);
                map.put(language,list);
                mapVoiceName_Voice.put(voice.getName(), voice);

            }
        }

        languages.addAll(map.keySet());

        if (mTTS.setOfVoices().isEmpty()) {
            spinnerVoice.setAdapter(null);
            spinnerLocale.setAdapter(null);
        }


        Collections.sort(languages);
        languages.add("install");

        adapterLocale = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
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
                language = spinnerLocale.getItemAtPosition(position).toString();
                if (language.equals("install")) {
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    installIntent = installIntent.setPackage(spinnerEngine.getSelectedItem().toString());
                    startActivity(installIntent);
                }

                if (mTTS.setOfVoices() != null && !language.equals("install"))
                    spinnerVoiceFill();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void spinnerVoiceFill() {
        List<Voice> voices;
        List<String> voicesNames = new ArrayList<>();

        voices = map.get(language);
        String firstInstalledVoice = null;

        for (Voice voice : voices) {
            voicesNames.add(voice.getName());
            if (firstInstalledVoice == null)
                if (!voice.getFeatures().contains("notInstalled"))
                    firstInstalledVoice = voice.getName();

        }

        Collections.sort(voicesNames);

        adapterVoice = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, voicesNames);
        spinnerVoice.setAdapter(adapterVoice);

        if (firstInstalledVoice != null) {
            int pos = adapterVoice.getPosition(firstInstalledVoice);
            spinnerVoice.setSelection(pos);
        }

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

                String stringVoice = parent.getItemAtPosition(position).toString();
                mTTS.setVoice(mapVoiceName_Voice.get(stringVoice));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void sendNote() {
        String word = editTextFront.getText().toString();
        if (PermissionChecker.doIHaveAnkiApiPermission(AnkiSend.this)) {
            long deckId = getIdOfDeck() != null ? getIdOfDeck() :
                    api.addNewDeck(spinnerDeck.getSelectedItem().toString());
            long modelId = getIdOfBasic() != null ? getIdOfBasic() :
                    api.addNewBasicModel("basic");
            Long err = api.addNote(modelId, deckId, new String[] {word, meaning}, null);
            if (err != null)
                Toast.makeText(AnkiSend.this, "Successfully added", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(AnkiSend.this, "error", Toast.LENGTH_LONG).show();
        } else {
            Intent shareIntent = ShareCompat.IntentBuilder.from(AnkiSend.this)
                    .setType("text/plain")
                    .setSubject(word)
                    .setText(meaning)
                    .getIntent();
            if (shareIntent.resolveActivity(AnkiSend.this.getPackageManager()) != null) {
                AnkiSend.this.startActivity(shareIntent);
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

    public void mCallback() {
        if (spinnerEngine.getSelectedItem() == null) {
            spinnerEngineFill();
        } else {
            spinnerLocaleFill();
        }
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
        mTTS.destroyTTS();
        super.onDestroy();
    }
}
