package com.zinhao.chtholly.network.gemini

data class Content(
    val parts: List<Part>?,
    val role: String
)