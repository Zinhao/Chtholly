package com.zinhao.chtholly.network.feishu

data class Status(
    val is_activated: Boolean,
    val is_exited: Boolean,
    val is_frozen: Boolean,
    val is_resigned: Boolean,
    val is_unjoin: Boolean
)