package com.zinhao.chtholly;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AccessibilityBoundView extends View {
    public AccessibilityBoundView(Context context) {
        this(context, null);
    }

    public AccessibilityBoundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        bound = new Rect();
        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3);
        rectPaint.setColor(Color.GREEN);

        textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(24);
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
            //只要宽度布局参数为wrap_content， 宽度给固定值200dp(处理方式不一，按照需求来)
            w = 500;
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            h = 500;
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }

    public void setNodeInfo(AccessibilityNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    private final Paint rectPaint;
    private final Paint textPaint;
    private final Rect bound;
    public void treeAndPrintLayout(AccessibilityNodeInfo nodeInfo, Canvas canvas){
        drawInfo(nodeInfo,canvas);
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo child = nodeInfo.getChild(i);
            if (child == null) {
                continue;
            }
            if (child.getChildCount() != 0) {
                treeAndPrintLayout(child, canvas);
            } else {
                drawInfo(child,canvas);
            }
        }
    }

    private void drawInfo(AccessibilityNodeInfo nodeInfo,Canvas canvas){
        nodeInfo.getBoundsInScreen(bound);
        if(nodeInfo.isClickable()){
            rectPaint.setColor(Color.GREEN);
        }else{
            rectPaint.setColor(Color.RED);
        }
        canvas.drawRect(bound, rectPaint);

        String text = nodeInfo.getViewIdResourceName();
        if(text!=null){
            text = text.replace(nodeInfo.getPackageName(),"");
            // 计算矩形的中心点
            int rectWidth = bound.right - bound.left;
            int rectHeight = bound.bottom - bound.top;

            // 获取文本的宽度和高度
            float textWidth = textPaint.measureText(text);
            float textHeight = textPaint.getTextSize(); // 在这里使用文本大小作为高度

            // 计算文本的绘制位置，使其位于矩形的中心
            float x = bound.left; // 文本的左下角 X
            float y = bound.top + textHeight; // 文本的基线 Y
            if(nodeInfo.isClickable()){
                textPaint.setColor(Color.YELLOW);
            }else{
                textPaint.setColor(Color.RED);
            }
            canvas.drawText(text,x,y, textPaint);
        }
    }
    private static final int statusBarHeight = 80;
    private AccessibilityNodeInfo nodeInfo;
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if(nodeInfo!=null){
            canvas.save();
            canvas.translate(0,-statusBarHeight);
            treeAndPrintLayout(nodeInfo,canvas);
        }
    }
}
