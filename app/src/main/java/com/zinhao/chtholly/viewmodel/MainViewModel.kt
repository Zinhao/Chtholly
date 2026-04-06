package com.zinhao.chtholly

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zinhao.chtholly.entity.AICharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // ==================== LiveData 状态 ====================

    // API Key
    private val _apiKey = MutableLiveData<String>()
    val apiKey: LiveData<String> = _apiKey

    // Bot 名称
    private val _botName = MutableLiveData<String>()
    val botName: LiveData<String> = _botName

    // 管理员名称
    private val _adminName = MutableLiveData<String>()
    val adminName: LiveData<String> = _adminName

    // 无障碍服务状态
    private val _accessibilityEnabled = MutableLiveData<Boolean>()
    val accessibilityEnabled: LiveData<Boolean> = _accessibilityEnabled

    // 当前会话角色信息
    private val _currentSoul = MutableLiveData<String>()
    val currentSoul: LiveData<String> = _currentSoul

    // Toast 消息
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // ==================== 初始化 ====================

    init {
        // 从 BotApp 加载初始值
        _apiKey.value = BotApp.getInstance().apiKey ?: ""
        _botName.value = BotApp.getInstance().botName ?: ""
        _adminName.value = BotApp.getInstance().adminName ?: ""
    }

    // ==================== 数据绑定方法 ====================

    // 修改 update 方法，避免同步磁盘写入
    fun updateApiKey(newKey: String) {
        _apiKey.value = newKey
        // 只更新内存，不立即写入磁盘
        BotApp.getInstance().apiKey = newKey
    }

    fun updateBotName(newName: String) {
        _botName.value = newName
        BotApp.getInstance().botName = newName // 确保这是内存操作
    }

    fun updateAdminName(newName: String) {
        _adminName.value = newName
        BotApp.getInstance().adminName = newName // 确保这是内存操作
    }

    fun saveConfig(){
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = BotApp.getInstance().getSharedPreferences()
            prefs.edit().apply {
                putString(BotApp.CONFIG_BOT_NAME, _botName.value ?: "")
                putString(BotApp.CONFIG_ADMIN_NAME, _adminName.value ?: "")
                putString(BotApp.CONFIG_API_KEY, _apiKey.value ?: "")
                putBoolean(BotApp.CONFIG_IS_FIRST_RUN, BotApp.getInstance().isFirstRun)
                apply()
            }
        }
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 检查无障碍服务状态
     */
    fun checkAccessibilityStatus(context: Context) {
        _accessibilityEnabled.value = isAccessibilitySettingsOn(context)
    }



    /**
     * 获取当前会话角色信息
     */
    fun refreshCurrentChara() {
        _currentSoul.value = BotApp.getInstance().aiSoul
    }

    // ==================== 点击事件处理 ====================

    /**
     * 显示悬浮窗
     */
    fun showFloatWindow(context: Context) {
        val service = NekoChatService.getInstance()
        if (service != null) {
            service.showCtrlWindow()
        } else {
            _toastMessage.value = "请先打开服务"
        }
    }

    // ==================== 生命周期方法 ====================

    /**
     * 消费 Toast 消息
     */
    fun consumeToastMessage() {
        _toastMessage.value = null
    }

    // ==================== 私有工具方法 ====================

    /**
     * 检查无障碍服务是否开启
     */
    private fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        val service = context.packageName + "/" + NekoChatService::class.java.canonicalName

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.applicationContext.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)

            while (splitter.hasNext()) {
                val accessibilityService = splitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    // ==================== 清理 ====================

    override fun onCleared() {
        super.onCleared()
        // 清理资源
    }

    fun doFirstRun(context: Context) {
        BotApp.getInstance().insert(AICharacter("猫娘", context.getString(R.string.neko_chara_short)))
        BotApp.getInstance().insert(AICharacter("VTuber", context.getString(R.string.v_tuber_desc)))
        BotApp.getInstance().isFirstRun = false
        saveConfig()
    }

}