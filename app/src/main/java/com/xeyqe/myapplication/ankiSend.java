package com.xeyqe.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ichi2.anki.api.AddContentApi;

import java.util.Map;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;


public class ankiSend extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";
    private static final int AD_PERM_REQUEST = 0;


    private EditText editTextFront;
    private EditText editTextBack;
    private Button button;
    private FloatingActionButton floatingActionButton;
    Context context = GlobalApplication.getAppContext();
    private AddContentApi api = new AddContentApi(context);
    private String meaning;

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
    }

    private Long getIdOfBasic() {
        Map<Long, String> modelList = api.getModelList();
        for (Map.Entry<Long, String> entry : modelList.entrySet()) {
            if (entry.getValue() == "Basic") {
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


}
