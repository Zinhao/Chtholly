package com.zinhao.chtholly.entity.node

data class NekoNode(
    val children: List<Children>,
    val `class`: String,
    val click: Boolean,
    val event: String,
    val longClick: Boolean,
    val time: String
)