package com.zinhao.chtholly.utils;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.zinhao.chtholly.BuildConfig;

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
    private static final boolean PRINT_TREE = false;
    public static JSONObject treeAndPrintLayout(AccessibilityNodeInfo nodeInfo, int treeIndex,boolean showClickArg) throws JSONException {
        JSONObject root = new JSONObject();
        if(nodeInfo == null)
            return root;
        root.put("id", nodeInfo.getViewIdResourceName());
        root.put("class",nodeInfo.getClassName());
        if(showClickArg){
            root.put("click",nodeInfo.isClickable());
            root.put("longClick",nodeInfo.isLongClickable());
        }
        root.put("desc", nodeInfo.getContentDescription());
        root.put("text", nodeInfo.getText());
        if (treeIndex == 0) {
            builder = new StringBuilder();
            if (BuildConfig.DEBUG && PRINT_TREE) {
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
            JSONObject childObject;
            if (child.getChildCount() != 0) {
                builder.append("__");
                if (BuildConfig.DEBUG && PRINT_TREE) {
                    child.getBoundsInScreen(bound);
                    Log.d(TAG, String.format(Locale.US, "treeInfo:%s%s class:%s, text:%s bound:%s click:%s longClick:%s check:%s desc:%s edit:%s",
                            builder, child.getViewIdResourceName(), child.getClassName(), child.getText(),
                            bound, child.isClickable(), child.isLongClickable(), child.isCheckable(), child.describeContents(),child.isEditable()));
                }
                builder.delete(builder.length() - 2, builder.length());
                builder.append("  ");
                childObject = treeAndPrintLayout(child, treeIndex + 1, showClickArg);
            } else {
                childObject = new JSONObject();
                childObject.put("id", child.getViewIdResourceName());
                childObject.put("class",child.getClassName());
                if(showClickArg){
                    childObject.put("click",child.isClickable());
                    childObject.put("longClick",child.isLongClickable());
                }
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
                if (BuildConfig.DEBUG && PRINT_TREE) {
                    child.getBoundsInScreen(bound);
                    Log.d(TAG, String.format(Locale.US, "treeInfo:%s%s class:%s, text:%s bound:%s click:%s longClick:%s check:%s desc:%s edit:%s",
                            builder, child.getViewIdResourceName(), child.getClassName(), child.getText(),
                            bound, child.isClickable(), child.isLongClickable(), child.isCheckable(), child.describeContents(),child.isEditable()));
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
        return  stringBuilder;
    }
}
