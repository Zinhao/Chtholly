package com.zinhao.chtholly.network.gemini

data class FunctionCall(
    val name: String,
    val args: Map<String, Any>
)

/***
 * "functionResponse": {
 *               "name": "check_flight",
 *               "response": {
 *                 "status": "delayed",
 *                 "departure_time": "12 PM"
 *                 }
 *               }
 *             }
 */
data class FunctionResponse(
    val name: String,
    val response: Map<String, Any>
)
