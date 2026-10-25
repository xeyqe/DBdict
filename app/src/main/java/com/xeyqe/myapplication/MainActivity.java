package com.xeyqe.myapplication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final VocabAdapter adapter = new VocabAdapter();
    private static final int READ_REQUEST_CODE = 42;
    private final Handler timeout = new Handler(Looper.getMainLooper());
    private final List<String> dictionaries = new ArrayList<>();
    private VocabRepository repository;
    private SearchView search;
    private MaterialButton dictionaryButton;
    private MaterialToolbar toolbar;
    private PopupMenu dictionaryMenu;
    private View emptyState;
    private TextView resultsTitle, resultsCount, emptyTitle, emptyDescription;
    private String selectedDictionary = "";
    private String importLanguage = "";
    private final RecyclerView.AdapterDataObserver resultsObserver = new RecyclerView.AdapterDataObserver() {
        @Override public void onChanged() { renderResults(); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        WindowInsetsHelper.apply(this);
        repository = new VocabRepository(getApplication());
        search = findViewById(R.id.search_view);
        dictionaryButton = findViewById(R.id.buLanguage);
        toolbar = findViewById(R.id.dictionaryToolbar);
        emptyState = findViewById(R.id.dictionaryEmpty);
        resultsTitle = findViewById(R.id.resultsTitle);
        resultsCount = findViewById(R.id.resultsCount);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptyDescription = findViewById(R.id.emptyDescription);
        RecyclerView recycler = findViewById(R.id.recycler_view);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.registerAdapterDataObserver(resultsObserver);
        if (state != null) selectedDictionary = state.getString("dictionary", "");
        dictionaryMenu = new PopupMenu(this, dictionaryButton);
        dictionaryMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_import_dictionary) performFileSearch();
            else {
                int index = item.getItemId() - 1;
                selectedDictionary = index >= 0 && index < dictionaries.size() ? dictionaries.get(index) : "";
                renderDictionary();
                reload();
            }
            return true;
        });
        dictionaryButton.setOnClickListener(v -> dictionaryMenu.show());
        dictionaryButton.setOnLongClickListener(v -> {
            confirmRemoval();
            return true;
        });
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_import_dictionary) performFileSearch();
            else if (item.getItemId() == R.id.action_delete_dictionary) confirmRemoval();
            return true;
        });
        repository.getAllLanguages().observe(this, names -> {
            dictionaries.clear();
            if (names != null) dictionaries.addAll(names);
            dictionaryMenu.getMenu().clear();
            dictionaryMenu.getMenu().add(0, 0, 0, R.string.dict_all);
            for (int i = 0; i < dictionaries.size(); i++) {
                dictionaryMenu.getMenu().add(1, i + 1, i + 1, dictionaries.get(i));
            }
            dictionaryMenu.getMenu().add(2, R.id.action_import_dictionary, dictionaries.size() + 1, R.string.dict_import);
            if (!selectedDictionary.isEmpty() && !dictionaries.contains(selectedDictionary)) {
                selectedDictionary = "";
                reload();
            }
            renderDictionary();
        });
        search.setIconified(false);
        search.setMaxWidth(Integer.MAX_VALUE);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                timeout.removeCallbacksAndMessages(null);
                reload();
                search.clearFocus();
                return true;
            }
            @Override public boolean onQueryTextChange(String query) {
                timeout.removeCallbacksAndMessages(null);
                timeout.postDelayed(MainActivity.this::reload, 250);
                return true;
            }
        });
        String initialQuery = state == null ? sharedText(getIntent()) : state.getString("query", "");
        search.setQuery(initialQuery, false);
        search.clearFocus();
        renderDictionary();
        renderResults();
        reload();
        adapter.setOnClickListener(vocab -> {
            if (!vocab.getHistory()) updateHistory(vocab, true);
            startActivity(new Intent(this, AnkiSend.class)
                    .putExtra(AnkiSend.EXTRA_WORD, vocab.getWord())
                    .putExtra(AnkiSend.EXTRA_MEANING, vocab.getMeaning())
                    .putExtra(AnkiSend.EXTRA_LANGUAGE, vocab.getLanguage()));
        });
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView view, @NonNull RecyclerView.ViewHolder source,
                                             @NonNull RecyclerView.ViewHolder target) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= adapter.getItemCount()) return;
                Vocab vocab = adapter.getVocabAt(position);
                updateHistory(vocab, !vocab.getHistory());
                reload();
            }
        }).attachToRecyclerView(recycler);
    }

    private void updateHistory(Vocab vocab, boolean recent) {
        Vocab updated = new Vocab(vocab.getWord(), vocab.getMeaning(), vocab.getLanguage(), recent);
        updated.setId(vocab.getId());
        repository.update(updated);
    }

    private void reload() {
        if (search.getQuery().toString().trim().isEmpty()) repository.getAllVocabs();
        else repository.getAllSearchedVocabs(search.getQuery().toString() + "%",
                selectedDictionary.isEmpty() ? "%" : selectedDictionary);
    }

    private void renderDictionary() {
        dictionaryButton.setText(selectedDictionary.isEmpty() ? getString(R.string.dict_all) : selectedDictionary);
        toolbar.getMenu().findItem(R.id.action_delete_dictionary).setVisible(!selectedDictionary.isEmpty());
    }

    private void renderResults() {
        boolean searching = !search.getQuery().toString().trim().isEmpty();
        int count = adapter.getItemCount();
        resultsTitle.setText(searching ? R.string.dict_matches : R.string.dict_recent);
        resultsCount.setText(getResources().getQuantityString(R.plurals.dict_word_count, count, count));
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        emptyTitle.setText(searching ? R.string.dict_no_results_title : R.string.dict_empty_title);
        emptyDescription.setText(searching ? R.string.dict_no_results_hint : R.string.dict_empty_hint);
    }

    private void confirmRemoval() {
        if (selectedDictionary.isEmpty()) return;
        String dictionary = selectedDictionary;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.dict_remove_title)
                .setMessage(getString(R.string.dict_remove_message, dictionary))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.dict_remove_action, (dialog, which) -> {
                    repository.deleteAllVocabs(dictionary);
                    selectedDictionary = "";
                    renderDictionary();
                    reload();
                }).show();
    }

    public void performFileSearch() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType("text/*"), READ_REQUEST_CODE);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.dict_picker_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, @Nullable Intent data) {
        super.onActivityResult(request, result, data);
        if (request != READ_REQUEST_CODE || result != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        View content = getLayoutInflater().inflate(R.layout.layout_dialog, null);
        EditText name = content.findViewById(R.id.edit_view_lang);
        TextInputLayout input = content.findViewById(R.id.dictionaryNameInput);
        name.setText(dictionaryName(uri));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(R.string.dict_import)
                .setView(content).setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.dict_import_action, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String dictionary = name.getText().toString().trim();
            if (dictionary.isEmpty()) {
                input.setError(getString(R.string.dict_name_required));
                return;
            }
            importLanguage = dictionary;
            dialog.dismiss();
            readTextFile(uri);
        }));
        dialog.show();
    }

    private String dictionaryName(Uri uri) {
        String filename = null;
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) filename = cursor.getString(0);
        } catch (RuntimeException ignored) { }
        if (filename == null || filename.trim().isEmpty()) return getString(R.string.dict_default_name);
        int extension = filename.lastIndexOf('.');
        return extension > 0 ? filename.substring(0, extension) : filename;
    }

    public void readTextFile(Uri uri) {
        ArrayList<Vocab> vocabs = new ArrayList<>();
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IOException("Document unavailable");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("##")) continue;
                    String[] entry = line.split("\t");
                    if (entry.length == 2) vocabs.add(new Vocab(entry[0], entry[1].replace("\\n", "<br>"), importLanguage, false));
                }
            }
            if (vocabs.isEmpty()) Toast.makeText(this, R.string.dict_import_invalid, Toast.LENGTH_LONG).show();
            else {
                repository.insertAll(vocabs);
                Toast.makeText(this, getResources().getQuantityString(R.plurals.dict_import_started,
                        vocabs.size(), vocabs.size()), Toast.LENGTH_LONG).show();
            }
        } catch (IOException | SecurityException error) {
            Toast.makeText(this, R.string.dict_import_failed, Toast.LENGTH_LONG).show();
        }
    }

    private static String sharedText(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) return text.trim();
        }
        return "";
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putString("dictionary", selectedDictionary);
        state.putString("query", search.getQuery().toString());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        timeout.removeCallbacksAndMessages(null);
        adapter.unregisterAdapterDataObserver(resultsObserver);
        adapter.setOnClickListener(null);
        super.onDestroy();
    }

    public static void updateAdapter(List<Vocab> vocabs) { adapter.setVocabs(vocabs); }
}
