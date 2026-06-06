package com.chatagent.data.model

enum class ApiProvider(
    val displayName: String,
    val baseUrl: String,
    val models: List<String>,
    val defaultModel: String,
    val website: String
) {
    AGNES(
        displayName = "Agnes AI",
        baseUrl = "https://apihub.agnes-ai.com/v1/chat/completions",
        models = listOf("agnes-2.0-flash", "agnes-1.5-flash"),
        defaultModel = "agnes-2.0-flash",
        website = "https://platform.agnes-ai.com"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com/v1/chat/completions",
        models = listOf("deepseek-v4-flash", "deepseek-v4-pro", "deepseek-chat", "deepseek-reasoner"),
        defaultModel = "deepseek-v4-flash",
        website = "https://platform.deepseek.com"
    ),
    MINIMAX(
        displayName = "MiniMax",
        baseUrl = "https://api.minimax.chat/v1/text/chatcompletion_v2",
        models = listOf("MiniMax-M3", "MiniMax-M2.7", "MiniMax-M2.5", "abab6.5s-chat"),
        defaultModel = "MiniMax-M3",
        website = "https://platform.minimaxi.com"
    ),
    GLM(
        displayName = "GLM (智谱)",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
        models = listOf("GLM-5.1", "GLM-5", "GLM-4.7", "glm-4-flash"),
        defaultModel = "GLM-5.1",
        website = "https://open.bigmodel.cn"
    ),
    KIMI(
        displayName = "Kimi (月之暗面)",
        baseUrl = "https://api.moonshot.cn/v1/chat/completions",
        models = listOf("kimi-k2.6", "kimi-k2.5", "kimi-k2-turbo", "moonshot-v1-8k"),
        defaultModel = "kimi-k2.6",
        website = "https://platform.moonshot.cn"
    ),
    MIMO(
        displayName = "MiMo (小米)",
        baseUrl = "https://api.mimo.ai/v1/chat/completions",
        models = listOf("MiMo-V2.5-Pro", "MiMo-V2.5", "MiMo-V2.5-TTS"),
        defaultModel = "MiMo-V2.5-Pro",
        website = "https://platform.xiaomimimo.com"
    ),
    OPENAI(
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1/chat/completions",
        models = listOf("gpt-4o", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo"),
        defaultModel = "gpt-4o",
        website = "https://platform.openai.com"
    ),
    CUSTOM(
        displayName = "自定义",
        baseUrl = "",
        models = emptyList(),
        defaultModel = "",
        website = ""
    );

    companion object {
        fun fromName(name: String): ApiProvider {
            return entries.find { it.name == name } ?: AGNES
        }
    }
}
