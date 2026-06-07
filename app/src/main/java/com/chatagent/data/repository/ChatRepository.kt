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
                if (loaded.isNotEmpty()) {
                    _conversations.value = loaded
                }
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
            val apiMessages = listOf(
                ApiMessage(role = "system", content = "你是一个 AI 助手，用中文回答问题。")
            ) + conversation.messages.map { msg ->
                ApiMessage(role = msg.role, content = msg.content)
            } + ApiMessage(role = "user", content = content)

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
                val responseBody = chatApiService.chatCompletions(
                    url = provider.baseUrl,
                    authorization = "Bearer $apiKey",
                    request = request
                )

                // 流式读取响应
                val reader = responseBody.byteStream().bufferedReader()
                val contentBuilder = StringBuilder()
                val thinkingBuilder = StringBuilder()
                var lineCount = 0
                var firstDataLine = ""

                reader.useLines { lines ->
                    lines.forEach { line ->
                        lineCount++
                        if (line.startsWith("data: ") && firstDataLine.isEmpty()) {
                            firstDataLine = line.take(200)
                        }
                        if (BuildConfig.DEBUG) Log.d("ChatRepository", "Line $lineCount: $line")
                        
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                if (BuildConfig.DEBUG) Log.d("ChatRepository", "Stream finished with [DONE]")
                                return@forEach
                            }
                            try {
                                val response = json.decodeFromString<com.chatagent.data.model.ChatResponse>(data)
                                val delta = response.choices?.firstOrNull()?.delta
                                
                                // 思考内容
                                if (delta?.reasoning_content != null) {
                                    thinkingBuilder.append(delta.reasoning_content)
                                    if (BuildConfig.DEBUG) Log.d("ChatRepository", "Thinking: ${delta.reasoning_content}")
                                    withContext(Dispatchers.Main) {
                                        onToken(delta.reasoning_content)
                                    }
                                }
                                
                                // 正式回复
                                if (delta?.content != null) {
                                    contentBuilder.append(delta.content)
                                    if (BuildConfig.DEBUG) Log.d("ChatRepository", "Delta: ${delta.content}")
                                    withContext(Dispatchers.Main) {
                                        onToken(delta.content)
                                    }
                                }
                            } catch (e: Exception) {
                                if (BuildConfig.DEBUG) Log.e("ChatRepository", "Parse error: ${e.message}")
                            }
                        }
                    }
                }

                val aiContent = contentBuilder.toString().ifEmpty {
                    val hint = if (lineCount <= 1) "服务器未返回数据"
                    else "收到 $lineCount 行数据但未解析到内容"
                    "（AI 未返回内容，请检查 API Key 和网络连接。$hint）"
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
}
