package com.zinhao.chtholly;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import android.widget.CompoundButton;
import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.zinhao.chtholly.databinding.FloatBlackScreenBinding;
import com.zinhao.chtholly.databinding.FloatBtBinding;
import com.zinhao.chtholly.databinding.FloatHelperBinding;
import com.zinhao.chtholly.databinding.FloatLogcatBinding;
import com.zinhao.chtholly.entity.*;
import com.zinhao.chtholly.network.FeiShuApi;
import com.zinhao.chtholly.session.GeminiSession;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.*;
import com.zinhao.chtholly.view.FloatWindowActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.zinhao.chtholly.BotApp.context;
import static com.zinhao.chtholly.utils.QQChatHandler.*;

@SuppressLint("AccessibilityPolicy")
public class NekoChatService extends AccessibilityService implements NetAiAskAble.DelayReplyCallback, MessageCallback {
    private static final String TAG = "NekoChatService";
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);
    public static WeakReference<NekoChatService> instance;
    private Handler mHandler;

    private ExoPlayer ttsAudioPlayer;
    private AccessibilityButtonController accessibilityButtonController;
    private boolean mIsAccessibilityButtonAvailable;


    FloatHelperBinding helperBinding;
    private WindowManager.LayoutParams helperViewParams;
    FloatLogcatBinding logcatBinding;
    private WindowManager.LayoutParams logcatViewParams;
    FloatBtBinding floatMenuBinding;
    private WindowManager.LayoutParams floatMenuParam;
    FloatBlackScreenBinding floatBlackScreenBinding;
    private WindowManager.LayoutParams blackScreenParams;
    private long nextShowBlackScreen = 160;
    private boolean floatInit;
    private boolean blackScreenShow = false;

    private void showBlackScreen() {
        if(!floatInit) {
            return;
        }
        if(blackScreenShow) {
            return;
        }
        mHandler.post(()->{
            floatMenuBinding.getRoot().setVisibility(View.GONE);
            floatBlackScreenBinding.getRoot().setVisibility(View.VISIBLE);
            blackScreenShow = true;
        });
    }

    private void hideBlackScreen() {
        if(!floatInit) {
            return;
        }
        if(!blackScreenShow) {
            return;
        }
        nextShowBlackScreen = timeTick + AUTO_SHOW_BLACK_SCREEN_DELAY;
        mHandler.post(()->{
            floatMenuBinding.getRoot().setVisibility(View.VISIBLE);
            floatBlackScreenBinding.getRoot().setVisibility(View.GONE);
            blackScreenShow = false;
        });
    }


    private WindowManager windowManager;

    private long lastReplyTime = System.currentTimeMillis();
    private final List<Command> waitQAs = new Vector<>();

    private QQChatHandler qqChatHandler;
    private WXChatHandler wxChatHandler;
    private RBChatHandler rbChatHandler;

    private SystemSettingsHandler systemSettingsHandler;

    private FeiShuApi feiShuApi;

    public FeiShuApi getFeiShuApi() {
        return feiShuApi;
    }

    private Timer mainTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        addLogcat("NekoChatService => onCreate");
        mainTimer = new Timer("qa_list_handler");
        FileLogger.INSTANCE.init(context());
        instance = new WeakReference<>(this);
        mHandler = new Handler(getMainLooper());
        ttsAudioPlayer = new ExoPlayer.Builder(this).build();
        qqChatHandler = QQHandlerCompat.INSTANCE.get(this,this);
        wxChatHandler = new WXChatHandler(this);
        rbChatHandler = new RBChatHandler(this);
        systemSettingsHandler = new SystemSettingsHandler(this);
        windowManager = getSystemService(WindowManager.class);
        helperViewParams = OverlayUtils.makeNotTouchWindowParams(0, 0, 0, 0);
        logcatViewParams = OverlayUtils.makeNotTouchWindowParams(0, 0, 0, 0);
        blackScreenParams = OverlayUtils.makeFloatWindowParams(0, 0, 0, 0);
        floatMenuParam = OverlayUtils.makeFloatWindowParams(300, 300, 1, 1);
        speakStartVoice();
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, getNotification());
        }
        mainTimer.schedule(mainTimeTask, 0, LOOP_INTERVAL);
        BotApp.getInstance().getSession();
        AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
            @Override
            public void run() {
                if(BotApp.getInstance().getFeishuAppId().isEmpty()){

                }else{
                    feiShuApi = new FeiShuApi(BotApp.getInstance().getFeishuAppId(),BotApp.getInstance().getFeishuAppSecret());
                }

            }
        });

    }

    private static final long MINUTE_MILL = 60*1000;
    private static final long HOUR_MILL = 60*MINUTE_MILL;
    private static final long BORING_ASK_TIME = 48 * HOUR_MILL;
    private static final int LOOP_INTERVAL = 500;

    private long timeTick = 0;
    private static final long AUTO_SHOW_BLACK_SCREEN_DELAY = 160;
    private final TimerTask mainTimeTask = new TimerTask() {
        @Override
        public void run() {
            timeTick++;
            if(timeTick % 120 == 0){
                qqChatHandler.plusHp();
            }
            if(timeTick > nextShowBlackScreen){
                showBlackScreen();
            }

            ReminderManager.ReminderItem reminderItem = ReminderManager.INSTANCE.getNextReminder();
            if(reminderItem != null){
                if(reminderItem.getStartTime() < System.currentTimeMillis()){
                    ReminderManager.INSTANCE.clearExpired();
                    addReminder(reminderItem);
                }
            }
            if (waitQAs.isEmpty()) {
                if(System.currentTimeMillis() - lastReplyTime > BORING_ASK_TIME){
                    lastReplyTime = System.currentTimeMillis();
                    mHandler.post(()-> {
                        backToQQChatUseShare(NekoAskAble.COME_BACK);
                        addToQAList(new Message(null,NekoAskAble.TIME_TOO_FAST,System.currentTimeMillis()));
                    });
                }
            } else {
                hideBlackScreen();
                mHandler.post(() -> {
                    if(waitQAs.isEmpty()){
                        return;
                    }
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if(root != null){
                        addLogcat("timer tick...");
                        handleQAs(root);
                        removeSuccessMessage();
                        root.recycle();
                    }
                });
            }
        }
    };

    @Override
    public void onInterrupt() {
        FileLogger.INSTANCE.d(TAG, "onInterrupt: ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        addLogcat("onServiceConnected: ");

        instance = new WeakReference<>(this);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this, R.style.Theme_Chtholly);
        LayoutInflater layoutInflater = LayoutInflater.from(contextThemeWrapper);

        logcatBinding = FloatLogcatBinding.inflate(layoutInflater, null, false);
        helperBinding = FloatHelperBinding.inflate(layoutInflater, null, false);
        floatMenuBinding = FloatBtBinding.inflate(layoutInflater, null, false);
        floatBlackScreenBinding = FloatBlackScreenBinding.inflate(layoutInflater, null, false);
        if (Settings.canDrawOverlays(this)) {
            // 有权限
            addFloatToWindow();
            logcatBinding.getRoot().setVisibility(View.GONE);
            helperBinding.getRoot().setVisibility(View.GONE);
            floatBlackScreenBinding.getRoot().setVisibility(View.GONE);
        }
        bindClickListener();
        controllerViewToMinSize();

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
                accessibilityButtonCallback, mHandler);
    }

    @Override
    public void onSystemActionsChanged() {
        addLogcat("onSystemActionsChanged...");
        super.onSystemActionsChanged();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        if (event.getPackageName() == null)
            return;

        if (event.getSource() != null && BuildConfig.DEBUG) {
            if (!event.getText().isEmpty() || event.getContentDescription() != null) {
                String sourcePackageName = event.getSource().getPackageName().toString();
                String logcat = "package:" + event.getPackageName()
                        + ", class:" + event.getClassName()
                        + ", text: " + event.getText()
                        + ", desc: " + event.getContentDescription()
                        + ",source:" + sourcePackageName;
                addLogcat(logcat);
            }
            saveTreeToJsonFile(event);
        }

        if(System.currentTimeMillis() - failedBackTime < 5*1000L){
            return;
        }

        if (QQChatHandler.PACKAGE_NAME.equals(event.getPackageName().toString())) {
            qqChatHandler.handle(event);
            logcatBinding.currentPage.setText(qqChatHandler.getCurrentPageName());
            logcatBinding.currentChatTitle.setText(qqChatHandler.getChatTitle());
        } else if (WXChatHandler.WX_PACKAGE_NAME.equals(event.getPackageName().toString())) {
            wxChatHandler.handle(event);
        } else if (RBChatHandler.PACKAGE_NAME.equals(event.getPackageName().toString())) {
            rbChatHandler.handle(event);
        } else if(SystemSettingsHandler.PACKAGE_NAME.equals(event.getPackageName().toString())){
            systemSettingsHandler.handle(event);
        }
        if(helperBinding.acbv.getVisibility() == View.VISIBLE){
            AccessibilityNodeInfo root = getRootInActiveWindow();
            helperBinding.acbv.setNodeInfo(root);
            helperBinding.acbv.postInvalidate();
        }
        if (logcatBinding.getRoot().getVisibility() == View.VISIBLE) {
            if(waitQAs.isEmpty()){
                logcatBinding.callApiProgress.setVisibility(View.GONE);
            }else{
                logcatBinding.callApiProgress.setVisibility(View.VISIBLE);
            }
        }
    }

    private String strWaitQAs(){
        if(waitQAs.isEmpty()){
            return "empty";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < waitQAs.size(); i++) {
            stringBuilder.append('【').append(i).append("】").append(waitQAs.get(i).getQuestion().getMessage()).append("\n");
        }
        return stringBuilder.toString();
    }

    private void backToQQChatUseShare(String reason){
        if(!CHAT_GROUP.equals(qqChatHandler.getCurrentPageName()) && qqChatHandler.getTargetChatTitle() != null){
            Message backMessage = new Message(null,
                    "返回对话窗口=>"+qqChatHandler.getTargetChatTitle(), System.currentTimeMillis());
            backMessage.setEnableCommand(true);

            Command backToChatMessage = new NekoAskAble(PACKAGE_NAME, backMessage);

            backToChatMessage.initShareStepTo(qqChatHandler.getChatTitle(),FUNC_SHARE_TEXT,reason);
            backToChatMessage.getAnswer().setMessage(null);
            backToChatMessage.setReplyReady(true);
            backToChatMessage.handle();
            waitQAs.add(backToChatMessage);
            updateWaitQaText();
        }
    }

    private void updateWaitQaText(){
        mHandler.post(()->{
            if(logcatBinding!= null && logcatBinding.getRoot().getVisibility() == View.VISIBLE){
                logcatBinding.tvWaitQAList.setText(strWaitQAs());
            }
        });
    }

    private long failedBackTime = System.currentTimeMillis();
    private void timeoutRemove(Command qa){
        failedBackTime = System.currentTimeMillis();
        qa.finishStepAction();
        waitQAs.remove(0);
        updateWaitQaText();
        addLogcat("timeoutRemove: "+qa.getQuestion().getMessage() + " => time out, remove! the size of after remove:"+ waitQAs.size());
    }

    // 检查队列的消息
    private void handleQAs(AccessibilityNodeInfo source) {
        if (source == null){
            return;
        }
        addLogcat("handleQAs..."+waitQAs.size());
        if (!waitQAs.isEmpty()) {
            Command qa = waitQAs.get(0);
            if (System.currentTimeMillis() - qa.getQuestion().getTimeStamp() > 60*1000L) {
                timeoutRemove(qa);
            }

            if (!qa.isReplyReady()) {
                addLogcat("handleQAs: wait answer ready:" + qa.getQuestion().getMessage());
                return;
            }
            if (qa.sendSuccess() && qa.actionSuccess()) {
                addLogcat("sendSuccess && actionSuccess: "+qa.getQuestion().getMessage());
                return;
            }
            if (qa.haveAction()) {
                if (doAction(source, qa)) {
                    // 一次处理一项
                    return;
                }
            } else {
                if(qa.getPackageName()!=null && qa.getPackageName().equals(FeiShuApi.FEI_SHU_PACKAGE) && feiShuApi!=null){
                    qa.finishTextReply();
                    AsyncHelper.INSTANCE.doAsyncPart(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                feiShuApi.sendTextMessage(qa.getAnswer().getMessage());
                            } catch (Exception e) {
                                FileLogger.INSTANCE.e(TAG, "err:"+e.getLocalizedMessage(),e);
                            }
                        }
                    });
                }else{
                    if (source.getPackageName().equals(qqChatHandler.getPackageName())) {
                        qqChatHandler.writeAndSend(qa);
                    } else if (source.getPackageName().equals(wxChatHandler.getPackageName())) {
                        wxChatHandler.writeAndSend(qa);
                    } else if (source.getPackageName().equals(rbChatHandler.getPackageName())) {
                        rbChatHandler.writeAndSend(qa);
                    }
                }
            }
            if (qa.sendSuccess()) {
                lastReplyTime = System.currentTimeMillis();
                addLogcat("handleQAs: send success:" + qa.getAnswer().getMessage());
            }
        }
    }

    private boolean doAction(AccessibilityNodeInfo source, Command qa) {
        if(qa.actionSuccess()){
            return true;
        }
        source.refresh();
        Step step = qa.getNextStep();
        boolean result = false;
        if (step.getDelay() != 0) {
            mHandler.postDelayed(() -> {
                step.setDelay(0);
            }, step.getDelay());
        } else {
            result = doStep(source,step);
            // 为防止卡死在一条指令上面，设置一个30秒超时，超时会自动完成任务。
            if (System.currentTimeMillis() - qa.getQuestion().getTimeStamp() > 30*1000L) {
                timeoutRemove(qa);
                backToQQChatUseShare("find view out time");
                return false;
            }
        }
        addLogcat(String.format(Locale.US, "doAction: %s result:%s ", step , result));
        if (!result) {
            qa.backStepList(step);
        }
        return result;
    }

    public boolean doStep(AccessibilityNodeInfo source,Step step){
        boolean result = false;
        if(step.getActionType() == Step.ActionType.function){
            if(step.getFunctionName()!=null && step.getFunctionArg()!=null){
                if(step.getFunctionName().equals(FUNC_SHARE_FILE)){
                    shareFile(new File(step.getFunctionArg()));
                    result = true;
                }else if(step.getFunctionName().equals(FUNC_SHARE_TEXT)){
                    shareText(step.getFunctionArg());
                    result = true;
                }
            }
        } else if (step.isGlobalAction()) {
            result = performGlobalAction(step.getActionId());
        }else if (step.getActionType() == Step.ActionType.input) {
            AccessibilityNodeInfo targetView = findTargetView(source, step);
            if(targetView!=null){
                Bundle arg = new Bundle();
                String sendMessage = step.getTargetText();
                arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sendMessage);
                result = targetView.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg);
            }
        } else {
            AccessibilityNodeInfo targetView = findTargetView(source, step);
            if (targetView != null) {
                if (step.isCustomGesture()) {
                    result = doCustomGesture(targetView, step);
                } else {
                    result = targetView.performAction(step.getActionId());
                }
            } else {
                addLogcat("doAction: 寻找视图失败" + step.getViewId());
                if (step.isCustomGesture()) {
                    if (step.getViewId() == null) {
                        result = doCustomGesture(source, step);
                    }
                }
            }
        }
        return result;
    }


    private void saveTreeToJsonFile(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        if (event.getSource() == null) {
            return;
        }
        try {
            if ("com.android.systemui:id/clock".equals(event.getSource().getViewIdResourceName())) {
                return;
            }
            if ("com.android.systemui".contentEquals(event.getPackageName())) {
                return;
            }
            String jsonFileName = treeFileName(event);
            JSONObject layoutTree = LayoutTreeUtils.treeAndPrintLayout(event.getSource(), 0, true);
            layoutTree.put("event", LayoutTreeUtils.getEventStringBuilder(event));
            layoutTree.put("time", dateTimeFormat.format(System.currentTimeMillis()));
            //com.tencent.mobileqq:id/listView1
            logcatBinding.currentTree.setText(jsonFileName);
            addLogcat("Generate Tree:"+ jsonFileName);
            LocalFileCache.getInstance().saveJSONObject(getApplicationContext(), layoutTree, jsonFileName);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeSuccessMessage() {
        boolean removeResult = waitQAs.removeIf(commandMessage ->
                commandMessage.sendSuccess() && commandMessage.actionSuccess()
        );
        if(removeResult){
            updateWaitQaText();
        }
    }

    private AccessibilityNodeInfo findTargetView(AccessibilityNodeInfo source, Step step) {
        if (step.isGlobalAction()) {
            return null;
        }
        AccessibilityNodeInfo targetView = findIndexInTargetNodeChildren(source, step.getViewId(), step.getInNodesPosition());
        if (targetView == null) {
            addLogcat("1.没找到:" + step.getViewId());
            if (step.getIndexMode() == Step.IndexTargetMode.text) {
                targetView = source;
            } else {
                return null;
            }

        }
        if (step.getIndexMode() == Step.IndexTargetMode.position) {
            for (int i = 0; i < step.getFindPosition().length; i++) {
                targetView = targetView.getChild(step.getFindPosition()[i]);
                if (targetView == null) {
                    addLogcat("2.没找到:" + step.getViewId());
                    return null;
                }
            }
            String needHasId = step.getNeedHasId();
            if (needHasId != null) {
                // 需要检查是否是包含目标
                if (!targetView.findAccessibilityNodeInfosByViewId(needHasId).isEmpty()) {
                    return targetView;
                } else {
                    addLogcat("is not has view id [" + needHasId + "] in " + targetView.getViewIdResourceName());
                    return null;
                }
            }
            return targetView;
        } else if (step.getIndexMode() == Step.IndexTargetMode.text) {
            targetView = findFirstTextInTargetNodeChildren(targetView, step.getTargetText(), step.getTargetTextViewId());
            for (int i = 0; i < step.getTargetParentTimes(); i++) {
                if (targetView != null) {
                    targetView = targetView.getParent();
                } else {
                    addLogcat("3.没找到parent:" + step.getViewId() + " =>" + i);
                    return null;
                }
            }
            return targetView;
        } else {
            return targetView;
        }

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
                ttsAudioPlayer.setMediaItem(builder.build());
                ttsAudioPlayer.prepare();
                ttsAudioPlayer.play();
            } catch (Exception e) {
                Log.e(TAG, "speakMessage: ", e);
            }
        });
    }

    private void speakStartVoice() {
        // ご主人様,またあなたと出会いました。
        MediaItem mediaItem = MediaItem.fromUri("asset:///start_voice_pcm_16.wav");
        ttsAudioPlayer.setMediaItem(mediaItem);
        ttsAudioPlayer.prepare();
        ttsAudioPlayer.play();
    }

    private void speakLeaveVoice() {
        // ご主人様,またあなたと出会いました。
        MediaItem mediaItem = MediaItem.fromUri("asset:///leave.wav");
        ttsAudioPlayer.setMediaItem(mediaItem);
        ttsAudioPlayer.prepare();
        ttsAudioPlayer.play();
        ttsAudioPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if (playbackState == Player.STATE_ENDED) {
                    ttsAudioPlayer.release(); // 播放结束后释放资源
                    stopSelf(); // 停止服务
                }
            }
        });
    }

    public boolean doCustomGesture(AccessibilityNodeInfo targetView, Step step) {
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

    public boolean doDoubleClick(AccessibilityNodeInfo targetView,Runnable after){
        GestureDescription gb = Command.DOUBLE_CLICK.onGesture(targetView);
        return dispatchGesture(gb, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.w(TAG, "dispatchGesture: onCompleted" + "doDoubleClick");
                mHandler.postDelayed(after,1000);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "dispatchGesture: onCancelled" + "doDoubleClick");
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
        addLogcat(String.format(Locale.CHINA, "addAlarm: %s %s", dateTimeFormat.format(remindTime), message));
    }

    public void addReminder(ReminderManager.ReminderItem reminderItem) {
        StaticAskAble staticAskAble = new StaticAskAble(getPackageName(),
                new Message(null,null,System.currentTimeMillis()),
                dateTimeFormat.format(reminderItem.getStartTime())+"需要去做的事情:"+reminderItem.getTitle());
        staticAskAble.handle();
        waitQAs.add(staticAskAble);
        updateWaitQaText();
    }

    @Override
    public void onReplySuccess(NetAiAskAble message) {
        mHandler.post(() -> {
            handleQAs(getRootInActiveWindow());
            removeSuccessMessage();
        });
    }
    private static long randomTime(long min, long max) {
        return Math.round(Math.random() * max * 60 * 1000) + min * 60 * 1000;
    }

    boolean isControllerMinSize = false;

    public void controllerViewToMinSize() {
        floatMenuBinding.ctrlTab.setVisibility(View.GONE);
        isControllerMinSize = true;
    }

    public void controllerViewToDefaultSize() {
        floatMenuBinding.ctrlTab.setVisibility(View.VISIBLE);
        isControllerMinSize = false;
    }

    public void addFloatToWindow() {
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            Intent rqIntent = new Intent(getApplicationContext(), FloatWindowActivity.class);
            rqIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(rqIntent);
        } else {
            if (!floatInit) {
                windowManager.addView(helperBinding.getRoot(), helperViewParams);
                windowManager.addView(logcatBinding.getRoot(), logcatViewParams);
                windowManager.addView(floatBlackScreenBinding.getRoot(),blackScreenParams);
                windowManager.addView(floatMenuBinding.getRoot(), floatMenuParam);
            }
            floatInit = true;
        }
    }

    public void addLogcat(String l) {
        FileLogger.INSTANCE.d(TAG, l);
        if (logcatBinding == null) {
            return;
        }
        logcatBinding.logcatView.appendLogcat(l);
    }

    @Override
    public void onFind(Message message) {
        addToQAList(message);
        mHandler.post(()->{
            logcatBinding.callApiProgress.setVisibility(View.VISIBLE);
        });
    }

    public void onFindFeiShuMessage(Message message){
        logcatBinding.callApiProgress.setVisibility(View.VISIBLE);
        Command mainMessage;
        if (BotApp.getInstance().getMode() == OpenAiSession.class) {
            mainMessage = new OpenAiAskAble(FeiShuApi.FEI_SHU_PACKAGE, message, this);
        } else if (BotApp.getInstance().getMode() == GeminiSession.class) {
            mainMessage = new GeminiAIAskAble(FeiShuApi.FEI_SHU_PACKAGE, message, this);
        } else {
            mainMessage = new NekoAskAble(FeiShuApi.FEI_SHU_PACKAGE, message);
        }
        mainMessage.handle();
        waitQAs.add(mainMessage);
        updateWaitQaText();
        addLogcat("waitQAs[" + waitQAs.size() + "] " + message.getSpeaker() + ": " + message.getMessage());
    }

    private void addToQAList(Message message){
        Command mainMessage;
        if (BotApp.getInstance().getMode() == OpenAiSession.class) {
            mainMessage = new OpenAiAskAble(getRootInActiveWindow().getPackageName().toString(), message, this);
        } else if (BotApp.getInstance().getMode() == GeminiSession.class) {
            mainMessage = new GeminiAIAskAble(getRootInActiveWindow().getPackageName().toString(), message, this);
        } else {
            mainMessage = new NekoAskAble(getRootInActiveWindow().getPackageName().toString(), message);
        }
        mainMessage.handle();
        waitQAs.add(mainMessage);
        updateWaitQaText();
        addLogcat("waitQAs[" + waitQAs.size() + "] " + message.getSpeaker() + ": " + message.getMessage());
    }


    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                getPackageName() + ".NekoChatService",
                "Chtholly Running Notification",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification() {
        return new Notification.Builder(this, getPackageName() + ".NekoChatService")
                .setContentTitle("NekoChatService")
                .setContentText("Service is running:" + BotApp.getInstance().getMode().getSimpleName())
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // 替换为你的图标
                .build();
    }

    public QQChatHandler getQqChatHandler() {
        return qqChatHandler;
    }

    public WXChatHandler getWxChatHandler() {
        return wxChatHandler;
    }


    @SuppressLint("ClickableViewAccessibility")
    public void bindClickListener() {

        floatMenuBinding.b1.setChecked(helperBinding.getRoot().getVisibility()==View.VISIBLE);
        floatMenuBinding.b2.setChecked(logcatBinding.getRoot().getVisibility()==View.VISIBLE);

        floatMenuBinding.saySwitch.setChecked(BotApp.getInstance().isWithSpeaker());

        floatMenuBinding.b1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                helperBinding.getRoot().setVisibility(isChecked?View.VISIBLE:View.GONE);
            }
        });
        floatMenuBinding.b2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                logcatBinding.getRoot().setVisibility(isChecked?View.VISIBLE:View.GONE);
            }
        });

        floatMenuBinding.saySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BotApp.getInstance().setWithSpeaker(isChecked);
            }
        });

        floatMenuBinding.testBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToQQChatUseShare("test back to chat");
            }
        });

        floatMenuBinding.ctrl.setOnTouchListener(new DragHand() {
            @Override
            public void onDragDelta(int xDelta, int yDelta) {
                floatMenuParam.x += xDelta;
                floatMenuParam.y += yDelta;
                windowManager.updateViewLayout(
                        floatMenuBinding.getRoot(),
                        floatMenuParam
                );
            }

            @Override
            public void onClick(View view) {
                if (isControllerMinSize) {
                    controllerViewToDefaultSize();
                } else {
                    controllerViewToMinSize();
                }
            }
        });
        floatBlackScreenBinding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideBlackScreen();
            }
        });
    }

    private static String treeFileName(AccessibilityEvent event) {
        assert event != null;
        assert event.getSource() != null;
        String idString = event.getSource().getViewIdResourceName();
        String fileName;
        CharSequence packageName = event.getPackageName();
        if (idString == null) {
            if (packageName != null) {
                fileName = event.getPackageName().toString();
            } else {
                fileName = "_null";
            }
        } else {
            fileName = idString;
        }
        return FilenameFilter.sanitizeWithReplacement("tree_" + fileName + ".json", '_');
    }

    public static final String FUNC_SHARE_TEXT = "share_text";
    public void shareText(String text) {
        Intent sendIntent = new Intent();
        sendIntent.setPackage(qqChatHandler.getPackageName());  // 指定目标应用包名
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");

        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(sendIntent);
        }catch (Exception e){
            FileLogger.INSTANCE.e(TAG, Objects.requireNonNull(e.getLocalizedMessage()), e);
        }
    }

    public static final String FUNC_SHARE_FILE = "share_file";
    public void shareFile(File file) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Uri uri = FileProvider.getUriForFile(NekoChatService.this, getPackageName() + ".fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setPackage(qqChatHandler.getPackageName());  // 指定目标应用包名
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, uri);

                intent.setType("*/*");

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    FileLogger.INSTANCE.e(TAG, Objects.requireNonNull(e.getLocalizedMessage()), e);
                }
            }
        });
    }


    public ChatPageViewIds currentChatPageIds(String packageName) {
        if (qqChatHandler.getPackageName().equals(packageName)) {
            return qqChatHandler.getChatPageViewIds();
        }
        return null;
    }

    public static NekoChatService getInstance() {
        if (instance != null) {
            return instance.get();
        }
        return null;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        FileLogger.INSTANCE.close();
        if(mainTimer!=null){
            mainTimer.cancel();
        }
        super.onDestroy();
        speakLeaveVoice();
    }
}
