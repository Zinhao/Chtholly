package com.zinhao.chtholly.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(
        entity = AICharacter.class,
        parentColumns = "id",
        childColumns = "characterId",
        onDelete = ForeignKey.CASCADE
), indices = @Index("characterId"))
public class ChatSession {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long characterId;
    public String title;
    public long createdAt;
    public long lastMessageAt;

    public ChatSession(long characterId, String title, long createdAt) {
        this.characterId = characterId;
        this.title = title;
        this.createdAt = createdAt;
        this.lastMessageAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCharacterId() {
        return characterId;
    }

    public void setCharacterId(long characterId) {
        this.characterId = characterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}
