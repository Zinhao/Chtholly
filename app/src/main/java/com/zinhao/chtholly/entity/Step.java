package com.zinhao.chtholly.entity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.view.accessibility.AccessibilityNodeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Step {
    private String packageName;
    private String viewId;
    private int actionId;
    private boolean globalAction;
    private long daley;
    private  boolean waiting = false;
    private boolean findChildByPosition = false;
    private int[] findPosition;
    private int inNodesPosition = 0;
    private NeedGesture needGesture;
    private String needHasId;

    public Step(String packageName, String viewId, int actionId, boolean globalAction) {
        this.packageName = packageName;
        this.viewId = viewId;
        this.actionId = actionId;
        this.globalAction = globalAction;
    }

    public Step(String packageName, String viewId, int actionId, boolean globalAction, long daley) {
        this(packageName, viewId, actionId, globalAction);
        this.daley = daley;
    }

    public Step(String packageName, String viewId, int actionId, boolean globalAction, long daley, boolean findChildByPosition, int[] findPosition) {
        this(packageName, viewId, actionId, globalAction, daley);
        this.findChildByPosition = findChildByPosition;
        this.findPosition = findPosition;
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
        return globalAction;
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

    public interface NeedGesture{
        GestureDescription onGesture(AccessibilityNodeInfo nodeInfo);
    }
}
