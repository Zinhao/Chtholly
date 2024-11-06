package com.zinhao.chtholly.entity;

import android.view.accessibility.AccessibilityNodeInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import kotlin.jvm.Transient;

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

    public Message(String speaker, String message, long timeStamp) {
        this.message = message;
        this.speaker = speaker;
        this.timeStamp = timeStamp;
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
}
