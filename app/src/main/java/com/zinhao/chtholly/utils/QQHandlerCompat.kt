package com.zinhao.chtholly.utils

import android.content.Context
import com.zinhao.chtholly.handlerImpl.QQV9275Impl

object QQHandlerCompat {
    fun get(context: Context,callback: MessageCallback): QQChatHandler{
        val versionCode = BaseChatHandler.getAppVersionCode(context, QQChatHandler.PACKAGE_NAME)
        return if(versionCode == 13520){
            QQV9275Impl(context,callback)
        }else{
            QQChatHandler(context,callback)
        }
    }
}