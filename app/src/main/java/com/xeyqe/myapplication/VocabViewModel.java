package com.xeyqe.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import java.util.List;

public class VocabViewModel extends AndroidViewModel {
    private VocabRepository repository;

    public VocabViewModel(@NonNull Application application) {
        super(application);
        repository = new VocabRepository(application);
    }

    public void insertAll(List<Vocab> vocabs) {
        repository.insertAll(vocabs);
    }

    public void update(Vocab vocab) {
        repository.update(vocab);
    }


    public void deleteAllNotes(String language) {
        repository.deleteAllVocabs(language);
    }

    public void getAllVocabs() {
        repository.getAllVocabs();
    }
    public void getAllSearchedVocabs(String hledany,String language) {
        repository.getAllSearchedVocabs(hledany, language);
    }

    public LiveData<List<String>> getGetAllLanguages() {
        return repository.getAllLanguages();
    }
}