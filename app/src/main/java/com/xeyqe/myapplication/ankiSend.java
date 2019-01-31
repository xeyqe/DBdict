package com.xeyqe.myapplication;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class ankiSend extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";

    private EditText editTextFront;
    private EditText editTextBack;
    private Button button;
    private FloatingActionButton floatingActionButton;


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

        editTextFront.setText(intent.getStringExtra(EXTRA_WORD));
        editTextBack.setText(intent.getStringExtra(EXTRA_MEANING));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String word = editTextFront.getText().toString();
                String meaning = editTextBack.getText().toString();

                Intent shareIntent = ShareCompat.IntentBuilder.from(ankiSend.this)
                        .setType("text/plain")
                        .setSubject(word)
                        .setText(meaning)
                        .getIntent();
                if (shareIntent.resolveActivity(ankiSend.this.getPackageManager()) != null) {
                    ankiSend.this.startActivity(shareIntent);
                }
            }
        });

    }
}
