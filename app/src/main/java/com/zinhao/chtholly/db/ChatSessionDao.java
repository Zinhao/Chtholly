package com.zinhao.chtholly.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.zinhao.chtholly.entity.ChatSession;

import java.util.List;

@Dao
public interface ChatSessionDao {
    @Insert
    long insert(ChatSession session);

    @Delete
    void delete(ChatSession session);

    @Query("SELECT * FROM ChatSession WHERE characterId = :characterId ORDER BY lastMessageAt DESC")
    List<ChatSession> getByCharacterId(long characterId);

    @Query("SELECT * FROM ChatSession WHERE characterId = :characterId ORDER BY lastMessageAt DESC LIMIT 1")
    ChatSession getLatestByCharacterId(long characterId);

    @Query("SELECT * FROM ChatSession WHERE id = :id")
    ChatSession getById(long id);

    @Query("UPDATE ChatSession SET lastMessageAt = :time WHERE id = :sessionId")
    void updateLastMessageTime(long sessionId, long time);
}
