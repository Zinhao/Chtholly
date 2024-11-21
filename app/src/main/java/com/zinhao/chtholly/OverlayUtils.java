package com.zinhao.chtholly;

import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;

public class OverlayUtils {

    public static WindowManager.LayoutParams makeFloatWindowParams(float x, float y, int width, int height) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        params.gravity = Gravity.START | Gravity.TOP;
        if (width == 0) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        }
        if (height == 0) {
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
        } else {
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }

        params.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        params.format = PixelFormat.RGBA_8888;
        params.x = (int) x;
        params.y = (int) y;
        return params;
    }

    public static WindowManager.LayoutParams makeNotTouchWindowParams(float x, float y, int width, int height) {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        params.gravity = Gravity.START | Gravity.TOP;
        if (width == 0) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
        } else {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        }
        if (height == 0) {
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
        } else {
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }

        params.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;


        params.format = PixelFormat.RGBA_8888;
        params.x = (int) x;
        params.y = (int) y;
        return params;
    }
}
