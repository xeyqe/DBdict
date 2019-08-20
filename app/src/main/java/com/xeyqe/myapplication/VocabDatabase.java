package com.xeyqe.myapplication;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Database(entities = {Vocab.class}, version = 1, exportSchema = false)
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

            Context context = GlobalApplication.getAppContext();
            BufferedReader reader = null;

            try {
                List<Vocab> vocabs2Import;
                vocabs2Import = new ArrayList<>();
                reader = new BufferedReader(
                        new InputStreamReader(context.getAssets().open("eng-cze.txt")));

                String line = reader.readLine();
                while (line != null) {
                        String[] separated = line.split("[_]{3}");

                        vocabs2Import.add(new Vocab(separated[0], separated[1], "eng-cze", false));
                        line = reader.readLine();
                }
                vocabDao.insertAll(vocabs2Import);
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