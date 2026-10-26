package com.xeyqe.myapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Transaction;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VocabDao {

    @Insert
    void insertAll(List<Vocab> vocabs);

    @Update
    void update(Vocab vocab);

    @Query("DELETE FROM vocab_table WHERE language = :language")
    void deleteAllVocabs(String language);

    @Query("SELECT COUNT(*) FROM vocab_table WHERE language = :name")
    int dictionaryEntryCount(String name);

    @Query("UPDATE vocab_table SET language = :newName WHERE language = :oldName")
    int updateDictionaryName(String oldName, String newName);

    /** Returns -1 for an existing name, 0 for a missing dictionary, or the renamed entry count. */
    @Transaction
    default int renameDictionary(String oldName, String newName) {
        if (!oldName.equals(newName) && dictionaryEntryCount(newName) > 0) return -1;
        return updateDictionaryName(oldName, newName);
    }

    @Query("SELECT * FROM vocab_table WHERE history = 1 ORDER BY word COLLATE NOCASE ASC")
    List<Vocab> getAllVocabs();

    @Query("SELECT * FROM vocab_table WHERE (:language IS NULL OR language = :language) AND word LIKE :hledany ORDER BY word COLLATE NOCASE ASC")
    List<Vocab> getSearchedVocabs(String hledany, String language);

    @Query("SELECT DISTINCT language as lang FROM vocab_table")
    LiveData<List<String>> getAllLanguages();
}