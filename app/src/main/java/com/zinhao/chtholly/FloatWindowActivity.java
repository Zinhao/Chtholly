package com.zinhao.chtholly;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Locale;

/** @noinspection deprecation*/
public class FloatWindowActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView t = new TextView(this);
        t.setText("123");
        setContentView(t);
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("还没有显示悬浮窗口的权限！")
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(String.format(Locale.US, "package:%s", getPackageName()))), 1);
                        }
                    }).setCancelable(false);
            AlertDialog askDrawOverlaysDialog = builder.create();
            askDrawOverlaysDialog.show();
        }
        initAccView();
        initCtrlView();
    }

    public void initCtrlView() {
        if(NekoChatService.getInstance().getCtrlView() == null){
            View view = LayoutInflater.from(this).inflate(R.layout.float_bt, null, false);
            Button showBtn = view.findViewById(R.id.show);
            showBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NekoChatService.getInstance().showAccWindow();
                }
            });

            Button hideBtn = view.findViewById(R.id.hide);
            hideBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NekoChatService.getInstance().hideAccWindow();
                }
            });

            View cv = view.findViewById(R.id.ctrl);
            cv.setOnTouchListener(new View.OnTouchListener() {
                private float downX, downY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        downX = event.getRawX();
                        downY = event.getRawY();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                        if (NekoChatService.getInstance() != null)
//                            App.getInstance().savePosition(ctrlBinder.getLrcWindowParams().x, ctrlBinder.getLrcWindowParams().y);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                        float nowX = event.getRawX();
                        float nowY = event.getRawY();
                        float moveX = nowX - downX;
                        float moveY = nowY - downY;
                        if (NekoChatService.getInstance() != null) {
                            NekoChatService.getInstance().getCtrlViewParams().x += moveX;
                            NekoChatService.getInstance().getCtrlViewParams().y += moveY;
                            getWindowManager().updateViewLayout(NekoChatService.getInstance().getCtrlView(),
                                    NekoChatService.getInstance().getCtrlViewParams());
                        }
                        downX = nowX;
                        downY = nowY;
                    }
                    return true;
                }
            });
            NekoChatService.getInstance().setCtrlView(view);
        }
        if (Settings.canDrawOverlays(this)) {
            // 有权限
            NekoChatService.getInstance().showCtrlWindow();
            finishAndRemoveTask();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initAccView() {
        if(NekoChatService.getInstance().getAccView() == null){
            View view = LayoutInflater.from(this).inflate(R.layout.float_helper, null, false);
            NekoChatService.getInstance().setAccView(view);
        }
        if (Settings.canDrawOverlays(this)) {
            // 有权限
//            NekoChatService.getInstance().showAccWindow();
        }
    }
}