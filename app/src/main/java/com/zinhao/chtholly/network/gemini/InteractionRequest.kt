package com.zinhao.chtholly.network.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Interactions API request body.
 * Endpoint: POST /v1beta/interactions
 */
@JsonClass(generateAdapter = true)
data class InteractionRequest(
    /** Model ID, e.g. "gemini-3.8-flash" */
    val model: String,
    /** Input: either a plain string or a list of interaction steps for multi-turn */
    val input: Any,
    /** Flat list of function tools */
    val tools: List<InteractionTool>? = null,
    @Json(name = "system_instruction") val systemInstruction: String? = null,
    @Json(name = "generation_config") val generationConfig: InteractionGenerationConfig? = null,
    /** Whether to store the interaction server-side for previous_interaction_id reuse */
    val store: Boolean = false,
    /** ID of a previous interaction for server-side state management */
    @Json(name = "previous_interaction_id") val previousInteractionId: String? = null,
    /** Enable server-sent events streaming */
    val stream: Boolean = false
)

/**
 * Tool definition for the Interactions API.
 * Unlike generateContent's nested functionDeclarations, tools are flat here.
 */
@JsonClass(generateAdapter = true)
data class InteractionTool(
    val type: String = "function",
    val name: String,
    val description: String,
    val parameters: Parameters
)

@JsonClass(generateAdapter = true)
data class InteractionGenerationConfig(
    @Json(name = "thinking_level") val thinkingLevel: String? = null
)

/**
 * A single step in an interaction.
 * For user_input / model_output: content contains text blocks.
 * For function_call: name + arguments.
 * For function_result: name + content (text with JSON response).
 */
@JsonClass(generateAdapter = true)
data class InteractionStep(
    val type: String,
    val content: List<InteractionContent>? = null,
    val name: String? = null,
    val arguments: Map<String, Any>? = null,
    @Json(name = "thought_signature") val thoughtSignature: String? = null
)

@JsonClass(generateAdapter = true)
data class InteractionContent(
    val type: String = "text",
    val text: String? = null
)
