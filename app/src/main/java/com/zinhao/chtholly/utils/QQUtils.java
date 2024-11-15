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

    public static final String UNKNOWN_PAGE = "com.tencent.mobileqq.unknown";
    public static final String MESSAGE_PAGE = "com.tencent.mobileqq.message_list";
    private static final String[] MESSAGE_PAGE_ID = new String[]{":id/ba1",":id/wjj",":id/wk0",":id/kbi",":id/eqe"};
    public static final String CHAT_PAGE = "com.tencent.mobileqq.chat";
    private static final String[] CHAT_PAGE_ID = new String[]{":id/title",":id/input",":id/gnt",":id/fun_btn"};
    private static final String DRAWER_PAGE = "com.tencent.mobileqq.chat_list_drawer";
    private static final String[] DRAWER_PAGE_ID = new String[]{"id/nyq","id/nz1","id/nx0","id/r2_","id/ny3","id/nxp","id/nz4","id/r1c","id/nyr","id/nyt","id/nxb","id/ny2","id/ny7","id/nyl","id/nxy","id/nxc"};
    private static final String MY_FRIENDS_PAGE = "com.tencent.mobileqq.my_friends_page";
    private static final String[] MY_FRIENDS_PAGE_ID = new String[]{"id/conversation_head","id/contact_count","id/i5l","id/ehy","id/ba0","id/group_item_layout","id/whv","id/qb_troop_list_view","id/text1","id/b8d","id/elv_buddies","id/ivTitleBtnRightImage","id/ba1","id/ivTitleName","id/kbi","id/k8u","id/khc","id/eqg","id/f7j","id/ukg","id/fmq","id/e3u","id/ixv","id/g49","id/i5m","id/rm","id/j_k","id/kmr","id/rl","id/s4t"};
    private static final String NEW_FRIENDS_PAGE = "com.tencent.mobileqq.new_friends_page";
    private static final String[] NEW_FRIENDS_PAGE_ID = new String[]{"id/c5","id/epz","id/i95","id/nickname","id/ls0","id/o_8","id/i8s","id/close","id/iz7","id/i30","id/iz","id/title","id/ibi","id/i92","id/a_9","id/m_d"};
    private static final String SEARCH_PAGE = "com.tencent.mobileqq.search_page";
    private static final String[] SEARCH_PAGE_ID = new String[]{"id/bl9","id/ble","id/kbs","id/blc","id/ujx","id/bl8","id/uk_","id/bld"};
    private static final String SEARCH_RESULT_PAGE = "com.tencent.mobileqq.search_result_page";
    private static final String[] SEARCH_RESULT_PAGE_ID = new String[]{"id/title","id/text1","id/f_u","id/bgt","id/text2","id/io1","id/io2","id/j64"};

    public static boolean hasAllId(AccessibilityNodeInfo nodeInfo,String... ids){
        for (String s : ids)
        {
            if(!s.startsWith(":")){
                s= ":"+s;
            }
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
        }else if(hasAllId(root,CHAT_PAGE_ID)){
            return CHAT_PAGE;
        }else if(hasAllId(root,DRAWER_PAGE_ID)){
            return DRAWER_PAGE;
        }else if(hasAllId(root,MY_FRIENDS_PAGE_ID)){
            return MY_FRIENDS_PAGE;
        }else if(hasAllId(root,NEW_FRIENDS_PAGE_ID)){
            return NEW_FRIENDS_PAGE;
        } else if(hasAllId(root,SEARCH_PAGE_ID)){
            return SEARCH_PAGE;
        }else if(hasAllId(root,SEARCH_RESULT_PAGE_ID)){
            return SEARCH_RESULT_PAGE;
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
