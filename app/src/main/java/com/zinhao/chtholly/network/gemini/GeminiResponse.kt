package com.zinhao.chtholly.network.gemini

data class GeminiResponse(
    val candidates: List<Candidate>,
    val createTime: String?,
    val modelVersion: String?,
    val responseId: String?,
    val usageMetadata: UsageMetadata?
)