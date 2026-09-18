package com.zinhao.chtholly.network.openai

/**
{
"type": "json_schema",
"json_schema": {
"name": "image_comment",
"schema": "",
"strict": true
}
}
 */
data class ResponseFormat(
    val json_schema: JsonSchema,
    val type: String
)

data class JsonSchema(
    val name: String,
    val schema: NekoSchem,
    val strict: Boolean
)