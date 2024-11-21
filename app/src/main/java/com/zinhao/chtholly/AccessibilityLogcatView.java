package com.zinhao.chtholly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityLogcatView extends View {
    private final List<String> logcatList = new ArrayList<>();
    public AccessibilityLogcatView(Context context) {
        this(context, null);
    }

    public AccessibilityLogcatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(24);
        logcatList.add("init logcat");
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

    public void appendLogcat(String logcat) {
        logcatList.add(logcat);
        postInvalidate();
    }

    private final Paint textPaint;

    private static final int statusBarHeight = 80;
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float y = getBottom();
        float textSize = textPaint.getTextSize();
        for (int i = 0; i < logcatList.size(); i++) {
            float textY = y - (logcatList.size() - i) * textSize;
            canvas.drawText(logcatList.get(i),0,textY,textPaint);
        }
    }
}
