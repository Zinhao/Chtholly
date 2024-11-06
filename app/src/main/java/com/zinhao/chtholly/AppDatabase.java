package com.zinhao.chtholly;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.AutoMigrationSpec;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.zinhao.chtholly.entity.AICharacter;
import com.zinhao.chtholly.entity.Message;
import org.jetbrains.annotations.NotNull;

@Database(entities = {Message.class, AICharacter.class},version = 2,autoMigrations = {
        @AutoMigration(
                from = 1,
                to = 2,
                spec = AppDatabase.MyAutoMigration.class
        )
})
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract AICharacterDao characterDao();
    static class MyAutoMigration implements AutoMigrationSpec {
        @Override
        public void onPostMigrate(@NonNull @NotNull SupportSQLiteDatabase db) {
            AutoMigrationSpec.super.onPostMigrate(db);
        }
    }
}

