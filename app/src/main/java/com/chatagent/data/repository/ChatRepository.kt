package com.chatagent.data.repository

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

            if (apiKey.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onError("请先设置 API Key")
                }
                return@withContext
            }

            // 构建请求
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

            try {
                val responseBody = chatApiService.chatCompletions(
                    url = provider.baseUrl,
                    authorization = "Bearer $apiKey",
                    request = request
                )

                val reader = responseBody.byteStream().bufferedReader()
                val contentBuilder = StringBuilder()

                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ")
                            if (data == "[DONE]") {
                                return@forEach
                            }
                            try {
                                val response = json.decodeFromString<com.chatagent.data.model.ChatResponse>(data)
                                val delta = response.choices?.firstOrNull()?.delta?.content
                                if (delta != null) {
                                    contentBuilder.append(delta)
                                    withContext(Dispatchers.Main) {
                                        onToken(delta)
                                    }
                                }
                            } catch (e: Exception) {
                                // 忽略解析错误
                            }
                        }
                    }
                }

                // 添加 AI 回复
                val aiMessage = Message(
                    id = System.currentTimeMillis().toString(),
                    role = "assistant",
                    content = contentBuilder.toString()
                )
                updateConversation(getConversation(conversationId)!!.copy(
                    messages = getConversation(conversationId)!!.messages + aiMessage,
                    updatedAt = System.currentTimeMillis()
                ))

                withContext(Dispatchers.Main) {
                    onComplete(contentBuilder.toString())
                }
            } catch (e: Exception) {
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
