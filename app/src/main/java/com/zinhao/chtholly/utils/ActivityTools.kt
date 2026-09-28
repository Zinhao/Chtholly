package com.zinhao.chtholly.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object ActivityTools {

    interface InsetReady{
        fun onInsetReady(insets: Insets)
    }

    fun AppCompatActivity.setSafeArea(topView: View, insetReady: InsetReady? = null){
        ViewCompat.setOnApplyWindowInsetsListener(topView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            // 旋转后，cutout.left 或 cutout.right 会自动有值
            // 结合系统栏和刘海，计算出最终需要的 padding
            val leftPadding = maxOf(bars.left, cutout.left)
            val rightPadding = maxOf(bars.right, cutout.right)
            val topPadding = bars.top // 横屏时状态栏高度通常变为 0 或很小
            val bottomPadding = bars.bottom

            v.setPadding(leftPadding, topPadding, rightPadding, v.paddingBottom)
            insetReady?.onInsetReady(Insets.of(leftPadding, topPadding, rightPadding, bottomPadding))
            insets
        }
    }
}