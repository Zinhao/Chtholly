package com.zinhao.chtholly.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.OverlayUtils
import com.zinhao.chtholly.R
import java.util.*

/** @noinspection deprecation
 */
class FloatWindowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.setText("需要悬浮窗口权限")
        setContentView(t)
        if (!Settings.canDrawOverlays(this)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("还没有显示悬浮窗口的权限！")
                .setNegativeButton("取消", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        dialog.dismiss()
                        finish()
                    }
                }).setPositiveButton("去开启", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        startActivityForResult(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse(String.format(Locale.US, "package:%s", getPackageName()))
                            ), 1
                        )
                    }
                }).setCancelable(false)
            val askDrawOverlaysDialog = builder.create()
            askDrawOverlaysDialog.show()
        }
        if (Settings.canDrawOverlays(this)) {
            // 有权限
            NekoChatService.getInstance().showCtrlWindow()
            finishAndRemoveTask()
        }
    }
}