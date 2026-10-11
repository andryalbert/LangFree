package com.langfree.database;

import android.content.Context;

import com.langfree.model.VocabularyItem;

import java.util.List;

public class VocabularyDao {
    private DatabaseHelper dbHelper;
    
    public VocabularyDao(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }
    
    public long insert(VocabularyItem item) {
        return dbHelper.addVocabularyItem(item);
    }
    
    public List<VocabularyItem> getAll() {
        return dbHelper.getAllVocabulary();
    }
    
    public void delete(long id) {
        dbHelper.deleteVocabularyItem(id);
    }
    
    public void clearAll() {
        dbHelper.clearVocabulary();
    }
}
