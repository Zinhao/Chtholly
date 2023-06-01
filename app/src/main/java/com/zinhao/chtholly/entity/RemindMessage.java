package com.zinhao.chtholly.entity;

public class RemindMessage extends Message{
    private long sendTime;
    private String master;

    public RemindMessage(String speaker, String message,long sendTime, String master) {
        super(speaker, message, sendTime);
        this.sendTime = sendTime;
        this.master = master;
    }

    public long getSendTime() {
        return sendTime;
    }

    public String getMaster() {
        return master;
    }
}
