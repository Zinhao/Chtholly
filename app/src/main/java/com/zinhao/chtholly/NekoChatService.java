package com.zinhao.chtholly;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.zinhao.chtholly.entity.*;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.QQUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.zinhao.chtholly.utils.QQUtils.*;

public class NekoChatService extends AccessibilityService implements OpenAiMessage.DelayReplyListener{
    private static final String TAG = "NekoChatService";
    private boolean ENABLE_ACTION = true;
    public static Class<?> mode = OpenAiSession.class;
    private static PackageInfo qqInfo;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);
    private static NekoChatService instance;

    private AccessibilityButtonController accessibilityButtonController;
    private boolean mIsAccessibilityButtonAvailable;

    private int dayCount = 0;
    private boolean todayMorning = false;
    private boolean todayNoon = false;
    private boolean todayAfter = false;
    private boolean todayEvening = false;
    private boolean todayBedTime = false;
    private boolean todayTest = false;

    private String botName = "botName";
    private String chatTitle = "";
    private Handler mHandler;
    private Command autoCommand;

    public static NekoChatService getInstance() {
        return instance;
    }


    private final List<RemindMessage> remindMessages = new Vector<>();
    // 等待回复的消息
    private final List<Command> waitQAs = new Vector<>();
    private Calendar calendar;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mHandler = new Handler(getMainLooper());
        calendar = Calendar.getInstance();
        dayCount = calendar.get(Calendar.DAY_OF_MONTH);
        PackageManager packageManager = getPackageManager();
        if(packageManager != null){
            List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
            for (PackageInfo p : packageInfoList) {
                if((p.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==0){
                    if(p.packageName.equals(QQ_PACKAGE_NAME)){
                        qqInfo = p;
                    }
                    Log.d(TAG, String.format("onCreate: package:%s verCode:%d version:%s", p.packageName, p.versionCode, p.versionName));
                }
            }
        }
    }

    /**
     *
     * @param seconds after now
     * @param message reply
     * @param master @master
     */
    public void addRemind(long seconds, String message, String master){
        long remindTime = System.currentTimeMillis()+seconds*1000L;
        Log.d(TAG, String.format(Locale.CHINA,"addAlarm: %s %s",dateTimeFormat.format(remindTime),message));
        remindMessages.add(new RemindMessage("system",message,remindTime,master));
    }

    private final Runnable delayCheck = new Runnable() {
        @Override
        public void run() {
            if(autoCommand!=null){
                Log.d(TAG, "autoMission: "+autoCommand.getQuestion().getMessage());
                autoCommand.ask();
                waitQAs.add(autoCommand);
            }
        }
    };

    private AccessibilityNodeInfo etInput;
    private AccessibilityNodeInfo btSend;
    private AccessibilityNodeInfo btPic;
    private AccessibilityNodeInfo tvTitle;

    private boolean isQQChatPage(AccessibilityNodeInfo nodeInfo){
        Log.d(TAG, "checkPage: ================================================");
        if(nodeInfo == null){
            Log.d(TAG, "checkPage:nodeInfo null!");
            return false;
        }
        if(!QQ_PACKAGE_NAME.equals(nodeInfo.getPackageName().toString())){
            Log.d(TAG, "checkPage:package name err!");
            return false;
        }
        AccessibilityNodeInfo input = findFirstNodeInfo(nodeInfo,QQUtils.getInputId());
        AccessibilityNodeInfo send = findFirstNodeInfo(nodeInfo,QQUtils.getSendButtonId());
        AccessibilityNodeInfo pic = findFirstNodeInfo(nodeInfo,QQUtils.getPicButtonId());
        AccessibilityNodeInfo title = findFirstNodeInfo(nodeInfo,QQUtils.getChatTitleId());

        Log.d(TAG, "checkPage: |-input:"+input);
        Log.d(TAG, "checkPage: |-send:"+send);
        Log.d(TAG, "checkPage: |-pic:"+pic);
        Log.d(TAG, "checkPage: |-title:"+title);
        Log.d(TAG, "checkPage: ================================================");
        if(input==null || send==null || pic==null || title == null){
            return false;
        }
        etInput = input;
        btSend = send;
        btPic = pic;
        tvTitle = title;
        chatTitle = tvTitle.getText().toString();
        Log.d(TAG, "isQQChatPage: "+tvTitle.getText());
        return true;
    }


    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "onServiceConnected: ");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        accessibilityButtonController = getAccessibilityButtonController();
        mIsAccessibilityButtonAvailable = accessibilityButtonController.isAccessibilityButtonAvailable();
        if (!mIsAccessibilityButtonAvailable) {
            return;
        }
        AccessibilityServiceInfo serviceInfo = getServiceInfo();
        serviceInfo.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
        setServiceInfo(serviceInfo);
        AccessibilityButtonController.AccessibilityButtonCallback accessibilityButtonCallback = new AccessibilityButtonController.AccessibilityButtonCallback() {
            @Override
            public void onClicked(AccessibilityButtonController controller) {
                Log.d(TAG, "Accessibility button pressed!");
            }

            @Override
            public void onAvailabilityChanged(
                    AccessibilityButtonController controller, boolean available) {
                if (controller.equals(accessibilityButtonController)) {
                    mIsAccessibilityButtonAvailable = available;
                }
            }
        };
        accessibilityButtonController.registerAccessibilityButtonCallback(
                accessibilityButtonCallback, null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        botName = BotApp.getInstance().getBotName();
        if(event == null)
            return;
        if(event.getPackageName() == null)
            return;

        autoMission(event.getPackageName().toString());
        
        AccessibilityNodeInfo root = getRootInActiveWindow();

        if((event.getContentChangeTypes()&AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT){
            Log.d(TAG, "onAccessibilityEvent: package:"+event.getPackageName() + ", text: "+event.getText() + ", desc: "+event.getContentDescription());
            if((event.getContentChangeTypes()&AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE){
                // chat 文本消息
                if(isQQChatPage(root)){
                    Command qaMessage = findAddNewQA(root);
                    if(qaMessage!=null){
                        qaMessage.ask();
                        waitQAs.add(qaMessage);
                    }
                }
            }else{

            }
        }else{
            Log.d(TAG, "onAccessibilityEvent: "+event);
            if((event.getContentChangeTypes()&AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE){
                // 拍一拍，欢迎消息，撤回消息
                if(isQQChatPage(root)){
//                    QQUtils.id2FindLastMessage(root);
//                    Log.d(TAG, "onAccessibilityEvent: 滚动list 拍一拍，欢迎消息，撤回消息");
                    if(event.getSource()!=null){
                        QQUtils.treeFindLastMessage(event.getSource(),0);
                    }
                }else{
                    // 聊天列表页面
                    if(event.getSource()!=null){
                        QQUtils.treeFindLastMessage(event.getSource(),0);
                    }
                }
            }else{
                if(event.getSource()!=null){
                    QQUtils.treeFindLastMessage(event.getSource(),0);
                }
            }
            if(waitQAs.isEmpty())
                return;
        }
        doSomething(root);
        removeSuccessMessage();
    }

    private void removeSuccessMessage(){
        waitQAs.removeIf(new Predicate<Command>() {
            @Override
            public boolean test(Command commandMessage) {
                return commandMessage.sendSuccess() && commandMessage.actionSuccess();
            }
        });
    }

    /***
     *
     * @param min minute
     * @param max minute
     * @return mills
     */
    private static long randomTime(long min,long max){
        return Math.round(Math.random()*max*60*1000)+min*60*1000;
    }

    private void autoMission(String packageName){
        remindMessages.removeIf(new Predicate<RemindMessage>() {
            @Override
            public boolean test(RemindMessage remindMessage) {
                if(remindMessage.getSendTime() < System.currentTimeMillis()){
                    Message message = new Message(remindMessage.getMaster(),"/SYSTEM MESSAGE",System.currentTimeMillis());
                    StaticMessage staticMessage = new StaticMessage(getPackageName(),message,remindMessage.message);
                    staticMessage.ask();
                    waitQAs.add(staticMessage);
                    return true;
                }
                return false;
            }
        });
        if(BuildConfig.DEBUG)
            return;
        calendar.setTimeInMillis(System.currentTimeMillis());
        int nowDayCount = calendar.get(Calendar.DAY_OF_MONTH);
        int nowHourCount = calendar.get(Calendar.HOUR_OF_DAY);
        if(nowDayCount!=dayCount){
            //下一个日子
            Log.d(TAG, "autoMission: new day come!");
            todayMorning = false;
            todayNoon = false;
            todayAfter = false;
            todayEvening = false;
            todayBedTime = false;
            todayTest = false;
            dayCount = nowDayCount;
        }
        if(nowHourCount > 9 && nowHourCount <= 11 && !todayMorning){
            long delayMillis = randomTime(3,7);
            autoCommand = new OpenAiMessage(packageName,new Message("system","早上好",System.currentTimeMillis()),this);
            Log.d(TAG, "autoMission:todayMorning will answer at "+ dateTimeFormat.format(System.currentTimeMillis()+delayMillis));
            mHandler.postDelayed(delayCheck,delayMillis);
            todayMorning = true;
        }
        if(nowHourCount > 11 && nowHourCount <= 13 && !todayNoon){
            long delayMillis = randomTime(4,15);
            autoCommand = new OpenAiMessage(packageName,new Message("system","中午好",System.currentTimeMillis()),this);
            Log.d(TAG, "autoMission:todayNoon will answer at "+ dateTimeFormat.format(System.currentTimeMillis()+delayMillis));
            mHandler.postDelayed(delayCheck,delayMillis);
            todayNoon = true;
        }
        if(nowHourCount > 13 && nowHourCount <= 19 && !todayAfter){
            long delayMillis = randomTime(20,30);
            autoCommand =new OpenAiMessage(packageName,new Message("system","下午好",System.currentTimeMillis()),this);
            Log.d(TAG, "autoMission:todayAfter will answer at "+ dateTimeFormat.format(System.currentTimeMillis()+delayMillis));
            mHandler.postDelayed(delayCheck,delayMillis);
            todayAfter = true;
        }
        if(nowHourCount > 19 && nowHourCount <= 22 && !todayEvening){
            long delayMillis = randomTime(5,15);
            Log.d(TAG, "autoMission:todayEvening will answer at "+ dateTimeFormat.format(System.currentTimeMillis()+delayMillis));
            autoCommand = new OpenAiMessage(packageName,new Message("system","晚上好",System.currentTimeMillis()),this);
            mHandler.postDelayed(delayCheck,delayMillis);
            todayEvening = true;
        }
        if(nowHourCount > 22 && !todayBedTime){
            long delayMillis = randomTime(5,10);
            Log.d(TAG, "autoMission:todayBedTime will answer at "+ dateTimeFormat.format(System.currentTimeMillis()+delayMillis));
            autoCommand = new OpenAiMessage(packageName,new Message("system","晚安",System.currentTimeMillis()),this);
            mHandler.postDelayed(delayCheck,delayMillis);
            todayBedTime = true;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    // 检查队列的消息
    private void doSomething(AccessibilityNodeInfo source){
        if(source == null)
            return;
        if(etInput == null)
            etInput = findInputNodeInfo(source);
        if(btSend == null)
            btSend = findSendNodeInfo(source);

        for (int i = 0; i < waitQAs.size(); i++) {
            Command qa = waitQAs.get(i);
            if(qa.getAnswer() == null || qa.getAnswer().getMessage() == null || (qa.sendSuccess() && qa.actionSuccess())){
                continue;
            }

            if(ENABLE_ACTION && qa.getAction()!=0){
                if(BotApp.getInstance().getAdminName().equals(chatTitle)){
                    if(doAction(source,qa)){
                        return;
                    }
                }else{
                    if(etInput != null && btSend!=null){
                        etInput.refresh();
                        qa.getAnswer().setMessage("不行的，这样不行的喵！");
                        writeMessage(etInput,qa);
                        btSend.refresh();
                        clickButton(btSend,qa);
                        qa.finish();
                    }
                }
            }else{
                if(etInput != null && btSend!=null){
                    etInput.refresh();
                    writeMessage(etInput,qa);
                    btSend.refresh();
                    clickButton(btSend,qa);
                }
            }

            if(qa.sendSuccess()){
                Log.d(TAG, "doSomething: send success:" + qa.getAnswer().getMessage());
            }
        }
    }

    /***
     * 仅适用于 手机QQ v8.9.50.10650
     * @param source
     * @param qa
     * @return
     */
    private boolean doAction(AccessibilityNodeInfo source, Command qa){
        source.refresh();
        if(!qa.actionSuccess()){
            Step step = qa.getNextStep();
            boolean result = false;
            if(step.getDaley()!=0){
                qa.back(step);
                result = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        step.setDaley(0);
                        doSomething(source);
                    }
                },step.getDaley());
            }else{
                if(step.isGlobalAction()){
                    result = performGlobalAction(step.getActionId());
                }else{
                    AccessibilityNodeInfo targetView = findFirstNodeInfo(source,step.getViewId());
                    if(targetView!=null){
                        result = targetView.performAction(step.getActionId());
                    }
                }
            }

            Log.d(TAG, String.format(Locale.US,"doAction: action:%s result:%s ",step,result));
            if(!result){
                qa.back(step);
            }
            return true;
        }
        return false;
    }

    private boolean writeMessage(AccessibilityNodeInfo inputEditText, Command qaMessage){
        if(!qaMessage.isWrite()){
            Bundle arg = new Bundle();
            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, qaMessage.getAnswer().getMessage());
            boolean result = inputEditText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,arg);
            qaMessage.setWrite(result);
        }
        return qaMessage.isWrite();
    }

    private boolean clickButton(AccessibilityNodeInfo sendButton, Command commandMessage){
        if(!commandMessage.isSend() && commandMessage.isWrite()){
            boolean result = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            commandMessage.setSend(result);
        }
        return commandMessage.isSend();
    }

    private @Nullable AccessibilityNodeInfo findInputNodeInfo(AccessibilityNodeInfo source){
        return findFirstNodeInfo(source,QQUtils.getInputId());
    }

    private @Nullable AccessibilityNodeInfo findSendNodeInfo(AccessibilityNodeInfo source){
        return findFirstNodeInfo(source, QQUtils.getSendButtonId());
    }

    private @Nullable AccessibilityNodeInfo findFirstNodeInfo(AccessibilityNodeInfo source, String viewId){
        List<AccessibilityNodeInfo> targets = source.findAccessibilityNodeInfosByViewId(viewId);
        AccessibilityNodeInfo inputNode = null;
        if(targets.size()!=0){
            inputNode = targets.get(0);
        }
        return inputNode;
    }

    private Command findAddNewQA(AccessibilityNodeInfo nodeInfo){
        String botName = BotApp.getInstance().getBotName();
//        Message lastMessage = treeFindLastMessage(nodeInfo,0);
        Message lastMessage;
        boolean isAdmin = BotApp.getInstance().getAdminName().equals(chatTitle);
        if(isAdmin){
            lastMessage = id2FindLastMessage(nodeInfo);
        }else{
            lastMessage = id2FindGroupLastMessage(nodeInfo);
        }
        if(lastMessage.speaker ==null || lastMessage.message ==null){
            return null;
        }
        if(isAdmin){

        }else{
            if(botName.equals(lastMessage.speaker)){
                return null;
            }
        }
        if(!isAtBot(lastMessage)){
            return null;
        }
        lastMessage.message = lastMessage.message.replace("@"+botName,"").trim();
        if(waitQAs.size()!=0){
            Command last = waitQAs.get(waitQAs.size()-1);
            if(last.getQuestion().equals(lastMessage)){
                return null;
            }
        }
        Log.d(TAG, String.format(Locale.US,"findLastMessage: %s:%s",lastMessage.speaker,lastMessage.message));
        BotApp.getInstance().insert(lastMessage);
        if(mode == OpenAiSession.class){
            return new OpenAiMessage(nodeInfo.getPackageName().toString(),lastMessage,this);
        }else{
            return new NekoMessage(nodeInfo.getPackageName().toString(),lastMessage);
        }
    }

    private boolean isAtBot(Message message){
        if(message.message.trim().startsWith("@"+botName)){
            return true;
        }
        if(message.message.trim().endsWith("@"+botName)){
            return true;
        }
        if(message.message.contains("@"+botName)){
            return true;
        }
        return false;
    }

    @Override
    public void onReply(OpenAiMessage message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doSomething(getRootInActiveWindow());
                removeSuccessMessage();
            }
        });
    }
}
