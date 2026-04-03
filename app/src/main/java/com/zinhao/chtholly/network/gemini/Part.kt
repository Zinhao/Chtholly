package com.zinhao.chtholly.network.gemini

data class Part(
    val text: String?,
    val functionCall: FunctionCall?,
    val thoughtSignature: String
)