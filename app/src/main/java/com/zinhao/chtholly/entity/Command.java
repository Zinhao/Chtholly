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
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.BuildConfig;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.db.AICharacterDao;
import com.zinhao.chtholly.session.RemoteChatApiSession;
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

    private final String packageName;

    private final Message question;
    private Message answer;

    private boolean write = false;
    private boolean send = false;

    private boolean outTime = false;
    protected boolean replyReady = false;

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
        if (handleAsk()) {
            return;
        }else {
            throwToChild();
        }
    }

    protected abstract boolean throwToChild();

    protected boolean handleAsk() {
        Log.i(TAG,"Command handleAsk:"+getQuestion().toString());
        if(getQuestion().isEnableCommand()){
            if(getQuestion().getMessage()== null){
                return false;
            }
            if(getQuestion().getMessage().isEmpty()){
                return false;
            }
            if(!getQuestion().getMessage().replace("@"+BotApp.getInstance().getBotName(),"").trim().startsWith("/")){
                return false;
            }
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
                replyReady = true;
                return true;
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(getClass().getSimpleName(), "Command invoke err: " + getClass().getSimpleName(), e);
                return true;
            }
        }else{
            return false;
        }

    }

    @HelpDoc(desc = "帮助")
    protected boolean help() {
        getAnswer().setMessage(getHelpStringBuilder().toString());
        return true;
    }
    @HelpDoc(desc = "运行信息")
    protected boolean runInfo(){
        getAnswer().setMessage(getRunInfo().toString());
        return true;
    }
    @HelpDoc(desc = "查看相册")
    protected boolean sendGallery() {
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
            getAnswer().setMessage("需要发送具体照片，请在按一下格式发送,如发送第1张和第5张($command 0 4),");
        }else{
            steps = NekoChatService.getInstance().getQqChatHandler().sendGalleryPicture(args);
            getAnswer().setMessage(NekoAskAble.OK);
        }
        return true;
    }

    @HelpDoc(desc = "[1 string arg]切换对话")
    private boolean switchToChat() {
        if(args.length == 1){
            if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
                steps = QQChatHandler.chooseShareTarget(args[0],NekoChatService.FUNC_SHARE_TEXT,NekoAskAble.COME_BACK);
                NekoChatService.getInstance().getQqChatHandler().setTargetChatTitle(args[0]);
            }
            getAnswer().setMessage(null);
            return true;
        }else {
            getAnswer().setMessage(NekoAskAble.HARD);
            return true;
        }
    }

    @HelpDoc(desc = "自动群打卡")
    protected boolean everyDayCheck() {
       if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
           steps = NekoChatService.getInstance().getQqChatHandler().everyDayCheck();
       }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }
    @HelpDoc(desc = "截图")
    protected boolean screenShot() {
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
    protected boolean sendNewestPic() {
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
    protected boolean takePhoto() {
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
    protected boolean recordVideo(){
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
            steps.add(new Step(packageName,":id"+ids[i], AccessibilityNodeInfo.ACTION_CLICK, Step.ActionType.normal,500));
        }
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }
    @HelpDoc(desc = "系统电量")
    protected boolean battery() {
        BatteryManager manager = (BatteryManager)BotApp.context().getSystemService(Context.BATTERY_SERVICE);
        int currentLevel = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        getAnswer().setMessage(String.format(Locale.CHINA,"%d%%,喵～",currentLevel));
        return true;
    }
    @HelpDoc(desc = "消息上下文")
    protected boolean printContext() {
        NekoSession nekoSession = BotApp.getInstance().getSession();
        if(nekoSession instanceof RemoteChatApiSession){
            String his = ((RemoteChatApiSession) nekoSession).getContextChat();
            getAnswer().setMessage(his);
        }else{
            getAnswer().setMessage(NekoAskAble.HARD);
        }
        return true;
    }

    @HelpDoc(desc = "切换模型")
    private boolean setModel() {
        NekoSession nekoSession = BotApp.getInstance().getSession();
        if(nekoSession instanceof RemoteChatApiSession){
            RemoteChatApiSession  remoteChatApiSession = (RemoteChatApiSession) nekoSession;
            if(args!=null && args.length>0){
                remoteChatApiSession.setModelIndex(Integer.parseInt(args[0]));
                getAnswer().setMessage(NekoAskAble.OK + " => "+  remoteChatApiSession.getCurrentModel().getStr());
            }else{
                List<RemoteChatApiSession.RemoteModel> models=remoteChatApiSession.getModelList();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("当前可用模型:\n");
                for (int i = 0;i<models.size();i++){
                    RemoteChatApiSession.RemoteModel model = models.get(i);
                    if(model.getStr().equals(remoteChatApiSession.getCurrentModel().getStr())){
                        stringBuilder.append("=> ");
                    }
                    stringBuilder.append(i).append(". ").append(model.getStr()).append("\n");
                }
                getAnswer().setMessage(stringBuilder.toString());
            }
        }else{
            getAnswer().setMessage("请先切换模式为 Gemini 或 Openai。");
        }
        return true;
    }

    @HelpDoc(desc = "AI人设")
    protected boolean printSoul() {
        String chara = BotApp.getInstance().getAiSoul();
        getAnswer().setMessage(String.format(Locale.CHINA,"这是%s的设定： %s。",BotApp.getInstance().getBotName(),chara));
        return true;
    }
    @HelpDoc(desc = "总结对话")
    protected boolean summarize() {
        NekoSession nekoSession = BotApp.getInstance().getSession();
        if(nekoSession instanceof  RemoteChatApiSession){
            RemoteChatApiSession remoteChatApiSession = (RemoteChatApiSession) nekoSession;
            int len = remoteChatApiSession.summarize();
            getAnswer().setMessage(String.format(Locale.CHINA,"%s 将为主人总结%d条对话。",BotApp.getInstance().getBotName(),len));
        }else{
            getAnswer().setMessage(NekoAskAble.DONT_SUPPORT);
        }
        return true;
    }
    @HelpDoc(desc = "[1 str arg]切换模式")
    private boolean switchMode() {
        if(geminiIgnoreCase.matcher(question.getMessage()).find()){
            getAnswer().setMessage(NekoAskAble.OK +" => gemini ai");
            BotApp.getInstance().setMode(GeminiSession.class);
        } else if(openaiIgnoreCase.matcher(question.getMessage()).find()){
            getAnswer().setMessage(NekoAskAble.OK+" => open ai");
            BotApp.getInstance().setMode(OpenAiSession.class);
        }else{
            getAnswer().setMessage(NekoAskAble.KOU_WAI +"=> neko");
            BotApp.getInstance().setMode(NekoSession.class);
        }
        return true;
    }
    @HelpDoc(desc = "视频通话")
    protected boolean videoCall() {
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
    protected boolean screenShare(){
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
                BotApp.getInstance().switchAISoul(newChara);
            }
        });
        getAnswer().setMessage(NekoAskAble.OK);
        return true;
    }
    @HelpDoc(desc = "like(\"switchSoul 1\")")
    private boolean switchSoul(){
        if(args == null || args.length == 0){
            BotApp.getInstance().loadAICharacter(new AICharacterDao.AICharacterGetAllListener() {
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
        BotApp.getInstance().loadAICharacter(new AICharacterDao.AICharacterGetAllListener() {
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
    @HelpDoc(desc = "清除上下文")
    private boolean clearContext(){
        NekoSession nekoSession = BotApp.getInstance().getSession();
        if(nekoSession instanceof RemoteChatApiSession){
            RemoteChatApiSession remoteChatApiSession = (RemoteChatApiSession) nekoSession;
            int clearLen = remoteChatApiSession.clearContext();
            getAnswer().setMessage(NekoAskAble.OK +"清除了"+String.valueOf(clearLen)+"条对话！");
        }else{
            getAnswer().setMessage(NekoAskAble.DONT_SUPPORT);
        }
        return true;
    }

    @HelpDoc(desc = "测试返回对话窗口")
    private boolean testBack(){
        return blockUserSpeak("丛雨");
    }

    public boolean initShareStepTo(String targetChatTitle,String functionName,String functionArg){
        if(QQChatHandler.PACKAGE_NAME.equals(packageName)){
            steps = QQChatHandler.chooseShareTarget(targetChatTitle,functionName,functionArg);
            getAnswer().setMessage(NekoAskAble.OK);
        }else{
            getAnswer().setMessage(NekoAskAble.DONT_SUPPORT);
        }
        return true;
    }

    public boolean blockUserSpeak(String userName) {
        if(packageName.equals(QQChatHandler.PACKAGE_NAME)){
            steps = NekoChatService.getInstance().getQqChatHandler().blockSay(userName);
            getAnswer().setMessage(NekoAskAble.BLOCK);
        }
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
            gb.addStroke(new GestureDescription.StrokeDescription(path,100,150));
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
        stringBuilder.append("Mode:").append(BotApp.getInstance().getMode().getSimpleName()).append("\n");
        stringBuilder.append("BaseUrl:").append(BotApp.getInstance().getChatUrl()).append("\n");
        stringBuilder.append("AdminName:").append(BotApp.getInstance().getAdminName()).append("\n");
        stringBuilder.append("SoulName:").append(BotApp.getInstance().getCurrentCharacter().getName()).append("\n");
        String apiKey = BotApp.getInstance().getApiKey();
        String apiKeySub = apiKey.substring(Math.max(apiKey.length()-5,0));
        stringBuilder.append("ApiKey:").append("sk-***********").append(apiKeySub).append("\n");
        if(NekoChatService.getInstance()!=null){

        }

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
        if(steps!=null){
            steps.clear();
        }
    }

    public void finishTextReply(){
        setSend(true);
        setWrite(true);
    }

    public void backStepList(Step step){
        if(steps!=null){
            steps.add(0,step);
        }
    }

    public boolean isReplyReady() {
        return replyReady;
    }

    public void setReplyReady(boolean replyReady) {
        this.replyReady = replyReady;
    }

    public Message getQuestion() {
        return question;
    }

    public Message getAnswer() {
        if(answer == null){
            answer = new Message(null,null,System.currentTimeMillis());
        }
        return answer;
    }

    public String questionWithSpeaker(){
        if(getQuestion().getSpeaker() == null || getQuestion().getSpeaker().isEmpty()){
            return getQuestion().getMessage();
        }
        if(getQuestion().tag!=null){
            return getQuestion().getSpeaker() +"("+getQuestion().tag+")" + "say:" + getQuestion().getMessage();
        }
        return getQuestion().getSpeaker() + "say:" + getQuestion().getMessage();
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
