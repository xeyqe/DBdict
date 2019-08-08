package com.xeyqe.myapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VocabDao {

    @Insert
    void insert(Vocab vocab);

    @Insert
    void insertAll(List<Vocab> vocabs);

    @Update
    void update(Vocab vocab);

    @Query("DELETE FROM vocab_table WHERE language = :language")
    void deleteAllVocabs(String language);

    @Query("SELECT * FROM vocab_table WHERE history = 1 ORDER BY word COLLATE NOCASE ASC")
    List<Vocab> getAllVocabs();

    @Query("SELECT * FROM vocab_table WHERE language LIKE :language AND word LIKE :hledany ORDER BY word COLLATE NOCASE ASC")
    List<Vocab> getSearchedVocabs(String hledany, String language);

    @Query("SELECT DISTINCT language as lang FROM vocab_table")
    LiveData<List<String>> getAllLanguages();
}