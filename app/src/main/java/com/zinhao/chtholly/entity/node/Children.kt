package com.zinhao.chtholly.entity.node

data class Children(
    val children: List<Children>?,
    val `class`: String,
    val click: Boolean?,
    val desc: String?,
    val id: String?,
    val longClick: Boolean?,
    val text: String?
)