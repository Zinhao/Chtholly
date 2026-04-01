package com.zinhao.chtholly.view

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.MainViewModel
import com.zinhao.chtholly.databinding.ActivityMainBinding
import com.zinhao.chtholly.entity.AICharacter
import per.goweii.layer.core.anim.AnimStyle
import per.goweii.layer.core.widget.SwipeLayout
import per.goweii.layer.dialog.DialogLayer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if(BotApp.getInstance().isFirstRun){
            viewModel.doFirstRun(this)
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }

        viewModel.checkAccessibilityStatus(this)
        viewModel.refreshCurrentChara()

        setupObservers()
        setupUI()

        // 加载数据
        viewModel.loadMessages()
    }

    private fun setupObservers() {
        // 观察 API Key
        viewModel.apiKey.observe(this) { key ->
            val editText = binding.textInputLayout.editText
            if (editText?.text.toString() != key) {
                editText?.setText(key)
            }
        }

        // 观察 Bot 名称
        viewModel.botName.observe(this) { name ->
            val editText = binding.textInputLayout2.editText
            if (editText?.text.toString() != name) {
                editText?.setText(name)
            }
        }

        // 观察管理员名称
        viewModel.adminName.observe(this) { name ->
            val editText = binding.textInputLayout3.editText
            if (editText?.text.toString() != name) {
                editText?.setText(name)
            }
        }

        // 观察无障碍服务状态
        viewModel.accessibilityEnabled.observe(this) { enabled ->
            binding.toggleButton.isChecked = enabled
        }

        // 观察当前角色
        viewModel.currentChara.observe(this) { chara ->
            binding.textView.text = chara
        }

        // 观察消息对话框准备状态
        viewModel.isMessageDialogReady.observe(this) { ready ->
            binding.button2.isEnabled = ready
        }

        // 观察 Toast 消息
        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.consumeToastMessage()
            }
        }

        // 观察对话框显示事件
        viewModel.showDialogEvent.observe(this) { dialogContent ->
            dialogContent?.let {
                DialogLayer(this)
                    .setContentView(it)
                    .setGravity(Gravity.BOTTOM)
                    .setSwipeDismiss(SwipeLayout.Direction.BOTTOM)
                    .setBackgroundDimDefault()
                    .setContentAnimator(AnimStyle.BOTTOM)
                    .show()
                viewModel.consumeDialogEvent()
            }
        }
    }

    private fun setupUI() {
        // API Key 输入监听
        binding.textInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateApiKey(s.toString())
            }
        })

        // Bot 名称输入监听
        binding.textInputLayout2.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateBotName(s.toString())
            }
        })

        // 管理员名称输入监听
        binding.textInputLayout3.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateAdminName(s.toString())
            }
        })

        binding.button6.setOnClickListener {
            viewModel.saveConfig()
        }

        // 按钮点击事件
        binding.toggleButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.button2.setOnClickListener {
            viewModel.showMessageDialog(this)
        }

        binding.button.setOnClickListener {
            startActivity(Intent(this, CharacterActivity::class.java))
        }

        binding.button4.setOnClickListener {
            startActivity(Intent(this, ServerSettingActivity::class.java))
        }

        binding.button5.setOnClickListener {
            viewModel.showFloatWindow(this)
        }

        binding.button7.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }
}