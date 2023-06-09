package com.zinhao.chtholly.entity;

import com.zinhao.chtholly.session.OpenAiSession;

import java.util.Locale;

public class StaticMessage extends Command{
    String staticAnswer;

    public StaticMessage(String packageName, Message question, String staticAnswer) {
        super(packageName, question);
        this.staticAnswer = staticAnswer;
    }

    @Override
    public boolean ask() {
        super.ask();
        getAnswer().setMessage(String.format(Locale.CHINA,"@%s %s",getQuestion().getSpeaker(),staticAnswer));
        OpenAiSession.getInstance().addAssistantChat(staticAnswer);
        return true;
    }
}
