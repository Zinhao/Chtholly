package com.zinhao.chtholly.utils

import android.content.Context
import com.zinhao.chtholly.handlerImpl.QQV13520Impl
import com.zinhao.chtholly.handlerImpl.QQV13690Impl

object QQHandlerCompat {
    fun get(context: Context,callback: MessageCallback): QQChatHandler{
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