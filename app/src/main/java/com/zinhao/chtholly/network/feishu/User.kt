package com.zinhao.chtholly.network.feishu

data class User(
    val email: String,
    val mobile: String,
    val status: Status,
    val user_id: String
)