package com.zinhao.chtholly.entity;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;

public class Step {
    private String packageName;
    private String viewId;
    private int actionId;
    private boolean globalAction;
    private long daley;

    public Step(String packageName, String viewId, int actionId, boolean globalAction) {
        this.packageName = packageName;
        this.viewId = viewId;
        this.actionId = actionId;
        this.globalAction = globalAction;
    }

    public Step(String packageName, String viewId, int actionId, boolean globalAction, long daley) {
        this.packageName = packageName;
        this.viewId = viewId;
        this.actionId = actionId;
        this.globalAction = globalAction;
        this.daley = daley;
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
    public String toString() {
        return actionDesc(actionId) + " " +(isGlobalAction()?"!":viewId) + ",daley "+daley;
    }

    private static String actionDesc(int id){
        switch (id){
            case AccessibilityNodeInfo.ACTION_CLICK:return "Click";
            case AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT:return "Screen Shot";
            case AccessibilityService.GLOBAL_ACTION_BACK:return "Global back";
            default:return "UnKnow";
        }
    }
}
