package com.zinhao.chtholly.entity;

import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.BuildConfig;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.db.AICharacterDao;
import com.zinhao.chtholly.session.ChatSession;
import com.zinhao.chtholly.session.GeminiSession;
import com.zinhao.chtholly.session.NekoSession;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.ChatPageViewIds;
import com.zinhao.chtholly.utils.QQChatHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/** @noinspection ALL*/
public abstract class Command{
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface HelpDoc {
        String desc();      // 格式要求，如 "yyyy-MM-dd"
    }
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

    // 对外暴露的统一入口（不可重写）
    public final void handle() {
        Log.i(TAG,"handle:"+getQuestion().getMessage());
        if (handleAsk()) {
            Log.i(TAG,"handleAsk true:"+getQuestion().getMessage());
            return;
        }else {
            throwToChild();
        }
    }

    protected abstract boolean throwToChild();

    protected boolean handleAsk() {
        Log.i(TAG,"Command handleAsk");
        if(getQuestion().getMessage().startsWith("/")){
            Log.i(TAG,"Command invoke");
            try {
                String[] methodAndArgs = parseArgs();
                String MethodName = methodAndArgs[0];
                if(methodAndArgs.length>1){
                    args = new String[methodAndArgs.length-1];
                    System.arraycopy(methodAndArgs, 1, args, 0, methodAndArgs.length - 1);
                }
                Method method = Command.class.getDeclaredMethod(MethodName.replace('/',' ').trim());
                method.invoke(this);
                return true;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), "Command invoke err: " + getClass().getSimpleName(), e);
                return true;
            }
        }else{
            return false;
        }

    }

    @HelpDoc(desc = "开启早中晚定时问侯")
    private boolean openAutoAction() {
        NekoChatService.getInstance().autoAsk = true;
        getAnswer().setMessage("已打开问候功能");
        return true;
    }
    @HelpDoc(desc = "关闭早中晚定时问侯")
    private boolean closeAutoAction() {
        NekoChatService.getInstance().autoAsk = false;
        getAnswer().setMessage("已关闭问候功能");
        return true;
    }

    @HelpDoc(desc = "帮助")
    private boolean help() {
        getAnswer().setMessage(getHelpStringBuilder().toString());
        return true;
    }
    @HelpDoc(desc = "运行信息")
    private boolean runInfo(){
        getAnswer().setMessage(getRunInfo().toString());
        return true;
    }
    @HelpDoc(desc = "查看相册")
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

    @HelpDoc(desc = "[1 int arg]切换对话")
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

    @HelpDoc(desc = "自动群打卡")
    private boolean everyDayCheck() {
       if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
           steps = NekoChatService.getInstance().getQqChatHandler().everyDayCheck();
       }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }
    @HelpDoc(desc = "截图")
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

    @HelpDoc(desc = "发送最新图片")
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
    @HelpDoc(desc = "拍照")
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
    @HelpDoc(desc = "录视频")
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
    @HelpDoc(desc = "点击界面元素")
    private boolean clickViewId() {
        steps = new Vector<>();
        String[] ids = getQuestion().getMessage().split(" ");
        for (int i = 1; i < ids.length; i++) {
            steps.add(new Step(packageName,":id"+ids[i], AccessibilityNodeInfo.ACTION_CLICK,false,500));
        }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }
    @HelpDoc(desc = "系统电量")
    private boolean battery() {
        BatteryManager manager = (BatteryManager)BotApp.context().getSystemService(Context.BATTERY_SERVICE);
        int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        getAnswer().setMessage(String.format(Locale.CHINA,"%d%%,喵～",currentLevel));
        return true;
    }
    @HelpDoc(desc = "消息上下文")
    private boolean printMessage() {
        ChatSession chatSession = NekoChatService.getInstance().getSession();
        String his = chatSession.getContextChat();
        getAnswer().setMessage(his);
        return true;
    }

    @HelpDoc(desc = "打印AI设定")
    private boolean printCharacter() {
        ChatSession chatSession = NekoChatService.getInstance().getSession();
        String chara = chatSession.getChara();
        getAnswer().setMessage(String.format(Locale.CHINA,"这是%s的设定： %s。",BotApp.getInstance().getBotName(),chara));
        return true;
    }
    @HelpDoc(desc = "总结对话")
    private boolean summarizeChat() {
        ChatSession chatSession = NekoChatService.getInstance().getSession();
        int len = chatSession.summarize();
        getAnswer().setMessage(String.format(Locale.CHINA,"%s 将为主人总结%d条对话。",BotApp.getInstance().getBotName(),len));
        return true;
    }
    @HelpDoc(desc = "[1 str arg]切换模式")
    private boolean switchBotOrAI() {
        if(geminiIgnoreCase.matcher(question.getMessage()).find()){
            getAnswer().setMessage(NekoAskAble.OK +" => gemini ai");
            NekoChatService.mode = GeminiSession.class;
        } else if(openaiIgnoreCase.matcher(question.getMessage()).find()){
            getAnswer().setMessage(NekoAskAble.OK+" => open ai");
            NekoChatService.mode = OpenAiSession.class;
        }else{
            getAnswer().setMessage(NekoAskAble.KOU_WAI +"=> neko");
            NekoChatService.mode = NekoSession.class;
        }
        return true;
    }
    @HelpDoc(desc = "视频通话")
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
    @HelpDoc(desc = "分享屏幕")
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
    @HelpDoc(desc = "like(\"newSoul name souldesc\" )")
    private boolean newSoul(){
        if(args.length<2){
            return true;
        }
        final AICharacter newChara = new AICharacter(args[0],args[1]);
        BotApp.getInstance().insert(newChara, new Runnable() {
            @Override
            public void run() {
                BotApp.getInstance().setCharacterId(newChara.getId());
                BotApp.getInstance().setCurrentCharacter(newChara);

                SharedPreferences.Editor editor = BotApp.getInstance().getSharedPreferences().edit();
                editor.putLong(BotApp.CONFIG_CURRENT_CHARACTER_ID,newChara.getId());
                editor.apply();

                ChatSession session = NekoChatService.getInstance().getSession();
                if(session!=null){
                    session.setChara(newChara.getDesc());
                }
            }
        });
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }
    @HelpDoc(desc = "like(\"switchSoul 1\")")
    private boolean switchSoul(){
        if(args == null || args.length == 0){
            BotApp.getInstance().select(new AICharacterDao.AICharacterGetAllListener() {
                @Override
                public void onSuccess(List<AICharacter> result) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (AICharacter character:result){
                        stringBuilder.append(character.getId()).append('.').append(character.getName()).append('\n');
                    }
                    getAnswer().setMessage(stringBuilder.toString());
                }
            });
            return true;
        }
        BotApp.getInstance().select(new AICharacterDao.AICharacterGetAllListener() {
            @Override
            public void onSuccess(List<AICharacter> result) {
                StringBuilder stringBuilder = new StringBuilder();
                for (AICharacter character:result){
                    if(args[0].equals(String.valueOf(character.getId()))){
                        BotApp.getInstance().switchAISoul(character);
                        break;
                    }
                }
                stringBuilder.append('\n');
                getAnswer().setMessage(stringBuilder.toString() + NekoAskAble.OK);
            }
        });
        return true;
    }

    private String[] parseArgs() {
        String input = getQuestion().getMessage().trim();
        if (input.isEmpty()) {
            return new String[0];
        }

        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (!inQuotes && (c == '"' || c == '\'')) {
                // 进入引号
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                // 退出引号
                inQuotes = false;
                quoteChar = 0;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                // 参数分隔（不在引号内）
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0);
                }
            } else {
                // 普通字符
                currentArg.append(c);
            }
        }

        // 添加最后一个参数
        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }

        return args.toArray(new String[0]);
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
        stringBuilder.append("Chtholly Ver").append(BuildConfig.VERSION_NAME).append('\n');
        // 使用 getDeclaredMethods() 获取所有声明的方法（含 private）
        Map<String, String> methodsMap = getMethodDescMap(Command.class);
        for (String methodKey :methodsMap.keySet()){
            stringBuilder.append('/')
                    .append(methodKey)
                    .append(' ')
                    .append(methodsMap.get(methodKey))
                    .append('\n');
        }
        return stringBuilder;
    }

    private static StringBuilder getRunInfo(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ver:").append(BuildConfig.VERSION_NAME).append("\n");
        stringBuilder.append("Mode:").append(NekoChatService.mode.getSimpleName()).append("\n");
        stringBuilder.append("BaseUrl:").append(BotApp.getInstance().getChatUrl()).append("\n");
        stringBuilder.append("AdminName:").append(BotApp.getInstance().getAdminName()).append("\n");
        stringBuilder.append("SoulName:").append(BotApp.getInstance().getCurrentCharacter().getName()).append("\n");
        String apiKey = BotApp.getInstance().apiKey;
        String apiKeySub = apiKey.substring(apiKey.length()-5);
        stringBuilder.append("ApiKey:").append("sk-***********").append(apiKeySub).append("\n");
        stringBuilder.append("CharacterId:").append(BotApp.getInstance().getCharacterId()).append("\n");
        return  stringBuilder;
    }

    /**
     * 获取方法及其 desc 的映射
     */
    public static Map<String, String> getMethodDescMap(Class<?> clazz) {
        Map<String, String> map = new HashMap<>();

        for (Method method : clazz.getDeclaredMethods()) {
            HelpDoc helpDoc = method.getAnnotation(HelpDoc.class);
            if (helpDoc != null) {
                map.put(method.getName(), helpDoc.desc());
            }
        }
        return map;
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
        if(answer == null){
            answer = new Message("base",null,System.currentTimeMillis());
        }
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
