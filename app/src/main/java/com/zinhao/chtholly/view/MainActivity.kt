package com.zinhao.chtholly.view

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.MainViewModel
import com.zinhao.chtholly.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        if(BotApp.getInstance().isFirstRun){
            viewModel.doFirstRun(this)
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.checkAccessibilityStatus(this)

        setupObservers()
        setupUI()

        viewModel.refreshCurrentChara()
        viewModel.updateBotName(BotApp.getInstance().botName)
        viewModel.updateAdminName(BotApp.getInstance().adminName)
    }

    private fun setupObservers() {
        // 观察 API Key
        viewModel.apiKey.observe(this) { key ->

        }

        // 观察 Bot 名称
        viewModel.botName.observe(this) { name ->
            binding.tvBotName.setText(name ?: "")
        }

        // 观察管理员名称
        viewModel.adminName.observe(this) { name ->
            setTitle("当前管理员: $name")
        }

        // 观察无障碍服务状态
        viewModel.accessibilityEnabled.observe(this) { enabled ->
            binding.toggleButton.isChecked = enabled
        }

        // 观察当前角色
        viewModel.currentSoul.observe(this) { chara ->
            binding.tvSoul.text = chara
        }

        // 观察 Toast 消息
        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.consumeToastMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCurrentChara()
        viewModel.checkAccessibilityStatus(this)
    }

    private fun setupUI() {
        // 按钮点击事件
        binding.toggleButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.button2.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        binding.button5.setOnClickListener {
            viewModel.showFloatWindow(this)
        }

        binding.button7.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }

        binding.btSoulEdit.setOnClickListener {
            startActivity(Intent(this, CharacterActivity::class.java))
        }
    }
}