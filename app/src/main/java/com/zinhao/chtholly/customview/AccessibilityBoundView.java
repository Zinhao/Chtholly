package com.zinhao.chtholly.customview;

import android.content.Context;
import android.graphics.*;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityBoundView extends View {

    private Paint rectPaint;
    private Paint textPaint;
    private Paint bgPaint;          // 新增：文字背景色
    private Rect bound;
    private AccessibilityNodeInfo nodeInfo;
    private int statusBarHeight;

    // 新增：绘制配置参数
    private static final int MAX_TEXT_LENGTH = 30;      // 最大显示字符数
    private static final int TEXT_PADDING = 4;          // 文字内边距
    private static final int LINE_HEIGHT = 28;          // 行高
    private static final int MIN_NODE_SIZE = 20;        // 最小绘制节点尺寸
    private static final int TEXT_OFFSET_Y = -5;        // 文字向上偏移，避免遮挡节点

    // 新增：已占用的文字区域，用于避让计算
    private List<Rect> occupiedRegions;

    public AccessibilityBoundView(Context context) {
        this(context, null);
    }

    public AccessibilityBoundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bound = new Rect();
        occupiedRegions = new ArrayList<>();

        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(3);
        rectPaint.setColor(Color.GREEN);

        textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(24);
        textPaint.setAntiAlias(true);

        // 新增：文字背景画笔，提升可读性
        bgPaint = new Paint();
        bgPaint.setColor(Color.argb(180, 0, 0, 0)); // 半透明黑底
        bgPaint.setStyle(Paint.Style.FILL);

        statusBarHeight = getStatusBarHeight();
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return (int) (24 * getResources().getDisplayMetrics().density);
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

        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            w = 500;
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
        invalidate();
    }

    /**
     * 优化1：先收集所有可见节点，再统一绘制，避免递归导致的层级混乱
     */
    public void treeAndPrintLayout(AccessibilityNodeInfo nodeInfo, Canvas canvas) {
        List<AccessibilityNodeInfo> visibleNodes = new ArrayList<>();
        collectVisibleNodes(nodeInfo, visibleNodes);

        // 按面积从大到小排序，大节点先绘制（层级感）
        visibleNodes.sort((a, b) -> {
            Rect rectA = new Rect();
            Rect rectB = new Rect();
            a.getBoundsInScreen(rectA);
            b.getBoundsInScreen(rectB);
            return Integer.compare(getArea(rectB), getArea(rectA));
        });

        // 清空已占用区域
        occupiedRegions.clear();

        for (AccessibilityNodeInfo node : visibleNodes) {
            drawInfoOptimized(node, canvas);
            if(node!= nodeInfo){
                node.recycle();
            }
        }
    }

    /**
     * 优化2：只收集可见且有意义显示的节点
     */
    private void collectVisibleNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> result) {
        if (node == null) return;

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        bounds.offset(0, -statusBarHeight);

        // 过滤条件：可见、尺寸足够、有内容
        boolean isVisible = node.isVisibleToUser() &&
                bounds.width() >= MIN_NODE_SIZE &&
                bounds.height() >= MIN_NODE_SIZE;

        boolean hasContent = !TextUtils.isEmpty(node.getText()) ||
                !TextUtils.isEmpty(node.getContentDescription()) ||
                node.isClickable() || node.isEditable();

        if (isVisible && hasContent) {
            result.add(node);
        }

        // 递归收集子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectVisibleNodes(child, result);
            }
        }
    }

    private int getArea(Rect rect) {
        return rect.width() * rect.height();
    }

    /**
     * 优化3：改进的绘制逻辑，包含避让机制
     */
    private void drawInfoOptimized(AccessibilityNodeInfo nodeInfo, Canvas canvas) {
        Rect localBound = new Rect();
        nodeInfo.getBoundsInScreen(localBound);
        localBound.offset(0, -statusBarHeight);

        // 绘制边框
        if (nodeInfo.isClickable()) {
            rectPaint.setColor(Color.GREEN);
        } else if (nodeInfo.isEditable()) {
            rectPaint.setColor(Color.BLUE);
        } else {
            rectPaint.setColor(Color.RED);
        }
        canvas.drawRect(localBound, rectPaint);

        // 构建显示文本
        String displayText = buildDisplayText(nodeInfo);
        if (TextUtils.isEmpty(displayText)) return;

        // 截断过长文本
        if (displayText.length() > MAX_TEXT_LENGTH) {
            displayText = displayText.substring(0, MAX_TEXT_LENGTH) + "...";
        }

        // 计算文字尺寸
        float textWidth = textPaint.measureText(displayText);
        float textHeight = textPaint.getTextSize();

        // 优化4：智能计算文字位置，避免重叠
        float[] textPos = calculateTextPosition(localBound, textWidth, textHeight);
        float x = textPos[0];
        float y = textPos[1];

        // 绘制文字背景（提升可读性）
        RectF bgRect = new RectF(
                x - TEXT_PADDING,
                y - textHeight,
                x + textWidth + TEXT_PADDING,
                y + TEXT_PADDING
        );
        canvas.drawRect(bgRect, bgPaint);

        // 根据节点类型设置文字颜色
        if (nodeInfo.isClickable()) {
            textPaint.setColor(Color.GREEN);
        } else if (nodeInfo.isEditable()) {
            textPaint.setColor(Color.CYAN);
        } else {
            textPaint.setColor(Color.YELLOW);
        }

        canvas.drawText(displayText, x, y, textPaint);

        // 标记已占用区域
        Rect occupied = new Rect(
                (int) bgRect.left, (int) bgRect.top,
                (int) bgRect.right, (int) bgRect.bottom
        );
        occupiedRegions.add(occupied);
    }

    /**
     * 优化5：构建更简洁的显示文本
     */
    private String buildDisplayText(AccessibilityNodeInfo node) {
        StringBuilder sb = new StringBuilder();

        // 优先显示文本内容
        CharSequence text = node.getText();
        if (!TextUtils.isEmpty(text)) {
            sb.append("t:");
            sb.append(text.toString().trim());
        }

        // 其次显示描述
        CharSequence desc = node.getContentDescription();
        if (!TextUtils.isEmpty(desc) && !desc.equals(text)) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("d:");
            sb.append(desc.toString().trim());
        }

        // 最后显示ID（简短形式）
        String viewId = node.getViewIdResourceName();
        if (viewId != null && sb.length() < 15) { // 内容少时才显示ID
            String shortId = viewId.replace(node.getPackageName() + ":", "");
            sb.append(" #").append(shortId);
        }

        // 标记特殊属性
        if (node.isEditable()) {
            sb.insert(0, "[输入] ");
        } else if (node.isClickable()) {
            sb.insert(0, "[可点] ");
        }

        return sb.toString().replaceAll("\\s+", " ");
    }

    /**
     * 优化6：计算最佳文字位置，避开已占用区域
     */
    private float[] calculateTextPosition(Rect nodeBound, float textWidth, float textHeight) {
        float x, y;

        // 尝试位置1：节点上方居中
        x = nodeBound.centerX() - textWidth / 2;
        y = nodeBound.top + TEXT_OFFSET_Y;

        // 检查是否与已占用区域重叠
        if (isOverlapping(x, y, textWidth, textHeight)) {
            // 尝试位置2：节点左侧
            x = nodeBound.left - textWidth - TEXT_PADDING;
            y = nodeBound.centerY() + textHeight / 2;

            if (isOverlapping(x, y, textWidth, textHeight)) {
                // 尝试位置3：节点右侧
                x = nodeBound.right + TEXT_PADDING;
                y = nodeBound.centerY() + textHeight / 2;

                if (isOverlapping(x, y, textHeight, textHeight)) {
                    // 尝试位置4：节点内部左上角
                    x = nodeBound.left + TEXT_PADDING;
                    y = nodeBound.top + textHeight;
                }
            }
        }

        // 边界检查：确保不超出屏幕
        x = Math.max(TEXT_PADDING, Math.min(x, getWidth() - textWidth - TEXT_PADDING));
        y = Math.max(textHeight, Math.min(y, getHeight() - TEXT_PADDING));

        return new float[]{x, y};
    }

    private boolean isOverlapping(float x, float y, float width, float height) {
        RectF newRect = new RectF(x, y - height, x + width, y);
        for (Rect occupied : occupiedRegions) {
            if (RectF.intersects(newRect, new RectF(occupied))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (nodeInfo != null) {
            canvas.save();
            treeAndPrintLayout(nodeInfo, canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (nodeInfo != null) {
            nodeInfo.recycle();
            nodeInfo = null;
        }
        occupiedRegions.clear();
    }
}