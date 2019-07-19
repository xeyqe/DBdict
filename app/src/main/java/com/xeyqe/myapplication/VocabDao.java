package com.xeyqe.myapplication;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface VocabDao {

    @Insert
    void insert(Vocab vocab);

    @Insert
    void insertAll(List<Vocab> vocabs);

    @Update
    void update(Vocab vocab);

    @Delete
    void delete(Vocab vocab);

    @Query("DELETE FROM vocab_table WHERE language = :language")
    void deleteAllVocabs(String language);

    @Query("SELECT * FROM vocab_table WHERE history = 1 ORDER BY word COLLATE NOCASE ASC")
    LiveData<List<Vocab>> getAllVocabs();

    @Query("SELECT * FROM vocab_table WHERE word LIKE :hledany ORDER BY word COLLATE NOCASE ASC")
    LiveData<List<Vocab>> getAllSearchedVocabs(String hledany);

    @Query("SELECT * FROM vocab_table WHERE language = :language AND word LIKE :hledany ORDER BY word COLLATE NOCASE ASC")
    LiveData<List<Vocab>> getSearchedVocabs(String hledany, String language);

    @Query("SELECT DISTINCT language as lang FROM vocab_table")
    List<String> getAllLanguages();
}