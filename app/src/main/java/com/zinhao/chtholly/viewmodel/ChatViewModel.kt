package com.zinhao.chtholly.viewmodel

import android.app.Application
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChatViewModel(application: Application) : AndroidViewModel(application) {

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
        BotApp.getInstance().select(MessageDao.MessageGetAllListener { result ->
            _messages.postValue(result)
            _isMessageDialogReady.postValue(true)
        })
//        viewModelScope.launch(Dispatchers.IO) {
//
//        }
    }

    fun sendMessage(currentUser: String, content: String) {
        val messageList = _messages.value?.toMutableList()?: return
        messageList.add(Message(currentUser,content, System.currentTimeMillis()))
        _messages.postValue(messageList)
    }
}