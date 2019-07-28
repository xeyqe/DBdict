package com.xeyqe.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import java.util.List;

public class VocabViewModel extends AndroidViewModel {
    private VocabRepository repository;
    private LiveData<List<Vocab>> allVocabs;
    private List<String> getAllLanguages;

    public VocabViewModel(@NonNull Application application) {
        super(application);
        repository = new VocabRepository(application);
        allVocabs = repository.getAllVocabs();
        getAllLanguages = repository.getAllLanguages();
    }

    public void insert(Vocab vocab) {
        repository.insert(vocab);
    }

    public void insertAll(List<Vocab> vocabs) {
        repository.insertAll(vocabs);
    }

    public void update(Vocab vocab) {
        repository.update(vocab);
    }

    public void delete(Vocab vocab) {
        repository.delete(vocab);
    }

    public void deleteAllNotes(String language) {
        repository.deleteAllVocabs(language);
    }

    public LiveData<List<Vocab>> getAllVocabs() {
        return allVocabs;
    }
    public LiveData<List<Vocab>> getAllSearchedVocabs(String hledany) {
        return repository.getAllSearchedVocabs(hledany);
    }
    public LiveData<List<Vocab>> getSearchedVocabs(String hledany, String language) {
        return repository.getSearchedVocabs(hledany, language);
    }

    public List<String> getGetAllLanguages() {
        return repository.getAllLanguages();
    }
}