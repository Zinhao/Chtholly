package com.zinhao.chtholly.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.zinhao.chtholly.BotApp;
import com.zinhao.chtholly.entity.Message;
import com.zinhao.chtholly.entity.StaticAskAble;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SensorTools implements SensorEventListener {
    //============================================陀螺仪====================================================//
    private static final String TAG = "SensorTools";
    private long serviceCreateTime = 0;
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA);
    private long lastReportVibration = 0;
    private static final long REPORT_RANGE = 15000;
    private SensorManager sensorManager;
    private static final float MIN_STR = 1.10000f;
    private final static DecimalFormat decimalFormat = new DecimalFormat("0.0000");
    private final List<Long> timestamps = new ArrayList<>(); // 存储数据点的时间戳
    private final List<Float> dataPoints = new ArrayList<>(); // 存储震动强度数据
    private static final int VIBRATION_LOG_END= 324;
    private static final int VIBRATION_LOGGING= 325;
    private int currentVibrationLogStatus;
    private static final boolean ENABLE_REPORT_VIBRATION = false;
    private Handler mHandler;

    public SensorTools(Context context,long serviceCreateTime) {
        this.serviceCreateTime = serviceCreateTime;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                Log.d(TAG, "onCreate: accelerometer is WakeUpSensor:" +  accelerometer.isWakeUpSensor());
            }
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        mHandler = new Handler(context.getMainLooper());
    }

    private OnVibrationStrengthListener listener = new OnVibrationStrengthListener() {
        @Override
        public void onVibrationStrengthChanged(float strength) {
            if(ENABLE_REPORT_VIBRATION && strength >= MIN_STR && System.currentTimeMillis() - lastReportVibration > REPORT_RANGE
                    && System.currentTimeMillis() - serviceCreateTime > 30000){
                //开始记录10秒内的震动数据
                dataPoints.clear();
                timestamps.clear();
                currentVibrationLogStatus = VIBRATION_LOGGING;
                lastReportVibration = System.currentTimeMillis();
                delayReportVibration();

                StaticAskAble s = new StaticAskAble(TAG,
                        new Message(BotApp.getInstance().getAdminName(),"/recordVideo",System.currentTimeMillis()),
                        "开始记录震动:"+strength);
                s.handle();
            }
            if(currentVibrationLogStatus == VIBRATION_LOGGING){
                timestamps.add(System.currentTimeMillis());
                dataPoints.add(strength);
            }
        }
    };
    private void delayReportVibration(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(timestamps.isEmpty() || dataPoints.isEmpty())
                    return;
                currentVibrationLogStatus = VIBRATION_LOG_END;
                String vibrationReportBuilder = "报告震动记录" + '\n' +
                        "记录开始时间为：" + dateTimeFormat.format(timestamps.get(0)) + '\n' +
                        "记录结束时间为：" + dateTimeFormat.format(timestamps.get(timestamps.size() - 1)) + '\n' +
                        "期间最大强度：" + decimalFormat.format(Collections.max(dataPoints));
                StaticAskAble s = new StaticAskAble("",
                        new Message(BotApp.getInstance().getAdminName(),"报告震动记录",System.currentTimeMillis()),
                        vibrationReportBuilder);

                s.handle();
            }
        },REPORT_RANGE);
    }

    public interface OnVibrationStrengthListener {
        void onVibrationStrengthChanged(float strength);
    }

    // 用于高通滤波器
    private final float[] gravity = new float[3];
    private static final float alpha = 0.8f; // 过滤系数

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 使用低通滤波器计算重力
        final float beta = 1.0f - alpha;
        gravity[0] = alpha * gravity[0] + beta * event.values[0];
        gravity[1] = alpha * gravity[1] + beta * event.values[1];
        gravity[2] = alpha * gravity[2] + beta * event.values[2];

        // 计算加速度（去掉重力的影响）
        float xAcc = event.values[0] - gravity[0];
        float yAcc = event.values[1] - gravity[1];
        float zAcc = event.values[2] - gravity[2];

        // 计算震动强度
        float acceleration = (float) Math.sqrt(xAcc * xAcc + yAcc * yAcc + zAcc * zAcc);

        // 假设震动的阈值
        if ( listener != null) {
            listener.onVibrationStrengthChanged(acceleration);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void setOnVibrationStrengthListener(OnVibrationStrengthListener listener) {
        this.listener = listener;
    }

    void close(){
        sensorManager.unregisterListener(this);
    }
}
