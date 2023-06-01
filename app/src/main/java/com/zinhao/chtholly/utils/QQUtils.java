package com.zinhao.chtholly.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.BuildConfig;
import com.zinhao.chtholly.entity.Message;

import java.util.List;
import java.util.Locale;

public class QQUtils {
    private static final String TAG = "QQUtils";
    public static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";
    // 提示信息
    public static final String QQ_TIP_MESSAGE_ID = ":id/graybar";
    public static final String QQ_RL_TITLE_ID = ":id/rlCommenTitle";

    private static Message emptyMessage;
    private static StringBuilder builder;
    private static final Rect bound = new Rect();

    public static Message id2FindGroupLastMessage(AccessibilityNodeInfo nodeInfo){
        emptyMessage = new Message(null,null,System.currentTimeMillis());
        List<AccessibilityNodeInfo> nickNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatNickId());
        List<AccessibilityNodeInfo> messageNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatTextId());
        if(nickNodes.size() == messageNodes.size() && nickNodes.size()!=0){
            Log.d(TAG, String.format(Locale.US,"id2FindGroupLastMessage: nikc:%d m:%d ============>",nickNodes.size(),messageNodes.size()));
            int lastIndex = nickNodes.size()-1;
            emptyMessage.setSpeaker(nickNodes.get(lastIndex).getText() + "");
            emptyMessage.setMessage(messageNodes.get(lastIndex).getText() + "");
            return emptyMessage;
        }else{
            Log.e(TAG, String.format(Locale.US,"id2FindGroupLastMessage: nikc:%d m:%d err ============>",nickNodes.size(),messageNodes.size()));
        }
        for (int i = 0; i < Math.min(messageNodes.size(),nickNodes.size()); i++) {
            AccessibilityNodeInfo n = nickNodes.get(i);
            AccessibilityNodeInfo m = messageNodes.get(i);
            Log.d(TAG, String.format(Locale.CHINA,"id2FindGroupLastMessage: nick:%s : %s",n.getText(),m.getText()));
        }
        return emptyMessage;
    }

    public static Message id2FindLastMessage(AccessibilityNodeInfo nodeInfo){
        emptyMessage = new Message(null,null,System.currentTimeMillis());
        List<AccessibilityNodeInfo> messageNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatTextId());
        if(messageNodes.size()==0)
            return emptyMessage;
        for (int i = 0; i < messageNodes.size(); i++) {
            AccessibilityNodeInfo m = messageNodes.get(i);
            Log.d(TAG, String.format(Locale.CHINA,"id2FindLastMessage: : %s",m.getText()));
        }
        int lastIndex = messageNodes.size()-1;
        emptyMessage.setSpeaker(BotApp.getInstance().getAdminName());
        emptyMessage.setMessage(messageNodes.get(lastIndex).getText() + "");
        return emptyMessage;
    }

    public static Message treeFindLastMessage(AccessibilityNodeInfo nodeInfo, int treeIndex){
        if(treeIndex == 0){
            emptyMessage = new Message(null,null,System.currentTimeMillis());
            builder = new StringBuilder();
            if(BuildConfig.DEBUG){
                Log.d(TAG, "treeInfo=========================================================");
            }
        }else{
            builder.append("|——");
        }
        if(BuildConfig.DEBUG){
            nodeInfo.getBoundsInScreen(bound);
            Log.d(TAG, String.format(Locale.US,"treeInfo:%s%s class:%s, desc:%s bound:%s click:%s",
                    builder,nodeInfo.getViewIdResourceName(),nodeInfo.getClassName(),nodeInfo.getText(),
                    bound,nodeInfo.isClickable()));
        }
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = nodeInfo.getChild(i);
            if(child == null)
                continue;
            if(child.getChildCount()!=0){
                treeFindLastMessage(child,treeIndex+1);
            }else {
                if(getChatTextId().equals(child.getViewIdResourceName())){
                    // 这是一条聊天记录
                    if(child.getText() ==null)
                        continue;
                    emptyMessage.message = child.getText().toString();
                }
                if(getChatNickId().equals(child.getViewIdResourceName())){
                    if(child.getText() == null)
                        continue;
                    emptyMessage.speaker = child.getText().toString();
                }
                if(BuildConfig.DEBUG){
                    child.getBoundsInScreen(bound);
                    Log.d(TAG, String.format(Locale.US,"treeInfo:%s%s class:%s, desc:%s bound:%s click:%s desc:%s",
                            builder,child.getViewIdResourceName(),child.getClassName(),child.getText(),
                            bound,child.isClickable(),child.describeContents()));
                }
            }
        }
        if(builder.length()>=3)
            builder.delete(builder.length()-3,builder.length());
        return emptyMessage;
    }

    //qq version code
    public static int versionCode = 3898;

    public static String getChatTitleId(){
        return QQ_PACKAGE_NAME + ":id/title";
    }

    public static String getChatNickId(){
        return QQ_PACKAGE_NAME + ":id/chat_item_nick_name";
    }
    public static String getChatTextId(){
        return QQ_PACKAGE_NAME + ":id/chat_item_content_layout";
    }
    public static String getNewUserId(){
        return QQ_PACKAGE_NAME + ":id/ae0";
    }
    public static String getInputId(){
        return QQ_PACKAGE_NAME + ":id/input";
    }
    public static String getPicButtonId(){
        switch (versionCode){
            case 3898: return QQ_PACKAGE_NAME + ":id/gnt";
        }
        return null;
    }

    public static String getPicCheckBoxId(){
        switch (versionCode){
            case 3898: return QQ_PACKAGE_NAME + ":id/qhq";
        }
        return null;
    }

    // 发送按钮
    public static String getSendButtonId(){
        switch (versionCode){
            case 3898: return QQ_PACKAGE_NAME + ":id/fun_btn";
            default: return QQ_PACKAGE_NAME + ":id/send_btn";
        }
    }
}
