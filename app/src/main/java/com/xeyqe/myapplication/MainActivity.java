package com.xeyqe.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import androidx.lifecycle.Observer;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final VocabAdapter adapter = new VocabAdapter();
    private static final int READ_REQUEST_CODE = 42;
    public static String mLng = "germanstina";
    private String language2Delete;
    private EditText languages;
    private Uri uri;
    private PopupMenu popupMenu3;
    private VocabRepository repository;
    private Handler timeout = new Handler();
    private String searchText;


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

        final SearchView searchView = findViewById(R.id.search_view);

        recyclerView.setAdapter(adapter);

        popupMenu3 = new PopupMenu(MainActivity.this, buLanguage);

        repository = new VocabRepository(this.getApplication());
        repository.getAllLanguages().observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> strings) {
                popupMenu3.getMenu().clear();
                popupMenu3.getMenu().add(0, 0,0,"vsjo");

                for (int i=1; i<strings.size()+1; i++) {
                    popupMenu3.getMenu().add(0, i,0,strings.get(i-1));
                }

                popupMenu3.getMenu().add(0, strings.size()+1,0,"add");
            }
        });

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    searchView.setQuery(sharedText.trim(),true);
                }
            }
        }

        searchView.setIconified(false);

        if (searchView.getQuery().toString().trim().length() == 0) {
            database();
        } else
            database(searchView.getQuery().toString(),buLanguage.getText().toString());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchText = newText;

                Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (searchText.trim().length() == 0) {
                    database();
                } else {
                    String language = buLanguage.getText().toString();
                            database(searchText, language);
                }
                    }
                };
                timeout.removeCallbacksAndMessages(null);
                timeout.postDelayed(mRunnable, 250);

                return true;
            }
        });

        adapter.setOnClickListener(new VocabAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Vocab vocab) {
                Intent intent = new Intent(MainActivity.this, AnkiSend.class);
                intent.putExtra(AnkiSend.EXTRA_WORD, vocab.getWord());
                intent.putExtra(AnkiSend.EXTRA_MEANING, vocab.getMeaning());
                intent.putExtra(AnkiSend.EXTRA_LANGUAGE, vocab.getLanguage());

                if (!vocab.getHistory()) {
                    String word = vocab.getWord();
                    String meaning = vocab.getMeaning();
                    String language = vocab.getLanguage();
                    int id = vocab.getId();
                    Vocab vocabUpdated = new Vocab(word, meaning, language, true);
                    vocabUpdated.setId(id);
                    repository.update(vocabUpdated);
                }

                MainActivity.this.startActivityForResult(intent, 1);
            }
        });


        buLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                popupMenu3.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String language = item.getTitle().toString();
                        if (language.equals("add")) {
                            if (PermissionChecker.doIHaveReadPermission(MainActivity.this))
                                performFileSearch();
                            else
                                PermissionChecker.checkReadPermission(MainActivity.this);
                        } else {
                            buLanguage.setText(language);
                            if (searchView.getQuery().toString().trim().length() == 0) {
                                database();
                            } else {
                                database(searchView.getQuery().toString(), language);
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
                if (!language2Delete.equals("vsjo")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setMessage("Do you want to delete " + language2Delete + " dictionary?")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    repository.deleteAllVocabs(language2Delete);
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
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder viewHolder1) {
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
                repository.update(vocabUpdated);
                String str = searchView.getQuery().toString();
                if (str.isEmpty())
                    database();
                else
                    database(str, buLanguage.getText().toString());
            }
        }).attachToRecyclerView(recyclerView);
    }


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
                uri = data.getData();

                final AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.layout_dialog, null);
                languages = mView.findViewById(R.id.edit_view_lang);
                dialog.setView(mView)
                        .setTitle("Languages")
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String language = languages.getText().toString();

                                mLng = language;
                                readTextFile(uri);
                            }
                        });
                dialog.show();
                String path = uri.getPath();
                String filename = path.substring(path.lastIndexOf(File.separator)+1);
                if (filename.indexOf(".") > 0)
                    filename = filename.substring(0, filename.lastIndexOf("."));
                languages.setText(filename);
            }
        }
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
                if (!line.startsWith("##") && line.contains("\t")) {
                    String[] separated = line.split("\t");
                    if (separated.length == 2)
                        listOfVocabs.add(new Vocab(separated[0], separated[1]
                                .replaceAll("\\\\n", "<br>"), mLng, false));
                }
                line = reader.readLine();
            }
            if (listOfVocabs.size() == 0)
                Toast.makeText(this, "Wrong input. I want word and meaning divided by tab.",
                        Toast.LENGTH_LONG).show();
            else
                repository.insertAll(listOfVocabs);
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
        repository.getAllVocabs();
    }

    private void database(String word, String language) {
        if (language.equals("vsjo"))
            language = "%";
        word += "%";
        repository.getAllSearchedVocabs(word, language);
    }

    public static void updateAdapter(List<Vocab> vocabs) {
        adapter.setVocabs(vocabs);
    }

}