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

val FileWriter = FunctionDeclaration(
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

val FileReader = FunctionDeclaration(
    description = "Read text from the file",
    name = "read_text",
    Parameters(
        properties = mapOf(
            Pair(
                "file_path", Properties(
                    description = "the file path",
                    type = "string",null
                )
            )
        ),
        listOf("file_path"),
        "object"
    )
)

val ListFiles = FunctionDeclaration(
    description = "list all file in the directory",
    name = "list_directory",
    Parameters(
        properties = mapOf(
            Pair(
                "dir_path", Properties(
                    description = "the path of the directory wait to list. If need list root,stay empty",
                    type = "string",null
                )
            ),
        ),
        listOf("dir_path",),
        "object"
    )
)

val ActionResult = FunctionDeclaration(
    description = "When you have completed the task or question raised by the user, call this function to end the conversation.",
    name = "result",
    Parameters(
        properties = mapOf(
            Pair(
                "summary", Properties(
                    description = "concise reporting",
                    type = "string",null
                )
            ),
        ),
        listOf("summary",),
        "object"
    )
)

val GEMINI_TOOLS = Tool(
    listOf(
        PrintInfo,FileWriter,FileReader,ListFiles,ActionResult
    )
)

data class FileInfo(val name: String, val isFile: Boolean,val size: Long,val time: Long)

data class Tool(
    val functionDeclarations: List<FunctionDeclaration>
)

data class FunctionDeclaration(
    val description: String,
    val name: String,
    val parameters: Parameters
)