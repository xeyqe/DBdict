package com.xeyqe.myapplication;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class VocabRepository {
    private VocabDao vocabDao;
    private LiveData<List<String>> getAllLanguages;

    public VocabRepository(Application application) {
        VocabDatabase database = VocabDatabase.getInstance(application);
        vocabDao = database.vocabDao();
        getAllLanguages = vocabDao.getAllLanguages();
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