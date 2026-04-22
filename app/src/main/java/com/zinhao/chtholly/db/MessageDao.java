package com.zinhao.chtholly.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.zinhao.chtholly.entity.Message;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insertAll(Message... users);

    @Insert
    void insert(Message message);

    @Delete
    void delete(Message user);

    @Query("SELECT * FROM message")
    List<Message> getAll();

    @Query("SELECT * FROM (SELECT * FROM message ORDER BY timeStamp DESC LIMIT 10) ORDER BY timeStamp ASC")
    List<Message> getLastTenMessages();

    interface MessageGetAllListener{
        void onSuccess(List<Message> result);
    }
}
