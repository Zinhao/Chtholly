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


    private long delay;

    /**
     * 丛目标viewId的子项中选择,findPosition [1,0,3] 指选中 viewId[1][0][3]
     */
    /// ============================================================ 根据位置搜索 ActionType.normal
    private boolean findChildByPosition = false;
    private int[] findPosition;
    private String needHasId;

    /// ============================================================ 根据文本搜索 ActionType.normal
    private String targetText;
    private String targetTextViewId;
    /// 先上寻找 父node 次数
    private int targetParentTimes = 0;

    /// ============================================================ 调用函数拉起页面 ActionType.function
    private String functionName;
    private String functionArg;



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
        custom,
        function,
        input
    }

    // 调用函数
    public Step(String functionName,String functionArg){
        this(null,null,0,ActionType.function);
        this.functionName = functionName;
        this.functionArg = functionArg;
    }


    public Step(String packageName, String viewId, int actionId, ActionType actionType) {
        this.packageName = packageName;
        this.viewId = viewId;
        this.actionId = actionId;
        this.actionType = actionType;
        delay =500;
        mode = IndexTargetMode.onlyOne;
    }

    // 普通操作
    public Step(String packageName, String viewId, int actionId, ActionType actionType, long delay) {
        this(packageName, viewId, actionId, actionType);
        this.delay = delay;
    }

    // 点击列表 item
    public Step(String packageName, String viewId, int actionId, ActionType actionType, long delay, boolean findChildByPosition, int[] findPosition) {
        this(packageName, viewId, actionId, actionType, delay);
        this.findChildByPosition = findChildByPosition;
        this.findPosition = findPosition;
        mode = IndexTargetMode.position;
    }

    // 点击文本 可向上寻找父节点
    public Step(String packageName, String viewId, int actionId, long delay, ActionType actionType, int targetParentTimes, String targetText, String targetTextViewId) {
        this(packageName, viewId, actionId, actionType, delay,false,new int[]{});
        this.targetParentTimes = targetParentTimes;
        this.targetText = targetText;
        this.targetTextViewId = targetTextViewId;
        mode = IndexTargetMode.text;
    }

    // 输入文本
    public Step(String packageName, String viewId, String inputText) {
        this.packageName = packageName;
        this.viewId = viewId;
        this.delay = 500;
        this.actionId = AccessibilityNodeInfo.ACTION_SET_TEXT;
        this.actionType = ActionType.input;
        targetText = inputText;
        mode = IndexTargetMode.onlyOne;
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

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public @NotNull String toString() {
        if(mode == IndexTargetMode.text){
            return  actionDesc(actionId) + "find "+(isGlobalAction()?"Global":targetText)+ " in " +viewId+ ", delay:"+ delay;
        }else if(actionType == ActionType.function){
            return "Function "+functionName +" => "+ functionArg +" delay:"+ delay;
        }
        return actionDesc(actionId) + " " +(isGlobalAction()?"Global":viewId)+ (isFindChildByPosition()?Arrays.toString(findPosition):" ") + ", delay:"+ delay;
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

    public void setNeedGesture(NeedGesture needGesture) {
        this.needGesture = needGesture;
    }

    public NeedGesture getNeedGesture() {
        return needGesture;
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

    public String getFunctionArg() {
        return functionArg;
    }

    public String getFunctionName() {
        return functionName;
    }

    public interface NeedGesture{
        GestureDescription onGesture(AccessibilityNodeInfo nodeInfo);
    }
}
