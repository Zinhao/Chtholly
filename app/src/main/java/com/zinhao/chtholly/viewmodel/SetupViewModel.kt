package com.zinhao.chtholly.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.utils.HostConsts
import androidx.core.content.edit
import com.zinhao.chtholly.view.adapter.SetupPagerAdapter

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
    private val _adminName = MutableLiveData<String>("")
    val adminName: LiveData<String> = _adminName

    private val _botName = MutableLiveData<String>("")
    val botName: LiveData<String> = _botName

    private val _botDescription = MutableLiveData<String>("")
    val botDescription: LiveData<String> = _botDescription

    private val _botSoulList = MutableLiveData<List<AICharacter>>()
    val botSoulList: LiveData<List<AICharacter>> = _botSoulList

    // ==================== 向导状态 ====================

    private val _currentStep = MutableLiveData<Int>(0)
    val currentStep: LiveData<Int> = _currentStep

    private val _totalSteps = MutableLiveData<Int>(SetupPagerAdapter.TOTAL_PAGE_COUNT)
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


    fun loadConfig(){
        _baseUrl.value = BotApp.getInstance().chatUrl
        _apiKey.value = BotApp.getInstance().apiKey
        _adminName.value = BotApp.getInstance().adminName
        _botName.value = BotApp.getInstance().botName
        _ttsServerUrl.value = BotApp.getInstance().ttsUrl
        _botDescription.value = BotApp.getInstance().aiSoul
        BotApp.getInstance().loadAICharacter {
            _botSoulList.postValue(it)
        }
    }
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
    }

    // ==================== 步骤控制 ====================

    fun goToNextStep() {
        if(_currentStep.value == 1){
            val chara = AICharacter(_botName.value,_botDescription.value)
            BotApp.getInstance().insert(chara,{
                _botDescription.postValue(chara.desc)
            })
        }
        val next = (_currentStep.value ?: 0) + 1
        if (next < (_totalSteps.value ?: SetupPagerAdapter.TOTAL_PAGE_COUNT)) {
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
        return url.isNotEmpty() &&
                (url.startsWith("http://") || url.startsWith("https://"))
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
        // 更新 BotApp 运行时配置
        BotApp.getInstance().apply {
            apiKey = _apiKey.value ?: ""
            botName = _botName.value ?: "红豆"
            adminName = _adminName.value ?: "Master"
            aiSoul = _botDescription.value
            ttsUrl = _ttsServerUrl.value
            chatUrl = _baseUrl.value
            isFirstRun = false
        }
        // 保存到 SharedPreferences
        val prefs = BotApp.getInstance().sharedPreferences
        prefs.edit().apply {
            putString(BotApp.CONFIG_CHAT_URL, _baseUrl.value)
            putString(BotApp.CONFIG_API_KEY, _apiKey.value)
            putString(BotApp.CONFIG_TTS_URL, _ttsServerUrl.value)
            putString(BotApp.CONFIG_ADMIN_NAME, _adminName.value)
            putString(BotApp.CONFIG_BOT_NAME, _botName.value)
            putString(BotApp.CONFIG_SOUL_DESC, _botDescription.value)
            putBoolean(BotApp.CONFIG_IS_FIRST_RUN, false)
            apply()
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
                _baseUrl.value = HostConsts.OPENAI_API_HOST
            }
            is ServerPreset.Gemini -> {
                _baseUrl.value = HostConsts.GEMINI_API_HOST
            }
            is ServerPreset.Custom -> {
                // 保持当前值或清空
                _baseUrl.value = HostConsts.GEMINI_PROXY_API_HOST
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
}