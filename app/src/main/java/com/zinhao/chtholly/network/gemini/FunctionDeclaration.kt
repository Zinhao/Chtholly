package com.zinhao.chtholly.network.gemini

val PrintInfo = FunctionDeclaration(
    description = "call function when you need",
    name = "call_function",
    Parameters(
        properties = mapOf(
            Pair(
                "name", Properties(
                    description = "the function name",
                    enum = listOf(
                        "runInfo",
                        "help",
                        "printContext",
                        "closeAutoAction",
                        "openAutoAction",
                        "printSoul",
                        "everyDayCheck",
                        "screenShot",
                        "sendNewestPic",
                        "summarize",
                        "screenShare",
                        "videoCall",
                        "sendGallery",
                        "battery",
                        "takePhoto",
                        "recordVideo",
                    ),
                    type = "string"
                )
            )
        ),
        listOf("name"),
        "object"
    )
)
val CodeGenerate = FunctionDeclaration(
    description = "Write text to the file, Save text to the file",
    name = "write_text_to_file",
    Parameters(
        properties = mapOf(
            Pair(
                "text_content", Properties(
                    description = "the text wait to write to file",
                    type = "string",null
                )
            ),
            Pair(
                "file_name", Properties(
                    description = "the file name, like \"main.java, app.dart\". No abs path! ",
                    type = "string",null
                )
            )
        ),
        listOf("text_content","file_name"),
        "object"
    )
)
val GEMINI_TOOLS = Tool(
    listOf(
        PrintInfo,CodeGenerate
    )
)

data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

data class FunctionDeclaration(
    val description: String,
    val name: String,
    val parameters: Parameters
)