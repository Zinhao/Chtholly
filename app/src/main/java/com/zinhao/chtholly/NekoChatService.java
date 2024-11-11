package com.zinhao.chtholly;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.zinhao.chtholly.entity.*;
import com.zinhao.chtholly.session.OpenAiSession;
import com.zinhao.chtholly.utils.ChatPageViewIds;
import com.zinhao.chtholly.utils.LocalFileCache;
import com.zinhao.chtholly.utils.QQUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

import static com.zinhao.chtholly.utils.QQUtils.*;

public class NekoChatService extends AccessibilityService implements OpenAiMessage.DelayReplyListener {
    private static final String TAG = "NekoChatService";
    public static Class<?> mode = OpenAiSession.class;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);
    private static NekoChatService instance;
    private Handler mHandler;
    private ExoPlayer mediaPlayer;
    private AccessibilityButtonController accessibilityButtonController;
    private boolean mIsAccessibilityButtonAvailable;

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

    /***
     * 机器人信息和聊天信息
     */
    private String chatTitle = "";
    private int chatsIndex = 0;

    public void setChatsIndex(int chatsIndex) {
        this.chatsIndex = chatsIndex;
    }

    public int getChatsIndex() {
        return chatsIndex;
    }

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
        mediaPlayer = new ExoPlayer.Builder(this).build();
        speakStartVoice();
    }

    /**
     * @param seconds after now
     * @param message reply
     * @param master  @master
     */
    public void addRemind(long seconds, String message, String master) {
        long remindTime = System.currentTimeMillis() + seconds * 1000L;
        Log.d(TAG, String.format(Locale.CHINA, "addAlarm: %s %s", dateTimeFormat.format(remindTime), message));
        remindMessages.add(new RemindMessage("system", message, remindTime, master));
    }

    private final Runnable delayCheck = new Runnable() {
        @Override
        public void run() {
            if (autoCommand != null) {
                Log.d(TAG, "autoMission: " + autoCommand.getQuestion().getMessage());
                autoCommand.ask();
                waitQAs.add(autoCommand);
            }
        }
    };

    private AccessibilityNodeInfo etInput;
    private AccessibilityNodeInfo btSend;
    public static final ChatPageViewIds chatPageViewIds = new ChatPageViewIds();
    private boolean isChatPage(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            Log.e(TAG, "checkPage:nodeInfo null!");
            return false;
        }
        //todo 目前只是适配qq聊天界面
        if (!QQ_PACKAGE_NAME.equals(nodeInfo.getPackageName().toString())) {
            Log.e(TAG, "checkPage:only support mobile qq!");
            return false;
        }
        //输入文本框id
        AccessibilityNodeInfo input = findFirstNodeInfo(nodeInfo, QQUtils.getInputId());
        chatPageViewIds.setInputViewId(QQUtils.getInputId());

        // 发送按钮id
        AccessibilityNodeInfo send = null;
        for (String viewId : SEND_BTN_IDS) {
            send = findFirstNodeInfo(nodeInfo, nodeInfo.getPackageName() + viewId);
            if (send != null) {
                chatPageViewIds.setSendBtnViewId(viewId);
                break;
            }
        }

        // 聊天标题id
        AccessibilityNodeInfo title = findFirstNodeInfo(nodeInfo, QQUtils.getChatTitleId());
        chatPageViewIds.setTitleViewId(QQUtils.getChatTitleId());

        // 确认 选择第一张图片的选择框id
        AccessibilityNodeInfo firstPicCheckBox = null;
        for (String viewId : PIC_CHECKBOX_IDS) {
            firstPicCheckBox = findFirstNodeInfo(nodeInfo, nodeInfo.getPackageName() + viewId);
            if (firstPicCheckBox != null) {
                chatPageViewIds.setFirstPicCheckBoxViewId(viewId);
                break;
            }
        }

        if (input == null || send == null) {
            Log.d(TAG, "checkPage:非聊天界面");
            return false;
        }
        etInput = input;
        btSend = send;
        if (title != null) {
            chatTitle = title.getText().toString();
            Log.d(TAG, "checkPage:聊天界面:" + title.getText());
        }
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

    private static StringBuilder builder;
    private static final Rect bound = new Rect();
    private static final boolean printTree = false;
    public static JSONObject treeAndPrintLayout(AccessibilityNodeInfo nodeInfo, int treeIndex) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("id", nodeInfo.getViewIdResourceName());
        root.put("class",nodeInfo.getClassName());
//        root.put("click",nodeInfo.isClickable());
//        root.put("longClick",nodeInfo.isLongClickable());
        root.put("desc", nodeInfo.getContentDescription());
        root.put("text", nodeInfo.getText());
        if (treeIndex == 0) {
//            emptyMessage = new Message(null,null,System.currentTimeMillis());
            builder = new StringBuilder();
            if (BuildConfig.DEBUG && printTree) {
                Log.d(TAG, "treeInfo:=========================================================>" + nodeInfo.getPackageName());
            }
            builder.append("|__");
        } else {
            builder.append("|  ");
        }
        JSONArray children = new JSONArray();
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = nodeInfo.getChild(i);
            if (child == null) {
                continue;
            }
            builder.delete(builder.length() - 2, builder.length());
            if (child.isClickable() || child.isCheckable() || child.isLongClickable()) {
                NekoChatService.chatPageViewIds.addActionableId(child);
            }
            JSONObject childObject;
            if (child.getChildCount() != 0) {
                builder.append("__");
                if (BuildConfig.DEBUG && printTree) {
                    child.getBoundsInScreen(bound);
                    Log.d(TAG, String.format(Locale.US, "treeInfo:%s%s class:%s, text:%s bound:%s click:%s longClick:%s check:%s desc:%s",
                            builder, child.getViewIdResourceName(), child.getClassName(), child.getText(),
                            bound, child.isClickable(), child.isLongClickable(), child.isCheckable(), child.describeContents()));
                }
                builder.delete(builder.length() - 2, builder.length());
                builder.append("  ");
                childObject = treeAndPrintLayout(child, treeIndex + 1);
            } else {
                childObject = new JSONObject();
                childObject.put("id", child.getViewIdResourceName());
                childObject.put("class",child.getClassName());
//                childObject.put("click",child.isClickable());
//                childObject.put("longClick",child.isLongClickable());
                childObject.put("desc", child.getContentDescription());
                childObject.put("text", child.getText());
                builder.append("__");
                if (getChatTextId().equals(child.getViewIdResourceName())) {
                    // 这是一条聊天记录
                    if (child.getText() == null)
                        continue;
//                    emptyMessage.message = child.getText().toString();
                }
                if (getChatNickId().equals(child.getViewIdResourceName())) {
                    if (child.getText() == null)
                        continue;
//                    emptyMessage.speaker = child.getText().toString();
                }
                if (BuildConfig.DEBUG && printTree) {
                    child.getBoundsInScreen(bound);
                    Log.d(TAG, String.format(Locale.US, "treeInfo:%s%s class:%s, text:%s bound:%s click:%s longClick:%s check:%s desc:%s",
                            builder, child.getViewIdResourceName(), child.getClassName(), child.getText(),
                            bound, child.isClickable(), child.isLongClickable(), child.isCheckable(), child.describeContents()));
                }
            }
            children.put(childObject);
            root.put("children", children);
        }
        if (builder.length() >= 3)
            builder.delete(builder.length() - 3, builder.length());

        return root;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null)
            return;
        if (event.getPackageName() == null)
            return;

        autoMission(event.getPackageName().toString());

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (BuildConfig.DEBUG) {
            StringBuilder stringBuilder = getStringBuilder(event);
            Log.d(TAG, stringBuilder + ": package:" + event.getPackageName() + ", text: " + event.getText() + ", desc: " + event.getContentDescription());
        }

        if (event.getSource() != null && BuildConfig.DEBUG) {
            try {
                JSONObject layoutTree = treeAndPrintLayout(event.getSource(), 0);
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
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) {
            if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                // chat 文本消息
                if (isChatPage(root)) {
                    Command qaMessage = findAddNewChatMessage(root);
                    if (qaMessage != null) {
                        qaMessage.ask();
                        waitQAs.add(qaMessage);
                    }
                }else {
                    Log.d(TAG, "onAccessibilityEvent: " + processNotChatPage(root));
                }
            }
        } else {
            if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                //todo 目前还没处理这类消息 拍一拍，欢迎消息，撤回消息
                Log.d(TAG, "onAccessibilityEvent: 拍一拍，欢迎消息，撤回消息");
                if(!isChatPage(root)){
                    Log.d(TAG, "onAccessibilityEvent: " + processNotChatPage(root));
                }
            }
        }
        if (waitQAs.isEmpty())
            return;
        doSomething(root);
        removeSuccessMessage();
    }

    private String processNotChatPage(AccessibilityNodeInfo root){
        if(!root.getPackageName().equals(QQ_PACKAGE_NAME)){
            return "only process qq page";
        }
        return QQUtils.checkWhatPage(root);
    }

    @NotNull
    private static StringBuilder getStringBuilder(AccessibilityEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) {
            stringBuilder.append("CONTENT_CHANGE_TYPE_TEXT");
        }
        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
            if (stringBuilder.length() != 0)
                stringBuilder.append(' ');
            stringBuilder.append("CONTENT_CHANGE_TYPE_SUBTREE");
        }
        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) == AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) {
            if (stringBuilder.length() != 0)
                stringBuilder.append(' ');
            stringBuilder.append("CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION");
        }
        return stringBuilder;
    }

    private void removeSuccessMessage() {
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
    private static long randomTime(long min, long max) {
        return Math.round(Math.random() * max * 60 * 1000) + min * 60 * 1000;
    }

    private void autoMission(String packageName) {
        if (!autoAsk) {
            return;
        }
        remindMessages.removeIf(new Predicate<RemindMessage>() {
            @Override
            public boolean test(RemindMessage remindMessage) {
                if (remindMessage.getSendTime() < System.currentTimeMillis()) {
                    Message message = new Message(remindMessage.getMaster(), "/SYSTEM MESSAGE", System.currentTimeMillis());
                    StaticMessage staticMessage = new StaticMessage(getPackageName(), message, remindMessage.message);
                    speakMessage(remindMessage.message);
                    staticMessage.ask();
                    waitQAs.add(staticMessage);
                    return true;
                }
                return false;
            }
        });
        if (BuildConfig.DEBUG)
            return;
        calendar.setTimeInMillis(System.currentTimeMillis());
        int nowDayCount = calendar.get(Calendar.DAY_OF_MONTH);
        int nowHourCount = calendar.get(Calendar.HOUR_OF_DAY);
        if (nowDayCount != dayCount) {
            //下一个日子
            Log.d(TAG, "autoMission: new day come!");
            todayMorning = false;
            todayNoon = false;
            todayAfter = false;
            todayEvening = false;
            todayBedTime = false;
            dayCount = nowDayCount;
        }
        if (nowHourCount > 8 && nowHourCount <= 10 && !todayMorning) {
            long delayMillis = randomTime(3, 7);
            autoCommand = new NekoMessage(packageName, new Message("system", "早上好", System.currentTimeMillis()));
            Log.d(TAG, "autoMission:todayMorning will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayMorning = true;
        }
        if (nowHourCount > 11 && nowHourCount <= 13 && !todayNoon) {
            long delayMillis = randomTime(4, 15);
            autoCommand = new NekoMessage(packageName, new Message("system", "中午好", System.currentTimeMillis()));
            Log.d(TAG, "autoMission:todayNoon will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
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
            autoCommand = new NekoMessage(packageName, new Message("system", "下午好", System.currentTimeMillis()));
            Log.d(TAG, "autoMission:todayAfter will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayAfter = true;
        }
        if (nowHourCount > 19 && nowHourCount <= 22 && !todayEvening) {
            long delayMillis = randomTime(5, 15);
            Log.d(TAG, "autoMission:todayEvening will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            autoCommand = new NekoMessage(packageName, new Message("system", "晚上好", System.currentTimeMillis()));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayEvening = true;
        }
        if (nowHourCount > 22 && !todayBedTime) {
            long delayMillis = randomTime(5, 10);
            Log.d(TAG, "autoMission:todayBedTime will answer at " + dateTimeFormat.format(System.currentTimeMillis() + delayMillis));
            autoCommand = new NekoMessage(packageName, new Message("system", "晚安", System.currentTimeMillis()));
            mHandler.postDelayed(delayCheck, delayMillis);
            todayBedTime = true;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt: ");
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy: ");
        super.onDestroy();
        speakLeaveVoice();
    }

    // 检查队列的消息
    private void doSomething(AccessibilityNodeInfo source) {
        if (source == null)
            return;
        if (etInput == null)
            etInput = findInputNodeInfo(source);
        if (btSend == null)
            btSend = findSendNodeInfo(source);
        for (Command qa : waitQAs) {
            if (qa.getAnswer() == null || qa.getAnswer().getMessage() == null || (qa.sendSuccess() && qa.actionSuccess())) {
                continue;
            }

            // todo 也许需要添加一个开关，允许或者拒绝动作的执行
            if (qa.haveAction()) {
                if (BotApp.getInstance().getAdminName().equals(chatTitle)) {
                    if (doAction(source, qa)) {
                        return;
                    }
                } else {
                    if (doAction(source, qa)) {
                        return;
                    }
                }
            } else {
                if (etInput != null && btSend != null) {
                    etInput.refresh();
                    if(writeMessage(etInput, qa)){
                        btSend.refresh();
                        boolean result = clickButton(btSend, qa);
                        if(!result){
                            Log.e(TAG, "doSomething: ", new RuntimeException("点击发送按钮失败"));
                        }
                    }
                }
            }

            if (qa.sendSuccess()) {
                Log.d(TAG, "doSomething: send success:" + qa.getAnswer().getMessage());
            }
        }
    }

    public void speakMessage(String speakMessage) {
        if (speakMessage == null)
            return;
        if (speakMessage.isEmpty())
            return;
        mHandler.post(() -> {
            try {
                String rawMessage = speakMessage.replace("&", " ");
                MediaItem.Builder builder = new MediaItem.Builder();
                String path = String.format(Locale.US, "%s/generate_voice?text=%s&speaker_id=%d&translate=1",
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

    private void speakStartVoice(){
        // ご主人様,またあなたと出会いました。
//        MediaItem mediaItem = MediaItem.fromUri("file:///android_asset/start_voice_pcm_16.wav");
        MediaItem mediaItem = MediaItem.fromUri("asset:///start_voice_pcm_16.wav");
        mediaPlayer.setMediaItem(mediaItem);
        mediaPlayer.prepare();
        mediaPlayer.play();
    }

    private void speakLeaveVoice(){
        // ご主人様,またあなたと出会いました。
//        MediaItem mediaItem = MediaItem.fromUri("file:///android_asset/start_voice_pcm_16.wav");
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

    /***
     * 目前仅适用于 手机QQ v8.9.50.10650
     */
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
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        step.setDaley(0);
                        doSomething(getRootInActiveWindow());
                    }
                }, step.getDaley());
            } else {
                if (step.isGlobalAction()) {
                    result = performGlobalAction(step.getActionId());
                } else {
                    AccessibilityNodeInfo targetView = findFirstNodeInfo(source, step.getViewId());
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
                        Log.e(TAG, "doAction: 寻找视图失败" + step.getViewId());
                        // 为防止卡死在一条指令上面，设置一个30秒超时，超时会自动完成任务。
                        if(System.currentTimeMillis() - qa.getQuestion().getTimeStamp() > 30000){
                            result = true;
                            qa.finishStepAction();
                            qa.getAnswer().setMessage("doAction: 寻找视图超时！结束任务。");
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

    public boolean doGesture(AccessibilityNodeInfo targetView, Step step) {
        GestureDescription gb = step.getNeedGesture().onGesture(targetView);
        return dispatchGesture(gb, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.w(TAG, "dispatchGesture: onCompleted"+ step);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "dispatchGesture: onCancelled"+ step);
            }
        }, mHandler);
    }

    private boolean writeMessage(AccessibilityNodeInfo inputEditText, Command qaMessage) {
        if (!qaMessage.isWrite()) {
            Bundle arg = new Bundle();
            String atMessage = String.format("@%s %s", qaMessage.getQuestion().getSpeaker(), qaMessage.getAnswer().getMessage());
            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, atMessage);
            boolean result = inputEditText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg);
            qaMessage.setWrite(result);
        }
        return qaMessage.isWrite();
    }

    private boolean clickButton(AccessibilityNodeInfo sendButton, Command commandMessage) {
        if (!commandMessage.isSend() && commandMessage.isWrite()) {
            boolean result = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            commandMessage.setSend(result);
        }
        return commandMessage.isSend();
    }

    private @Nullable AccessibilityNodeInfo findInputNodeInfo(AccessibilityNodeInfo source) {
        return findFirstNodeInfo(source, chatPageViewIds.getInputViewId());
    }

    private @Nullable AccessibilityNodeInfo findSendNodeInfo(AccessibilityNodeInfo source) {
        return findFirstNodeInfo(source, chatPageViewIds.getSendBtnViewId());
    }

    private static @Nullable AccessibilityNodeInfo findFirstNodeInfo(AccessibilityNodeInfo source, String viewId) {
        List<AccessibilityNodeInfo> targets = source.findAccessibilityNodeInfosByViewId(viewId);
        AccessibilityNodeInfo inputNode = null;
        if (!targets.isEmpty()) {
            inputNode = targets.get(0);
        }
        return inputNode;
    }

    private Command findAddNewChatMessage(AccessibilityNodeInfo nodeInfo) {
        String botName = BotApp.getInstance().getBotName();
        Message lastMessage;
        // todo 只有聊天标题正确或许还不行，也许得未来得加验证
        boolean isAdmin = BotApp.getInstance().getAdminName().equals(chatTitle);
        if (isAdmin) {
            lastMessage = id2FindAdminLastMessage(nodeInfo);
        } else {
            lastMessage = id2FindGroupLastMessage(nodeInfo);
        }
        if (lastMessage.speaker == null || lastMessage.message == null) {
            return null;
        }
        if (isAdmin) {
            // 此处不要去验证$message.speaker,因为id2FindAdminLastMessage()中，speaker都填的是$AdminName
            if(isAtName(lastMessage,BotApp.getInstance().getAdminName())){
                Log.i(TAG, "findAddNewChatMessage:last is admin message!");
                return null;
            }
        } else {
            if (!isAtName(lastMessage,botName)) {
                return null;
            }
            if (botName.equals(lastMessage.speaker)) {
                return null;
            }
        }

        Log.i(TAG, "findAddNewChatMessage: " + lastMessage.getMessage());
        lastMessage.message = lastMessage.message.replace("@" + botName, "").trim();
        if (!waitQAs.isEmpty()) {
            Command last = waitQAs.get(waitQAs.size() - 1);
            if (last.getQuestion().equals(lastMessage)) {
                Log.i(TAG, "findAddNewChatMessage: last message is same!");
                return null;
            }
        }
        Log.i(TAG, String.format(Locale.US, "findAddNewChatMessage: %s:%s", lastMessage.speaker, lastMessage.message));
        BotApp.getInstance().insert(lastMessage);
        if (mode == OpenAiSession.class) {
            return new OpenAiMessage(nodeInfo.getPackageName().toString(), lastMessage, this);
        } else {
            return new NekoMessage(nodeInfo.getPackageName().toString(), lastMessage);
        }
    }

    private static boolean isAtName(Message message, String name) {
        if(message==null)
            return false;
        if (message.message.trim().startsWith("@" + name)) {
            return true;
        }
        if (message.message.trim().endsWith("@" + name)) {
            return true;
        }
        return message.message.contains("@" + name);
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
