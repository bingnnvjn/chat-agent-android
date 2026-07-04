package com.chatagent.data.repository

import android.util.Log
import com.chatagent.BuildConfig
import com.chatagent.data.api.ChatApiService
import com.chatagent.data.local.ConversationStorage
import com.chatagent.data.model.ApiMessage
import com.chatagent.data.model.ApiProvider
import com.chatagent.data.model.ChatRequest
import com.chatagent.data.model.ChatTemplateKwargs
import com.chatagent.data.model.Conversation
import com.chatagent.data.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApiService: ChatApiService,
    private val settingsRepository: SettingsRepository,
    private val conversationStorage: ConversationStorage
) {
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: Flow<List<Conversation>> = _conversations.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        loadConversations()
    }

    private fun loadConversations() {
        scope.launch {
            conversationStorage.conversations.collect { loaded ->
                _conversations.value = loaded
            }
        }
    }

    private suspend fun saveConversations() {
        conversationStorage.saveConversations(_conversations.value)
    }

    fun createConversation(): Conversation {
        val conversation = Conversation(
            id = System.currentTimeMillis().toString(),
            title = "新对话"
        )
        _conversations.value = listOf(conversation) + _conversations.value
        scope.launch { saveConversations() }
        return conversation
    }

    fun deleteConversation(id: String) {
        _conversations.value = _conversations.value.filter { it.id != id }
        scope.launch { saveConversations() }
    }

    fun getConversation(id: String): Conversation? {
        return _conversations.value.find { it.id == id }
    }

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        image: String? = null,
        enableThinking: Boolean = false,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit = {},
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val conversation = getConversation(conversationId) ?: return@withContext

            // 添加用户消息
            val userMessage = Message(
                id = System.currentTimeMillis().toString(),
                role = "user",
                content = content,
                image = image
            )
            updateConversation(conversation.copy(
                messages = conversation.messages + userMessage,
                title = if (conversation.messages.isEmpty()) content.take(30) else conversation.title,
                updatedAt = System.currentTimeMillis()
            ))

            // 获取 API 配置
            val provider = settingsRepository.currentProvider.first()
            val apiKey = settingsRepository.getApiKey(provider).first()
            val model = settingsRepository.currentModel.first().ifEmpty { provider.defaultModel }

            if (BuildConfig.DEBUG) {
                Log.d("ChatRepository", "Provider: ${provider.displayName}")
                Log.d("ChatRepository", "Model: $model")
                Log.d("ChatRepository", "URL: ${provider.baseUrl}")
            }

            if (apiKey.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onError("请先设置 API Key")
                }
                return@withContext
            }

            // 构建请求消息
            val apiMessages = mutableListOf(
                ApiMessage.text("system", "你是一个 AI 助手，用中文回答问题。")
            )

            // 历史消息（纯文本）
            conversation.messages.forEach { msg ->
                apiMessages.add(ApiMessage.text(msg.role, msg.content))
            }

            // 当前用户消息 — 判断是否含图片
            val userApiMsg = if (image != null) {
                // 从相册选图 → base64 data URL
                ApiMessage.multimodal("user", content, image)
            } else {
                // 检测输入文本中是否包含图片 URL
                val imageUrl = extractImageUrl(content)
                if (imageUrl != null) {
                    val text = content.replace(imageUrl, "").trim()
                    ApiMessage.multimodal("user", text.ifEmpty { "描述这张图片" }, imageUrl)
                } else {
                    ApiMessage.text("user", content)
                }
            }
            apiMessages.add(userApiMsg)

            if (BuildConfig.DEBUG) Log.d("ChatRepository", "Image: $image")

            val request = ChatRequest(
                model = model,
                messages = apiMessages,
                stream = true,
                temperature = 0.7,
                max_tokens = 4096,
                chat_template_kwargs = if (enableThinking) ChatTemplateKwargs(enable_thinking = true) else null
            )

            if (BuildConfig.DEBUG) Log.d("ChatRepository", "Request body: $request")

            try {
                val rawResponse = chatApiService.chatCompletions(
                    url = provider.baseUrl,
                    authorization = "Bearer $apiKey",
                    request = request
                )

                // 流式读取响应
                val reader = rawResponse.byteStream().bufferedReader()
                val contentBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()
                var allText = ""
                var hadDelta = false

                reader.useLines { lines ->
                    for (line in lines) {
                        allText += line + "\n"
                        if (BuildConfig.DEBUG) Log.d("ChatRepository", "Line: $line")

                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") continue
                        try {
                            val r = json.decodeFromString<com.chatagent.data.model.ChatResponse>(data)
                            val d = r.choices?.firstOrNull()?.delta
                            if (d?.reasoning_content != null) {
                                thinkingBuilder.append(d.reasoning_content)
                                withContext(Dispatchers.Main) { onThinkingToken(d.reasoning_content) }
                            }
                            if (d?.content != null) {
                                contentBuilder.append(d.content); hadDelta = true
                                withContext(Dispatchers.Main) { onToken(d.content) }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 非流式: 整个响应是 JSON
                if (!hadDelta) {
                    try {
                        val r = json.decodeFromString<com.chatagent.data.model.ChatResponse>(allText)
                        val m = r.choices?.firstOrNull()?.message
                        if (m?.content != null) {
                            val text = (m.content as? kotlinx.serialization.json.JsonPrimitive)?.content ?: m.content.toString()
                            contentBuilder.append(text)
                            withContext(Dispatchers.Main) { onToken(text) }
                        }
                    } catch (_: Exception) {
                        if (BuildConfig.DEBUG) Log.e("ChatRepository", "Non-stream fallback failed")
                    }
                }

                val aiContent = contentBuilder.toString().ifEmpty {
                    val preview = allText.take(300).replace("\n", " ")
                    "（AI 无内容: ${allText.length}字节: $preview）"
                }
                val aiThinking = thinkingBuilder.toString().ifEmpty { null }
                if (BuildConfig.DEBUG) {
                    Log.d("ChatRepository", "Content: $aiContent")
                    Log.d("ChatRepository", "Thinking: $aiThinking")
                }

                val currentConv = getConversation(conversationId) ?: return@withContext
                val aiMessage = Message(
                    id = System.currentTimeMillis().toString(),
                    role = "assistant",
                    content = aiContent,
                    thinkingContent = aiThinking
                )
                updateConversation(currentConv.copy(
                    messages = currentConv.messages + aiMessage,
                    updatedAt = System.currentTimeMillis()
                ))

                withContext(Dispatchers.Main) {
                    onComplete(aiContent)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("ChatRepository", "Error: ${e.message}", e)
                }
                val detail = buildString {
                    append("[错误] ${e.message}")
                    if (e is java.net.ConnectException) append("\n→ 无法连接服务器，检查网络")
                    if (e is java.net.SocketTimeoutException) append("\n→ 连接超时")
                    if (e is java.io.IOException) {
                        val msg = e.message ?: ""
                        if (msg.contains("401") || msg.contains("unauthorized", true)) append("\n→ HTTP 401 认证失败，API Key 可能无效")
                        if (msg.contains("403")) append("\n→ HTTP 403 权限不足")
                        if (msg.contains("429")) append("\n→ HTTP 429 请求太频繁")
                        if (msg.contains("500")) append("\n→ HTTP 500 服务器内部错误")
                    }
                }.toString()
                withContext(Dispatchers.Main) {
                    onError(detail)
                }
            }
        }
    }

    private fun updateConversation(conversation: Conversation) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversation.id) conversation else it
        }
        scope.launch { saveConversations() }
    }

    // ═══════════════════════════════════════════
    // Agent Loop — Tool Calling 支持
    // ═══════════════════════════════════════════

    /**
     * Agent 模式的消息发送 — 支持 Tool Calling
     * LLM 可以返回文本回复或工具调用，工具执行结果自动回传
     */
    suspend fun agentSendMessage(
        conversationId: String,
        content: String,
        tools: List<com.chatagent.data.model.Tool>,
        image: String? = null,
        enableThinking: Boolean = false,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit = {},
        onToolCallStart: (toolName: String, args: String) -> Unit = { _, _ -> },
        onToolResult: (toolName: String, result: String) -> Unit = { _, _ -> },
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val conversation = getConversation(conversationId)
            if (conversation == null) {
                withContext(Dispatchers.Main) { onError("对话不存在") }
                return@withContext
            }

            val provider = settingsRepository.currentProvider.first()
            val apiKey = settingsRepository.getApiKey(provider).first()
            val model = settingsRepository.currentModel.first().ifEmpty { provider.defaultModel }

            if (apiKey.isEmpty()) {
                withContext(Dispatchers.Main) { onError("请先设置 API Key") }
                return@withContext
            }

            // 添加用户消息到历史
            val userMsg = Message(
                id = "user_${System.currentTimeMillis()}",
                role = "user", content = content, image = image
            )
            updateConversation(conversation.copy(
                messages = conversation.messages + userMsg,
                updatedAt = System.currentTimeMillis()
            ))

            // Agent Loop：发送→解析→执行工具→再发送→...→直到LLM回复文本
            var currentConv = getConversation(conversationId) ?: return@withContext
            var loopCount = 0
            val maxLoops = 10  // 防止无限循环

            while (loopCount < maxLoops) {
                loopCount++

                // 构建 API 请求
                val apiMessages = buildApiMessages(currentConv, content, image)

                val request = ChatRequest(
                    model = model,
                    messages = apiMessages,
                    stream = true,
                    temperature = 0.7,
                    max_tokens = 4096,
                    tools = tools,
                    tool_choice = com.chatagent.data.model.ToolChoice(type = "auto"),
                    chat_template_kwargs = if (enableThinking) ChatTemplateKwargs(enable_thinking = true) else null
                )

                if (BuildConfig.DEBUG) Log.d("ChatRepository", "Agent loop #$loopCount")

                // 调用 LLM
                val rawResponse = chatApiService.chatCompletions(
                    url = provider.baseUrl,
                    authorization = "Bearer $apiKey",
                    request = request
                )

                val reader = rawResponse.byteStream().bufferedReader()
                val contentBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()
                // tool_calls 是流式分片的，需要累积
                val toolCalls = mutableMapOf<Int, StringBuilder>()   // index → arguments
                val toolNames = mutableMapOf<Int, String>()          // index → name
                val toolIds = mutableMapOf<Int, String>()            // index → id
                var isToolCall = false
                var allText = ""

                reader.useLines { lines ->
                    for (line in lines) {
                        allText += line + "\n"
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") continue
                        try {
                            val r = json.decodeFromString<com.chatagent.data.model.ChatResponse>(data)
                            val d = r.choices?.firstOrNull()?.delta

                            // 思考内容
                            if (d?.reasoning_content != null) {
                                thinkingBuilder.append(d.reasoning_content)
                                withContext(Dispatchers.Main) { onThinkingToken(d.reasoning_content) }
                            }

                            // 工具调用（流式分片）
                            if (d?.tool_calls != null) {
                                isToolCall = true
                                for (tc in d.tool_calls) {
                                    // tool_calls 可能是流式分片，index 标识同一调用
                                    val idx = tc.index ?: 0
                                    if (tc.id != null) toolIds[idx] = tc.id
                                    if (tc.function?.name != null) {
                                        toolNames[idx] = tc.function.name
                                    }
                                    if (tc.function?.arguments != null) {
                                        val buf = toolCalls.getOrPut(idx) { StringBuilder() }
                                        buf.append(tc.function.arguments)
                                    }
                                }
                            }

                            // 普通文本（也可能是 tool_calls 之前的文本思考）
                            if (d?.content != null && !isToolCall) {
                                contentBuilder.append(d.content)
                                withContext(Dispatchers.Main) { onToken(d.content) }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // ─── 处理非流式响应 ───
                if (contentBuilder.isEmpty() && !isToolCall) {
                    try {
                        val r = json.decodeFromString<com.chatagent.data.model.ChatResponse>(allText)
                        val m = r.choices?.firstOrNull()?.message
                        if (m?.content != null) {
                            val text = (m.content as? kotlinx.serialization.json.JsonPrimitive)?.content ?: m.content.toString()
                            contentBuilder.append(text)
                            withContext(Dispatchers.Main) { onToken(text) }
                        }
                        // 非流式响应的 tool_calls
                        val mTc = r.choices?.firstOrNull()?.tool_calls
                        if (mTc != null && mTc.isNotEmpty()) {
                            isToolCall = true
                            for (tc in mTc) {
                                val idx = toolCalls.size
                                toolIds[idx] = tc.id ?: "call_${System.currentTimeMillis()}_$idx"
                                toolNames[idx] = tc.function?.name ?: continue
                                tc.function?.let { fn ->
                                    val buf = toolCalls.getOrPut(idx) { StringBuilder() }
                                    buf.append(fn.arguments)
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // ─── 处理工具调用 ───
                if (isToolCall && toolNames.isNotEmpty()) {
                    // 把工具调用消息加入历史
                    for ((idx, name) in toolNames) {
                        val args = toolCalls[idx]?.toString() ?: "{}"
                        val callId = toolIds[idx] ?: "call_${System.currentTimeMillis()}_$idx"

                        if (BuildConfig.DEBUG) Log.d("ChatRepository", "Tool call: $name($args)")

                        // 添加 tool_call 消息到历史
                        val toolCallMsg = Message(
                            id = callId,
                            role = "assistant",
                            content = "",
                            type = com.chatagent.data.model.MessageType.TOOL_CALL,
                            toolCallId = callId,
                            toolName = name,
                            toolArgs = args
                        )
                        currentConv = getConversation(conversationId) ?: break
                        updateConversation(currentConv.copy(
                            messages = currentConv.messages + toolCallMsg,
                            updatedAt = System.currentTimeMillis()
                        ))

                        // 通知 UI
                        withContext(Dispatchers.Main) { onToolCallStart(name, args) }

                        // 工具执行（需要外部传入 executor）
                        // 这里用回调通知外部去执行，结果通过 AgentExecutor 回传
                        // 实际上工具执行在 ViewModel 层通过 AgentExecutor 完成
                        // 这里只在消息中记录 tool_call，不执行
                    }

                    // 注意：实际的工具执行和结果回传在 ViewModel 层做
                    // 因为 ChatRepository 没有 AgentExecutor 的引用
                    // 循环会在 ViewModel 中控制
                    break  // 先退出，让 ViewModel 层处理工具执行
                }

                // ─── LLM 回复了文本 → 完成 ───
                val aiContent = contentBuilder.toString()
                if (aiContent.isNotEmpty()) {
                    val aiMsg = Message(
                        id = "ai_${System.currentTimeMillis()}",
                        role = "assistant",
                        content = aiContent,
                        thinkingContent = thinkingBuilder.toString().ifEmpty { null }
                    )
                    currentConv = getConversation(conversationId) ?: return@withContext
                    updateConversation(currentConv.copy(
                        messages = currentConv.messages + aiMsg,
                        updatedAt = System.currentTimeMillis()
                    ))
                    withContext(Dispatchers.Main) { onComplete(aiContent) }
                    return@withContext
                }

                // 既无文本也无工具调用 → 错误
                break
            }

            if (loopCount >= maxLoops) {
                withContext(Dispatchers.Main) { onError("Agent 循环超过最大次数，已终止") }
            }
        }
    }

    /**
     * 将工具执行结果发送回 LLM（Agent Loop 的下一次调用）
     */
    suspend fun agentContinueWithToolResult(
        conversationId: String,
        toolCallId: String,
        toolName: String,
        toolResult: String,
        tools: List<com.chatagent.data.model.Tool>,
        enableThinking: Boolean = false,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit = {},
        onToolCallStart: (String, String) -> Unit = { _, _ -> },
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            var currentConv = getConversation(conversationId) ?: return@withContext

            // 添加 tool_result 消息到历史
            val resultMsg = Message(
                id = "tr_${System.currentTimeMillis()}",
                role = "tool",
                content = toolResult,
                type = com.chatagent.data.model.MessageType.TOOL_RESULT,
                toolCallId = toolCallId,
                toolName = toolName
            )
            updateConversation(currentConv.copy(
                messages = currentConv.messages + resultMsg,
                updatedAt = System.currentTimeMillis()
            ))

            val provider = settingsRepository.currentProvider.first()
            val apiKey = settingsRepository.getApiKey(provider).first()
            val model = settingsRepository.currentModel.first().ifEmpty { provider.defaultModel }

            currentConv = getConversation(conversationId) ?: return@withContext
            val apiMessages = buildApiMessagesFromConv(currentConv)

            val request = ChatRequest(
                model = model,
                messages = apiMessages,
                stream = true,
                temperature = 0.7,
                max_tokens = 4096,
                tools = tools,
                tool_choice = com.chatagent.data.model.ToolChoice(type = "auto"),
                chat_template_kwargs = if (enableThinking) ChatTemplateKwargs(enable_thinking = true) else null
            )

            // 发送请求并解析响应（同上的流式解析逻辑）
            sendAgentRequest(
                provider = provider,
                apiKey = apiKey,
                request = request,
                onToken = onToken,
                onThinkingToken = onThinkingToken,
                onToolCallStart = onToolCallStart,
                onComplete = onComplete,
                onError = onError,
                conversationId = conversationId
            )
        }
    }

    // ─── Agent 请求解析（复用） ───
    private suspend fun sendAgentRequest(
        provider: ApiProvider,
        apiKey: String,
        request: ChatRequest,
        onToken: (String) -> Unit,
        onThinkingToken: (String) -> Unit,
        onToolCallStart: (String, String) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit,
        conversationId: String
    ) {
        try {
            val rawResponse = chatApiService.chatCompletions(
                url = provider.baseUrl,
                authorization = "Bearer $apiKey",
                request = request
            )

            val reader = rawResponse.byteStream().bufferedReader()
            val contentBuilder = StringBuilder()
            val toolCalls = mutableMapOf<Int, StringBuilder>()
            val toolNames = mutableMapOf<Int, String>()
            val toolIds = mutableMapOf<Int, String>()
            var isToolCall = false

            reader.useLines { lines ->
                for (line in lines) {
                    if (!line.startsWith("data: ")) continue
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") continue
                    try {
                        val r = json.decodeFromString<com.chatagent.data.model.ChatResponse>(data)
                        val d = r.choices?.firstOrNull()?.delta

                        if (d?.reasoning_content != null) {
                            withContext(Dispatchers.Main) { onThinkingToken(d.reasoning_content) }
                        }

                        if (d?.tool_calls != null) {
                            isToolCall = true
                            for (tc in d.tool_calls) {
                                val idx = tc.index ?: 0
                                if (tc.id != null) toolIds[idx] = tc.id
                                if (tc.function?.name != null) toolNames[idx] = tc.function.name
                                if (tc.function?.arguments != null) {
                                    toolCalls.getOrPut(idx) { StringBuilder() }.append(tc.function.arguments)
                                }
                            }
                        }

                        if (d?.content != null && !isToolCall) {
                            contentBuilder.append(d.content)
                            withContext(Dispatchers.Main) { onToken(d.content) }
                        }
                    } catch (_: Exception) {}
                }
            }

            if (isToolCall && toolNames.isNotEmpty()) {
                // 通知 ViewModel 层执行工具
                for ((idx, name) in toolNames) {
                    val args = toolCalls[idx]?.toString() ?: "{}"

                    val toolCallMsg = Message(
                        id = toolIds[idx] ?: "call_${System.currentTimeMillis()}_$idx",
                        role = "assistant", content = "",
                        type = com.chatagent.data.model.MessageType.TOOL_CALL,
                        toolCallId = toolIds[idx],
                        toolName = name, toolArgs = args
                    )
                    var conv = getConversation(conversationId) ?: return@sendAgentRequest
                    updateConversation(conv.copy(
                        messages = conv.messages + toolCallMsg,
                        updatedAt = System.currentTimeMillis()
                    ))
                    withContext(Dispatchers.Main) { onToolCallStart(name, args) }
                }
                return
            }

            // 文本回复
            val aiContent = contentBuilder.toString().ifEmpty {
                try {
                    // 尝试非流式回退
                    ""
                } catch (_: Exception) {
                    "(AI 未返回内容)"
                }
            }
            val aiMsg = Message(
                id = "ai_${System.currentTimeMillis()}",
                role = "assistant", content = aiContent
            )
            var conv = getConversation(conversationId) ?: return@sendAgentRequest
            updateConversation(conv.copy(
                messages = conv.messages + aiMsg,
                updatedAt = System.currentTimeMillis()
            ))
            withContext(Dispatchers.Main) { onComplete(aiContent) }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError(e.message ?: "请求失败") }
        }
    }

    // ─── 工具函数 ───

    private fun buildApiMessages(conv: Conversation, content: String, image: String?): List<ApiMessage> {
        val msgs = mutableListOf(ApiMessage.text("system", "你是一个 AI 助手，用中文回答问题。你可以使用工具来完成各种任务。"))
        conv.messages.forEach { msg ->
            when (msg.type) {
                com.chatagent.data.model.MessageType.TOOL_CALL -> {
                    // 转为 API tool_call 格式
                    val tc = kotlinx.serialization.json.buildJsonObject {
                        put("role", JsonPrimitive("assistant"))
                        putJsonObject("content") {}
                        put("tool_calls", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("id", JsonPrimitive(msg.toolCallId ?: ""))
                                put("type", JsonPrimitive("function"))
                                putJsonObject("function") {
                                    put("name", JsonPrimitive(msg.toolName ?: ""))
                                    put("arguments", JsonPrimitive(msg.toolArgs ?: "{}"))
                                }
                            })
                        })
                    }
                    msgs.add(ApiMessage(msg.role, tc))
                }
                com.chatagent.data.model.MessageType.TOOL_RESULT -> {
                    val tr = kotlinx.serialization.json.buildJsonObject {
                        put("role", JsonPrimitive("tool"))
                        put("tool_call_id", JsonPrimitive(msg.toolCallId ?: ""))
                        put("content", JsonPrimitive(msg.content))
                    }
                    msgs.add(ApiMessage("tool", tr))
                }
                else -> {
                    msgs.add(ApiMessage.text(msg.role, msg.content))
                }
            }
        }
        // 添加当前用户消息
        if (image != null) {
            msgs.add(ApiMessage.multimodal("user", content, image))
        } else {
            msgs.add(ApiMessage.text("user", content))
        }
        return msgs
    }

    private fun buildApiMessagesFromConv(conv: Conversation): List<ApiMessage> {
        val msgs = mutableListOf(ApiMessage.text("system", "你是一个 AI 助手，用中文回答问题。你可以使用工具来完成各种任务。"))
        conv.messages.forEach { msg ->
            when (msg.type) {
                com.chatagent.data.model.MessageType.TOOL_CALL -> {
                    val tc = kotlinx.serialization.json.buildJsonObject {
                        put("role", JsonPrimitive("assistant"))
                        put("content", JsonPrimitive(""))
                        put("tool_calls", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.buildJsonObject {
                                put("id", JsonPrimitive(msg.toolCallId ?: ""))
                                put("type", JsonPrimitive("function"))
                                putJsonObject("function") {
                                    put("name", JsonPrimitive(msg.toolName ?: ""))
                                    put("arguments", JsonPrimitive(msg.toolArgs ?: "{}"))
                                }
                            })
                        })
                    }
                    msgs.add(ApiMessage(msg.role, tc))
                }
                com.chatagent.data.model.MessageType.TOOL_RESULT -> {
                    val tr = kotlinx.serialization.json.buildJsonObject {
                        put("role", JsonPrimitive("tool"))
                        put("tool_call_id", JsonPrimitive(msg.toolCallId ?: ""))
                        put("content", JsonPrimitive(msg.content))
                    }
                    msgs.add(ApiMessage("tool", tr))
                }
                else -> {
                    msgs.add(ApiMessage.text(msg.role, msg.content))
                }
            }
        }
        return msgs
    }

    /** 从文本中提取图片 URL */
    private fun extractImageUrl(text: String): String? {
        val pattern = Regex("""https?://[^\s]+\.(?:jpg|jpeg|png|gif|webp|bmp)(?:\?[^\s]*)?""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.value
    }
}


