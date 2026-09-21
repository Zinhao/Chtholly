package com.zinhao.chtholly.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.zinhao.chtholly.entity.AICharacter;

import java.util.List;

@Dao
public interface AICharacterDao {
    @Insert
    void insertAll(AICharacter... characters);

    @Insert
    Long insert(AICharacter character);

    @Delete
    void delete(AICharacter character);

    @Query("SELECT * FROM aicharacter")
    List<AICharacter> getAll();

    @Query("SELECT COUNT(*) FROM aicharacter")
    int getAICharacterCount();

    @Query("SELECT * FROM aicharacter where aicharacter.id = :id")
    AICharacter getAICharacterById(long id);

    @Query("SELECT * FROM aicharacter WHERE builtin = 1 LIMIT 1")
    AICharacter getBuiltinCharacter();

    @Query("UPDATE aicharacter SET roleplay = :roleplay WHERE id = :id")
    void updateRoleplay(long id, boolean roleplay);

    @Update
    void update(AICharacter character);

    interface AICharacterGetAllListener{
        void onSuccess(List<AICharacter> result);
    }
}
