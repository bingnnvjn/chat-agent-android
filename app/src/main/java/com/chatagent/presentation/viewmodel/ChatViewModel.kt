package com.chatagent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatagent.data.agent.AgentExecutor
import com.chatagent.data.model.ApiProvider
import com.chatagent.data.model.Conversation
import com.chatagent.data.model.Message
import com.chatagent.data.model.MessageType
import com.chatagent.data.repository.ChatRepository
import com.chatagent.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversations = chatRepository.conversations

    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _enableThinking = MutableStateFlow(false)
    val enableThinking: StateFlow<Boolean> = _enableThinking.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _streamingThinking = MutableStateFlow("")
    val streamingThinking: StateFlow<String> = _streamingThinking.asStateFlow()

    // 缓存 StateFlow，避免每次 getter 创建新实例
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _apiKeyCache = mutableMapOf<String, StateFlow<String>>()

    private val _wallpaperUri = MutableStateFlow("")
    val wallpaperUri: StateFlow<String> = _wallpaperUri.asStateFlow()

    private val _enableEffects = MutableStateFlow(false)
    val enableEffects: StateFlow<Boolean> = _enableEffects.asStateFlow()

    /** Agent 模式标志位 — true 时使用 Agent Loop */
    private val _agentMode = MutableStateFlow(false)
    val agentMode: StateFlow<Boolean> = _agentMode.asStateFlow()

    /** Agent 执行器 */
    private val agentExecutor = AgentExecutor().apply {
        registerDefaultTools(
            webSearch = { query ->
                // 使用百度搜索
                try {
                    val url = java.net.URL("https://www.baidu.com/s?wd=${java.net.URLEncoder.encode(query, "utf-8")}")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    val reader = conn.inputStream.bufferedReader()
                    val html = reader.readText().take(2000)
                    reader.close()
                    "搜索 \"$query\" 完成。返回${html.length}字节内容。"
                } catch (e: Exception) {
                    "搜索失败: ${e.message}"
                }
            },
            stockQuote = { code ->
                try {
                    // 新浪财经免费接口
                    val prefix = if (code.startsWith("6") || code.startsWith("9")) "sh" else "sz"
                    val url = java.net.URL("https://hq.sinajs.cn/list=${prefix}${code}")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.setRequestProperty("Referer", "https://finance.sina.com.cn")
                    val reader = conn.inputStream.bufferedReader(java.nio.charset.Charset.forName("GBK"))
                    val data = reader.readText()
                    reader.close()
                    // 解析返回格式: var hq_str_sh600519="茅台,1800.50,1795.00,..."
                    val parts = data.split("\"")
                    if (parts.size >= 2) {
                        val fields = parts[1].split(",")
                        if (fields.size >= 4) {
                            """股票: ${fields[0]}
代码: $code
开盘: ${fields[1]}
昨收: ${fields[2]}
当前: ${fields[3]}""".trimIndent()
                        } else {
                            "股票代码 $code 数据格式异常: $data"
                        }
                    } else {
                        "股票代码 $code 查询无结果"
                    }
                } catch (e: Exception) {
                    "查询失败: ${e.message}"
                }
            }
        )
    }

    /** 当前正在执行的工具信息（用于 UI 展示）*/
    private val _currentToolCall = MutableStateFlow<Pair<String, String>?>(null)
    val currentToolCall: StateFlow<Pair<String, String>?> = _currentToolCall.asStateFlow()

    private var darkThemeJob: Job? = null

    init {
        // 初始化暗色主题
        darkThemeJob = viewModelScope.launch {
            settingsRepository.isDarkTheme.collect { _isDarkTheme.value = it }
        }
        // 初始化 provider 和 model
        viewModelScope.launch {
            settingsRepository.currentProvider.collect { p ->
                _uiState.value = _uiState.value.copy(currentProvider = p)
            }
        }
        viewModelScope.launch {
            settingsRepository.currentModel.collect { m ->
                _uiState.value = _uiState.value.copy(currentModel = m)
            }
        }
        // 加载思考模式状态
        viewModelScope.launch {
            settingsRepository.enableThinking.collect { _enableThinking.value = it }
        }
        // 加载壁纸
        viewModelScope.launch {
            settingsRepository.wallpaperUri.collect { _wallpaperUri.value = it }
        }
        // 加载液态玻璃效果开关
        viewModelScope.launch {
            settingsRepository.enableEffects.collect { _enableEffects.value = it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        darkThemeJob?.cancel()
    }

    fun apiKeyForProvider(provider: ApiProvider): StateFlow<String> {
        return _apiKeyCache.getOrPut(provider.name) {
            val flow = MutableStateFlow("")
            viewModelScope.launch {
                settingsRepository.getApiKey(provider).collect { flow.value = it }
            }
            flow.asStateFlow()
        }
    }

    fun createConversation() {
        chatRepository.createConversation().let { conv ->
            _currentConversation.value = conv
        }
    }

    fun selectConversation(id: String) {
        _currentConversation.value = chatRepository.getConversation(id)
    }

    fun deleteConversation(id: String) {
        chatRepository.deleteConversation(id)
        if (_currentConversation.value?.id == id) {
            _currentConversation.value = null
        }
    }

    fun sendMessage(content: String, image: String? = null) {
        if (content.isBlank() && image == null) return

        var conversation = _currentConversation.value
        if (conversation == null) {
            conversation = chatRepository.createConversation()
            _currentConversation.value = conversation
        }

        val conv = conversation
        val thinking = _enableThinking.value

        // 立即添加用户消息到界面
        val userMsg = Message(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            content = content,
            image = image
        )
        _currentConversation.value = conv.copy(
            messages = conv.messages + userMsg,
            updatedAt = System.currentTimeMillis()
        )

        if (_agentMode.value) {
            // ─── Agent 模式：使用 Agent Loop ───
            sendAgentMessage(conv.id, content, image, thinking)
        } else {
            // ─── 普通聊天模式 ───
            sendChatMessage(conv.id, content, image, thinking)
        }
    }

    /** 切换 Agent 模式 */
    fun toggleAgentMode() {
        _agentMode.value = !_agentMode.value
    }

    // ═══════════════════════════════════════
    // 普通聊天模式
    // ═══════════════════════════════════════

    private fun sendChatMessage(
        convId: String,
        content: String,
        image: String?,
        thinking: Boolean
    ) {
        viewModelScope.launch {
            _isStreaming.value = true
            _streamingContent.value = ""
            _streamingThinking.value = ""
            chatRepository.sendMessage(
                conversationId = convId,
                content = content,
                image = image,
                enableThinking = thinking,
                onToken = { token ->
                    _streamingContent.value = _streamingContent.value + token
                },
                onThinkingToken = { token ->
                    _streamingThinking.value = _streamingThinking.value + token
                },
                onComplete = {
                    val updated = chatRepository.getConversation(convId)
                    if (updated != null) _currentConversation.value = updated
                    _streamingContent.value = ""
                    _streamingThinking.value = ""
                    _isStreaming.value = false
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error)
                    _streamingContent.value = ""
                    _streamingThinking.value = ""
                    _isStreaming.value = false
                }
            )
        }
    }

    // ═══════════════════════════════════════
    // Agent 模式：Agent Loop
    // ═══════════════════════════════════════

    private fun sendAgentMessage(
        convId: String,
        content: String,
        image: String?,
        thinking: Boolean
    ) {
        viewModelScope.launch {
            try {
                _isStreaming.value = true
                _streamingContent.value = ""
                _streamingThinking.value = ""

                val tools = agentExecutor.toApiTools()

                chatRepository.agentSendMessage(
                    conversationId = convId,
                    content = content,
                    tools = tools,
                    image = image,
                    enableThinking = thinking,
                    onToken = { token ->
                        _streamingContent.value = _streamingContent.value + token
                    },
                    onThinkingToken = { token ->
                        _streamingThinking.value = _streamingThinking.value + token
                    },
                    onToolCallStart = { toolName, args ->
                        _currentToolCall.value = toolName to args
                    },
                    onComplete = { _ ->
                        executePendingToolCalls(convId, thinking, mutableSetOf())
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(errorMessage = "Agent错误: $error")
                        resetStreaming()
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Agent异常: ${e.message}")
                resetStreaming()
            }
        }
    }

    /**
     * 执行挂起的工具调用，并继续 Agent Loop
     */
    private fun executePendingToolCalls(convId: String, thinking: Boolean, processedIds: MutableSet<String>) {
        viewModelScope.launch {
            try {
                val conv = chatRepository.getConversation(convId) ?: return@launch
                // 只找未处理的 tool_call 消息
                val toolCalls = conv.messages.filter {
                    it.type == MessageType.TOOL_CALL && it.id !in processedIds
                }
                if (toolCalls.isEmpty()) {
                    // 没有工具调用，LLM 已回复文本
                    val updated = chatRepository.getConversation(convId)
                    if (updated != null) _currentConversation.value = updated
                    resetStreaming()
                    return@launch
                }

                // 执行每个工具调用
                for (tc in toolCalls) {
                    val name = tc.toolName ?: continue
                    processedIds.add(tc.id)
                    val argsJson = tc.toolArgs ?: "{}"
                    val args = try {
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            .decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(argsJson)
                            .mapValues { it.value.toString().trim('"') }
                    } catch (_: Exception) {
                        emptyMap()
                    }

                    _currentToolCall.value = name to argsJson

                    // 执行工具
                    val result = agentExecutor.execute(name, args)

                    // 把工具结果发回 LLM
                    chatRepository.agentContinueWithToolResult(
                        conversationId = convId,
                        toolCallId = tc.toolCallId ?: tc.id,
                        toolName = name,
                        toolResult = result,
                        tools = agentExecutor.toApiTools(),
                        enableThinking = thinking,
                        onToken = { token ->
                            _streamingContent.value = _streamingContent.value + token
                        },
                        onThinkingToken = { token ->
                            _streamingThinking.value = _streamingThinking.value + token
                        },
                        onToolCallStart = { tName, tArgs ->
                            _currentToolCall.value = tName to tArgs
                        },
                        onComplete = {
                            executePendingToolCalls(convId, thinking, processedIds)
                        },
                        onError = { error ->
                            _uiState.value = _uiState.value.copy(errorMessage = "工具执行错误: $error")
                            resetStreaming()
                        }
                    )
                    return@launch  // 一次只处理一个，递归处理剩下的
                }

                val updated = chatRepository.getConversation(convId)
                if (updated != null) _currentConversation.value = updated
                resetStreaming()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Agent循环异常: ${e.message}")
                resetStreaming()
            }
        }
    }

    private fun resetStreaming() {
        _streamingContent.value = ""
        _streamingThinking.value = ""
        _isStreaming.value = false
        _currentToolCall.value = null
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }

    fun toggleThinking() {
        val newVal = !_enableThinking.value
        _enableThinking.value = newVal
        viewModelScope.launch { settingsRepository.setThinking(newVal) }
    }

    fun setProvider(provider: ApiProvider) {
        viewModelScope.launch {
            settingsRepository.setProvider(provider)
            settingsRepository.setModel(provider.defaultModel)
        }
    }

    fun setModel(model: String) {
        viewModelScope.launch { settingsRepository.setModel(model) }
    }

    fun setApiKey(provider: ApiProvider, apiKey: String) {
        viewModelScope.launch { settingsRepository.setApiKey(provider, apiKey) }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(isDark) }
    }

    fun setWallpaperUri(uri: String) {
        viewModelScope.launch { settingsRepository.setWallpaperUri(uri) }
    }

    fun toggleEffects() {
        val newVal = !_enableEffects.value
        _enableEffects.value = newVal
        viewModelScope.launch { settingsRepository.setEffects(newVal) }
    }
}

data class ChatUiState(
    val currentProvider: ApiProvider = ApiProvider.AGNES,
    val currentModel: String = "",
    val errorMessage: String? = null
)
