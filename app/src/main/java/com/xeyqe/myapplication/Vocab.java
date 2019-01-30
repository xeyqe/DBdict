package com.xeyqe.myapplication;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "vocab_table")
public class Vocab {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String word;

    private String meaning;

    private String language;

    public Vocab(String word, String meaning, String language) {
        this.word = word;
        this.meaning = meaning;
        this.language = language;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getLanguage() {
        return language;
    }
}