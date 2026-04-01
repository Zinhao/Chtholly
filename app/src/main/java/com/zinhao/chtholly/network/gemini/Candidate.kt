package com.zinhao.chtholly.network.gemini

data class Candidate(
    val content: Content,
    val finishReason: String
)