package com.zinhao.chtholly;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
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
        init();

    }

    @SuppressLint("ClickableViewAccessibility")
    public void init() {
        if(NekoChatService.getInstance().getFloatView() == null){
            View view = LayoutInflater.from(this).inflate(R.layout.float_helper, null, false);
            View contentView = view.findViewById(R.id.content);
            Button closeBtn = view.findViewById(R.id.close);
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NekoChatService.getInstance().hideFloatWindow();
                }
            });

            Button minBtn = view.findViewById(R.id.min);
            minBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(contentView.getVisibility() == View.GONE){
                        contentView.setVisibility(View.VISIBLE);
                    }else {
                        contentView.setVisibility(View.GONE);
                    }
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
                            NekoChatService.getInstance().getFloatViewParams().x += moveX;
                            NekoChatService.getInstance().getFloatViewParams().y += moveY;
                            getWindowManager().updateViewLayout(NekoChatService.getInstance().getFloatView(),
                                    NekoChatService.getInstance().getFloatViewParams());
                        }
                        downX = nowX;
                        downY = nowY;
                    }
                    return true;
                }
            });
            NekoChatService.getInstance().setFloatView(view);
        }
        if (Settings.canDrawOverlays(this)) {
            // 有权限
            NekoChatService.getInstance().showFloatWindow();
            finishAndRemoveTask();
        }
    }
}