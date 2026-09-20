package com.zinhao.chtholly.network.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Interactions API response body.
 * Endpoint: POST /v1beta/interactions
 */
@JsonClass(generateAdapter = true)
data class InteractionResponse(
    /** Unique interaction ID, can be used as previous_interaction_id for server-side state */
    val id: String?,
    /** Ordered list of execution steps (model_output, function_call, etc.) */
    val steps: List<InteractionStep>?,
    /** Convenience: concatenated text from the last consecutive text blocks */
    @Json(name = "output_text") val outputText: String?,
    /** Token usage metadata */
    @Json(name = "usage_metadata") val usageMetadata: UsageMetadata?
)
