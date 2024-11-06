package com.zinhao.chtholly.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class AICharacter {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public String desc;

    public AICharacter(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
