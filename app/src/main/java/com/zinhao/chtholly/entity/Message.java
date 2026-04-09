package com.zinhao.chtholly.entity;

import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Entity
public class Message {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String message;
    public String speaker;
    public long timeStamp;
    @Ignore
    public AccessibilityNodeInfo nodeInfo;
    @Ignore
    public int leve;
    @Ignore
    public String tag;
    @Ignore
    public boolean other = false;
    @Ignore
    private boolean enableCommand = false;

    public Message(String speaker, String message, long timeStamp) {
        this.message = message;
        this.speaker = speaker;
        this.timeStamp = timeStamp;
    }

    public Message(String speaker, String message, long timeStamp,boolean other) {
        this.message = message;
        this.speaker = speaker;
        this.timeStamp = timeStamp;
        this.other = other;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message1 = (Message) o;
        if (!Objects.equals(message, message1.message)) return false;
        return Objects.equals(speaker, message1.speaker);
    }

    @NonNull
    @Override
    public @NotNull String toString() {
        return tag +" [leve"+ leve +"] "+ speaker+": "+ message +", permission:"+enableCommand;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (speaker != null ? speaker.hashCode() : 0);
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        return result;
    }

    public void setNodeInfo(AccessibilityNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public AccessibilityNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public int getLeve() {
        return leve;
    }

    public void setLeve(int leve) {
        this.leve = leve;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isOther() {
        return other;
    }

    public boolean isEnableCommand() {
        return enableCommand;
    }

    public void setEnableCommand(boolean enableCommand) {
        this.enableCommand = enableCommand;
    }
}
