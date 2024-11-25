package com.zinhao.chtholly.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.zinhao.chtholly.BuildConfig;
import com.zinhao.chtholly.NekoChatService;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import static com.zinhao.chtholly.utils.QQChatHandler.getChatNickId;
import static com.zinhao.chtholly.utils.QQChatHandler.getChatTextId;

public class LayoutTreeUtils {

    private static StringBuilder builder;
    private static final String TAG = "LayoutTreeUtils";
    private static final Rect bound = new Rect();
    private static final boolean printTree = true;
    public static JSONObject treeAndPrintLayout(AccessibilityNodeInfo nodeInfo, int treeIndex) throws JSONException {
        JSONObject root = new JSONObject();
        if(nodeInfo == null)
            return root;
        root.put("id", nodeInfo.getViewIdResourceName());
        root.put("class",nodeInfo.getClassName());
//        root.put("click",nodeInfo.isClickable());
//        root.put("longClick",nodeInfo.isLongClickable());
        root.put("desc", nodeInfo.getContentDescription());
        root.put("text", nodeInfo.getText());
        if (treeIndex == 0) {
            builder = new StringBuilder();
            if (BuildConfig.DEBUG && printTree) {
                String pageName = QQChatHandler.checkWhatPage(nodeInfo);
                Log.d(TAG, "\uD83D\uDE21"+pageName+":===============================================>" + nodeInfo.getPackageName());
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
//            if (child.isClickable() || child.isCheckable() || child.isLongClickable()) {
//                NekoChatService.chatPageViewIds.addActionableId(child);
//            }
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
                }
                if (getChatNickId().equals(child.getViewIdResourceName())) {
                    if (child.getText() == null)
                        continue;
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

    @NotNull
    public static StringBuilder getEventStringBuilder(AccessibilityEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(event.toString());
        Log.d(TAG, "getEventStringBuilder: "+stringBuilder);
        return  stringBuilder;
//        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) {
//            stringBuilder.append("CONTENT_CHANGE_TYPE_TEXT");
//        }
//        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) {
//            if (stringBuilder.length() != 0)
//                stringBuilder.append(' ');
//            stringBuilder.append("CONTENT_CHANGE_TYPE_SUBTREE");
//        }
//        if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) == AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) {
//            if (stringBuilder.length() != 0)
//                stringBuilder.append(' ');
//            stringBuilder.append("CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION");
//        }
//        return stringBuilder;
    }
}
