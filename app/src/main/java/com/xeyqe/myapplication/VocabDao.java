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