package com.zinhao.chtholly.entity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.view.accessibility.AccessibilityNodeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Step {
    private String packageName;
    private IndexTargetMode mode;

    private String viewId;
    private int inNodesPosition = 0;


    private final ActionType actionType;
    private int actionId;
    private NeedGesture needGesture;


    private long daley;
    private boolean waiting = false;


    /**
     * 丛目标viewId的子项中选择,findPosition [1,0,3] 指选中 viewId[1][0][3]
     */
    /// ============================================================ 根据位置搜索
    private boolean findChildByPosition = false;
    private int[] findPosition;
    private String needHasId;

    /// ============================================================ 根据文本搜索
    private String targetText;
    private String targetTextViewId;
    /// 先上寻找 父node 次数
    private int targetParentTimes = 0;

    public boolean isCustomGesture() {
        return getActionType() == Step.ActionType.custom;
    }


    public enum IndexTargetMode{
        onlyOne,
        position,
        text,
    }

    public enum ActionType{
        normal,
        global,
        custom
    }


    public Step(String packageName, String viewId, int actionId, ActionType actionType) {
        this.packageName = packageName;
        this.viewId = viewId;
        this.actionId = actionId;
        this.actionType = actionType;
        mode = IndexTargetMode.onlyOne;
    }

    public Step(String packageName, String viewId, int actionId, ActionType actionType, long daley) {
        this(packageName, viewId, actionId, actionType);
        this.daley = daley;
    }

    public Step(String packageName, String viewId, int actionId, ActionType actionType, long daley, boolean findChildByPosition, int[] findPosition) {
        this(packageName, viewId, actionId, actionType, daley);
        this.findChildByPosition = findChildByPosition;
        this.findPosition = findPosition;
        mode = IndexTargetMode.position;
    }

    public Step(String packageName, String viewId, int actionId, long daley, ActionType actionType, int targetParentTimes,String targetText,String targetTextViewId) {
        this(packageName, viewId, actionId, actionType, daley,false,new int[]{});
        this.targetParentTimes = targetParentTimes;
        this.targetText = targetText;
        this.targetTextViewId = targetTextViewId;
        mode = IndexTargetMode.text;
    }

    public void setInNodesPosition(int inNodesPosition) {
        this.inNodesPosition = inNodesPosition;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getViewId() {
        if(viewId!=null && viewId.startsWith(":")){
            return packageName+viewId;
        }
        return viewId;
    }

    public int getActionId() {
        return actionId;
    }

    public boolean isGlobalAction() {
        return actionType == ActionType.global;
    }

    public long getDaley() {
        return daley;
    }

    public void setDaley(long daley) {
        this.daley = daley;
    }

    @Override
    public @NotNull String toString() {
        return actionDesc(actionId) + " " +(isGlobalAction()?"🟧":viewId)+ (isFindChildByPosition()?Arrays.toString(findPosition):"_") + ",daley "+daley;
    }

    private static String actionDesc(int id){
        switch (id){
            case AccessibilityNodeInfo.ACTION_CLICK:return "Click";
            case AccessibilityNodeInfo.ACTION_LONG_CLICK:return "Long Click";
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:return "Custom Gesture";
            case AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT:return "Global Screen Shot";
            case AccessibilityService.GLOBAL_ACTION_BACK:return "Global back";
            default:return "UnKnow";
        }
    }

    public boolean isFindChildByPosition() {
        return findChildByPosition;
    }

    public void setFindChildByPosition(boolean findChildByPosition) {
        this.findChildByPosition = findChildByPosition;
    }

    public int[] getFindPosition() {
        return findPosition;
    }

    public int getInNodesPosition() {
        return inNodesPosition;
    }

    public void setFindPosition(int[] findPosition) {
        this.findPosition = findPosition;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }

    public void setNeedGesture(NeedGesture needGesture) {
        this.needGesture = needGesture;
    }

    public NeedGesture getNeedGesture() {
        return needGesture;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public String getNeedHasId() {
        if(needHasId !=null && needHasId.startsWith(":")){
            return packageName+needHasId;
        }
        return needHasId;
    }

    public void setNeedHasId(String needHasId) {
        this.needHasId = needHasId;
    }

    public IndexTargetMode getIndexMode() {
        return mode;
    }

    public void setMode(IndexTargetMode mode) {
        this.mode = mode;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    public String getTargetTextViewId() {
        if(targetTextViewId!=null && targetTextViewId.startsWith(":")){
            return packageName+targetTextViewId;
        }
        return targetTextViewId;
    }

    public void setTargetTextViewId(String targetTextViewId) {
        this.targetTextViewId = targetTextViewId;
    }

    public int getTargetParentTimes() {
        return targetParentTimes;
    }

    public void setTargetParentTimes(int targetParentTimes) {
        this.targetParentTimes = targetParentTimes;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public interface NeedGesture{
        GestureDescription onGesture(AccessibilityNodeInfo nodeInfo);
    }
}
