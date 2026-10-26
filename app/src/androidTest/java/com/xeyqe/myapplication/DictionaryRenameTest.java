package com.xeyqe.myapplication;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DictionaryRenameTest {
    private VocabDatabase database;
    private VocabDao dao;

    @Before public void createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), VocabDatabase.class).build();
        dao = database.vocabDao();
        dao.insertAll(Arrays.asList(new Vocab("Hund", "pes", "German", true),
                new Vocab("Katze", "kočka", "German", false),
                new Vocab("dog", "pes", "English", false)));
    }

    @After public void closeDatabase() { database.close(); }

    @Test public void renamePreservesWordsIdsAndHistoryAndUsesExactFilterNames() {
        List<Vocab> before = dao.getSearchedVocabs("Hund", "German");
        assertEquals(2, dao.renameDictionary("German", "Deutsch_100%"));
        assertEquals(0, dao.dictionaryEntryCount("German"));
        assertEquals(2, dao.dictionaryEntryCount("Deutsch_100%"));
        Vocab after = dao.getSearchedVocabs("Hund", "Deutsch_100%").get(0);
        assertEquals(before.get(0).getId(), after.getId());
        assertEquals("pes", after.getMeaning());
        assertTrue(after.getHistory());
        assertEquals(1, dao.getAllVocabs().size());
        assertEquals(1, dao.dictionaryEntryCount("English"));
        dao.insertAll(Arrays.asList(new Vocab("other", "meaning", "DeutschX1000", false)));
        assertEquals(2, dao.getSearchedVocabs("%", "Deutsch_100%").size());
        assertEquals(4, dao.getSearchedVocabs("%", null).size());
    }

    @Test public void rejectsDuplicateNameWithoutMergingDictionaries() {
        assertEquals(-1, dao.renameDictionary("German", "English"));
        assertEquals(2, dao.dictionaryEntryCount("German"));
        assertEquals(1, dao.dictionaryEntryCount("English"));
    }

    @Test public void missingDictionaryDoesNotChangeOtherEntries() {
        assertEquals(0, dao.renameDictionary("Missing", "New name"));
        assertEquals(2, dao.dictionaryEntryCount("German"));
        assertEquals(0, dao.dictionaryEntryCount("New name"));
    }
}
