package com.langfree.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.langfree.model.ChatMessage;
import com.langfree.model.Conversation;
import com.langfree.model.DeviceLicenseEntry;
import com.langfree.model.Mistake;
import com.langfree.model.QuizSession;
import com.langfree.model.QuizTurn;

import java.io.File;

/**
 * Base SQLite locale. Les conversations, messages et erreurs y sont stockés.
 */
@Database(
        entities = {Conversation.class, ChatMessage.class, Mistake.class, QuizSession.class, QuizTurn.class,
                DeviceLicenseEntry.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String NAME = "langfree.db";
    private static volatile AppDatabase instance;

    public abstract ConversationDao conversationDao();

    public abstract MessageDao messageDao();

    public abstract MistakeDao mistakeDao();

    public abstract QuizSessionDao quizSessionDao();

    public abstract QuizTurnDao quizTurnDao();

    public abstract DeviceLicenseDao deviceLicenseDao();

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `device_licenses` ("
                    + "`codeHash` TEXT NOT NULL, "
                    + "`acceptedAt` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`codeHash`))");
        }
    };

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`startedAt` INTEGER NOT NULL, "
                    + "`updatedAt` INTEGER NOT NULL, "
                    + "`levelEstimate` TEXT, "
                    + "`questionCount` INTEGER NOT NULL)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `quiz_turns` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`sessionId` INTEGER NOT NULL, "
                    + "`question` TEXT, "
                    + "`answer` TEXT, "
                    + "`correction` TEXT, "
                    + "`explanation` TEXT, "
                    + "`levelEstimate` TEXT, "
                    + "`createdAt` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_quiz_turns_sessionId` "
                    + "ON `quiz_turns` (`sessionId`)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    Context app = context.getApplicationContext();
                    discardLegacyDatabase(app);
                    instance = Room.databaseBuilder(app, AppDatabase.class, NAME)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
