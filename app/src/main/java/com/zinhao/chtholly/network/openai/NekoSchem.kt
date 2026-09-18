package com.zinhao.chtholly.network.openai


data class NekoSchem(
    val type: String,
    val properties: Properties? = null,
    val required: List<String>? = null,
    val additionalProperties: Boolean? = null,
)


data class Properties(
    val willingnessToChat: IntArg? = null,
    val replyMessage: StringArgs,
)

data class IntArg(
    val type: String,
    var description: String,
    val minimum: Int? = null,
    val maximum: Int? = null
)

data class StringArgs(
    val type: String,
    var description: String,
)

