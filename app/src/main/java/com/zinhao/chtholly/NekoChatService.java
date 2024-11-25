package com.zinhao.chtholly;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.zinhao.chtholly.entity.*;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.zinhao.chtholly.utils.QQChatHandler.*;

public class NekoChatService extends AccessibilityService implements OpenAiAskAble.DelayReplyCallback, MessageCallback {
    private static final String TAG = "NekoChatService";
    public static Class<?> mode = OpenAiSession.class;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);
    private static NekoChatService instance;
    private Handler mHandler;
    private ExoPlayer mediaPlayer;
    private AccessibilityButtonController accessibilityButtonController;
    private boolean mIsAccessibilityButtonAvailable;

    private boolean accShow;
    private boolean accIsAlpha;
    private View accView;
    private WindowManager.LayoutParams accViewParams;

    private boolean logcatShow;
    private boolean logcatAlpha;
    private View aclv;
    private AccessibilityLogcatView accessibilityLogcatView;
    private WindowManager.LayoutParams logcatViewParams;

    private boolean ctrlShow;
    private View ctrlView;
    private WindowManager.LayoutParams ctrlViewParams;
    private AccessibilityBoundView accessibilityBoundView;

    private WindowManager windowManager;
    /***
     * 每日自动问候
     */
    public boolean autoAsk = true;
    private int dayCount = 0;
    private boolean todayMorning = false;
    private boolean todayNoon = false;
    private boolean todayAfter = false;
    private boolean todayEvening = false;
    private boolean todayBedTime = false;
    private Command autoCommand;

    public ChatPageViewIds currentChatPageIds(String packageName){
        if(qqChatHandler.getPackageName().equals(packageName)){
            return qqChatHandler.getChatPageViewIds();
        }
        return null;
    }

    public static NekoChatService getInstance() {
        return instance;
    }

    private final List<RemindMessage> remindMessages = new Vector<>();

    private final List<Command> waitQAs = new Vector<>();
    private Calendar calendar;
    private QQChatHandler qqChatHandler;
    private WXChatHandler wxChatHandler;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mHandler = new Handler(getMainLooper());
        calendar = Calendar.getInstance();
        dayCount = calendar.get(Calendar.DAY_OF_MONTH);
        mediaPlayer = new ExoPlayer.Builder(this).build();
        qqChatHandler = new QQChatHandler(this);
        wxChatHandler = new WXChatHandler(this);
        windowManager = getSystemService(WindowManager.class);
        accViewParams = OverlayUtils.makeNotTouchWindowParams(0,0,0,0);
        ctrlViewParams = OverlayUtils.makeFloatWindowParams(400,400,1,1);
        speakStartVoice();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null)
            return;
        if (event.getPackageName() == null)
            return;

        autoMission(event.getPackageName().toString());
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo source = event.getSource();
        if(accView !=null){
            if(accessibilityBoundView == null)
                accessibilityBoundView = accView.findViewById(R.id.acbv);
            if(source == null && root!=null){
                accessibilityBoundView.setNodeInfo(root);
            }else if(source !=null && root == null){
                accessibilityBoundView.setNodeInfo(source);
            }else if(source != null){
                accessibilityBoundView.setNodeInfo(source);
            }
            accessibilityBoundView.postInvalidate();
        }

        if(aclv!=null){
            if(accessibilityLogcatView == null)
                accessibilityLogcatView = aclv.findViewById(R.id.aclv);
        }


        if (BuildConfig.DEBUG) {
            LayoutTreeUtils.getEventStringBuilder(event);
            //EventType: TYPE_WINDOW_CONTENT_CHANGED; EventTime: 338363649;
            // PackageName: com.android.systemui; MovementGranularity: 0; Action: 0;
            // ContentChangeTypes: [CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION];
            // WindowChangeTypes: [] [ ClassName: android.widget.ImageView; Text: []; ContentDescription: QQ通知：二次元入口 (2条新消息);
            // ItemCount: -1; CurrentItemIndex: -1; Ena
            // : package:com.tencent.mobileqq, text: [[有人@我]景皓(二次元入口):@丛雨 最近有点低迷，我想你说点鼓励的话语], desc: null
            if(!event.getText().isEmpty() || event.getContentDescription()!=null){
                String logcat ="package:" + event.getPackageName() + ", class:"+event.getClassName()+", text: " + event.getText() + ", desc: " + event.getContentDescription();
                Log.i(TAG, logcat);
            }
        }
        String pageName = processNotChatPage(event.getSource());
        if(!UNKNOWN_PAGE.equals(pageName) && !NULL_ROOT.equals(pageName)){
            NekoChatService.getInstance().addLogcat( "onAccessibilityEvent: " + pageName);
            Log.d(TAG, "onAccessibilityEvent: " + pageName);
        }


        if (event.getSource() != null) {
            if(BuildConfig.DEBUG){
                try {
                    if("com.android.systemui:id/clock".equals(event.getSource().getViewIdResourceName())){
//                        Log.d(TAG, "onAccessibilityEvent: " + "s");
                    }else{
                        JSONObject layoutTree = LayoutTreeUtils.treeAndPrintLayout(root, 0);
                        //com.tencent.mobileqq:id/listView1
                        String idString = event.getSource().getViewIdResourceName();
                        String fileName;
                        if (idString == null) {
                            fileName = "_null";
                        } else {
                            fileName = event.getSource().getViewIdResourceName().replace(event.getPackageName() + ":id/", "_")
                                    .replace("/", "_")
                                    .replace(":", "_");
                        }
                        LocalFileCache.getInstance().saveJSONObject(getApplicationContext(), layoutTree, "tree" + fileName + ".json");
                    }

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if(QQ_PACKAGE_NAME.equals(event.getPackageName().toString())){
            qqChatHandler.handle(event);
        }else if(WXChatHandler.WX_PACKAGE_NAME.equals(event.getPackageName().toString())){
            wxChatHandler.initChatPage(event.getSource());
            wxChatHandler.handle(event);
        }
        if (waitQAs.isEmpty()) {
            return;
        }
        handleQAs(root);
        removeSuccessMessage();
    }

    private String processNotChatPage(AccessibilityNodeInfo root) {
        if (root == null) {
            return NULL_ROOT;
        }
        return QQChatHandler.checkWhatPage(root);
    }

    private void removeSuccessMessage() {
        waitQAs.removeIf(commandMessage -> {
            addLogcat("removeSuccessMessage:"+commandMessage.getAnswer().getMessage());
            Step step = commandMessage.getNextStep();
            if(step!=null){
                addLogcat("removeSuccessMessage:"+step);
            }
            return commandMessage.sendSuccess() && commandMessage.actionSuccess();
        });
    }

    private void autoMission(String packageName) {
        if (!autoAsk) {
            return;
        }
        remindMessages.removeIf(remindMessage -> {
            if (remindMessage.getSendTime() < System.currentTimeMillis()) {
                Message message = new Message(remindMessage.getMaster(), "/SYSTEM MESSAGE", System.currentTimeMillis());
                StaticAskAble staticAskAble = new StaticAskAble(getPackageName(), message, remindMessage.message);
                playTTSVoiceFromNetWork(remindMessage.message);
                staticAskAble.ask();
                waitQAs.add(staticAskAble);
                return true;
            }
            return false;
        });
        if (BuildConfig.DEBUG)
            return;
        calendar.setTimeInMillis(System.currentTimeMillis());
        int nowDayCount = calendar.get(Calendar.DAY_OF_MONTH);
        int nowHourCount = calendar.get(Calendar.HOUR_OF_DAY);
        if (nowDayCount != dayCount) {
            //下一个日子
            addLogcat( "autoMission: new day come!");
            todayMorning = false;
            todayNoon = false;
            todayAfter = false;
            todayEvening = false;
            todayBedTime = false;
            dayCount = nowDayCount;
        }
        if (nowHourCount > 8 && nowHourCount <= 10 && !todayMorning) {
            long delayMillis = randomTime(3, 7);
            autoCommand = new NekoAskAble(packageName, new Message("system", "早上好", System.currentTimeMillis()));
            addLogcat("autoMission:todayMorning will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayMorning = true;
        }
        if (nowHourCount > 11 && nowHourCount <= 13 && !todayNoon) {
            long delayMillis = randomTime(4, 15);
            autoCommand = new NekoAskAble(packageName, new Message("system", "中午好", System.currentTimeMillis()));
            addLogcat("autoMission:todayNoon will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayNoon = true;

            List<Step> steps = new ArrayList<>();
            steps.add(new Step(getPackageName(), ":id/qn4", AccessibilityNodeInfo.ACTION_CLICK, false));
            steps.add(new Step(getPackageName(), ":id/nfz", AccessibilityNodeInfo.ACTION_CLICK, false, 500));
            Command command = new Command(
                    getPackageName(),
                    new Message("SYSTEM", "打卡任务", System.currentTimeMillis()),
                    steps);
            command.ask();
            waitQAs.add(command);
        }
        if (nowHourCount > 13 && nowHourCount <= 19 && !todayAfter) {
            long delayMillis = randomTime(20, 30);
            autoCommand = new NekoAskAble(packageName, new Message("system", "下午好", System.currentTimeMillis()));
            addLogcat("autoMission:todayAfter will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayAfter = true;
        }
        if (nowHourCount > 19 && nowHourCount <= 22 && !todayEvening) {
            long delayMillis = randomTime(5, 15);
            addLogcat("autoMission:todayEvening will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            autoCommand = new NekoAskAble(packageName, new Message("system", "晚上好", System.currentTimeMillis()));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayEvening = true;
        }
        if (nowHourCount > 22 && !todayBedTime) {
            long delayMillis = randomTime(5, 10);
            addLogcat("autoMission:todayBedTime will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            autoCommand = new NekoAskAble(packageName, new Message("system", "晚安", System.currentTimeMillis()));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayBedTime = true;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt: ");
    }

    // 检查队列的消息
    private void handleQAs(AccessibilityNodeInfo source) {
        if (source == null)
            return;
        for (Command qa : waitQAs) {
            if (qa.getAnswer() == null || qa.getAnswer().getMessage() == null || (qa.sendSuccess() && qa.actionSuccess())) {
                continue;
            }
            // todo 也许需要添加一个开关，允许或者拒绝动作的执行
            if (qa.haveAction()) {
                if (doAction(source, qa)) {
                    // 一次处理一项
                    return;
                }
            } else {
                if(source.getPackageName().equals(qqChatHandler.getPackageName())){
                    qqChatHandler.writeAndSend(qa);
                }else if(source.getPackageName().equals(wxChatHandler.getPackageName())){
                    wxChatHandler.writeAndSend(qa);
                }
            }
            if (qa.sendSuccess()) {
                addLogcat("doSomething: send success:" + qa.getAnswer().getMessage());
            }
        }
    }

    private boolean doAction(AccessibilityNodeInfo source, Command qa) {
        source.refresh();
        if (!qa.actionSuccess()) {
            Step step = qa.getNextStep();
            boolean result = false;
            if (step.getDaley() != 0) {
                qa.back(step);
                result = true;
                if (step.isWaiting()) {
                    return false;
                }
                // 延后执行,先放回队列,时间到之后设置daley为0,取出来执行
                mHandler.postDelayed(() -> {
                    step.setDaley(0);
                    handleQAs(getRootInActiveWindow());
                }, step.getDaley());
            } else {
                if (step.isGlobalAction()) {
                    result = performGlobalAction(step.getActionId());
                } else {
                    AccessibilityNodeInfo targetView = findIndexNodeInfo(source, step.getViewId(),step.getInNodesPosition());
                    if (step.isFindChildByPosition() && targetView != null) {
                        for (int i = 0; i < step.getFindPosition().length; i++) {
                            targetView = targetView.getChild(step.getFindPosition()[i]);
                            if (targetView == null) {
                                break;
                            }
                        }
                    }
                    if (targetView != null) {
                        if (step.getActionId() == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                            result = doGesture(targetView, step);
                        } else {
                            result = targetView.performAction(step.getActionId());
                        }
                    } else {
                        addLogcat("doAction: 寻找视图失败" + step.getViewId());
                        // 为防止卡死在一条指令上面，设置一个30秒超时，超时会自动完成任务。
                        if (System.currentTimeMillis() - qa.getQuestion().getTimeStamp() > 30000) {
                            result = true;
                            qa.finishStepAction();
                            addLogcat("doAction: 寻找视图超时！结束任务。");
                        }

                    }
                }
            }
            Log.w(TAG, String.format(Locale.US, "doAction: %s result:%s ", step, result));
            if (!result) {
                qa.back(step);
            }
            return true;
        }
        return false;
    }

    public void playTTSVoiceFromNetWork(String speakMessage) {
        if (speakMessage == null)
            return;
        if (speakMessage.isEmpty())
            return;
        mHandler.post(() -> {
            try {
                String rawMessage = speakMessage.replace("&", " ");
                MediaItem.Builder builder = new MediaItem.Builder();
                String path = String.format(Locale.US, "%s/generate_voice?text=%s&speaker_id=%d&translate=0",
                        BotApp.getInstance().getTtsUrl(),
                        rawMessage,
                        BotApp.getInstance().getSpeakerId());
                builder.setUri(path);
                MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder();
                metaBuilder.setTitle("audio_1");
                builder.setMediaMetadata(metaBuilder.build());
                mediaPlayer.setMediaItem(builder.build());
                mediaPlayer.prepare();
                mediaPlayer.play();
            } catch (Exception e) {
                Log.e(TAG, "speakMessage: ", e);
            }
        });
    }

    private void speakStartVoice() {
        // ご主人様,またあなたと出会いました。
        MediaItem mediaItem = MediaItem.fromUri("asset:///start_voice_pcm_16.wav");
        mediaPlayer.setMediaItem(mediaItem);
        mediaPlayer.prepare();
        mediaPlayer.play();
    }

    private void speakLeaveVoice() {
        // ご主人様,またあなたと出会いました。
        MediaItem mediaItem = MediaItem.fromUri("asset:///leave.wav");
        mediaPlayer.setMediaItem(mediaItem);
        mediaPlayer.prepare();
        mediaPlayer.play();
        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if (playbackState == Player.STATE_ENDED) {
                    mediaPlayer.release(); // 播放结束后释放资源
                    stopSelf(); // 停止服务
                }
            }
        });
    }

    public boolean doGesture(AccessibilityNodeInfo targetView, Step step) {
        GestureDescription gb = step.getNeedGesture().onGesture(targetView);
        return dispatchGesture(gb, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.w(TAG, "dispatchGesture: onCompleted" + step);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "dispatchGesture: onCancelled" + step);
            }
        }, mHandler);
    }

    /**
     * @param seconds after now
     * @param message reply
     * @param master  @master
     */
    public void addRemind(long seconds, String message, String master) {
        long remindTime = System.currentTimeMillis() + seconds * 1000L;
        addLogcat( String.format(Locale.CHINA, "addAlarm: %s %s", dateTimeFormat.format(remindTime), message));
        remindMessages.add(new RemindMessage("system", message, remindTime, master));
    }

    @Override
    public void onReply(OpenAiAskAble message) {
        mHandler.post(() -> {
            handleQAs(getRootInActiveWindow());
            removeSuccessMessage();
        });
    }

    public void setChatsIndex(int chatsIndex) {
        qqChatHandler.setChatsIndex(chatsIndex);
    }

    public int getChatsIndex() {
        return qqChatHandler.getChatsIndex();
    }

    private final Runnable delayCheck = new Runnable() {
        @Override
        public void run() {
            if (autoCommand != null) {
                autoCommand.ask();
                waitQAs.add(autoCommand);
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        addLogcat("onServiceConnected: ");
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

    private static long randomTime(long min, long max) {
        return Math.round(Math.random() * max * 60 * 1000) + min * 60 * 1000;
    }

    public void setAccView(View accView) {
        this.accView = accView;
    }

    public View getAccView() {
        return accView;
    }

    public View getCtrlView() {
        return ctrlView;
    }

    public void setCtrlView(View ctrlView) {
        this.ctrlView = ctrlView;
    }

    public WindowManager.LayoutParams getAccViewParams() {
        return accViewParams;
    }

    public WindowManager.LayoutParams getCtrlViewParams() {
        return ctrlViewParams;
    }

    public void showAccWindow() {
        if (!Settings.canDrawOverlays(getApplicationContext()) || accView == null) {
            Intent rqIntent = new Intent(getApplicationContext(), FloatWindowActivity.class);
            rqIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(rqIntent);
        } else {
            if (!accShow) {
                windowManager.removeView(ctrlView);
                windowManager.addView(accView, accViewParams);
                if(accessibilityBoundView!=null){
                    accessibilityBoundView.postInvalidate();
                }
                windowManager.addView(ctrlView, ctrlViewParams);

            }
            accShow = true;
        }
    }

    public boolean isAccShow() {
        return accShow;
    }

    public void setAccIsAlpha(boolean alpha){
        accIsAlpha = alpha;
        accView.setAlpha(alpha?0:1);
    }

    public boolean isAccIsAlpha() {
        return accIsAlpha;
    }

    public boolean isLogcatShow() {
        return logcatShow;
    }

    public boolean isLogcatAlpha() {
        return logcatAlpha;
    }

    public void setLogcatAlpha(boolean logcatAlpha) {
        this.logcatAlpha = logcatAlpha;
        accessibilityLogcatView.setAlpha(logcatAlpha?0:1);
    }

    public View getAccessibilityLogcatView() {
        return aclv;
    }

    public void setAccessibilityLogcatView(View aclv) {
        this.aclv = aclv;
    }

    public void showLogcat() {
        if (!Settings.canDrawOverlays(getApplicationContext()) || accView == null) {
            Intent rqIntent = new Intent(getApplicationContext(), FloatWindowActivity.class);
            rqIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(rqIntent);
        } else {
            if (!logcatShow) {
                windowManager.removeView(ctrlView);

                windowManager.addView(aclv, accViewParams);
                if(aclv!=null){
                    aclv.postInvalidate();
                }

                windowManager.addView(ctrlView, ctrlViewParams);

            }
            logcatShow = true;
        }
    }

    public void showCtrlWindow() {
        if (!Settings.canDrawOverlays(getApplicationContext()) || ctrlView == null) {
            Intent rqIntent = new Intent(getApplicationContext(), FloatWindowActivity.class);
            rqIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(rqIntent);
        } else {
            if (!ctrlShow) {
                windowManager.addView(ctrlView, ctrlViewParams);
            }
            ctrlShow = true;
        }
    }

    public void hideCtrlWindow() {
        if (ctrlView == null)
            return;
        if (ctrlShow) {
            windowManager.removeView(ctrlView);
        }
        ctrlShow = false;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        super.onDestroy();
        speakLeaveVoice();
    }

    public void addLogcat(String l){
        if(accessibilityLogcatView!=null){
            accessibilityLogcatView.appendLogcat(l);
        }
    }

    @Override
    public void onFind(Message message) {
        Command command;
        if (mode == OpenAiSession.class) {
            command =  new OpenAiAskAble(getRootInActiveWindow().getPackageName().toString(), message, this);
        } else {
            command = new NekoAskAble(getRootInActiveWindow().getPackageName().toString(), message);
        }
        command.ask();
        waitQAs.add(command);
        addLogcat("waitQAs["+waitQAs.size()+"] " +message.getSpeaker()+ ": "+message.getMessage());

    }
}
