package com.zinhao.chtholly.entity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.CallSuper;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.ChatPageViewIds;
import com.zinhao.chtholly.utils.QQUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class Command implements AskAble {
    private static final String SWITCH_COMMAND_CH = "/切换到";
    private static final String SWITCH_COMMAND_EN = "/switch to";

    private static final String SEVER_BATTERY = "/电量";
    private static final String COMMAND_LIST = "/help";
    private static final String FIRST_PIC = "/send_first_pic";
    private static final String CLICK_ID = "/c";
    private static final String TAKE_PHOTO = "/拍照";
    private static final String SCREEN_SHOT = "/截屏";
    private static final String SUMMARIZE_CHAT = "/sc";
    private static final String PRINT_CHARA = "/性格描述";
    private static final String PRINT_CHATS = "/历史对话";
    private static final String CLOSE_AUTO = "/关闭问候功能";
    private static final String OPEN_AUTO = "/打开问候功能";
    private static final String VIDEO_CALL_F = "/前置视频通话";
    private static final String VIDEO_CALL_M = "/后置视频通话";
    private static final String EVERY_DAY_CHECK = "/打卡";
    private static final String SWITCH_CHATS = "/切换聊天";
    private static final String SEND_GALLERY = "/发送相册图片";

    private final String packageName;
    private final Message question;
    private Message answer;
    private boolean write = false;
    private boolean send = false;
    private boolean outTime = false;

    private List<Step> steps;

    public Command(String packageName, Message question) {
        this.packageName = packageName;
        this.question = question;
    }

    public Command(String packageName, Message question, List<Step> steps) {
        this.packageName = packageName;
        this.question = question;
        this.steps = steps;
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
        return steps == null || steps.isEmpty();
    }

    @CallSuper
    @Override
    public boolean ask() {
        ChatPageViewIds cpvi = NekoChatService.chatPageViewIds;
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
            steps = new Vector<>();
            String[] ids = getQuestion().getMessage().split(" ");
            for (int i = 1; i < ids.length; i++) {
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id"+ids[i], AccessibilityNodeInfo.ACTION_CLICK,false,500));
            }
            getAnswer().setMessage(NekoMessage.OK);
            return true;
        }

        if(getQuestion().getMessage().equals(FIRST_PIC)){
            // 发送最新一张图 /gnt /qhp /fun_btn /gnt  三星
            // 发送最新一张图 /gnt /qhq /send_btn /gnt  pixel3
            // 发送最新一张图 /gnt /dpo /send_btn /gnt  ONE PLUS
            steps = new Vector<>();
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, cpvi.getFirstPicCheckBoxViewId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, cpvi.getSendBtnViewId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,300));
            getAnswer().setMessage(NekoMessage.OK);
            return true;
        }
        if(getQuestion().getMessage().equals(TAKE_PHOTO)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                steps = new Vector<>();
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/go6", AccessibilityNodeInfo.ACTION_CLICK,false));
                //打开闪光灯
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME+".aelight_impl",":id/py", AccessibilityNodeInfo.ACTION_CLICK,false,500));
                // 切换前置
//                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME+".aelight_impl",":id/pv", AccessibilityNodeInfo.ACTION_CLICK,false,500));
                // 拍照
                Step gestureStep = new Step(QQUtils.QQ_PACKAGE_NAME+".aelight_impl",":id/a74",AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,false,500);
                gestureStep.setNeedGesture(CLICK);
                steps.add(gestureStep);
                //发送
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME+".aelight_impl",":id/ut", AccessibilityNodeInfo.ACTION_CLICK,false,1500));
                getAnswer().setMessage(NekoMessage.OK+"请耐心等待");
            }else{
                getAnswer().setMessage(NekoMessage.DONT_SUPPORT);
            }
            return true;
        }
        if(getQuestion().getMessage().equals(SCREEN_SHOT)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                steps = new Vector<>();
                steps.add(new Step(null,null, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,true));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, cpvi.getFirstPicCheckBoxViewId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, cpvi.getSendBtnViewId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                getAnswer().setMessage(NekoMessage.OK);
            }else{
                getAnswer().setMessage(NekoMessage.DONT_SUPPORT);
            }
            return true;
        }

        if(getQuestion().getMessage().equals(VIDEO_CALL_F)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //todo VIDEO CALL F
                // :id/gny [:id/icon_viewPager 1->2] :id/bbt
                steps = new Vector<>();
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/gny", AccessibilityNodeInfo.ACTION_CLICK,false));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/icon_viewPager", AccessibilityNodeInfo.ACTION_CLICK,false,500,true,new int[]{0,1}));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, ":id/bbt", AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, ":id/g76", AccessibilityNodeInfo.ACTION_CLICK,false,5500));
                getAnswer().setMessage(NekoMessage.OK);
            }
            return true;
        }

        if(getQuestion().getMessage().equals(VIDEO_CALL_M)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                //todo VIDEO CALL M
                steps = new Vector<>();
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/gny", AccessibilityNodeInfo.ACTION_CLICK,false));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/icon_viewPager", AccessibilityNodeInfo.ACTION_CLICK,false,500,true,new int[]{0,1}));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, ":id/bbt", AccessibilityNodeInfo.ACTION_CLICK,false,300));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, ":id/gd7", AccessibilityNodeInfo.ACTION_CLICK,false,5500));
                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, ":id/g76", AccessibilityNodeInfo.ACTION_CLICK,false,500));
                getAnswer().setMessage(NekoMessage.OK);
            }
            return true;
        }

        if(getQuestion().getMessage().equals(EVERY_DAY_CHECK)){
            steps = new Vector<>();
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/qn4",AccessibilityNodeInfo.ACTION_CLICK,false));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/nfz",AccessibilityNodeInfo.ACTION_CLICK,false,500));
            steps.add(new Step(null,null,AccessibilityService.GLOBAL_ACTION_BACK,true,500));
            getAnswer().setMessage(NekoMessage.OK);
            return true;
        }

        if(getQuestion().getMessage().startsWith(SWITCH_CHATS)){
            if(getQuestion().getMessage().contains(" ")){
                String[] ids = getQuestion().getMessage().split(" ");
                int position = 0;
                if(ids.length == 2){
                    try{
                        position = Integer.parseInt(ids[1]);
                    }catch (Exception e){
                        getAnswer().setMessage(NekoMessage.HARD);
                        return true;
                    }
                    NekoChatService.getInstance().setChatsIndex(position);
                    steps = new Vector<>();
                    steps.add(new Step(null,null,AccessibilityService.GLOBAL_ACTION_BACK,true));
                    steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/recent_chat_list", AccessibilityNodeInfo.ACTION_CLICK,false,1500,true,new int[]{position+1}));
                    getAnswer().setMessage(NekoMessage.COME_BACK);
                }
            }else {
                //截图聊天列表并发送截图
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    int position = NekoChatService.getInstance().getChatsIndex();
                    steps = new Vector<>();
                    steps.add(new Step(null, null, AccessibilityService.GLOBAL_ACTION_BACK, true));
                    steps.add(new Step(null, null, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT, true,500));
                    steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/recent_chat_list", AccessibilityNodeInfo.ACTION_CLICK,false,500,true,new int[]{position+1}));
                    steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                    steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, cpvi.getFirstPicCheckBoxViewId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                    steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, cpvi.getSendBtnViewId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                    steps.add(new Step(QQUtils.QQ_PACKAGE_NAME, QQUtils.getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,false,500));
                    getAnswer().setMessage("看好需要切换的聊天的位置，使用“/切换聊天 0”切换至第一个聊天，数字表示聊天的索引。");
                }else {
                    getAnswer().setMessage(NekoMessage.HARD);
                    return true;
                }
            }
            return true;
        }

        if(getQuestion().getMessage().startsWith(SEND_GALLERY)){
//           /c /gnt /p2 /lmy
            String[] ids = getQuestion().getMessage().split(" ");
            int position = 0;
            if(ids.length == 2){
                try{
                    position = Integer.parseInt(ids[1]);
                }catch (Exception e){
                    return true;
                }
            }
            steps = new Vector<>();
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/gnt",AccessibilityNodeInfo.ACTION_CLICK,false));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/p2",AccessibilityNodeInfo.ACTION_CLICK,false,500));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/photo_list_gv",AccessibilityNodeInfo.ACTION_CLICK,false,500,true,new int[]{position,1}));
//            Step gestureStep = new Step(QQUtils.QQ_PACKAGE_NAME,":id/photo_list_gv",AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,false,500);
//            gestureStep.setNeedGesture(SCROLL_DOWN);
//            steps.add(gestureStep);
//            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/photo_list_gv",AccessibilityNodeInfo.ACTION_CLICK,false,1500,true,new int[]{position,1}));
            steps.add(new Step(QQUtils.QQ_PACKAGE_NAME,":id/send_btn",AccessibilityNodeInfo.ACTION_CLICK,false,500));
            getAnswer().setMessage(NekoMessage.OK);
            return true;
        }

        if(getQuestion().getMessage().equals(COMMAND_LIST)){
            StringBuilder stringBuilder = getHelpStringBuilder();
            getAnswer().setMessage(stringBuilder.toString());
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

    public static final Step.NeedGesture SCROLL_DOWN = new Step.NeedGesture() {
        @Override
        public GestureDescription onGesture(AccessibilityNodeInfo targetView) {
            GestureDescription.Builder gb = new GestureDescription.Builder();
            Rect r = new Rect();
            targetView.getBoundsInScreen(r);
            Path path = new Path();
            PointF p = new PointF((r.left+r.right)/2f,(r.top+r.bottom)/2f);
            path.moveTo(p.x,p.y);
            path.lineTo(p.x,0);
            gb.addStroke(new GestureDescription.StrokeDescription(path,0,400));
            return  gb.build();
        }
    };

    public static final Step.NeedGesture CLICK = new Step.NeedGesture() {
        @Override
        public GestureDescription onGesture(AccessibilityNodeInfo targetView) {
            GestureDescription.Builder gb = new GestureDescription.Builder();
            Rect r = new Rect();
            targetView.getBoundsInScreen(r);
            Path path = new Path();
            PointF p = new PointF((r.left+r.right)/2f,(r.top+r.bottom)/2f);
            path.moveTo(p.x,p.y);
            path.lineTo(p.x,p.y);
            gb.addStroke(new GestureDescription.StrokeDescription(path,0,50));
            return  gb.build();
        }
    };

    public static final Step.NeedGesture DOUBLE_CLICK = new Step.NeedGesture() {
        @Override
        public GestureDescription onGesture(AccessibilityNodeInfo targetView) {
            GestureDescription.Builder gb = new GestureDescription.Builder();
            Rect r = new Rect();
            targetView.getBoundsInScreen(r);
            Path path = new Path();
            PointF p = new PointF((r.left+r.right)/2f,(r.top+r.bottom)/2f);
            path.moveTo(p.x,p.y);
            path.lineTo(p.x,p.y);
            gb.addStroke(new GestureDescription.StrokeDescription(path,0,50));
            gb.addStroke(new GestureDescription.StrokeDescription(path,75,50));
            return  gb.build();
        }
    };

    @NotNull
    public static StringBuilder getHelpStringBuilder() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(COMMAND_LIST).append(' ').append("查看帮助").append('\n');
        stringBuilder.append(SWITCH_COMMAND_EN).append(' ').append("切换模式，openai或者其他(只会喵喵叫)").append('\n');
        stringBuilder.append(SWITCH_COMMAND_CH).append(' ').append("同上").append('\n');
        stringBuilder.append(SEVER_BATTERY).append(' ').append("宿主手机电量").append('\n');
        stringBuilder.append(FIRST_PIC).append(' ').append("发送最新得一张图片").append('\n');
        stringBuilder.append(CLICK_ID).append(' ').append("点击界面元素，开发用").append('\n');
        stringBuilder.append(TAKE_PHOTO).append(' ').append("拍一张照片并发送").append('\n');
        stringBuilder.append(SCREEN_SHOT).append(' ').append("截图并发送").append('\n');
        stringBuilder.append(SUMMARIZE_CHAT).append(' ').append("开始总结对话，一般不用手动调用").append('\n');
        stringBuilder.append(PRINT_CHARA).append(' ').append("AI得性格描述").append('\n');
        stringBuilder.append(PRINT_CHATS).append(' ').append("消息上下文").append('\n');
        stringBuilder.append(CLOSE_AUTO).append(' ').append("开启早中晚定时问侯").append('\n');
        stringBuilder.append(OPEN_AUTO).append(' ').append("关闭早中晚定时问侯").append('\n');
        stringBuilder.append(VIDEO_CALL_F).append(' ').append("开启前置视频全群通话").append('\n');
        stringBuilder.append(VIDEO_CALL_M).append(' ').append("开启后置视频全群通话").append('\n');
        stringBuilder.append(EVERY_DAY_CHECK).append(' ').append("会打卡，并无什么用处").append('\n');
        stringBuilder.append(SWITCH_CHATS).append(' ').append("切换聊天群").append('\n');
        stringBuilder.append(SEND_GALLERY).append(' ').append("切换聊天").append('\n');
        return stringBuilder;
    }

    public static final Pattern openaiIgnoreCase = Pattern.compile("(?i)openai");

    public Step getNextStep(){
        if(steps == null || steps.isEmpty()){
            return null;
        }
        return steps.remove(0);
    }

    public void finishStepAction(){
        steps.clear();
    }

    public void finishTextReply(){
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

    public String getPackageName() {
        return packageName;
    }

    public boolean haveAction(){
        return steps!=null && !steps.isEmpty();
    }
}
