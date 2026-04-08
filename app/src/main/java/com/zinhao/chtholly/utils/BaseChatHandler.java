package com.zinhao.chtholly.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;

import java.util.List;

public abstract class BaseChatHandler {
    public final static String TAG = "BaseChatHandler";
    public static final String UNKNOWN_PAGE = "unknown page";
    protected abstract boolean isAtName(Message message,String name);
    protected MessageCallback messageCallback;

    public abstract void handle(AccessibilityEvent event);
    public abstract String getPackageName();
    public abstract boolean writeAndSend(Command command);
    public abstract String beforeWriteMessage(Command command);
    protected AccessibilityNodeInfo etInputNode;
    protected AccessibilityNodeInfo btSendNode;

    // 获取指定包名的版本信息
    public static String getAppVersion(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            return packageInfo.versionName; // 或 versionCode
        } catch (PackageManager.NameNotFoundException e) {
            return null; // 未安装
        }
    }

    public static int getAppVersionCode(Context context,String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            return packageInfo.versionCode; // 或 versionCode
        } catch (PackageManager.NameNotFoundException e) {
            return 0; // 未安装
        }
    }

    public BaseChatHandler(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public boolean writeMessage(Command qaMessage) {
        if(qaMessage.getAnswer().getMessage() == null){
            return true;
        }
        if(etInputNode==null){
            return false;
        }
        if (!qaMessage.isWrite()) {
            if(etInputNode.isEditable()){
                Bundle arg = new Bundle();
                String sendMessage = beforeWriteMessage(qaMessage);
                Log.i(TAG,"writeMessage:"+sendMessage);
                arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, sendMessage);
                boolean result = etInputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg);
                if(!result){
//                    Log.e("MyAccessibilityService", "Failed to set text directly.");
                    // 如果直接设置文本失败，可以逐个字符发送输入事件
                    for (char c : sendMessage.toString().toCharArray()) {
                        sendCharacter(c,etInputNode);
                    }
                }
                qaMessage.setWrite(result);
            }else {
                NekoChatService.getInstance().addLogcat("isEditable false");
            }
        }
        return qaMessage.isWrite();
    }

    private void sendCharacter(char c,AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            // 发送字符的逻辑可以通过模拟按键实现
            // 这里我们需要使用 AccessibilityService 模拟输入
            // 但是 Android 的无障碍 API 本身不支持直接模拟按键
            // 所以我们可以通过在输入框中添加字符的方式间接实现

            // 这里可以使用 performAction 添加字符
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, String.valueOf(c));
            boolean success = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (!success) {
                Log.e("MyAccessibilityService", "Failed to send character: " + c);
            }
        }
    }

    public boolean pasteMessage(Command qaMessage){
        if(etInputNode.isEditable()){
            boolean result= etInputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            qaMessage.setWrite(result);
        }else {
            NekoChatService.getInstance().addLogcat("isEditable false");
        }
        return qaMessage.isWrite();
    }

// ==================================以下是公共静态方法
    public static boolean clickButton(AccessibilityNodeInfo sendButton, Command commandMessage) {
        boolean result = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        commandMessage.setSend(result);
        return commandMessage.isSend();
    }

    public static @Nullable AccessibilityNodeInfo findFirstNodeInfo(AccessibilityNodeInfo source, String viewId) {
        return findIndexInTargetNodeChildren(source,viewId,0);
    }

    public static @Nullable AccessibilityNodeInfo findIndexInTargetNodeChildren(AccessibilityNodeInfo source, String viewId, int position) {
        if(viewId == null){
            return null;
        }
        if(source == null){
            return null;
        }
        if(viewId.equals(source.getViewIdResourceName())){
            return source;
        }
        List<AccessibilityNodeInfo> targets = source.findAccessibilityNodeInfosByViewId(viewId);
        AccessibilityNodeInfo target = null;
        if (!targets.isEmpty()) {
            if(position<targets.size()){
                target = targets.get(position);
            }
        }
        return target;
    }

    public static @Nullable AccessibilityNodeInfo findFirstTextInTargetNodeChildren(AccessibilityNodeInfo source, String text,String viewId) {
        List<AccessibilityNodeInfo> targets = source.findAccessibilityNodeInfosByText(text);
        FileLogger.INSTANCE.i(TAG,"findFirstTextInTargetNodeChildren: "+text + " ,len:"+targets.size());
        AccessibilityNodeInfo target = null;
        if (!targets.isEmpty()) {
            target = targets.get(0);
            if(target == null){
                return null;
            }
            if(viewId!=null){
                if(!viewId.equals(target.getViewIdResourceName())){
                    FileLogger.INSTANCE.i(TAG,"findFirstTextInTargetNodeChildren: "+viewId + " ,find:"+target.getViewIdResourceName());
                    return null;
                }
            }
        }
        return target;
    }

    public static boolean hasAllId(AccessibilityNodeInfo nodeInfo,String... ids){
        for (String s : ids)
        {
            if(s.startsWith(nodeInfo.getPackageName().toString())){
                s = s.replace(nodeInfo.getPackageName().toString(),"");
            }
            if(!s.startsWith(":") ){
                s = ":"+s;
            }
            List<AccessibilityNodeInfo> nodeInfoList = nodeInfo
                    .findAccessibilityNodeInfosByViewId(nodeInfo.getPackageName() + s);
            if(nodeInfoList.isEmpty()){
                return false;
            }
        }
        return true;
    }
}

