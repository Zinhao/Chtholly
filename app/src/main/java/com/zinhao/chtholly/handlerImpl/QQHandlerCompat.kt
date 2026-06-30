package com.zinhao.chtholly.handlerImpl

import android.content.Context
import com.zinhao.chtholly.utils.BaseChatHandler
import com.zinhao.chtholly.utils.FileLogger
import com.zinhao.chtholly.utils.MessageCallback

object QQHandlerCompat {
    fun get(context: Context, callback: MessageCallback): QQChatHandler {
        val versionCode = BaseChatHandler.getAppVersionCode(context, QQChatHandler.PACKAGE_NAME)
        FileLogger.i("QQHandlerCompat","qq version code:${versionCode}")
        return if(versionCode == 13520){
            QQV13520Impl(context,callback)
        }else if(versionCode == 13690){
            // 9.2.80
            QQV13690Impl(context, callback)
        }else{
            QQChatHandler(context,callback)
        }
    }
}