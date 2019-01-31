package com.xeyqe.myapplication;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private VocabViewModel vocabViewModel;
    private FloatingActionButton button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        button = findViewById(R.id.buChangeActivity);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeActivity();
            }
        });


        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        EditText editText = findViewById(R.id.edit_text);


        final VocabAdapter adapter = new VocabAdapter();
        recyclerView.setAdapter(adapter);

        vocabViewModel = ViewModelProviders.of(this).get(VocabViewModel.class);
        vocabViewModel.getAllVocabs().observe(this, new Observer<List<Vocab>>() {
            @Override
            public void onChanged(@Nullable List<Vocab> vocabs) {
                adapter.setVocabs(vocabs);
            }
        });


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                vocabViewModel.getAllSearchedVocabs(s.toString()).observe(MainActivity.this, new Observer<List<Vocab>>() {

                    @Override
                    public void onChanged(@Nullable List<Vocab> vocabs) {
                        adapter.setVocabs(vocabs);
                    }
                });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        adapter.setOnClickListener(new VocabAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Vocab vocab) {
                Intent intent = new Intent(MainActivity.this, ankiApi.class);
                intent.putExtra(ankiApi.EXTRA_WORD, vocab.getWord());
                intent.putExtra(ankiApi.EXTRA_MEANING, vocab.getMeaning());
                startActivityForResult(intent, 1);
            }
        });
    }

    public void changeActivity() {
        Intent intent = new Intent(this, myMemory.class);
        startActivity(intent);
    }


}