package com.zinhao.chtholly.entity;

public class StaticAskAble extends Command{
    String staticAnswer;

    public StaticAskAble(String packageName, Message question, String staticAnswer) {
        super(packageName, question);
        this.staticAnswer = staticAnswer;
    }

    @Override
    public boolean ask() {
        super.ask();
        getAnswer().setMessage(staticAnswer);
        return true;
    }
}
