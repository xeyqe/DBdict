package com.xeyqe.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import java.util.List;

public class VocabViewModel extends AndroidViewModel {
    private VocabRepository repository;
    private List<Vocab> allVocabs;
    private LiveData<List<String>> getAllLanguages;

    public VocabViewModel(@NonNull Application application) {
        super(application);
        repository = new VocabRepository(application);
        //allVocabs = repository.getAllVocabs();
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

    public void getAllVocabs() {
        //return allVocabs;
        repository.getAllVocabs();
    }
    public void getAllSearchedVocabs(String hledany,String language) {
        repository.getAllSearchedVocabs(hledany, language);
    }
    /*public List<Vocab> getSearchedVocabs(String hledany, String language) {
        return repository.getAllSearchedVocabs(hledany, language);
    }*/

    public LiveData<List<String>> getGetAllLanguages() {
        return repository.getAllLanguages();
    }
}