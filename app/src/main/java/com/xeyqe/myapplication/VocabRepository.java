package com.xeyqe.myapplication;

import android.app.Application;
import androidx.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class VocabRepository {
    private VocabDao vocabDao;
    private LiveData<List<Vocab>> allVocabs;
    private List<String> getAllLanguages;

    public VocabRepository(Application application) {
        VocabDatabase database = VocabDatabase.getInstance(application);
        vocabDao = database.vocabDao();
        allVocabs = vocabDao.getAllVocabs();
    }

    public void insert(Vocab vocab) {
        new InsertVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void insertAll(List<Vocab> vocabs) {
        new InsertAllVocabsAsyncTask(vocabDao).execute(vocabs);
    }

    public void update(Vocab vocab) {
        new UpdateVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void delete(Vocab vocab) {
        new DeleteVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void deleteAllVocabs(String language) {
        new DeleteAllVocabsAsyncTask(vocabDao).execute(language);
    }

    public List<String> getAllLanguages() {
        new GetAllLanguagesAsyncTask(vocabDao).execute();
        return getAllLanguages;
    }

    public LiveData<List<Vocab>> getAllVocabs() {
        return allVocabs;
    }
    public LiveData<List<Vocab>> getAllSearchedVocabs(String hledany) {
        return vocabDao.getAllSearchedVocabs(hledany + "%");
    }
    public LiveData<List<Vocab>> getSearchedVocabs(String hledany, String language) {
        return vocabDao.getSearchedVocabs(hledany + "%", language);
    }

    private static class InsertVocabAsyncTask extends AsyncTask<Vocab, Void, Void> {
        private VocabDao vocabDao;

        private InsertVocabAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected Void doInBackground(Vocab... vocabs) {
            vocabDao.insert(vocabs[0]);
            return null;
        }
    }

    private class GetAllLanguagesAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private VocabDao vocabDao;

        private GetAllLanguagesAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            return vocabDao.getAllLanguages();

        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            getAllLanguages = strings;

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

    private static class DeleteVocabAsyncTask extends AsyncTask<Vocab, Void, Void> {
        private VocabDao vocabDao;

        private DeleteVocabAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected Void doInBackground(Vocab... vocabs) {
            vocabDao.delete(vocabs[0]);
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