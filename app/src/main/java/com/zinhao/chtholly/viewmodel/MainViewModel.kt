package com.zinhao.chtholly

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.entity.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
    private val _currentChara = MutableLiveData<String>()
    val currentChara: LiveData<String> = _currentChara

    // 消息列表
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    // 消息对话框是否可显示（数据加载完成后）
    private val _isMessageDialogReady = MutableLiveData<Boolean>(false)
    val isMessageDialogReady: LiveData<Boolean> = _isMessageDialogReady

    // Toast 消息
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // 对话框显示事件
    private val _showDialogEvent = MutableLiveData<View?>()
    val showDialogEvent: LiveData<View?> = _showDialogEvent

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
     * 加载消息列表
     */
    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            BotApp.getInstance().select(object : MessageDao.MessageGetAllListener {
                override fun onSuccess(result: List<Message>) {
                    _messages.postValue(result)
                    _isMessageDialogReady.postValue(true)
                }
            })
        }
    }

    /**
     * 获取当前会话角色信息
     */
    fun refreshCurrentChara() {
        val service = NekoChatService.getInstance()
        val session = service?.getSession()
        _currentChara.value = BotApp.getInstance().currentCharacter.desc
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

    /**
     * 显示消息对话框
     */
    fun showMessageDialog(context: Context) {
        val messagesList = _messages.value ?: return

        val dialogContent = LayoutInflater.from(context).inflate(R.layout.bottom_dialog, null, false)
        val listView = dialogContent.findViewById<ListView>(R.id.list)
        listView?.adapter = MessageAdapter(context, android.R.layout.simple_list_item_2, messagesList)

        _showDialogEvent.value = dialogContent
    }

    // ==================== 生命周期方法 ====================

    /**
     * 消费 Toast 消息
     */
    fun consumeToastMessage() {
        _toastMessage.value = null
    }

    /**
     * 消费对话框事件
     */
    fun consumeDialogEvent() {
        _showDialogEvent.value = null
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

    // ==================== 数据类定义 ====================

    sealed class NavigationEvent {
        object AccessibilitySettings : NavigationEvent()
        object CharacterSettings : NavigationEvent()
        object VoiceSettings : NavigationEvent()
    }

    // ==================== Adapter（可移到单独文件）====================

    class MessageAdapter(
        context: Context,
        resource: Int,
        objects: List<Message>
    ) : ArrayAdapter<Message>(context, resource, objects) {

        @NonNull
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView

            if (view == null) {
                view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
            }

            view?.let {
                val tvTitle = it.findViewById<TextView>(android.R.id.text1)
                tvTitle.text = getItem(position)?.getSpeaker()

                val tvResult = it.findViewById<TextView>(android.R.id.text2)
                tvResult.text = getItem(position)?.getMessage()
            }

            return view!!
        }
    }

    // ==================== 清理 ====================

    override fun onCleared() {
        super.onCleared()
        // 清理资源
    }

    fun doFirstRun(context: Context) {
        BotApp.getInstance().insert(AICharacter("猫娘", context.getString(R.string.neko_chara_1)))
        BotApp.getInstance().insert(AICharacter("VTuber", context.getString(R.string.v_tuber_desc)))
        BotApp.getInstance().isFirstRun = false
        saveConfig()
    }

}