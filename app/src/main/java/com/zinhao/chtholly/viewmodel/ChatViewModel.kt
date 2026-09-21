package com.zinhao.chtholly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.MessageDao
import com.zinhao.chtholly.entity.*
import com.zinhao.chtholly.session.GeminiSession
import com.zinhao.chtholly.session.OpenAiSession
import com.zinhao.chtholly.utils.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChatViewModel(application: Application) : AndroidViewModel(application) , NetAiAskAble.DelayReplyCallback {
    private val TAG = ChatViewModel::class.java.simpleName
    // 消息列表
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    // 消息对话框是否可显示（数据加载完成后）
    private val _isMessageDialogReady = MutableLiveData<Boolean>(false)
    val isMessageDialogReady: LiveData<Boolean> = _isMessageDialogReady

    private val _autoCompleteArr = MutableLiveData<List<Pair<String, String>>>(emptyList())
    val autoCompleteArr: LiveData<List<Pair<String, String>>> = _autoCompleteArr

    // Streaming state
    private val _streamingMessage = MutableLiveData<Message?>()
    val streamingMessage: LiveData<Message?> = _streamingMessage

    private val _isStreaming = MutableLiveData<Boolean>(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    private var currentStreamMessage: Message? = null

    private inner class StreamingCallback : NetAiAskAble.StreamCallback {
        override fun onStreamStart(message: NetAiAskAble) {
            _isStreaming.postValue(true)
            val tempMessage = Message(BotApp.getInstance().botName, "", System.currentTimeMillis(), BotApp.getInstance().currentSessionId)
            currentStreamMessage = tempMessage
            _streamingMessage.postValue(tempMessage)
        }

        override fun onStreamChunk(message: NetAiAskAble, chunk: String) {
            currentStreamMessage?.let {
                it.message = chunk
                _streamingMessage.postValue(it)
            }
        }

        override fun onStreamComplete(message: NetAiAskAble) {
            _isStreaming.postValue(false)
            currentStreamMessage?.let {
                addBotMessage(it)
                BotApp.getInstance().insert(it)
            }
            currentStreamMessage = null
            _streamingMessage.postValue(null)
        }

        override fun onStreamError(message: NetAiAskAble, e: Exception) {
            _isStreaming.postValue(false)
            currentStreamMessage = null
            _streamingMessage.postValue(null)
        }
    }

    /**
     * 加载消息列表（按当前会话）
     */
    fun loadMessages() {
        val sessionId = BotApp.getInstance().currentSessionId
        loadMessages(sessionId)
    }

    /**
     * 加载指定会话的消息列表
     */
    fun loadMessages(sessionId: Long) {
        BotApp.getInstance().loadMessageBySession(sessionId, MessageDao.MessageGetAllListener { result ->
            _messages.postValue(result)
            _isMessageDialogReady.postValue(false)
        })
        val map = Command.getMethodDescMap(Command::class.java)
        _autoCompleteArr.value = map.toList()
    }


    fun addBotMessage(message: Message) {
        val messageList = _messages.value?.toMutableList()?: return
        messageList.add(message)
        _messages.postValue(messageList)
    }

    fun sendMessage(currentUser: String, content: String) {
        val newMessage = Message(currentUser, content, System.currentTimeMillis())
        newMessage.isEnableCommand = true

        val messageList = _messages.value?.toMutableList()?: return
        messageList.add(newMessage)
        _messages.value = messageList

        val mainAskable = createAskable(newMessage)
        // Set stream callback for OpenAI session
        if (mainAskable is NetAiAskAble) {
            FileLogger.d(TAG,"setStreamCallback")
            mainAskable.setStreamCallback(StreamingCallback())
        }
        BotApp.getInstance().insert(newMessage)
        viewModelScope.launch(Dispatchers.IO) {
            mainAskable.handle()
            if (mainAskable.isReplyReady){
                mainAskable.answer?.let {
                    addBotMessage(it)
                }

            }
        }
    }

    fun createAskable(message: Message): Command {
        val mainAskable: Command?
        if (BotApp.getInstance().mode == OpenAiSession::class.java) {
            mainAskable = OpenAiAskAble(BotApp.getInstance().packageName, message, this)
        } else if (BotApp.getInstance().mode == GeminiSession::class.java) {
            mainAskable = GeminiAIAskAble(BotApp.getInstance().packageName, message, this)
        } else {
            mainAskable = NekoAskAble(BotApp.getInstance().packageName, message)
        }
        return mainAskable
    }

    override fun onReplySuccess(message: NetAiAskAble?) {
        addBotMessage(message!!.answer)
        message.finishTextReply()
        message.finishStepAction()
    }
}