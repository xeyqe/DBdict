package com.xeyqe.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MyDialog.MyDialogListener {
    private VocabViewModel vocabViewModel;
    private final VocabAdapter adapter = new VocabAdapter();
    private static final int READ_REQUEST_CODE = 42;
    public static String mLng = "germanstina";
    private String language2Delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        final Button buLanguage = findViewById(R.id.buLanguage);



        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        final EditText editText = findViewById(R.id.edit_text);

        recyclerView.setAdapter(adapter);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    editText.setText(sharedText);
                    database(sharedText);
                }
            }
        }

        if (TextUtils.isEmpty(editText.getText())) {
            database();
        }


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(editText.getText())) {
                    database();
                } else {
                    String language = buLanguage.getText().toString();
                    if (language.equals("vsjo"))
                        database(s.toString());
                    else
                        database(s.toString(), language);
                }

            }
        });

        adapter.setOnClickListener(new VocabAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Vocab vocab) {
                Intent intent = new Intent(MainActivity.this, ankiSend.class);
                intent.putExtra(ankiSend.EXTRA_WORD, vocab.getWord());
                intent.putExtra(ankiSend.EXTRA_MEANING, vocab.getMeaning());

                if (vocab.getHistory() == false) {
                    String word = vocab.getWord();
                    String meaning = vocab.getMeaning();
                    String language = vocab.getLanguage();
                    int id = vocab.getId();
                    Vocab vocabUpdated = new Vocab(word, meaning, language, true);
                    vocabUpdated.setId(id);
                    vocabViewModel.update(vocabUpdated);
                }

                startActivityForResult(intent, 1);
            }
        });


        buLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu3 = new PopupMenu(MainActivity.this, buLanguage);
                List list = vocabViewModel.getGetAllLanguages();

                popupMenu3.getMenu().add(0, 0,0,"vsjo");

                for (int i=1; i<list.size()+1; i++) {
                    popupMenu3.getMenu().add(0, i,0,list.get(i-1).toString());
                }

                popupMenu3.getMenu().add(0, list.size()+1,0,"add");


                popupMenu3.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String language = item.getTitle().toString();
                        if (language.equals("add")) {
                            DictionaryImport.checkPermission(MainActivity.this);
                            MyDialog myDialog = new MyDialog();
                            myDialog.show(getSupportFragmentManager(), "my dialog");
                            openDialog();
                        } else {
                            buLanguage.setText(language);
                            if (TextUtils.isEmpty(editText.getText())) {
                                database();
                            } else {
                                database(editText.getText().toString(), language);
                            }
                        }
                        return true;
                    }
                });


                popupMenu3.show();
            }
        });

        buLanguage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                language2Delete = buLanguage.getText().toString();
                if (language2Delete != "vjso") {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setMessage("Do you want to delete " + language2Delete + " dictionary?")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    vocabViewModel.deleteAllNotes(language2Delete);
                                    buLanguage.setText("vsjo");
                                }
                            })
                            .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    builder.show();
                }

                return true;
            }
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                Vocab vocab = adapter.getVocabAt(viewHolder.getAdapterPosition());
                String word = vocab.getWord();
                String meaning = vocab.getMeaning();
                String language = vocab.getLanguage();
                Boolean history = vocab.getHistory();
                int id = vocab.getId();
                Vocab vocabUpdated = new Vocab(word, meaning, language, !history);
                vocabUpdated.setId(id);
                vocabViewModel.update(vocabUpdated);
            }
        }).attachToRecyclerView(recyclerView);
    }

    @Override
    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();

                readTextFile(uri);
            }
        }
    }

    public void openDialog() {
        MyDialog exampleDialog = new MyDialog();
        exampleDialog.show(getSupportFragmentManager(), "example dialog");
    }



    @Override
    public void applyTexts(String lng) {
        mLng = lng;
    }

    public void readTextFile(Uri uri) {

        ArrayList<Vocab> listOfVocabs = new ArrayList<>();

        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("##")) {
                    String[] separated = line.split("\t");
                    listOfVocabs.add(new Vocab(separated[0], separated[1], mLng, false));
                }
                line = reader.readLine();
            }
            vocabViewModel.insertAll(listOfVocabs);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void database() {
        vocabViewModel = ViewModelProviders.of(this).get(VocabViewModel.class);
        vocabViewModel.getAllVocabs().observe(this, new Observer<List<Vocab>>() {
            @Override
            public void onChanged(@Nullable List<Vocab> vocabs) {
                adapter.setVocabs(vocabs);
            }
        });
    }

    private void database(String s) {
        vocabViewModel = ViewModelProviders.of(this).get(VocabViewModel.class);
        vocabViewModel.getAllSearchedVocabs(s).observe(MainActivity.this, new Observer<List<Vocab>>() {
            @Override
            public void onChanged(@Nullable List<Vocab> vocabs) {
                adapter.setVocabs(vocabs);
            }
        });
    }

    private void database(String s, String r) {
        vocabViewModel = ViewModelProviders.of(this).get(VocabViewModel.class);
        vocabViewModel.getSearchedVocabs(s, r).observe(MainActivity.this, new Observer<List<Vocab>>() {
            @Override
            public void onChanged(@Nullable List<Vocab> vocabs) {
                adapter.setVocabs(vocabs);
            }
        });
    }
}