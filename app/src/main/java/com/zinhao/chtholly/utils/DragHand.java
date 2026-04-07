package com.zinhao.chtholly.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

public abstract class DragHand implements View.OnTouchListener,View.OnClickListener{
    private float downX, downY;
    private int downRawX, downRawY;
    private static final int TOUCH_SLOP = 10; // 判定为拖动的最小移动距离（像素）
    private static final int LONG_PRESS_TIMEOUT = 400; // 长按时长阈值

    private boolean isDragging = false;
    private Runnable longPressRunnable;
    private final Handler mHandler;


    public DragHand() {
        this.mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();  // 使用getX()而非getRawX()，避免计算偏移量
                downY = event.getY();
                downRawX = (int) event.getRawX();
                downRawY = (int) event.getRawY();
                isDragging = false;

                // 延时检测长按（可选）
                mHandler.postDelayed(longPressRunnable = () -> {
                    isDragging = true; // 长按后自动进入拖动模式
                }, LONG_PRESS_TIMEOUT);

                return true; // 必须返回true才能接收后续MOVE和UP

            case MotionEvent.ACTION_MOVE:
                float moveX = Math.abs(event.getX() - downX);
                float moveY = Math.abs(event.getY() - downY);

                // 超过阈值，判定为拖动
                if (moveX > TOUCH_SLOP || moveY > TOUCH_SLOP || isDragging) {
                    if (!isDragging) {
                        isDragging = true;
                        mHandler.removeCallbacks(longPressRunnable);
                    }
                    // 计算移动增量（使用Raw坐标确保跨屏幕边界准确）
                    int deltaX = (int) (event.getRawX() - downRawX);
                    int deltaY = (int) (event.getRawY() - downRawY);

                    onDragDelta(deltaX,deltaY);

                    downRawX = (int) event.getRawX();
                    downRawY = (int) event.getRawY();
                }
                return true;

            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(longPressRunnable);
                if (!isDragging) {
                    onClick(v);
                }
                // 重置状态
                isDragging = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(longPressRunnable);
                isDragging = false;
                return true;
        }
        return false;
    }


    public abstract void onDragDelta(int xDelta, int yDelta);

}
