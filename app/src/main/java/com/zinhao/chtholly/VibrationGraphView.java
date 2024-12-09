package com.zinhao.chtholly;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VibrationGraphView extends View {
    private static final long MAX_DATA_TIME = 30000; // 30秒
    private List<Long> timestamps; // 存储数据点的时间戳
    private List<Float> dataPoints; // 存储震动强度数据
    private Paint paint;
    private int width, height;
    private final float maxValue = 3.3354f; // 最大值，可根据实际情况调整
    private final float minValue = 0.0f;

    private float logMax = 0.0f;
    private float historyMaxLv = 0;
    private final static DecimalFormat decimalFormat = new DecimalFormat("0.0000");
    public VibrationGraphView(Context context) {
        super(context);
        init();
    }

    public VibrationGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VibrationGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dataPoints = new ArrayList<>();
        timestamps = new ArrayList<>();
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
        paint.setTextSize(35);
        paint.setAntiAlias(true);
    }

    // 更新数据并重绘
    public void updateData(float value) {
        long currentTime = System.currentTimeMillis();
        logMax = Math.max(value,logMax);
        // 添加新的数据点和时间戳
        dataPoints.add(value);
        timestamps.add(currentTime);

        // 移除超过 30 秒的数据
        while (!timestamps.isEmpty() && (currentTime - timestamps.get(0) > MAX_DATA_TIME)) {
            timestamps.remove(0);
            dataPoints.remove(0);
        }

        postInvalidate(); // 触发重绘
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.isEmpty()) return;

        // 绘制坐标轴
        canvas.drawLine(0, height, width, height, paint); // X轴
        canvas.drawLine(0, 0, 0, height, paint); // Y轴

        float xInterval = (float) width / Math.max(dataPoints.size() - 1, 1);
        for (int i = 0; i < dataPoints.size() - 1; i++) {
            float startX = i * xInterval;
            float startY = height - (dataPoints.get(i) / maxValue * height);
            float stopX = (i + 1) * xInterval;
            float stopY = height - (dataPoints.get(i + 1) / maxValue * height);
            canvas.drawLine(startX, startY, stopX, stopY, paint);
        }
        drawRightAxis(canvas,maxValue,minValue);
        float value = dataPoints.get(dataPoints.size()-1);
        long timeOffset = 300;
        if(timestamps.size()>=2){
            timeOffset = timestamps.get(timestamps.size()-1) -  timestamps.get(timestamps.size()-2);
        }

        float rank = calculateMagnitude(value,timeOffset);
        historyMaxLv = Math.max(rank,historyMaxLv);
        float startY = (float) (getTop() + getBottom()) /2;
        canvas.drawText("history lv="+decimalFormat.format(historyMaxLv), 10, startY+=60,paint);
        canvas.drawText("current lv="+decimalFormat.format(rank), 10, startY+=60,paint);
        canvas.drawText("history max="+decimalFormat.format(logMax)+ "m/s^2", 10,startY+=60 ,paint);
        canvas.drawText("current max="+decimalFormat.format(Collections.max(dataPoints))+ "m/s^2", 10, startY+=60,paint);
        canvas.drawText("current a ="+decimalFormat.format(value)+ "m/s^2", 10, startY+=60,paint);


    }

    private void drawRightAxis(Canvas canvas,float maxValue,float minValue) {
        float width = getWidth();
        float height = getHeight();

        // 绘制坐标尺
        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(2);

        // 绘制 Y 轴坐标线
        float rightX = width - 50; // 右侧坐标尺的位置
        int count = 10;
        for (int i = 0; i <= count; i++) { // 10个刻度
            float y = height - (i * (height / count)) * (maxValue - minValue) / count;
            canvas.drawLine(rightX, y, rightX + 10, y, axisPaint); // 坐标线

            // 添加刻度值
            String value = String.format("%.2f", minValue + (maxValue - minValue) * (i*1.0f / count));
            canvas.drawText(value, rightX + 15, y + 5, axisPaint); // 刻度值
        }
    }

    // 根据加速度计算震级
    public static float calculateMagnitude(float acceleration, float timeInMilliseconds) {
        // 将时间转换为秒
        float timeInSeconds = timeInMilliseconds / 1000.0f;

        // 使用毫米为单位计算位移
        float displacement = 0.5f * acceleration * (timeInSeconds * timeInSeconds); // 位移计算
        float displacementMicrometers = displacement * 1_000_000; // 转换为微米

        // 根据位移计算震级（使用里氏震级公式）
        float magnitude = (float) (Math.log10(displacementMicrometers) + 3 * Math.log10(8 * timeInSeconds) - 2.92);
        return magnitude;
    }

    public float convertAccelerationToMicroMeters(float accelerationG, float timeInSeconds) {
        // 1g ≈ 9.81 m/s²
        float accelerationMps2 = accelerationG * 9.81f; // 转换加速度为 m/s²
        // 计算位移 (m)
        float displacementMeters = 0.5f * accelerationMps2 * timeInSeconds * timeInSeconds;
        // 转换位移为微米
        float displacementMicrometers = displacementMeters * 1_000_000; // 1 m = 1,000,000 μm
        return displacementMicrometers; // 返回结果
    }
}

