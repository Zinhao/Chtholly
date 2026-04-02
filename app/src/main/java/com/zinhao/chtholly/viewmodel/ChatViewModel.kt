package com.zinhao.chtholly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.*
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.session.OpenAiSession
import com.zinhao.chtholly.utils.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChatViewModel(application: Application) : AndroidViewModel(application) , NetAiAskAble.DelayReplyCallback {

    // 消息列表
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    // 消息对话框是否可显示（数据加载完成后）
    private val _isMessageDialogReady = MutableLiveData<Boolean>(false)
    val isMessageDialogReady: LiveData<Boolean> = _isMessageDialogReady

    /**
     * 加载消息列表
     */
    fun loadMessages() {
        BotApp.getInstance().loadMessage(MessageDao.MessageGetAllListener { result ->
            _messages.postValue(result)
            _isMessageDialogReady.postValue(false)
        })
    }

    fun addBotMessage(message: Message) {
        val messageList = _messages.value?.toMutableList()?: return
        messageList.add(message)
        _messages.postValue(messageList)
    }

    fun sendMessage(currentUser: String, content: String) {
        val newMessage = Message(currentUser, content, System.currentTimeMillis())

        val messageList = _messages.value?.toMutableList()?: return
        messageList.add(newMessage)
        _messages.value = messageList

        val mainMessage = createMessage(newMessage)
        BotApp.getInstance().insert(newMessage)
        viewModelScope.launch(Dispatchers.IO) {
            mainMessage.handle()
            if (mainMessage.isReplay){
                mainMessage.answer?.let {
                    addBotMessage(it)
                }

            }
        }
    }

    fun createMessage(message: Message): Command {
        val mainMessage: Command?
        if (NekoChatService.mode == OpenAiSession::class.java) {
            mainMessage = OpenAiAskAble(BotApp.getInstance().packageName, message, this)
        } else if (NekoChatService.mode == GeminiSession::class.java) {
            mainMessage = GeminiAIAskAble(BotApp.getInstance().packageName, message, this)
        } else {
            mainMessage = NekoAskAble(BotApp.getInstance().packageName, message)
        }
        return mainMessage
    }

    override fun onReply(message: NetAiAskAble?) {
        addBotMessage(message!!.answer)
        message.finishTextReply()
        message.finishStepAction()
    }
}