package com.zinhao.chtholly.customview;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class AccessibilityBoundView extends View {

    private Paint rectPaint;
    private Paint textPaint;
    private Rect bound;
    private AccessibilityNodeInfo nodeInfo;
    private int statusBarHeight;  // 改为动态获取

    public AccessibilityBoundView(Context context) {
        this(context, null);
    }

    public AccessibilityBoundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bound = new Rect();

        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3);
        rectPaint.setColor(Color.GREEN);

        textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(24);

        // 方式1：通过资源 ID 获取状态栏高度（推荐，兼容性好）
        statusBarHeight = getStatusBarHeight();
    }

    /**
     * 通过系统资源 ID 获取状态栏高度（支持所有 API 级别）
     */
    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        // 兜底方案：根据密度计算 24dp（常见状态栏高度）
        return (int) (24 * getResources().getDisplayMetrics().density);
    }

    /**
     * 方式2：通过 WindowInsets 获取（API 23+，更精确，支持刘海屏）
     * 需要在 View attach 到 Window 后调用
     */
    @RequiresApi(api = android.os.Build.VERSION_CODES.M)
    private void initWithWindowInsets() {
        setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
            } else {
                statusBarHeight = insets.getStableInsetTop();
            }
            return insets;
        });
    }

    /**
     * 方式3：外部手动设置（适用于特殊场景，如全屏/沉浸式模式）
     */
    public void setStatusBarHeight(int height) {
        this.statusBarHeight = height;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        int w = widthSpecSize;
        int h = heightSpecSize;

        //处理wrap_content的几种特殊情况
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            w = 500;  //单位是px
            h = 500;
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            w = 500;
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            h = 500;
        }
        setMeasuredDimension(w, h);
    }

    public void setNodeInfo(AccessibilityNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
        invalidate();  // 设置后刷新
    }

    public void treeAndPrintLayout(AccessibilityNodeInfo nodeInfo, Canvas canvas) {
        drawInfo(nodeInfo, canvas);
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = nodeInfo.getChild(i);
            if (child == null) {
                continue;
            }
            if (child.getChildCount() != 0) {
                treeAndPrintLayout(child, canvas);
            } else {
                drawInfo(child, canvas);
            }
        }
    }

    private void drawInfo(AccessibilityNodeInfo nodeInfo, Canvas canvas) {
        nodeInfo.getBoundsInScreen(bound);

        // 修正坐标：减去状态栏高度，将屏幕坐标转为 View 坐标
        bound.offset(0, -statusBarHeight);

        if (nodeInfo.isClickable()) {
            rectPaint.setColor(Color.GREEN);
        } else {
            rectPaint.setColor(Color.RED);
        }
        canvas.drawRect(bound, rectPaint);

        if (nodeInfo.isEditable()) {
            drawTextInCenter(canvas, "可输入", bound, textPaint);
        }

        CharSequence text = nodeInfo.getText();
        CharSequence desc = nodeInfo.getContentDescription();
        String viewIdResourceName = nodeInfo.getViewIdResourceName();

        String mergeStr;
        if(text!=null && desc!=null){
            mergeStr = String.format("%s(%s)[%s]",viewIdResourceName,text,desc);
        }else if(desc!=null){
            mergeStr = String.format("%s[%s]",viewIdResourceName,desc);
        }else if(text!=null){
            mergeStr = String.format("%s(%s)",viewIdResourceName,text);
        }else{
            mergeStr = viewIdResourceName;
        }
        if(mergeStr!=null){
            String finalMergeStr = mergeStr.replace(nodeInfo.getPackageName(), "");

            float textHeight = textPaint.getTextSize();
            float x = bound.left;
            float y = bound.top + textHeight;

            if (nodeInfo.isClickable()) {
                textPaint.setColor(Color.BLACK);
            } else {
                textPaint.setColor(Color.RED);
            }
            canvas.drawText(finalMergeStr, x, y, textPaint);
        }

    }

    private static void drawTextInCenter(Canvas canvas, String text, Rect bound, Paint textPaint) {
        float textWidth = textPaint.measureText(text);
        float textHeight = textPaint.getTextSize();
        float x = bound.centerX() - textWidth / 2;
        float y = bound.centerY() + textHeight / 2;
        canvas.drawText(text, x, y, textPaint);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (nodeInfo != null) {
            // 保存画布状态
            canvas.save();
            treeAndPrintLayout(nodeInfo, canvas);
            // 恢复画布状态（重要！）
            canvas.restore();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 清理引用，避免内存泄漏
        if (nodeInfo != null) {
            nodeInfo.recycle();
            nodeInfo = null;
        }
    }
}