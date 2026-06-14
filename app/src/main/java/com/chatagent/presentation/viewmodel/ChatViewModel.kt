package com.chatagent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatagent.data.model.ApiProvider
import com.chatagent.data.model.Conversation
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
        val userMsg = com.chatagent.data.model.Message(
            id = "user_${System.currentTimeMillis()}",
            role = "user",
            content = content,
            image = image
        )
        _currentConversation.value = conv.copy(
            messages = conv.messages + userMsg,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _isStreaming.value = true
            _streamingContent.value = ""
            _streamingThinking.value = ""
            chatRepository.sendMessage(
                conversationId = conv.id,
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
                    val updated = chatRepository.getConversation(conv.id)
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
}

data class ChatUiState(
    val currentProvider: ApiProvider = ApiProvider.AGNES,
    val currentModel: String = "",
    val errorMessage: String? = null
)
