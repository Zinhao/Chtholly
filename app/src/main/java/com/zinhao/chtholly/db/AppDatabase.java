package com.zinhao.chtholly.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.entity.ChatSession;
import com.zinhao.chtholly.entity.Message;
import org.jetbrains.annotations.NotNull;

@Database(entities = {Message.class, AICharacter.class, ChatSession.class}, version = 5, autoMigrations = {
        @AutoMigration(
                from = 1,
                to = 2,
                spec = AppDatabase.MyAutoMigration.class
        )
})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract AICharacterDao characterDao();
    public abstract ChatSessionDao chatSessionDao();

    static class MyAutoMigration implements AutoMigrationSpec {
        @Override
        public void onPostMigrate(@NonNull @NotNull SupportSQLiteDatabase db) {
            AutoMigrationSpec.super.onPostMigrate(db);
        }
    }

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull @NotNull SupportSQLiteDatabase database) {
            // 1. Create ChatSession table
            database.execSQL("CREATE TABLE IF NOT EXISTS `ChatSession` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`characterId` INTEGER NOT NULL, "
                    + "`title` TEXT, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "`lastMessageAt` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ChatSession_characterId` ON `ChatSession` (`characterId`)");

            // 2. Create a ChatSession for each existing AICharacter
            database.execSQL("INSERT INTO `ChatSession` (`characterId`, `title`, `createdAt`, `lastMessageAt`) "
                    + "SELECT `id`, `name`, strftime('%s','now') * 1000, strftime('%s','now') * 1000 FROM `AICharacter`");

            // 3. Create new Message table with correct schema (including foreign key)
            database.execSQL("CREATE TABLE IF NOT EXISTS `Message_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`message` TEXT, "
                    + "`speaker` TEXT, "
                    + "`timeStamp` INTEGER NOT NULL, "
                    + "`sessionId` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`sessionId`) REFERENCES `ChatSession`(`id`) ON DELETE CASCADE)");

            // 4. Copy data from old Message table to new one
            database.execSQL("INSERT INTO `Message_new` (`id`, `message`, `speaker`, `timeStamp`, `sessionId`) "
                    + "SELECT `id`, `message`, `speaker`, `timeStamp`, (SELECT MIN(`id`) FROM `ChatSession`) FROM `Message`");

            // 5. Drop old table and rename new one
            database.execSQL("DROP TABLE IF EXISTS `Message`");
            database.execSQL("ALTER TABLE `Message_new` RENAME TO `Message`");

            // 6. Create index on sessionId
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_Message_sessionId` ON `Message` (`sessionId`)");
        }
    };

    public static Migration createMigration3_4(Context context) {
        boolean globalRoleplay = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
                .getBoolean("roleplay", true);
        int defaultVal = globalRoleplay ? 1 : 0;
        return new Migration(3, 4) {
            @Override
            public void migrate(@NonNull @NotNull SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE `AICharacter` ADD COLUMN `roleplay` INTEGER NOT NULL DEFAULT " + defaultVal);
            }
        };
    }

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull @NotNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `AICharacter` ADD COLUMN `builtin` INTEGER NOT NULL DEFAULT 0");
        }
    };
}
