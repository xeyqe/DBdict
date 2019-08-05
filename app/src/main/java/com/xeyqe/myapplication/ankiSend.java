package com.xeyqe.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ichi2.anki.api.AddContentApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;


public class ankiSend extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";
    public static final String EXTRA_LANGUAGE = "com.xeyqe.myapplication.EXTRA_LANGUAGE";

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String TTS_ENGINE = "ttsEngine";
    public static final String TTS_LOCALE = "ttsLocale";
    public static final String TTS_VOICE = "ttsVoice";
    public static final String ANKI_DECK = "ankiDeck";

    private static final int AD_PERM_REQUEST = 0;

    private String loadedEngine;
    private String loadedLocale;
    private String loadedVoice;
    private String loadedDeck;

    private EditText editTextFront;
    private EditText editTextBack;
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



    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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

        button = findViewById(R.id.button);
        buTTS = findViewById(R.id.buTTS);
        spinnerEngine = findViewById(R.id.spinnerEngine);
        spinnerLocale = findViewById(R.id.spinnerLocale);
        spinnerVoice = findViewById(R.id.spinnerVoice);
        spinnerDeck = findViewById(R.id.spinnerDeck);

        map = new HashMap<String, Voice>();

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

        if (isApiAvailable(ankiSend.this)) {
            if (shouldRequestPermission()) {
                requestPermission(ankiSend.this, AD_PERM_REQUEST);
            }

            if (!shouldRequestPermission()) {
                spinnerDeck.setEnabled(true);
                spinnerDeckFill();
            }
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String word = editTextFront.getText().toString();
                Pattern pattern = Pattern.compile("\\[sound:(.*?)\\]");
                Matcher matcher = pattern.matcher(word);
                if (matcher.find())
                {
                    try {
                        String path = getCollectionPath();
                        String text = word.replaceAll(" \\[sound:.*\\.wav\\]", "");

                        file = new File(path+matcher.group(1));
                        if (verifyStoragePermissions(ankiSend.this)) {

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

                if (isApiAvailable(ankiSend.this) && !shouldRequestPermission()) {
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

                saveSharedPreferences();
            }
        });

        buTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verifyStoragePermissions(ankiSend.this))
                    speak();
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

        loadSharedPreferences();
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


        for (Voice voice : mTTS.getVoices()) {
            if (!voice.getFeatures().contains("notInstalled") && !voice.isNetworkConnectionRequired()) {
            //if (!voice.isNetworkConnectionRequired()) {
                //voices.add(voice.getName());
                String locale = voice.getLocale().toString();
                if (!locales.contains(locale))
                    locales.add(locale);
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

        for (Voice voice : mTTS.getVoices()) {
            if (!voice.getFeatures().contains("notInstalled") && !voice.isNetworkConnectionRequired() &&
                voice.getLocale().toString().equals(spinnerLocale.getSelectedItem().toString())) {

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
        editTextFront.setText(text + " [sound:"+ mTTS.getVoice().getName() + "_" + filename+"]");
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        loadedEngine = sharedPreferences.getString(lang+TTS_ENGINE, "");
        loadedLocale = sharedPreferences.getString(lang+TTS_LOCALE, "");
        loadedVoice = sharedPreferences.getString(lang+TTS_VOICE, "");
        loadedDeck = sharedPreferences.getString(lang+ANKI_DECK, "");
    }

    private void saveSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(lang+TTS_ENGINE, spinnerEngine.getSelectedItem().toString());
        editor.putString(lang+TTS_LOCALE, spinnerLocale.getSelectedItem().toString());
        editor.putString(lang+TTS_VOICE, spinnerVoice.getSelectedItem().toString());
        editor.putString(lang+ANKI_DECK, spinnerDeck.getSelectedItem().toString());

        editor.apply();
    }

    public static boolean verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED;
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

    public static boolean isApiAvailable(Context context) {
        return AddContentApi.getAnkiDroidPackageName(context) != null;
    }

    public boolean shouldRequestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        return ContextCompat.checkSelfPermission(this, READ_WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission(Activity callbackActivity, int callbackCode) {
        ActivityCompat.requestPermissions(callbackActivity, new String[]{READ_WRITE_PERMISSION}, callbackCode);
    }

    public static String getCollectionPath() {
        return new File(getDefaultAnkiDroidDirectory(), "collection.anki2").getParent() +
                File.separator + "collection.media" + File.separator;
    }

    public static String getDefaultAnkiDroidDirectory() {
        return new File(Environment.getExternalStorageDirectory(), "AnkiDroid").getAbsolutePath();
    }

}
