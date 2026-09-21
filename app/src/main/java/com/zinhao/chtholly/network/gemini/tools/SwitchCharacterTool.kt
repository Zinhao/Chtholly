package com.zinhao.chtholly.network.gemini.tools

import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.db.AICharacterDao
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.entity.NetAiAskAble
import com.zinhao.chtholly.network.FunImpl
import com.zinhao.chtholly.network.FunctionDeclaration
import com.zinhao.chtholly.network.ToolCallback
import com.zinhao.chtholly.network.gemini.FunctionCall
import com.zinhao.chtholly.network.gemini.Parameters
import com.zinhao.chtholly.network.gemini.Properties
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

val SwitchCharacterTool = FunctionDeclaration(
    description = "Switch to a different AI character by ID, or list all available characters if no ID is provided. Each character has its own personality and independent roleplay setting.",
    name = "switch_character",
    Parameters(
        properties = mapOf(
            "character_id" to Properties(
                description = "The ID of the character to switch to. Omit this parameter to list all available characters.",
                type = "integer", null
            )
        ),
        required = listOf(),
        type = "object"
    ),
    funImpl = object : FunImpl {
        override fun call(
            functionCall: FunctionCall,
            callback: ToolCallback,
            netAiAskAble: NetAiAskAble,
            thoughtSignature: String?
        ) {
            try {
                val app = BotApp.getInstance()
                val idArg = functionCall.args["character_id"]

                if (idArg == null) {
                    // List all characters
                    val latch = CountDownLatch(1)
                    var characters: List<AICharacter>? = null
                    app.loadAICharacter(AICharacterDao.AICharacterGetAllListener { result ->
                        characters = result
                        latch.countDown()
                    })
                    latch.await(5, TimeUnit.SECONDS)

                    if (characters == null) {
                        callback.addToolErr(functionCall.name, Exception("Failed to load characters"), thoughtSignature)
                        return
                    }

                    val currentId = app.currentCharacter?.id ?: -1
                    val array = JSONArray()
                    for (c in characters!!) {
                        array.put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("roleplay", c.isRoleplay)
                            put("is_current", c.id == currentId)
                        })
                    }

                    val result = JSONObject().apply {
                        put("success", true)
                        put("characters", array)
                        put("current_character_id", currentId)
                    }
                    callback.addToolResponse(
                        functionCall.name,
                        "character_result",
                        result.toString(),
                        thoughtSignature
                    )
                } else {
                    // Switch to specified character
                    val targetId = idArg.toString().toLong()
                    val latch = CountDownLatch(1)
                    var found: AICharacter? = null
                    app.loadAICharacter(AICharacterDao.AICharacterGetAllListener { result ->
                        for (c in result) {
                            if (c.id == targetId) {
                                found = c
                                break
                            }
                        }
                        latch.countDown()
                    })
                    latch.await(5, TimeUnit.SECONDS)

                    if (found == null) {
                        callback.addToolResponse(
                            functionCall.name,
                            "character_result",
                            JSONObject().apply {
                                put("success", false)
                                put("error", "Character with ID $targetId not found.")
                            }.toString(),
                            thoughtSignature
                        )
                        return
                    }

                    app.switchAISoul(found)
                    val result = JSONObject().apply {
                        put("success", true)
                        put("character_id", found!!.id)
                        put("character_name", found!!.name)
                        put("roleplay_enabled", found!!.isRoleplay)
                    }
                    callback.addToolResponse(
                        functionCall.name,
                        "character_result",
                        result.toString(),
                        thoughtSignature
                    )
                }
            } catch (e: Exception) {
                callback.addToolErr(functionCall.name, e, thoughtSignature)
            }
        }
    }
)
