package com.zinhao.chtholly.network.gemini

data class FunctionCall(
    val name: String,
    val args: Map<String, Any>
)
