package com.zinhao.chtholly.entity;

import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.CallSuper;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.session.ChatSession;
import com.zinhao.chtholly.session.GeminiSession;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.ChatPageViewIds;
import com.zinhao.chtholly.utils.QQChatHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/** @noinspection ALL*/
public class Command implements AskAble {
    private static final String TAG = "Command";
    private static final String SWITCH_COMMAND_EN = "/switchBotOrAI";

    private static final String SEVER_BATTERY = "/battery";
    private static final String COMMAND_LIST = "/help";
    private static final String FIRST_PIC = "/sendNewestPic";
    private static final String CLICK_ID = "/clickViewId";
    private static final String TAKE_PHOTO = "/takePhoto";
    private static final String SCREEN_SHOT = "/screenShot";
    private static final String SUMMARIZE_CHAT = "/summarizeChat";
    private static final String PRINT_CHARA = "/printCharacter";
    private static final String PRINT_CHATS = "/printMessage";
    private static final String CLOSE_AUTO = "/closeAutoAction";
    private static final String OPEN_AUTO = "/openAutoAction";
    private static final String VIDEO_CALL = "/videoCall";
    private static final String EVERY_DAY_CHECK = "/everyDayCheck";
    private static final String SWITCH_CHATS = "/switchChat";
    private static final String SEND_GALLERY = "/sendGallery";
    private static final String SHARE_SCREEN = "/shareScreen";
    private static final String RECORD_VIDEO = "/recordVideo";

    private final String packageName;
    private final Message question;
    private Message answer;
    private boolean write = false;
    private boolean send = false;
    private boolean outTime = false;
    private String[] args;

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
        if(answer == null){
            answer = new Message(BotApp.getInstance().getBotName(),null,System.currentTimeMillis());
        }
        if(!getQuestion().getMessage().startsWith("/")){
            return false;
        }
        try {
            String[] methodAndArgs = args();
            String MethodName = methodAndArgs[0];
            if(methodAndArgs.length>1){
                args = new String[methodAndArgs.length-1];
                System.arraycopy(methodAndArgs, 1, args, 0, methodAndArgs.length - 1);
            }
            Method method = Command.class.getDeclaredMethod(MethodName.replace('/',' ').trim());
            return (boolean) method.invoke(this);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Log.e(getClass().getSimpleName(), "ask: ", e);
            return true;
        }
    }

    private boolean openAutoAction() {
        NekoChatService.getInstance().autoAsk = true;
        getAnswer().setMessage("已打开问候功能");
        return true;
    }

    private boolean closeAutoAction() {
        NekoChatService.getInstance().autoAsk = false;
        getAnswer().setMessage("已关闭问候功能");
        return true;
    }

    private boolean help() {
        StringBuilder stringBuilder = getHelpStringBuilder();
        getAnswer().setMessage(stringBuilder.toString());
        return true;
    }

    private boolean sendGallery() {
        //todo 仅适配QQ
        // /c /gnt /p2 /lmy
        ChatPageViewIds cpvi = NekoChatService.getInstance().currentChatPageIds(getPackageName());
        if(cpvi == null){
            Log.e(TAG, "sendGallery: ", new RuntimeException("ChatPageViewIds is null"));
            return false;
        }
        if(args == null || args.length == 0){
            // todo 发送最近照片截图，尚未测试
            steps = NekoChatService.getInstance().getQqChatHandler().sendGalleryPreview();
            getAnswer().setMessage("需要发送具体照片，请在按一下格式发送,如发送第1张和第5张("+SEND_GALLERY+" 0 4),");
        }else{
            steps = NekoChatService.getInstance().getQqChatHandler().sendGalleryPicture(args);
            getAnswer().setMessage(NekoAskAble.OK);
        }
        return true;
    }

    private boolean switchChat() {
        ChatPageViewIds cpvi = NekoChatService.getInstance().currentChatPageIds(getPackageName());
        if(cpvi == null){
            Log.e(TAG, "switchChat: ", new RuntimeException("ChatPageViewIds is null"));
            return false;
        }
        if(getQuestion().getMessage().contains(" ")){
            String[] ids = getQuestion().getMessage().split(" ");
            int position = 0;
            if(ids.length == 2){
                try{
                    position = Integer.parseInt(ids[1]);
                }catch (Exception e){
                    getAnswer().setMessage(NekoAskAble.HARD);
                    return true;
                }
                NekoChatService.getInstance().setChatsIndex(position);
                if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
                    steps = NekoChatService.getInstance().getQqChatHandler().switchChatNow(position);
                }
                getAnswer().setMessage(NekoAskAble.COME_BACK);
                return true;
            }
        }else {
            //截图聊天列表并发送截图
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
                    steps = NekoChatService.getInstance().getQqChatHandler().switchChatQuery();
                }
                getAnswer().setMessage("看好需要切换的聊天的位置，使用("+SWITCH_CHATS+" 0)切换至第一个聊天，数字表示聊天的索引。");
                return true;
            }else {
                getAnswer().setMessage(NekoAskAble.HARD);
                return true;
            }
        }
        return true;
    }

    private boolean everyDayCheck() {
       if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
           steps = NekoChatService.getInstance().getQqChatHandler().everyDayCheck();
       }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }

    private boolean screenShot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
                steps = NekoChatService.getInstance().getQqChatHandler().screenShot();
            }
            getAnswer().setMessage(NekoAskAble.OK);
        }else{
            getAnswer().setMessage(NekoAskAble.DONT_SUPPORT);
        }
        return true;
    }

    private boolean sendNewestPic() {
        // 发送最新一张图 /gnt /qhp /fun_btn /gnt  三星
        // 发送最新一张图 /gnt /qhq /send_btn /gnt  pixel3
        // 发送最新一张图 /gnt /dpo /send_btn /gnt  ONE PLUS
        if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
            steps = NekoChatService.getInstance().getQqChatHandler().sendNewestPic();
        }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }

    private boolean takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
                steps = NekoChatService.getInstance().getQqChatHandler().takePhoto();
            }
            getAnswer().setMessage(NekoAskAble.OK+"请耐心等待");
        }else{
            getAnswer().setMessage(NekoAskAble.DONT_SUPPORT);
        }
        return true;
    }

    private boolean recordVideo(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if(packageName.equals(QQChatHandler.PACKAGE_NAME)){
                steps = NekoChatService.getInstance().getQqChatHandler().recordVideo();
                getAnswer().setMessage(NekoAskAble.OK+"请耐心等待");
            }
        }else{
            getAnswer().setMessage(NekoAskAble.DONT_SUPPORT);
        }
        return true;
    }

    private boolean clickViewId() {
        steps = new Vector<>();
        String[] ids = getQuestion().getMessage().split(" ");
        for (int i = 1; i < ids.length; i++) {
            steps.add(new Step(packageName,":id"+ids[i], AccessibilityNodeInfo.ACTION_CLICK,false,500));
        }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }

    private boolean battery() {
        BatteryManager manager = (BatteryManager)BotApp.context().getSystemService(Context.BATTERY_SERVICE);
        int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        getAnswer().setMessage(String.format(Locale.CHINA,"%d%%,喵～",currentLevel));
        return true;
    }

    private boolean printMessage() {
        ChatSession chatSession = NekoChatService.getInstance().getSession();
        String his = chatSession.getContextChat();
        getAnswer().setMessage(his);
        return true;
    }

    private boolean printCharacter() {
        ChatSession chatSession = NekoChatService.getInstance().getSession();
        String chara = chatSession.getChara();
        getAnswer().setMessage(String.format(Locale.CHINA,"这是%s的设定： %s。",BotApp.getInstance().getBotName(),chara));
        return true;
    }

    private boolean summarizeChat() {
        ChatSession chatSession = NekoChatService.getInstance().getSession();
        int len = chatSession.summarize();
        getAnswer().setMessage(String.format(Locale.CHINA,"%s 将为主人总结%d条对话。",BotApp.getInstance().getBotName(),len));
        return true;
    }

    private boolean switchBotOrAI() {
        if(geminiIgnoreCase.matcher(question.getMessage()).find()){
            answer.setMessage(NekoAskAble.OK +" => gemini ai");
            NekoChatService.mode = GeminiSession.class;
        } else if(openaiIgnoreCase.matcher(question.getMessage()).find()){
            answer.setMessage(NekoAskAble.OK+" => open ai");
            NekoChatService.mode = OpenAiSession.class;
        }else{
            answer.setMessage(NekoAskAble.KOU_WAI +"=> neko");
            NekoChatService.mode = NekoSession.class;
        }
        return true;
    }

    private boolean videoCall() {
        // :id/gny [:id/icon_viewPager 1->2] :id/bbt
        boolean mainCamera;
        if(args == null){
            mainCamera = true;
        }else{
            if(args.length == 0){
                mainCamera = true;
            }else{
                mainCamera = args[0].equals("false") || args[0].equals('0');
            }
        }

        if(packageName.equals(QQChatHandler.PACKAGE_NAME)){
            steps = NekoChatService.getInstance().getQqChatHandler().videoCall(mainCamera);
        }

        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }

    private boolean shareScreen(){
        steps = new Vector<>();
        //将步骤委托给 QQChatHandler
        if(packageName.equals(QQChatHandler.PACKAGE_NAME)){
            steps = NekoChatService.getInstance().getQqChatHandler().shareScreen();
            getAnswer().setMessage(NekoAskAble.OK);
            return true;
        }
        getAnswer().setMessage(NekoAskAble.HARD);
        return true;

    }

    private String[] args(){
        if(getQuestion().getMessage().contains(" ")){
            return getQuestion().getMessage().split(" ");
        }else {
            return new String[]{getQuestion().getMessage().trim()};
        }
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

    public static final Step.NeedGesture SWIPE_DOWN_FAST = new Step.NeedGesture() {
        @Override
        public GestureDescription onGesture(AccessibilityNodeInfo targetView) {
            GestureDescription.Builder gb = new GestureDescription.Builder();
            Rect r = new Rect();
            targetView.getBoundsInScreen(r);
            Path path = new Path();
            PointF p = new PointF((r.left+r.right)/2f,r.bottom);
            path.moveTo(p.x,p.y);
            path.lineTo(p.x,Math.max(0,r.centerY()));
            GestureDescription.StrokeDescription gestureDescription = new GestureDescription.StrokeDescription(path,0,50,false);

//            GestureDescription.StrokeDescription
            gb.addStroke(gestureDescription);
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

    public static final Step.NeedGesture PRESS_10S = new Step.NeedGesture() {
        @Override
        public GestureDescription onGesture(AccessibilityNodeInfo targetView) {
            GestureDescription.Builder gb = new GestureDescription.Builder();
            Rect r = new Rect();
            targetView.getBoundsInScreen(r);
            Path path = new Path();
            PointF p = new PointF((r.left+r.right)/2f,(r.top+r.bottom)/2f);
            path.moveTo(p.x,p.y);
            path.lineTo(p.x,p.y);
            gb.addStroke(new GestureDescription.StrokeDescription(path,0,10000));
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
        stringBuilder.append(VIDEO_CALL).append(' ').append("视频全群通话").append('\n');
        stringBuilder.append(EVERY_DAY_CHECK).append(' ').append("会打卡，并无什么用处").append('\n');
        stringBuilder.append(SWITCH_CHATS).append(' ').append("切换聊天群").append('\n');
        stringBuilder.append(SEND_GALLERY).append(' ').append("发送相册截图").append('\n');
        stringBuilder.append(SHARE_SCREEN).append(' ').append("发起分享屏幕").append('\n');
        Method[] methods = Command.class.getMethods();
        for (Method method : methods) {
            // 检查方法的修饰符是否为私有
            if (Modifier.isPrivate(method.getModifiers())) {
                stringBuilder.append('/').append(method.getName()).append(' ').append("").append('\n');
            }
        }
        return stringBuilder;
    }

    public static final Pattern openaiIgnoreCase = Pattern.compile("(?i)openai");
    public static final Pattern geminiIgnoreCase = Pattern.compile("(?i)gemini");

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
