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

    /**
     * 获取最后 10 条消息
     * ORDER BY id DESC: 按 ID 从大到小排列（最新的在前）
     * LIMIT 10: 只取前 10 条
     */
    @Query("SELECT * FROM message ORDER BY id DESC LIMIT 10")
    List<Message> getLastTenMessages();

    interface MessageGetAllListener{
        void onSuccess(List<Message> result);
    }
}
