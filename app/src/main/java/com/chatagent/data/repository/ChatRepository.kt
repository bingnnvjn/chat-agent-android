package com.chatagent.data.repository

import android.util.Log
import com.chatagent.data.api.ChatApiService
import com.chatagent.data.local.ConversationStorage
import com.chatagent.data.model.ApiMessage
import com.chatagent.data.model.ApiProvider
import com.chatagent.data.model.ChatRequest
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

    private val json = Json { ignoreUnknownKeys = true }
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

            Log.d("ChatRepository", "Provider: ${provider.displayName}, Model: $model, API Key: ${apiKey.take(8)}...")

            if (apiKey.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onError("请先设置 API Key")
                }
                return@withContext
            }

            // 构建请求消息（不包含刚添加的用户消息，避免重复）
            val apiMessages = listOf(
                ApiMessage(role = "system", content = "你是一个 AI 助手，用中文回答问题。")
            ) + conversation.messages.map {
                ApiMessage(role = it.role, content = it.content)
            } + ApiMessage(role = "user", content = content)

            val request = ChatRequest(
                model = model,
                messages = apiMessages,
                stream = true,
                temperature = 0.7,
                max_tokens = 4096
            )

            Log.d("ChatRepository", "Request URL: ${provider.baseUrl}")
            Log.d("ChatRepository", "Request messages count: ${apiMessages.size}")

            try {
                val responseBody = chatApiService.chatCompletions(
                    url = provider.baseUrl,
                    authorization = "Bearer $apiKey",
                    request = request
                )

                Log.d("ChatRepository", "Response received, starting to read stream...")

                val reader = responseBody.byteStream().bufferedReader()
                val contentBuilder = StringBuilder()
                var hasContent = false
                var lineCount = 0

                reader.useLines { lines ->
                    lines.forEach { line ->
                        lineCount++
                        Log.d("ChatRepository", "Line $lineCount: $line")
                        
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ")
                            if (data == "[DONE]") {
                                Log.d("ChatRepository", "Stream finished with [DONE]")
                                return@forEach
                            }
                            try {
                                val response = json.decodeFromString<com.chatagent.data.model.ChatResponse>(data)
                                val delta = response.choices?.firstOrNull()?.delta?.content
                                if (delta != null) {
                                    contentBuilder.append(delta)
                                    hasContent = true
                                    Log.d("ChatRepository", "Delta content: $delta")
                                    withContext(Dispatchers.Main) {
                                        onToken(delta)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Parse error: ${e.message}, data: $data")
                            }
                        } else if (line.isNotEmpty()) {
                            Log.d("ChatRepository", "Non-data line: $line")
                        }
                    }
                }

                Log.d("ChatRepository", "Stream reading finished. Total lines: $lineCount, hasContent: $hasContent")

                // 添加 AI 回复
                val aiContent = if (hasContent) contentBuilder.toString() else "（AI 未返回内容，请检查 API Key 和网络连接）"
                val currentConv = getConversation(conversationId) ?: return@withContext
                val aiMessage = Message(
                    id = System.currentTimeMillis().toString(),
                    role = "assistant",
                    content = aiContent
                )
                updateConversation(currentConv.copy(
                    messages = currentConv.messages + aiMessage,
                    updatedAt = System.currentTimeMillis()
                ))

                withContext(Dispatchers.Main) {
                    onComplete(aiContent)
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Request failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "请求失败")
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
