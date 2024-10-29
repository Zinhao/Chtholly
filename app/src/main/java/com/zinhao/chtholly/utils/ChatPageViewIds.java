package com.zinhao.chtholly.utils;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;

public class ChatPageViewIds {
    private String inputViewId = ":id/not_init";
    private String sendBtnViewId = ":id/not_init";
    private String titleViewId = ":id/not_init";
    private String firstPicCheckBoxViewId = ":id/qhq";
    public final HashMap<String,AccessibilityNodeInfo> actionableIdMap = new HashMap<>();

    public String getInputViewId() {
        return inputViewId;
    }

    public void setInputViewId(String inputViewId) {
        this.inputViewId = inputViewId;
    }

    public String getSendBtnViewId() {
        return sendBtnViewId;
    }

    public void setSendBtnViewId(String sendBtnViewId) {
        this.sendBtnViewId = sendBtnViewId;
    }

    public String getTitleViewId() {
        return titleViewId;
    }

    public void setTitleViewId(String titleViewId) {
        this.titleViewId = titleViewId;
    }

    public String getFirstPicCheckBoxViewId() {
        return firstPicCheckBoxViewId;
    }

    public void setFirstPicCheckBoxViewId(String firstPicCheckBoxViewId) {
        this.firstPicCheckBoxViewId = firstPicCheckBoxViewId;
    }

    public void addActionableId(AccessibilityNodeInfo info){
        actionableIdMap.put(info.getViewIdResourceName(),info);
    }
}
