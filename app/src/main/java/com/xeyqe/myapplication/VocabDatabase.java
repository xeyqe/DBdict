package com.xeyqe.myapplication;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

@Database(entities = {Vocab.class}, version = 1)
public abstract class VocabDatabase extends RoomDatabase {

    private static VocabDatabase instance;

    public abstract VocabDao vocabDao();

    public static synchronized VocabDatabase getInstance(Context context) {
        if (instance == null) {

            instance = Room.databaseBuilder(context.getApplicationContext(),
                    VocabDatabase.class, "vocab_database")
                    .fallbackToDestructiveMigration()
                    .addCallback(roomCallback)
                    .build();
        }
        return instance;
    }

    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            new PopulateDbAsyncTask(instance).execute();
        }
    };

    private static class PopulateDbAsyncTask extends AsyncTask<Void, Void, Void> {
        private VocabDao vocabDao;

        private PopulateDbAsyncTask(VocabDatabase db) {
            vocabDao = db.vocabDao();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Pattern neco = Pattern.compile("^##.*");

            Context context = GlobalApplication.getAppContext();
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(
                        new InputStreamReader(context.getAssets().open("eng-cze.txt")));

                String line = reader.readLine();
                while (line != null) {
                    if (!neco.matcher(line).matches()) {

                        String[] separated = line.split("\t");

                        vocabDao.insert(new Vocab(separated[0], separated[1], "eng-cze"));
                        line = reader.readLine();
                    }
                }
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
            return null;
        }
    }
}