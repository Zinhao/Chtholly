package com.zinhao.chtholly.entity;

public class StaticAskAble extends Command{
    String staticAnswer;

    public StaticAskAble(String packageName, Message question, String staticAnswer) {
        super(packageName, question);
        this.staticAnswer = staticAnswer;
        setReplyReady(true);
    }

    @Override
    protected boolean throwToChild() {
        return false;
    }

    @Override
    public boolean handleAsk() {
        getAnswer().setMessage(staticAnswer);
        return true;
    }
}
