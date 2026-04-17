package com.zinhao.chtholly.utils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.RequiresApi;
import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.NekoChatService;
import com.zinhao.chtholly.entity.Command;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.entity.Step;

import java.util.*;

public class QQChatHandler extends BaseChatHandler {
    private static final String TAG = "QQChatHandler";
    public static final String PACKAGE_NAME = "com.tencent.mobileqq";

    protected final List<Message> messageList = new Vector<>();

    protected String currentPageName;
    private String targetChatTitle = null;
    protected String chatTitle;
    private final String versionName;
    private int versionCode = 0;

    public QQChatHandler(Context context, MessageCallback messageCallback) {
        super(messageCallback);
        versionName = getAppVersion(context,PACKAGE_NAME);
        versionCode = getAppVersionCode(context,PACKAGE_NAME);
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    @Override
    public boolean writeAndSend(Command qa) {
        if(qa.getAnswer().getMessage() == null || qa.getAnswer().getMessage().isEmpty()){
            return true;
        }
        if (etInputNode != null && btSendNode != null) {
            etInputNode.refresh();
            if (writeMessage(qa)) {
                btSendNode.refresh();
                boolean result = BaseChatHandler.clickButton(btSendNode, qa);
                if (!result) {
                    if(NekoChatService.getInstance()!=null){
                        NekoChatService.getInstance().addLogcat("writeAndSend: id[" + btSendNode.getViewIdResourceName() + ']'+"点击发送按钮失败");
                    }
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public String beforeWriteMessage(Command command) {
        if(command.getQuestion().getSpeaker() == null || command.getQuestion().getSpeaker().isEmpty()){
            return command.getAnswer().getMessage();
        }
        return String.format("@%s %s", command.getQuestion().getSpeaker(), command.getAnswer().getMessage());
    }

    public void findLastMessage(AccessibilityNodeInfo nodeInfo){
        if(messageCallback == null){
            return;
        }
        String botName = BotApp.getInstance().getBotName();
        boolean isPersonal = !nodeInfo.findAccessibilityNodeInfosByViewId(getPackageName() + ":id/title_sub").isEmpty();
        Message hitMessage = null;
        if (isPersonal) {
            hitMessage = id2FindPersonLastMessage(nodeInfo);
        } else {
            hitMessage = id2FindGroupLastMessage(nodeInfo);
        }
        if(hitMessage == null){
            return;
        }
        if (hitMessage.speaker == null || hitMessage.message == null) {
            return;
        }
        if (isPersonal) {
            // 此处不要去验证$message.speaker,因为id2FindAdminLastMessage()中，speaker都填的是$AdminName
            if (isAtName(hitMessage, BotApp.getInstance().getAdminName())) {
                Log.i(TAG, "findAddNewChatMessage:last is @admin message!");
                return;
            }
        } else {
            if (!isAtName(hitMessage, botName)) {
                if(!hitMessage.isOther()){
                    return;
                }
            }
            if (botName.equals(hitMessage.speaker)) {
                return;
            }
        }

        hitMessage.message = hitMessage.message.replace("@" + botName, "").trim();
        if (!messageList.isEmpty()) {
            Message last = messageList.get(messageList.size() - 1);
            if (last.message.equals(hitMessage.message) && System.currentTimeMillis() - last.getTimeStamp() < 10000) {
                Log.d(TAG, "findLastMessage: in close time, same message:"+last.message);
                //in close time, same message
                return;
            }
        }
        if("群主".equals(hitMessage.tag) || "管理员".equals(hitMessage.tag)) {
            hitMessage.setEnableCommand(true);
        }
        if(NekoChatService.getInstance()!=null){
            NekoChatService.getInstance().addLogcat(
                    String.format(Locale.US, "✨findAddNewChatMessage: %s", hitMessage));
        }
        BotApp.getInstance().insert(hitMessage);
        messageList.add(hitMessage);
        messageCallback.onFind(hitMessage);
    }

    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public boolean isAtName(Message message, String name) {
        if (message == null)
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
    public void handle(AccessibilityEvent event) {
        if(event == null)
            return;
        if(event.getSource() == null)
            return;
        String pageName =checkWhatPage(event.getSource());
        currentPageName = pageName;
        if(QQChatHandler.UPDATE_DIALOG_PAGE.equals(pageName)){
            Log.d(TAG, "handle: is update dialog");
            AccessibilityNodeInfo closeBtn = findFirstNodeInfo(event.getSource(),getPackageName()+":id/x3c");
            if(closeBtn!=null){
                closeBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return;
            }
        }
        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) {
            if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
                // chat 文本消息
                if (QQChatHandler.CHAT_GROUP.equals(pageName)) {
                    updatePageNeedNode(event.getSource());
                    findLastMessage(event.getSource());
                }
            }
        } else {
            if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
//                拍一拍，欢迎消息，撤回消息
                if (QQChatHandler.CHAT_GROUP.equals(pageName)) {
                    updatePageNeedNode(event.getSource());
                    findLastMessage(event.getSource());
                }
            }
        }
    }

    public String getCurrentPageName() {
        return currentPageName;
    }

    private List<Message> a6b2FindGroupAllMessage(AccessibilityNodeInfo nodeInfo){
        List<Message> allMessages = new ArrayList<>();
        List<AccessibilityNodeInfo> messageItemList = nodeInfo.findAccessibilityNodeInfosByViewId(getChatMessageItemRootId());
        for (int i = 0; i < messageItemList.size(); i++) {
            AccessibilityNodeInfo messageItem = messageItemList.get(i);
            Message emptyMessage = new Message(null,null,System.currentTimeMillis());
            for (int j = 0; j < messageItem.getChildCount(); j++) {
                AccessibilityNodeInfo messageItemChild = messageItem.getChild(j);
                if(messageItemChild == null){
                    continue;
                }
                if(getChatMessageTimeStampId().equals(messageItemChild.getViewIdResourceName())){
                    //ab6[0] = chat_item_time_stamp[text] = 23:02
                }
                if(getChatMessageSpeakerInfoId().equals(messageItemChild.getViewIdResourceName())){
                    // ab6[2] = nbt[0]["desc"]= 成员等级
                    // ab6[2] = nbt[1]["desc"] = 6
                    // ab6[2] = nbt[2]["text"] = 群主
                    for (int k = 0; k < messageItemChild.getChildCount(); k++) {
                        AccessibilityNodeInfo nbtChild = messageItemChild.getChild(k);
                        if("android.widget.ImageView".contentEquals(nbtChild.getClassName()) && k!=0){
                            String desc = nbtChild.getContentDescription().toString();
                            int leve = 0;
                            try {
                                leve= Integer.parseInt(desc) ;
                            }catch (NumberFormatException e){
                                e.printStackTrace();
                            }
                            emptyMessage.setLeve(leve);
                        }
                        if("android.widget.TextView".contentEquals(nbtChild.getClassName()) && k!=0){
                            String tag = nbtChild.getText().toString();
                            emptyMessage.setTag(tag);
                        }
                    }
                }
                if(getChatNickId().equals(messageItemChild.getViewIdResourceName())){
                    // ab6[3] = chat_item_nick_name[text] = 发言人
                    CharSequence textOrNull = messageItemChild.getText();
                    if(textOrNull != null){
                        emptyMessage.setSpeaker(textOrNull.toString());
                    }
                }
                if(getChatTextId().equals(messageItemChild.getViewIdResourceName())){
                    // ab6[4] = chat_item_content_layout[text] = 消息正文
                    CharSequence textOrNull = messageItemChild.getText();
                    if(textOrNull != null){
                        emptyMessage.setMessage(textOrNull.toString());
                    }
                }
            }
            allMessages.add(emptyMessage);
        }
        return allMessages;
    }

    public Message id2FindGroupLastMessage(AccessibilityNodeInfo nodeInfo){
        Message grayBarHitMessage = grayBarMessage(nodeInfo);
        if(grayBarHitMessage!=null){
            return grayBarHitMessage;
        }
        List<Message> a6bMessageList = a6b2FindGroupAllMessage(nodeInfo);
        if(!a6bMessageList.isEmpty()){
            return a6bMessageList.get(a6bMessageList.size()-1);
        }
        return null;
    }

    private Message grayBarMessage(AccessibilityNodeInfo nodeInfo){
        /**
         * {
         *       "class": "android.widget.LinearLayout",
         *       "click": false,
         *       "longClick": false,
         *       "children": [
         *         {
         *           "id": "com.tencent.mobileqq:id\/graybar",
         *           "class": "android.widget.TextView",
         *           "click": true,
         *           "longClick": true,
         *           "desc": "景皓戳了戳你",
         *           "text": "景皓icon戳了戳你"
         *         }
         *       ]
         *     }
         */
        AccessibilityNodeInfo listView1 = null;
        if(getChatListView1Id().equals(nodeInfo.getViewIdResourceName())){
            listView1 = nodeInfo;
        }else{
            List<AccessibilityNodeInfo> listView1List = nodeInfo.findAccessibilityNodeInfosByViewId(getChatListView1Id());
            if(listView1List!=null && !listView1List.isEmpty()){
                listView1 = listView1List.get(0);
            }
        }
        if(listView1 == null){
            return null;
        }
        int listView1Len = listView1.getChildCount();
        AccessibilityNodeInfo grayBarLayout = listView1.getChild(listView1Len-1);
        if("android.widget.LinearLayout".contentEquals(grayBarLayout.getClassName())){
            for (int i = 0; i < grayBarLayout.getChildCount(); i++) {
                AccessibilityNodeInfo grayBar = grayBarLayout.getChild(i);
                if("com.tencent.mobileqq:id/graybar".equals(grayBar.getViewIdResourceName())){
                    return new Message("",grayBar.getText().toString(),System.currentTimeMillis(),true);
                }
            }
        }
        return null;
    }

    public void setTargetChatTitle(String targetChatTitle) {
        this.targetChatTitle = targetChatTitle;
    }
    public String getTargetChatTitle() {
        return targetChatTitle;
    }

    public String getChatTitle() {
        return chatTitle;
    }


    public final ChatPageViewIds chatPageViewIds = new ChatPageViewIds();
    public String getSendButtonId(){
        return SEND_BTN_IDS[1];
    }
    private void updatePageNeedNode(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            Log.e(TAG, "initChatPage:nodeInfo null!");
            return;
        }
        if (!PACKAGE_NAME.equals(nodeInfo.getPackageName().toString())) {
            Log.e(TAG, "initChatPage:only support mobile qq!");
            return;
        }
        //输入文本框id
        AccessibilityNodeInfo input = findFirstNodeInfo(nodeInfo, getInputId());
        chatPageViewIds.setInputViewId(getInputId());

        // 发送按钮id
        AccessibilityNodeInfo send;
        send = findFirstNodeInfo(nodeInfo, nodeInfo.getPackageName() +getSendButtonId());
        chatPageViewIds.setSendBtnViewId(getSendButtonId());

        // 聊天标题id
        AccessibilityNodeInfo title = findFirstNodeInfo(nodeInfo, getChatTitleId());
        chatPageViewIds.setTitleViewId(getChatTitleId());

        // 确认 选择第一张图片的选择框id
        chatPageViewIds.setFirstPicCheckBoxViewId(PIC_CHECKBOX_IDS[1]);

        if (input == null || send == null) {
            Log.d(TAG, "updatePageNeedNode:is not a chat page");
            return;
        }
        etInputNode = input;
        btSendNode = send;
        if (title != null) {
            /***
             * 聊天信息
             */
            CharSequence charSequence = title.getText();
            if(charSequence!=null){
                chatTitle = charSequence.toString();
                if(targetChatTitle == null){
                    targetChatTitle = chatTitle;
                }
                if(NekoChatService.getInstance()!=null){
                    NekoChatService.getInstance().addLogcat("current chat page:" + chatTitle);
                }
            }
        }
    }



    public AccessibilityNodeInfo getBtSendNode() {
        return btSendNode;
    }

    public AccessibilityNodeInfo getEtInputNode() {
        return etInputNode;
    }

    public ChatPageViewIds getChatPageViewIds() {
        return chatPageViewIds;
    }

    public Message id2FindPersonLastMessage(AccessibilityNodeInfo nodeInfo){
        List<AccessibilityNodeInfo> messageNodes = nodeInfo.findAccessibilityNodeInfosByViewId(getChatTextId());
        if(messageNodes.isEmpty())
            return null;

        for (int i = 0; i < messageNodes.size(); i++) {
            AccessibilityNodeInfo m = messageNodes.get(i);
//            Log.d(TAG, String.format(Locale.CHINA,"id2FindLastMessage: : %s",m.getText()));
        }
        int lastIndex = messageNodes.size()-1;
        AccessibilityNodeInfo lastNodeInfo = messageNodes.get(lastIndex);
        CharSequence text = lastNodeInfo.getText();
        if(text != null){
            Message emptyMessage = new Message(null,null,System.currentTimeMillis());
            emptyMessage.setSpeaker(BotApp.getInstance().getAdminName());
            emptyMessage.setMessage(text.toString());
            return emptyMessage;
        }else {
            return null;
        }
    }

    public static final String NULL_ROOT = "null_root";
    public static final String MESSAGE_PAGE = "com.tencent.mobileqq.message_list";
    private static final String[] MESSAGE_PAGE_ID = new String[]{":id/ba1",":id/wjj",":id/wk0",":id/kbi",":id/eqe"};

    public static final String CHAT_PERSON = "com.tencent.mobileqq.person_chat";
    public static final String CHAT_GROUP = "com.tencent.mobileqq.group_chat";
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

    private static final String UPDATE_DIALOG_PAGE = "com.tencent.mobileqq.update_page";
    private static final String[] UPDATE_DIALOG_PAGE_ID = new String[]{"id/x3e","id/x3h","id/x3c","id/x3i","id/x3d","id/x3j"};

    public String checkWhatPage(AccessibilityNodeInfo root){
        if(root == null){
            return NULL_ROOT;
        }
        if(hasAllId(root,MESSAGE_PAGE_ID)){
            return MESSAGE_PAGE;
        }else if(hasAllId(root,DRAWER_PAGE_ID)){
            return DRAWER_PAGE;
        }else if(hasAllId(root,CHAT_PAGE_ID)){
            return CHAT_GROUP;
        }else if(hasAllId(root,MY_FRIENDS_PAGE_ID)){
            return MY_FRIENDS_PAGE;
        }else if(hasAllId(root,NEW_FRIENDS_PAGE_ID)){
            return NEW_FRIENDS_PAGE;
        } else if(hasAllId(root,SEARCH_PAGE_ID)){
            return SEARCH_PAGE;
        }else if(hasAllId(root,SEARCH_RESULT_PAGE_ID)){
            return SEARCH_RESULT_PAGE;
        }else if(hasAllId(root,UPDATE_DIALOG_PAGE_ID)){
            return UPDATE_DIALOG_PAGE;
        }
        return UNKNOWN_PAGE;
    }


    public String getChatMessageItemRootId(){
        // ab6[0] = chat_item_time_stamp[text] = 23:02

        // ab6[2] = nbt[0]["desc"]= 成员等级
        // ab6[2] = nbt[1]["desc"] = 6
        // ab6[2] = nbt[2]["text"] = 群主

        // ab6[3] = chat_item_nick_name[text] = 发言人
        // ab6[4] = chat_item_content_layout[text] = 消息正文
        return PACKAGE_NAME + ":id/a6b";
    }

    public String getChatTitleId(){
        return PACKAGE_NAME + ":id/title";
    }

    public static String getChatMessageTimeStampId(){return PACKAGE_NAME + ":id/chat_item_time_stamp";}
    public static String getChatMessageSpeakerInfoId(){return PACKAGE_NAME + ":id/nbt";}
    public static String getChatNickId(){
        return PACKAGE_NAME + ":id/chat_item_nick_name";
    }
    public static String getChatTextId(){
        return PACKAGE_NAME + ":id/chat_item_content_layout";
    }
    public String getChatListView1Id(){
        return PACKAGE_NAME + ":id/listView1";
    }

    public static String getNewUserId(){
        return PACKAGE_NAME + ":id/ae0";
    }
    public String getInputId(){
        return PACKAGE_NAME + ":id/input";
    }
    private String getPicButtonId(){
        return PACKAGE_NAME + ":id/gnt";
    }

    // 图片的选择框
    public static final String[] PIC_CHECKBOX_IDS  = new String[]{
            ":id/qdf",
            ":id/qhq",
    };

    // 发送按钮
    public static final String[] SEND_BTN_IDS  = new String[]{
            ":id/send_btn",//   图片发送按钮
            ":id/fun_btn",//  文字发送按钮
    };


    public List<Step> shareScreen() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(PACKAGE_NAME,":id/gny", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        steps.add(new Step(PACKAGE_NAME,":id/icon_viewPager", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500,true,new int[]{0,3}));
        steps.add(new Step(PACKAGE_NAME, ":id/dialogRightBtn", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(PACKAGE_NAME, ":id/bbt", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,1500));
        // 关闭扬声器
        steps.add(new Step(PACKAGE_NAME, ":id/g71", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        // menu
//        steps.add(new Step(QQChatHandler.QQ_PACKAGE_NAME, ":id/sp5", AccessibilityNodeInfo.ACTION_CLICK,false,500));
        // 分享屏幕
//        steps.add(new Step(QQChatHandler.QQ_PACKAGE_NAME, ":id/i4o", AccessibilityNodeInfo.ACTION_CLICK,false,500,true,new int[]{2}));
        // 小窗
        steps.add(new Step(PACKAGE_NAME, ":id/g76", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,2500));
        return steps;
    }

    public List<Step> videoCall(boolean mainCamera) {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/gny", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/icon_viewPager", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500,true,new int[]{0,1}));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, ":id/bbt", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,300));
        if(mainCamera){
            //切换后置摄像头
            steps.add(new Step(QQChatHandler.PACKAGE_NAME, ":id/gd7", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,4000));
        }
        //小窗
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, ":id/g76", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        return steps;
    }

    public List<Step> recordVideo() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/go6", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        // 录像
        Step gestureStep = new Step(QQChatHandler.PACKAGE_NAME +".aelight_impl",":id/a74",AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,Step.ActionType.custom,500);
        gestureStep.setNeedGesture(Command.PRESS_10S);
        steps.add(gestureStep);
        //发送
        steps.add(new Step(QQChatHandler.PACKAGE_NAME +".aelight_impl",":id/ut", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,15000));
        return steps;
    }

    public List<Step> takePhoto() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/go6", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        //打开闪光灯
        steps.add(new Step(QQChatHandler.PACKAGE_NAME +".aelight_impl",":id/py", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        // 切换前置
//                steps.add(new Step(QQUtils.QQ_PACKAGE_NAME+".aelight_impl",":id/pv", AccessibilityNodeInfo.ACTION_CLICK,false,500));
        // 拍照
        Step gestureStep = new Step(QQChatHandler.PACKAGE_NAME +".aelight_impl",":id/a74",AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,Step.ActionType.custom,500);
        gestureStep.setNeedGesture(Command.CLICK);
        steps.add(gestureStep);
        //发送
        steps.add(new Step(QQChatHandler.PACKAGE_NAME +".aelight_impl",":id/ut", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,1500));
        return steps;
    }

    public List<Step> sendNewestPic() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, chatPageViewIds.getFirstPicCheckBoxViewId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,300));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, chatPageViewIds.getSendBtnViewId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,300));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,300));
        return steps;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    public List<Step> screenShot() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(null,null, AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,Step.ActionType.global));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, chatPageViewIds.getFirstPicCheckBoxViewId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, chatPageViewIds.getSendBtnViewId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        return steps;
    }

    public List<Step> everyDayCheck() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/qn4",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/nfz",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(null,null,AccessibilityService.GLOBAL_ACTION_BACK,Step.ActionType.global,500));
        return steps;
    }

    public List<Step> switchChatNow(int position) {
        List<Step> steps = new Vector<>();
        steps.add(new Step(null,null,AccessibilityService.GLOBAL_ACTION_BACK,Step.ActionType.global));

        Step clickChatItem = new Step(QQChatHandler.PACKAGE_NAME,":id/recent_chat_list", AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,1500,true,new int[]{position+1});
        clickChatItem.setNeedHasId(":id/relativeItem");
        steps.add(clickChatItem);
        return steps;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    public List<Step> sendGalleryPreview() {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/gnt",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/p2",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,null,AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,Step.ActionType.global,2500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,null,AccessibilityService.GLOBAL_ACTION_BACK,Step.ActionType.global,500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, chatPageViewIds.getFirstPicCheckBoxViewId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,400));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, chatPageViewIds.getSendBtnViewId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,300));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME, getPicButtonId(), AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,300));
        return steps;
    }

    public List<Step> sendGalleryPicture(String[] args) {
        List<Step> steps = new Vector<>();
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/gnt",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/p2",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        for (int i = 0; i < args.length; i++) {
            int position;
            try {
                position = Integer.parseInt(args[i]);
                if(position>=0){
                    steps.add(new Step(QQChatHandler.PACKAGE_NAME,
                            ":id/photo_list_gv",AccessibilityNodeInfo.ACTION_CLICK,
                            Step.ActionType.normal,500,true,
                            new int[]{position,1}));
                }

            } catch (Exception e) {
                steps.clear();
                return steps;
            }
        }
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/send_btn",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,500));
        return steps;
    }

    public static List<Step> chooseShareTarget(String targetText,String functionName,String functionArg){
        List<Step> steps = new Vector<>();
        if(functionName!=null){
            steps.add(new Step(functionName,functionArg));
        }
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,
                ":id/listView1",AccessibilityNodeInfo.ACTION_CLICK,
                2000, Step.ActionType.normal,
                1,
                targetText,
                ":id/text1"));
        steps.add(new Step(QQChatHandler.PACKAGE_NAME,":id/dialogRightBtn",AccessibilityNodeInfo.ACTION_CLICK,Step.ActionType.normal,1500));

        return steps;
    }
}
