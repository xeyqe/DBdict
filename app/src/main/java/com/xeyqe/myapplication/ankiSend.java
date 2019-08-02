package com.xeyqe.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.ichi2.anki.api.AddContentApi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;


public class ankiSend extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";
    private static final int AD_PERM_REQUEST = 0;


    private EditText editTextFront;
    private EditText editTextBack;
    private Button button;
    private Button buTTS;
    private FloatingActionButton floatingActionButton;
    Context context = GlobalApplication.getAppContext();
    private AddContentApi api = new AddContentApi(context);
    private String meaning;
    private File file;
    private TextToSpeech mTTS;
    private Map<String, Locale> map;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anki_send);

        floatingActionButton = findViewById(R.id.buChangeActivity);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ankiSend.this, myMemory.class);
                startActivity(intent);
            }
        });


        editTextFront = findViewById(R.id.editTextFront);
        editTextBack = findViewById(R.id.editTextBack);
        button = findViewById(R.id.button);
        buTTS = findViewById(R.id.buTTS);

        map = new HashMap<String, Locale>();



        Intent intent = getIntent();

        meaning = intent.getStringExtra(EXTRA_MEANING);
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


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String word = editTextFront.getText().toString();
                //String meaning = editTextBack.getText().toString();

                if (isApiAvailable(ankiSend.this)) {
                    if (shouldRequestPermission()) {
                        requestPermission(ankiSend.this, AD_PERM_REQUEST);
                    }
                    long deckId = getIdOfDeck() != null ? getIdOfDeck() : api.addNewDeck("DBdict");
                    long modelId = getIdOfBasic() != null ? getIdOfBasic() : api.addNewBasicModel("Basic");
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
        });

        buTTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String path = getCollectionPath();
                    String filename = editTextFront.getText().toString()
                            .replaceAll(" ", "_") + ".wav";
                    file = new File(path+filename);
                    verifyStoragePermissions(ankiSend.this);

                    boolean newFile = file.createNewFile();
                    if (newFile) {
                        speak(path, filename);
                    }
                } catch (IOException e){
                    Log.e("NWFILE", e.toString());
                }
            }
        });

        buTTS.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu3 = new PopupMenu(ankiSend.this, buTTS);


                int i =0;
                for (Locale loc : mTTS.getAvailableLanguages()) {
                    //Log.e("LNG", loc.toString() + mTTS.isLanguageAvailable(loc));

                    if (mTTS.isLanguageAvailable(loc) == 1) {
                        //int result = mTTS.setLanguage(loc);
                        int result = mTTS.setLanguage(loc);


                        if (!mTTS.getVoice().getFeatures().contains("notInstalled")) {


                            if (!(result == TextToSpeech.LANG_MISSING_DATA
                                    || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                                String locName = loc.getDisplayName();
                                popupMenu3.getMenu().add(0, i, 0, locName);
                                map.put(locName, loc);
                            }
                        }
                    }
                    i++;
                }

                popupMenu3.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                          @Override
                                                          public boolean onMenuItemClick(MenuItem item) {
                                                              mTTS.setLanguage(map.get(item.getTitle().toString()));
                                                              buTTS.setText(item.getTitle().toString());
                                                              return false;
                                                          }
                                                      });
                        popupMenu3.show();

                return true;
            }
        });

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    } else {
                        buTTS.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

    }

    private void speak(String path, String filename) {
        String text = editTextFront.getText().toString();



        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

        mTTS.synthesizeToFile(text, null, new File(path+filename),null);
        editTextFront.setText(text + " [sound:"+ mTTS.getVoice().getName() + "_" + filename+"]");
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }





    private Long getIdOfBasic() {
        Map<Long, String> modelList = api.getModelList();
        for (Map.Entry<Long, String> entry : modelList.entrySet()) {
            if (entry.getValue().equals("Basic")) {
                Toast.makeText(this, "neco", Toast.LENGTH_LONG).show();
                return entry.getKey();
            }
        }
        return null;
    }


    private Long getIdOfDeck() {
        Map<Long, String> deckList = api.getDeckList();
        if (deckList != null) {
            for (Map.Entry<Long, String> entry : deckList.entrySet()) {
                if (entry.getValue().contains("DBdict")) {
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
