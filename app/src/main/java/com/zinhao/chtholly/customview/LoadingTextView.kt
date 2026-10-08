package com.zinhao.chtholly.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.sin

class LoadingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    // 文字画笔
    private val textPaintRandom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = this@LoadingTextView.textSize
        typeface = this@LoadingTextView.typeface
    }

    // 随机字符池（可自由替换）
    private var charPool: CharArray = "01ABCDEF#$%&*@".toCharArray()

    // 要绘制的随机文字数量（相当于原来的三个点）
    private var randomCharCount = 3

    // 每个随机字符当前的值
    private val currentChars = CharArray(randomCharCount) { ' ' }

    // 刷新间隔（ms），避免每帧都换字
    private var refreshInterval = 80L
    private var lastRefreshTime = 0L

    // 跳动相关
    private var bounceHeight = dp2px(6f)
    private var textGap = dp2px(6f)
    private var charSpacing = dp2px(2f)

    // 动画进度 0f ~ 1f
    private var animProgress = 0f

    // 加载开关
    private var loading = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                if (!animator.isStarted) animator.start()
            } else {
                if (animator.isStarted) {
                    animator.cancel()
                    animProgress = 0f
                }
            }
            invalidate()
        }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 900L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animProgress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!loading) return

        val layout = layout ?: return
        if (layout.lineCount == 0) return

        // 定时刷新随机字符
        maybeRefreshChars()

        val lastLine = layout.lineCount - 1
        val lineRight = layout.getLineRight(lastLine)
        val baseline = layout.getLineBaseline(lastLine).toFloat()

        // 以基线作为文字落地点
        val baseY = baseline

        // 起始 X：文字末尾 + 间距
        var startX = lineRight + textGap

        // 每个字符宽度可能不同，逐个累加绘制
        for (i in 0 until randomCharCount) {
            val phase = (animProgress + i / randomCharCount.toFloat()) % 1f
            val offsetY = -bounceHeight * sin(phase * Math.PI).toFloat()

            val c = currentChars[i].toString()
            canvas.drawText(c, startX, baseY + offsetY, textPaintRandom)

            // 累加下一个字符的起始位置
            startX += textPaintRandom.measureText(c) + charSpacing
        }
    }

    /** 按时间间隔刷新随机字符 */
    private fun maybeRefreshChars() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < refreshInterval) return
        lastRefreshTime = now
        for (i in currentChars.indices) {
            currentChars[i] = charPool.random()
        }
    }

    fun startLoading() {
        loading = true
    }

    fun stopLoading() {
        loading = false
    }

    fun isLoading(): Boolean = loading

    /** 自定义字符池 */
    fun setCharPool(pool: String) {
        if (pool.isNotEmpty()) {
            charPool = pool.replace(' ','.')
                .replace('\n', '.')
                .replace('\t', '.')
                .toCharArray()
        }
    }

    /** 自定义随机字符数量 */
    fun setRandomCharCount(count: Int) {
        if (count <= 0) return
        randomCharCount = count
        // 重建数组长度
        val newArr = CharArray(count) { ' ' }
        currentChars.indices.take(count).forEach { newArr[it] = currentChars[it] }
        // 注意 currentChars 是 val，这里改为可变
    }

    /** 设置随机刷新间隔，越小变化越快 */
    fun setRefreshInterval(intervalMs: Long) {
        refreshInterval = intervalMs.coerceAtLeast(16L)
    }

    /** 文字大小跟随 TextView 变化 */
    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        textPaintRandom.textSize = this.textSize
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (loading && !animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (animator.isStarted) {
            animator.cancel()
            animProgress = 0f
        }
    }

    private fun dp2px(dp: Float): Float =
        dp * resources.displayMetrics.density

    fun setRandomTextColor(color: Int) {
        textPaintRandom.color = color
        invalidate()
    }
}