package com.chatagent.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatagent.data.model.ApiProvider
import com.chatagent.data.model.Conversation
import com.chatagent.data.model.Message
import com.chatagent.data.repository.ChatRepository
import com.chatagent.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /** 流式响应当前累积文本，避免每个 token 刷新整个对话 */
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.currentProvider.collect { provider ->
                _uiState.value = _uiState.value.copy(currentProvider = provider)
            }
        }
        viewModelScope.launch {
            settingsRepository.currentModel.collect { model ->
                _uiState.value = _uiState.value.copy(currentModel = model)
            }
        }
    }

    fun createConversation() {
        val conversation = chatRepository.createConversation()
        _currentConversation.value = conversation
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

        // 自动创建对话
        var conversation = _currentConversation.value
        if (conversation == null) {
            conversation = chatRepository.createConversation()
            _currentConversation.value = conversation
        }

        val conv = conversation
        val thinking = _enableThinking.value

        viewModelScope.launch {
            _isStreaming.value = true
            _streamingContent.value = ""
            chatRepository.sendMessage(
                conversationId = conv.id,
                content = content,
                image = image,
                enableThinking = thinking,
                onToken = { token ->
                    _streamingContent.value = _streamingContent.value + token
                },
                onComplete = { fullContent ->
                    _currentConversation.value = chatRepository.getConversation(conv.id)
                    _streamingContent.value = ""
                    _isStreaming.value = false
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error)
                    _streamingContent.value = ""
                    _isStreaming.value = false
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun toggleThinking() {
        _enableThinking.value = !_enableThinking.value
    }

    fun setProvider(provider: ApiProvider) {
        viewModelScope.launch {
            settingsRepository.setProvider(provider)
            settingsRepository.setModel(provider.defaultModel)
        }
    }

    fun setModel(model: String) {
        viewModelScope.launch {
            settingsRepository.setModel(model)
        }
    }

    fun setApiKey(provider: ApiProvider, apiKey: String) {
        viewModelScope.launch {
            settingsRepository.setApiKey(provider, apiKey)
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDark)
        }
    }

    fun apiKeyForProvider(provider: ApiProvider): StateFlow<String> {
        val flow = MutableStateFlow("")
        viewModelScope.launch {
            settingsRepository.getApiKey(provider).collect { flow.value = it }
        }
        return flow.asStateFlow()
    }

    val isDarkTheme: StateFlow<Boolean>
        get() {
            val flow = MutableStateFlow(true)
            viewModelScope.launch {
                settingsRepository.isDarkTheme.collect { flow.value = it }
            }
            return flow.asStateFlow()
        }
}

data class ChatUiState(
    val currentProvider: ApiProvider = ApiProvider.AGNES,
    val currentModel: String = "",
    val errorMessage: String? = null
)
