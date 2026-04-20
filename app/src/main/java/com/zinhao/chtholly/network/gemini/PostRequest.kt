package com.zinhao.chtholly.network.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.zinhao.chtholly.network.Tool

@JsonClass(generateAdapter = true)
data class PostRequest(
    val tools: List<Tool>? = null,
    @Json(name = "system_instruction")var systemInstruction: SystemInstruction,
    val generationConfig:GenerationConfig,
    val contents:List<Content>
)

data class SystemInstruction(val parts:List<Part>)
data class GenerationConfig(val thinkingConfig:ThinkingConfig)
data class ThinkingConfig(val thinkingLevel: String)