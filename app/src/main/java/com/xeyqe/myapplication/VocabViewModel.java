package com.xeyqe.myapplication;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

public class VocabViewModel extends AndroidViewModel {
    private VocabRepository repository;
    private LiveData<List<Vocab>> allVocabs;

    public VocabViewModel(@NonNull Application application) {
        super(application);
        repository = new VocabRepository(application);
        allVocabs = repository.getAllVocabs();
    }

    public void insert(Vocab vocab) {
        repository.insert(vocab);
    }

    public void update(Vocab vocab) {
        repository.update(vocab);
    }

    public void delete(Vocab vocab) {
        repository.delete(vocab);
    }

    public void deleteAllNotes() {
        repository.deleteAllVocabs();
    }

    public LiveData<List<Vocab>> getAllVocabs() {
        return allVocabs;
    }
    public LiveData<List<Vocab>> getAllSearchedVocabs(String hledany) {
        return repository.getAllSearchedVocabs(hledany);
    }
}