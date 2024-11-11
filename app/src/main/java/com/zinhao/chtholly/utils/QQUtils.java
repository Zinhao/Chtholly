package com.zinhao.chtholly.utils;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.entity.Message;

import java.util.List;
import java.util.Locale;

public class QQUtils {
    private static final String TAG = "QQUtils";
    public static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";
    public static final String MESSAGE_PAGE = "com.tencent.mobileqq.message_list";
    public static final String UNKNOWN_PAGE = "com.tencent.mobileqq.unknown";
    // 提示信息
    public static final String QQ_TIP_MESSAGE_ID = ":id/graybar";
    public static final String QQ_RL_TITLE_ID = ":id/rlCommenTitle";

    private static Message emptyMessage;

    public static Message id2FindGroupLastMessage(AccessibilityNodeInfo nodeInfo){
        emptyMessage = new Message(null,null,System.currentTimeMillis());
        List<AccessibilityNodeInfo> nickNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatNickId());
        List<AccessibilityNodeInfo> messageNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatTextId());
        if(nickNodes.size() == messageNodes.size() && !nickNodes.isEmpty()){
            Log.i(TAG, String.format(Locale.US,"id2FindGroupLastMessage: nikc:%d m:%d ====================================>",nickNodes.size(),messageNodes.size()));
            for (int i = 0; i < Math.min(messageNodes.size(),nickNodes.size()); i++) {
                AccessibilityNodeInfo n = nickNodes.get(i);
                AccessibilityNodeInfo m = messageNodes.get(i);
                Log.i(TAG, String.format(Locale.CHINA,"id2FindGroupLastMessage: nick:%s : %s",n.getText(),m.getText()));
            }
            int lastIndex = nickNodes.size()-1;
            emptyMessage.setSpeaker(nickNodes.get(lastIndex).getText()+"");
            emptyMessage.setMessage(messageNodes.get(lastIndex).getText()+"");
            emptyMessage.setNodeInfo(messageNodes.get(lastIndex));
        }else{
            Log.e(TAG, String.format(Locale.US,"id2FindGroupLastMessage: nikc:%d m:%d err ============>",nickNodes.size(),messageNodes.size()));
        }
        return emptyMessage;
    }

    public static Message id2FindAdminLastMessage(AccessibilityNodeInfo nodeInfo){
        emptyMessage = new Message(null,null,System.currentTimeMillis());
        List<AccessibilityNodeInfo> messageNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatTextId());
        if(messageNodes.isEmpty())
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

    private static final String[] MESSAGE_PAGE_ID = new String[]{":id/ba1",":id/wjj",":id/wk0",":id/kbi",":id/eqe"};

    public static boolean hasAllId(AccessibilityNodeInfo nodeInfo,String... ids){
        for (String s : ids) {
            List<AccessibilityNodeInfo> nodeInfoList = nodeInfo
                    .findAccessibilityNodeInfosByViewId(nodeInfo.getPackageName() + s);
            if(nodeInfoList.isEmpty()){
                return false;
            }
        }
        return true;
    }

    public static String checkWhatPage(AccessibilityNodeInfo root){
        if(hasAllId(root,MESSAGE_PAGE_ID)){
            return MESSAGE_PAGE;
        }
        return UNKNOWN_PAGE;
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

    // 图片的选择框
    public static final String[] PIC_CHECKBOX_IDS  = new String[]{
            ":id/qdf",
            ":id/qhq",
    };

    // 发送按钮
    public static final String[] SEND_BTN_IDS  = new String[]{
            ":id/send_btn",
            ":id/fun_btn",
    };

}
