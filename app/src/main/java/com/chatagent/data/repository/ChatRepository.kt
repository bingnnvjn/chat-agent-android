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

    /** 从文本中提取图片 URL */
    private fun extractImageUrl(text: String): String? {
        val pattern = Regex("""https?://[^\s]+\.(?:jpg|jpeg|png|gif|webp|bmp)(?:\?[^\s]*)?""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.value
    }
}
