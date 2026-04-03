package com.zinhao.chtholly.network.gemini

data class Parameters(
    val properties: Map<String, Properties>,
    val required: List<String>,
    val type: String
)