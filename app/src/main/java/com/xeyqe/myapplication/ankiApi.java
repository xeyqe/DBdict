package com.xeyqe.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class ankiApi extends AppCompatActivity {
    public static final String EXTRA_WORD = "com.xeyqe.myapplication.EXTRA_WORD";
    public static final String EXTRA_MEANING = "com.xeyqe.myapplication.EXTRA_MEANING";

    private EditText editTextFront;
    private EditText editTextBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anki_api);


        editTextFront = findViewById(R.id.editTextFront);
        editTextBack = findViewById(R.id.editTextBack);

        Intent intent = getIntent();

        editTextFront.setText(intent.getStringExtra(EXTRA_WORD));
        editTextBack.setText(intent.getStringExtra(EXTRA_MEANING));

    }
}
