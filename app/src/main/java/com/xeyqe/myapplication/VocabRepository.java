package com.xeyqe.myapplication;

import android.app.Application;
import android.os.AsyncTask;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

import androidx.lifecycle.LiveData;

import java.util.List;

public class VocabRepository {
    private static final ExecutorService IMPORTS = Executors.newSingleThreadExecutor();
    private final Application application;
    private final VocabDatabase database;
    private VocabDao vocabDao;
    private LiveData<List<String>> getAllLanguages;

    public VocabRepository(Application application) {
        this.application = application;
        database = VocabDatabase.getInstance(application);
        vocabDao = database.vocabDao();
        getAllLanguages = vocabDao.getAllLanguages();
    }

    /** Parse and insert in a single transaction so failed imports leave no partial dictionaries. */
    public void importDictionary(Uri uri, String name, IntConsumer completion) {
        IMPORTS.execute(() -> {
            int[] count = {0};
            try (InputStream input = application.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException("Document unavailable");
                database.runInTransaction(() -> {
                    List<Vocab> batch = new ArrayList<>(500);
                    try {
                        count[0] = DictionaryImporter.read(input, application.getCacheDir(), name,
                                (word, meaning, dictionary) -> {
                                    batch.add(new Vocab(word, meaning, dictionary, false));
                                    if (batch.size() == 500) {
                                        vocabDao.insertAll(batch);
                                        batch.clear();
                                    }
                                });
                        if (!batch.isEmpty()) vocabDao.insertAll(batch);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                });
            } catch (IOException | RuntimeException error) {
                count[0] = -1;
            }
            int result = count[0];
            new Handler(Looper.getMainLooper()).post(() -> completion.accept(result));
        });
    }

    public void renameDictionary(String oldName, String newName, IntConsumer completion) {
        IMPORTS.execute(() -> {
            int result;
            try {
                result = vocabDao.renameDictionary(oldName, newName);
            } catch (RuntimeException error) {
                result = 0;
            }
            int renamed = result;
            new Handler(Looper.getMainLooper()).post(() -> completion.accept(renamed));
        });
    }

    public void insertAll(List<Vocab> vocabs) {
        new InsertAllVocabsAsyncTask(vocabDao).execute(vocabs);
    }

    public void update(Vocab vocab) {
        new UpdateVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void deleteAllVocabs(String language) {
        new DeleteAllVocabsAsyncTask(vocabDao).execute(language);
    }

    public LiveData<List<String>> getAllLanguages() {
        return getAllLanguages;
    }

    public void getAllSearchedVocabs(String search, String language) {
        new GetSearchedVocabsAsyncTask(vocabDao).execute(search, language);
    }

    public void getAllVocabs() {
        new GetAllVocabsAsyncTask(vocabDao).execute();
    }

    private static class GetAllVocabsAsyncTask extends AsyncTask<Void, Void, List<Vocab>> {
        private VocabDao vocabDao;

        private GetAllVocabsAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected List<Vocab> doInBackground(Void... voids) {
            return vocabDao.getAllVocabs();
        }

        @Override
        protected void onPostExecute(List<Vocab> vocabs) {
            super.onPostExecute(vocabs);
            MainActivity.updateAdapter(vocabs);
        }
    }

    private static class GetSearchedVocabsAsyncTask extends AsyncTask<String, Void, List<Vocab>> {
        private VocabDao vocabDao;

        private GetSearchedVocabsAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected List<Vocab> doInBackground(String... strings) {
            return vocabDao.getSearchedVocabs(strings[0],strings[1]);
        }

        @Override
        protected void onPostExecute(List<Vocab> vocabs) {
            super.onPostExecute(vocabs);
            MainActivity.updateAdapter(vocabs);
        }
    }

    private static class InsertAllVocabsAsyncTask extends AsyncTask<List<Vocab>, Void, Void> {
        private VocabDao vocabDao;

        private InsertAllVocabsAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected Void doInBackground(List<Vocab>... vocabs) {
            vocabDao.insertAll(vocabs[0]);
            return null;
        }
    }

    private static class UpdateVocabAsyncTask extends AsyncTask<Vocab, Void, Void> {
        private VocabDao vocabDao;

        private UpdateVocabAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected Void doInBackground(Vocab... vocabs) {
            vocabDao.update(vocabs[0]);
            return null;
        }
    }

    private static class DeleteAllVocabsAsyncTask extends AsyncTask<String, Void, Void> {
        private VocabDao vocabDao;

        private DeleteAllVocabsAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected Void doInBackground(String... language) {
            vocabDao.deleteAllVocabs(language[0]);
            return null;
        }
    }
}