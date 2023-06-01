package com.zinhao.chtholly;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import com.zinhao.chtholly.entity.Message;

@Database(entities = {Message.class},version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
}
