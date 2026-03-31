package com.zinhao.chtholly.networt

data class GeminiResponse(
    val candidates: List<Candidate>,
    val createTime: String,
    val modelVersion: String,
    val responseId: String,
    val usageMetadata: UsageMetadata
)