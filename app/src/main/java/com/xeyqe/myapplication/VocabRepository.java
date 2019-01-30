package com.xeyqe.myapplication;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class VocabRepository {
    private VocabDao vocabDao;
    private LiveData<List<Vocab>> allVocabs;
    private List<Vocab> allSearchedVocabs;

    public VocabRepository(Application application) {
        VocabDatabase database = VocabDatabase.getInstance(application);
        vocabDao = database.vocabDao();
        allVocabs = vocabDao.getAllVocabs();
    }

    public void insert(Vocab vocab) {
        new InsertVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void update(Vocab vocab) {
        new UpdateVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void delete(Vocab vocab) {
        new DeleteVocabAsyncTask(vocabDao).execute(vocab);
    }

    public void deleteAllVocabs() {
        new DeleteAllVocabsAsyncTask(vocabDao).execute();
    }

    public LiveData<List<Vocab>> getAllVocabs() {
        return allVocabs;
    }
    public LiveData<List<Vocab>> getAllSearchedVocabs(String hledany) {
        return vocabDao.getAllSearchedVocabs("%" + hledany + "%");
    }

    /*public void getAllSearchedVocabs(String hledany) {
        new GetAllSearchedVocabsAsyncTask(vocabDao).execute(hledany);
    }*/


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

    private static class DeleteAllVocabsAsyncTask extends AsyncTask<Void, Void, Void> {
        private VocabDao vocabDao;

        private DeleteAllVocabsAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            vocabDao.deleteAllVocabs();
            return null;
        }
    }

    /*private static class GetAllSearchedVocabsAsyncTask extends AsyncTask<String, Void, List<Vocab>> {
        private VocabDao vocabDao;

        private GetAllSearchedVocabsAsyncTask(VocabDao vocabDao) {
            this.vocabDao = vocabDao;
        }

        @Override
        protected List<Vocab> doInBackground(String... strings) {
            return vocabDao.getAllSearchedVocabs(strings[0]);
        }


    }*/
}