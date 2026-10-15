package com.langfree.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.langfree.model.ChatMessage;
import com.langfree.model.Conversation;
import com.langfree.model.Mistake;

import java.io.File;

/**
 * Base SQLite locale. Les conversations, messages et erreurs y sont stockés.
 */
@Database(
        entities = {Conversation.class, ChatMessage.class, Mistake.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String NAME = "langfree.db";
    private static volatile AppDatabase instance;

    public abstract ConversationDao conversationDao();

    public abstract MessageDao messageDao();

    public abstract MistakeDao mistakeDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    Context app = context.getApplicationContext();
                    discardLegacyDatabase(app);
                    instance = Room.databaseBuilder(app, AppDatabase.class, NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * L'ancienne application a laissé un langfree.db sans table Room.
     * Room le prend pour une base préinstallée et plante :
     * « Pre-packaged database has an invalid schema ».
     */
    private static void discardLegacyDatabase(Context context) {
        File database = context.getDatabasePath(NAME);
        if (!database.exists() || isRoomDatabase(database)) {
            return;
        }
        context.deleteDatabase(NAME);
    }

    private static boolean isRoomDatabase(File database) {
        SQLiteDatabase sqlite = null;
        try {
            sqlite = SQLiteDatabase.openDatabase(
                    database.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor cursor = sqlite.rawQuery(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'room_master_table' LIMIT 1",
                    null)) {
                return cursor.moveToFirst();
            }
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            if (sqlite != null) {
                sqlite.close();
            }
        }
    }
}
