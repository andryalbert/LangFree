package com.langfree.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.langfree.model.Message;
import com.langfree.model.VocabularyItem;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper instance;
    
    private static final String DATABASE_NAME = "langfree.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table messages
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_CORRECTION = "correction";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    
    // Table vocabulary
    private static final String TABLE_VOCABULARY = "vocabulary";
    private static final String COLUMN_ENGLISH = "english";
    private static final String COLUMN_FRENCH = "french";
    
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Créer la table messages
        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TYPE + " INTEGER,"
                + COLUMN_TEXT + " TEXT,"
                + COLUMN_CORRECTION + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_MESSAGES_TABLE);
        
        // Créer la table vocabulary
        String CREATE_VOCABULARY_TABLE = "CREATE TABLE " + TABLE_VOCABULARY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ENGLISH + " TEXT,"
                + COLUMN_FRENCH + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER"
                + ")";
        db.execSQL(CREATE_VOCABULARY_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VOCABULARY);
        onCreate(db);
    }
    
    // ===== MESSAGES =====
    
    public long addMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TYPE, message.getType());
        values.put(COLUMN_TEXT, message.getText());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        
        // Sérialiser la correction (simplifié)
        if (message.getCorrection() != null) {
            String correctionJson = message.getCorrection().getCorrectedText() + "|" + 
                                    message.getCorrection().getExplanation();
            values.put(COLUMN_CORRECTION, correctionJson);
        }
        
        long id = db.insert(TABLE_MESSAGES, null, values);
        db.close();
        return id;
    }
    
    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " + COLUMN_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                message.setType(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                message.setText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT)));
                message.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                
                // Désérialiser la correction (simplifié)
                String correctionJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CORRECTION));
                if (correctionJson != null && !correctionJson.isEmpty()) {
                    String[] parts = correctionJson.split("\\|");
                    if (parts.length >= 2) {
                        // Pour l'instant, on ne restaure pas complètement la correction
                        // On pourrait améliorer cela avec une sérialisation JSON complète
                    }
                }
                
                messages.add(message);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return messages;
    }
    
    public void clearMessages() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
        db.close();
    }
    
    // ===== VOCABULARY =====
    
    public long addVocabularyItem(VocabularyItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_ENGLISH, item.getEnglish());
        values.put(COLUMN_FRENCH, item.getFrench());
        values.put(COLUMN_TIMESTAMP, item.getTimestamp());
        
        long id = db.insert(TABLE_VOCABULARY, null, values);
        db.close();
        return id;
    }
    
    public List<VocabularyItem> getAllVocabulary() {
        List<VocabularyItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selectQuery = "SELECT * FROM " + TABLE_VOCABULARY + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                VocabularyItem item = new VocabularyItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                item.setEnglish(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH)));
                item.setFrench(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FRENCH)));
                item.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                
                items.add(item);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return items;
    }
    
    public void deleteVocabularyItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VOCABULARY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
    
    public void clearVocabulary() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VOCABULARY, null, null);
        db.close();
    }
}
