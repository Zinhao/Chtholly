package com.zinhao.chtholly.network.openai


data class Schem(
    val type: String,
    val properties: RolePlayProperties? = null,
    val required: List<String>? = null,
    val additionalProperties: Boolean? = null,
)


data class RolePlayProperties(
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

