package com.zinhao.chtholly.networt

data class UsageMetadata(
    val candidatesTokenCount: Int,
    val candidatesTokensDetails: List<CandidatesTokensDetail>,
    val promptTokenCount: Int,
    val promptTokensDetails: List<PromptTokensDetail>,
    val thoughtsTokenCount: Int,
    val totalTokenCount: Int,
    val trafficType: String
)