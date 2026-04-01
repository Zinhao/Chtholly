package com.zinhao.chtholly.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zinhao.chtholly.BotApp

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    // ==================== 配置数据 ====================

    // 步骤1: 服务器配置
    private val _baseUrl = MutableLiveData<String>("")
    val baseUrl: LiveData<String> = _baseUrl

    private val _apiKey = MutableLiveData<String>("")
    val apiKey: LiveData<String> = _apiKey

    // 步骤2: TTS配置
    private val _ttsServerUrl = MutableLiveData<String>("")
    val ttsServerUrl: LiveData<String> = _ttsServerUrl

    private val _ttsVoiceId = MutableLiveData<String>("")
    val ttsVoiceId: LiveData<String> = _ttsVoiceId

    // 步骤3: 角色配置
    private val _adminName = MutableLiveData<String>("狗秀金什麽")
    val adminName: LiveData<String> = _adminName

    private val _botName = MutableLiveData<String>("红豆")
    val botName: LiveData<String> = _botName

    private val _botDescription = MutableLiveData<String>("")
    val botDescription: LiveData<String> = _botDescription

    // ==================== 向导状态 ====================

    private val _currentStep = MutableLiveData<Int>(0)
    val currentStep: LiveData<Int> = _currentStep

    private val _totalSteps = MutableLiveData<Int>(4)
    val totalSteps: LiveData<Int> = _totalSteps

    private val _canGoNext = MutableLiveData<Boolean>(false)
    val canGoNext: LiveData<Boolean> = _canGoNext

    private val _setupComplete = MutableLiveData<Boolean>(false)
    val setupComplete: LiveData<Boolean> = _setupComplete

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 防抖 Handler
    private val handler = Handler(Looper.getMainLooper())
    private var validationRunnable: Runnable? = null

    // ==================== 数据更新方法 ====================

    fun updateBaseUrl(url: String) {
        _baseUrl.value = url.trim()
        BotApp.getInstance().chatUrl = _baseUrl.value
        scheduleValidation()
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key.trim()
        BotApp.getInstance().apiKey = _apiKey.value
        scheduleValidation()
    }

    fun updateTtsServerUrl(url: String) {
        _ttsServerUrl.value = url.trim()
        BotApp.getInstance().ttsUrl = _ttsServerUrl.value
        scheduleValidation()
    }

    fun updateTtsVoiceId(voiceId: String) {
        _ttsVoiceId.value = voiceId.trim()
        scheduleValidation()
    }

    fun updateAdminName(name: String) {
        _adminName.value = name.trim()
        BotApp.getInstance().adminName = _adminName.value
        scheduleValidation()
    }

    fun updateBotName(name: String) {
        _botName.value = name.trim()
        BotApp.getInstance().botName = _botName.value
        scheduleValidation()
    }

    fun updateBotDescription(desc: String) {
        _botDescription.value = desc.trim()
//        BotApp.getInstance()
    }

    // ==================== 步骤控制 ====================

    fun goToNextStep() {
        val next = (_currentStep.value ?: 0) + 1
        if (next < (_totalSteps.value ?: 4)) {
            _currentStep.value = next
            validateCurrentStep()
        } else {
            finishSetup()
        }
    }

    fun goToPreviousStep() {
        val prev = (_currentStep.value ?: 0) - 1
        if (prev >= 0) {
            _currentStep.value = prev
            validateCurrentStep()
        }
    }

    fun setCurrentStep(step: Int) {
        _currentStep.value = step
        validateCurrentStep()
    }

    // ==================== 验证逻辑 ====================

    private fun scheduleValidation() {
        validationRunnable?.let { handler.removeCallbacks(it) }
        validationRunnable = Runnable { validateCurrentStep() }
        handler.postDelayed(validationRunnable!!, 200)
    }

    private fun validateCurrentStep() {
        val step = _currentStep.value ?: 0
        val isValid = when (step) {
            0 -> validateServerStep()
            1 -> validateTtsStep()
            2 -> validatePersonaStep()
            else -> true
        }
        _canGoNext.value = isValid
    }

    private fun validateServerStep(): Boolean {
        val url = _baseUrl.value ?: ""
        val key = _apiKey.value ?: ""
        return url.isNotEmpty() &&
                (url.startsWith("http://") || url.startsWith("https://")) &&
                key.isNotEmpty()
    }

    private fun validateTtsStep(): Boolean {
        val url = _ttsServerUrl.value ?: ""
        return url.isEmpty() || // TTS 可选
                (url.startsWith("http://") || url.startsWith("https://"))
    }

    private fun validatePersonaStep(): Boolean {
        return (_adminName.value ?: "").isNotEmpty() &&
                (_botName.value ?: "").isNotEmpty()
    }

    // ==================== 完成设置 ====================

    private fun finishSetup() {
        // 保存到 SharedPreferences
        val prefs = BotApp.getInstance().sharedPreferences
        prefs.edit().apply {
            putString("base_url", _baseUrl.value)
            putString("api_key", _apiKey.value)
            putString("tts_server_url", _ttsServerUrl.value)
            putString("tts_voice_id", _ttsVoiceId.value)
            putString("admin_name", _adminName.value)
            putString("bot_name", _botName.value)
            putString("bot_description", _botDescription.value)
            putBoolean("setup_completed", true)
            apply()
        }

        // 更新 BotApp 运行时配置
        BotApp.getInstance().apply {
            apiKey = _apiKey.value ?: ""
            botName = _botName.value ?: "狗秀金什麽"
            adminName = _adminName.value ?: "红豆"
        }

        _setupComplete.value = true
    }

    fun consumeError() {
        _errorMessage.value = null
    }

    // ==================== 预设配置 ====================

    fun applyPresetConfig(preset: ServerPreset) {
        when (preset) {
            is ServerPreset.OpenAI -> {
                _baseUrl.value = "https://api.openai-proxy.org/v1/chat/completions"
            }
            is ServerPreset.Gemini -> {
                _baseUrl.value = "https://api.openai-proxy.org/google/v1beta"
            }
            is ServerPreset.Custom -> {
                // 保持当前值或清空
            }
        }
        scheduleValidation()
    }

    sealed class ServerPreset {
        object OpenAI : ServerPreset()
        object Gemini : ServerPreset()
        object Custom : ServerPreset()
    }

    // ==================== 清理 ====================

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

    // 在 SetupViewModel 中添加
    private val _ttsSpeed = MutableLiveData<Float>(1.0f)
    val ttsSpeed: LiveData<Float> = _ttsSpeed

    private val _ttsPitch = MutableLiveData<Float>(1.0f)
    val ttsPitch: LiveData<Float> = _ttsPitch

    fun updateTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
    }

    fun updateTtsPitch(pitch: Float) {
        _ttsPitch.value = pitch
    }
}