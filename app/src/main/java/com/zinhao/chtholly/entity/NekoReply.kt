package com.zinhao.chtholly.entity

/***
 {
   "willingnessToChat": 1,//1 到 100
   "replyMessage": "你好呀，有什么需要我帮忙的吗？"
 }
 */
data class NekoReply(
    val willingnessToChat:Int,
    val replyMessage: String
)