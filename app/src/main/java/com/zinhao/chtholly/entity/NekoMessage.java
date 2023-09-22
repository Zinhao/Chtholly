package com.zinhao.chtholly.entity;

import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.OpenAiSession;

public class NekoMessage extends Command {
    public static final String LOOK_NODE = "/看看你的本子";
    public static final String STUDY = "/主人说";
    public static final String SAY = "/要回答";
    public static final String HARD = "好复杂，听不懂的喵~";
    public static final String DONT_SUPPORT= "我做不到，主人！喵~";
    public static final String OK = "好的喵~";
    public static final String THINK = "要怎么做呢？喵？";
    public static final String NOT_FORGET = "我会记住的喵!";
    public static final String ASK_NORMAL_TEMP = "主人，$安喵~";
    public static final String TOO_HIGH = "墙太高爬不出去喵，我尽力吧。";
    public static final String KOU_WAI = "还以为永远都见不到你了呢？";

    public NekoMessage(String packageName, Message question) {
        super(packageName, question);
    }

    @Override
    public boolean ask() {
        if(super.ask()){
            return true;
        }
        if(throwQuestion()){
            return true;
        }
        return NekoSession.getInstance().startAsk(this);
    }

    public boolean throwQuestion(){
        return false;
    }
}
