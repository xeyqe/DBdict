package com.xeyqe.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vocab_table")
public class Vocab {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String word;
    private String meaning;
    private String language;
    private boolean history;

    public Vocab(String word, String meaning, String language, boolean history) {
        this.word = word;
        this.meaning = meaning;
        this.language = language;
        this.history = history;
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

    public boolean getHistory() {
        return history;
    }
}