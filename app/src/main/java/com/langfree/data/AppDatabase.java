package com.langfree.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.langfree.model.ChatMessage;
import com.langfree.model.Conversation;
import com.langfree.model.Mistake;

/**
 * Base SQLite locale. Les conversations, messages et erreurs y sont stockés.
 */
@Database(
        entities = {Conversation.class, ChatMessage.class, Mistake.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract ConversationDao conversationDao();

    public abstract MessageDao messageDao();

    public abstract MistakeDao mistakeDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "langfree.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
