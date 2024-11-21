package com.zinhao.chtholly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityLogcatView extends View {
    private final List<String> logcatList = new ArrayList<>();
    private int lineHeight;
    public AccessibilityLogcatView(Context context) {
        this(context, null);
    }

    public AccessibilityLogcatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        textPaint = new TextPaint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(24);
        lineHeight = 24;
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

    private final TextPaint textPaint;

    private static final int statusBarHeight = 80;
    private static final List<String> waitDrawLineText = new ArrayList<>();
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float textY = getBottom();
        float maxWidth = getWidth()-lineHeight*2;
        try {
            for (int i = logcatList.size()-1; i >=0; i--) {
                String logcat = logcatList.get(i);
                waitDrawLineText.clear();
                int seek = 0;
                do{
                    seek  = textPaint.breakText(logcat,false, maxWidth,null);
                    String _t = logcat.substring(0,seek);
                    waitDrawLineText.add(_t);
                    logcat = logcat.substring(seek);
                }while (!logcat.isEmpty());

                for (int j = waitDrawLineText.size()-1; j >= 0; j--) {
                    textY -= lineHeight;
                    if(textY<0){
                        return;
                    }
                    canvas.drawText(waitDrawLineText.get(j),(float)lineHeight,textY,textPaint);
                }
            }
        }catch (Exception e){
            Log.e(getClass().getSimpleName(), "onDraw: ",e);

        }

    }
}
