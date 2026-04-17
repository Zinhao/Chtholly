package com.zinhao.chtholly.network.feishu

import com.zinhao.chtholly.network.feishu.Data

data class OpenIdResult(
    val code: Int,
    val `data`: Data,
    val msg: String
)