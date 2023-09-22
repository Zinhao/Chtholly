package com.zinhao.chtholly.entity;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.CallSuper;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.QQUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;
import java.util.regex.Pattern;

public class Command implements AskAble {
    private static final String SWITCH_COMMAND_CH = "/切换到";
    private static final String SWITCH_COMMAND_EN = "/switch to";

    private static final String SEVER_BATTERY = "/电量";
    private static final String FIRST_PIC = "/p";
    private static final String CLICK_ID = "/c";
    private static final String TAKE_PHOTO = "/拍照";
    private static final String SCREEN_SHOT = "/截屏";
    private static final String SUMMARIZE_CHAT = "/sc";
    private static final String PRINT_CHARA = "/性格描述";
    private static final String PRINT_CHATS = "/历史对话";
    private static final String CLOSE_AUTO = "/关闭问候功能";
    private static final String OPEN_AUTO = "/打开问候功能";

    private final String packageName;
    private final Message question;
    private Message answer;
    private boolean write = false;
    private boolean send = false;
    private boolean outTime = false;

    public static final int ACTION_SEND_PIC = 290;
    public static final int ACTION_CLICK_ID = 291;
    public static final int ACTION_TAKE_PHOTO = 292;
    public static final int ACTION_SCREEN_SHOT = 293;
    public static final int ACTION_SEND_TEXT = 0;

    private List<Step> steps;
    private int action = 0;

    public Command(String packageName, Message question) {
        this.packageName = packageName;
        this.question = question;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Command qaMessage = (Command) o;

        if (!Objects.equals(question, qaMessage.question)) return false;
        return Objects.equals(answer, qaMessage.answer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(question, question);
    }

    public boolean sendSuccess() {
        return write && send;
    }

    public boolean actionSuccess(){
        return steps == null || steps.size() == 0;
    }

    @CallSuper
    @Override
    public boolean ask() {
        if(answer == null){
            answer = new Message(BotApp.getInstance().getBotName(),null,System.currentTimeMillis());
        }
        if(getQuestion().message.startsWith(SWITCH_COMMAND_EN) || getQuestion().message.startsWith(SWITCH_COMMAND_CH)){
            if(openaiIgnoreCase.matcher(question.getMessage()).find()){
                answer.setMessage(NekoMessage.TOO_HIGH);
                NekoChatService.mode = OpenAiSession.class;
            }else{
                answer.setMessage(NekoMessage.KOU_WAI);
                NekoChatService.mode = NekoSession.class;
            }
            return true;
        }
        if(getQuestion().getMessage().equals(SUMMARIZE_CHAT)){
            int len = OpenAiSession.getInstance().summarize();
            getAnswer().setMessage(String.format(Locale.CHINA,"%s 将为主人总结%d条对话。",BotApp.getInstance().getBotName(),len));
            return true;
        }

        if(getQuestion().getMessage().equals(PRINT_CHARA)){
            String chara = OpenAiSession.getInstance().getChara();
            getAnswer().setMessage(String.format(Locale.CHINA,"这是%s的设定： %s。",BotApp.getInstance().getBotName(),chara));
            return true;
        }
        if(getQuestion().getMessage().equals(PRINT_CHATS)){
            String his = OpenAiSession.getInstance().getContextChat();
            getAnswer().setMessage(his);
            return true;
        }

        if(getQuestion().getMessage().equals(SEVER_BATTERY)){
            BatteryManager manager = (BatteryManager)BotApp.context().getSystemService(Context.BATTERY_SERVICE);
            int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            getAnswer().setMessage(String.format(Locale.CHINA,"%d%%,喵～",currentLevel));
            return true;
        }
        if(getQuestion().getMessage().startsWith(CLICK_ID) && getQuestion().getMessage().contains(" ")){
            action = ACTION_CLICK_ID;
            steps = new Vector<>();
            String[] ids = getQuestion().getMessage().split(" ");
            for (int i = 1; i < ids.length; i++) {
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id"+ids[i], AccessibilityNodeInfo.ACTION_CLICK,false));
            }
            getAnswer().setMessage(NekoMessage.OK);
            return true;
        }

        if(getQuestion().getMessage().equals(FIRST_PIC)){
            action = ACTION_SEND_PIC;
            // 发送最新一张图 /gnt /qhp /fun_btn /gnt  三星
            // 发送最新一张图 /gnt /qhq /send_btn /gnt  pixel3
            steps = new Vector<>();
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicCheckBoxId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getSendButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
            getAnswer().setMessage(NekoMessage.OK);
            return true;
        }
        if(getQuestion().getMessage().equals(TAKE_PHOTO)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                action = ACTION_TAKE_PHOTO;
                steps = new Vector<>();
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/go6", AccessibilityNodeInfo.ACTION_CLICK,false));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME+".aelight_impl",":id/pv", AccessibilityNodeInfo.ACTION_CLICK,false));
                steps.add(new Step(null,null, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,true,3000));
                steps.add(new Step(null,null, AccessibilityService.GLOBAL_ACTION_BACK,true,10000));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicCheckBoxId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getSendButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                getAnswer().setMessage(NekoMessage.OK+"请耐心等待");
            }else{
                getAnswer().setMessage(NekoMessage.DONT_SUPPORT);
            }
            return true;
        }
        if(getQuestion().getMessage().equals(SCREEN_SHOT)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                action = ACTION_SCREEN_SHOT;
                steps = new Vector<>();
                steps.add(new Step(null,null, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,true));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,3000));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicCheckBoxId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getSendButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
                getAnswer().setMessage(NekoMessage.OK);
            }else{
                getAnswer().setMessage(NekoMessage.DONT_SUPPORT);
            }
            return true;
        }

        if(getQuestion().getMessage().equals(CLOSE_AUTO)){
            NekoChatService.getInstance().autoAsk = false;
            getAnswer().setMessage("已关闭问候功能");
            return true;
        }

        if(getQuestion().getMessage().equals(OPEN_AUTO)){
            NekoChatService.getInstance().autoAsk = true;
            getAnswer().setMessage("已打开问候功能");
            return true;
        }
        return false;
    }

    public static final Pattern openaiIgnoreCase = Pattern.compile("(?i)openai");

    public Step getNextStep(){
        if(steps == null || steps.size() == 0){
            return null;
        }
        return steps.remove(0);
    }

    public void finish(){
        steps.clear();
        setSend(true);
        setWrite(true);
    }

    public void back(Step step){
        if(steps!=null){
            steps.add(0,step);
        }
    }

    public Message getQuestion() {
        return question;
    }

    public Message getAnswer() {
        return answer;
    }

    public boolean isWrite() {
        return write;
    }

    public void setWrite(boolean write) {
        this.write = write;
    }

    public boolean isSend() {
        return send;
    }

    public void setSend(boolean send) {
        this.send = send;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getPackageName() {
        return packageName;
    }
}
