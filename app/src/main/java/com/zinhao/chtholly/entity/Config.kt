package com.zinhao.chtholly.entity

data class AppConfig(
    val chat_url: String,

    val feishu_app_id: String = "",
    val feishu_app_secret: String = "",
    val feishu_receive_openid: String = "",


    val tts_url: String = "",


    val api_key_config: String = "",

    // 修正后的字段名
    val bot_name: String = "",
    val admin_name: String = "",
    val soul_description: String = "",

    val is_first_run: Boolean = false,
    val with_speaker: Boolean = true
)