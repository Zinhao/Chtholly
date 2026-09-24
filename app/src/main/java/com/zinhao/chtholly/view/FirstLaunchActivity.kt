package com.zinhao.chtholly.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.databinding.ActivityFirstLaunchBinding
import com.zinhao.chtholly.session.RemoteChatApiSession
import com.zinhao.chtholly.utils.HostConsts

/**
 * 首次启动页：仅收集最小信息（Base URL + API Key），
 * 保存后进入 [ChatActivity]，其余配置由 Agent 在对话中完成。
 *
 * 注意：本页不清除 isFirstRun，由 Agent 调用 /finishSetup 或
 * finish_setup 工具完成配置后才清除。
 */
class FirstLaunchActivity : AppCompatActivity() {
    val binding by lazy { ActivityFirstLaunchBinding.inflate(layoutInflater) }

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val host = result.data?.getStringExtra("host")
            if (!host.isNullOrEmpty()) {
                binding.etBaseUrl.setText(host)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupPresetDropdown()
        setupInputListeners()
        binding.btScan.setOnClickListener {
            val intent = Intent(this, ScanNetActivity::class.java)
            intent.putExtra(ScanNetActivity.EXTRA_START_PORT, 1230)
            intent.putExtra(ScanNetActivity.EXTRA_END_PORT, 1240)
            scanLauncher.launch(intent)
        }
        binding.btStart.setOnClickListener { startChat() }
        loadConfig()
    }

    private fun loadConfig() {
        val prefs = BotApp.getInstance().sharedPreferences
        binding.etBaseUrl.setText(prefs.getString(BotApp.CONFIG_CHAT_URL, HostConsts.LOCAL_HOST))
        binding.etApiKey.setText(prefs.getString(BotApp.CONFIG_API_KEY, "input-your-api-key"))
    }

    private fun setupPresetDropdown() {
        val presets = listOf("OpenAI", "Gemini", "自定义")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, presets)
        binding.presetDropdown.setAdapter(adapter)
        binding.presetDropdown.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> binding.etBaseUrl.setText(HostConsts.OPENAI_API_HOST)
                1 -> binding.etBaseUrl.setText(HostConsts.GEMINI_API_HOST)
                2 -> binding.etBaseUrl.setText(BotApp.getInstance().chatUrl)
            }
            // 局域网扫描仅对自定义服务器有意义
            binding.btScan.visibility = if (position == 2) View.VISIBLE else View.GONE
        }
    }

    private fun setupInputListeners() {
        binding.etBaseUrl.addTextChangedListener {
            binding.baseUrlLayout.error = null
        }
    }

    private fun startChat() {
        val url = binding.etBaseUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        if (!isHttpUrl(url)) {
            binding.baseUrlLayout.error = "请填写以 http:// 或 https:// 开头的服务器地址"
            return
        }
        val app = BotApp.getInstance()
        app.chatUrl = url
        app.apiKey = apiKey
        app.sharedPreferences.edit()
            .putString(BotApp.CONFIG_CHAT_URL, url)
            .putString(BotApp.CONFIG_API_KEY, apiKey)
            .apply()
        val apiSession = app.apiSession
        if(apiSession is RemoteChatApiSession){
            apiSession.updateChatUrl(url)
        }
        startActivity(Intent(this, ChatActivity::class.java).putExtra(ChatActivity.IS_FIRST_TIME, true))
        finish()
    }

    private fun isHttpUrl(url: String): Boolean {
        return (url.startsWith("http://") || url.startsWith("https://")) && url.length > 10
    }
}
