package com.chatagent.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chatagent.presentation.components.ChatInput
import com.chatagent.presentation.components.FloatingTopBar
import com.chatagent.presentation.components.Sidebar
import com.chatagent.presentation.ui.theme.ChatAgentTheme
import com.chatagent.presentation.viewmodel.ChatUiState
import com.chatagent.presentation.viewmodel.ChatViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun MainScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState(initial = true)
    val enableThinking by viewModel.enableThinking.collectAsState()
    ChatAgentTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    val uiState by viewModel.uiState.collectAsState(ChatUiState())
    val conversations by viewModel.conversations.collectAsState(emptyList())
    val currentConversation by viewModel.currentConversation.collectAsState(null)
    var showSidebar by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    var inputText by remember { mutableStateOf("") }
    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 背景捕获层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .layerBackdrop(backdrop)
        )

        // 主聊天界面
        ChatScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // 悬浮顶栏（适配状态栏）
        FloatingTopBar(
            backdrop = backdrop,
            title = currentConversation?.title ?: "Chat Agent",
            currentProvider = uiState.currentProvider,
            onMenuClick = { showSidebar = true },
            onNewChatClick = { viewModel.createConversation() },
            onModelSelect = { model -> viewModel.setModel(model) },
            modifier = Modifier.padding(top = 12.dp)
        )

        // 悬浮底栏
        ChatInput(
            backdrop = backdrop,
            value = inputText,
            onValueChange = { inputText = it },
            enableThinking = enableThinking,
            onToggleThinking = { viewModel.toggleThinking() },
            onSend = {
                if (inputText.isNotBlank() || pendingImageUri != null) {
                    viewModel.sendMessage(inputText, pendingImageUri?.toString())
                    inputText = ""
                    pendingImageUri = null
                }
            },
            onImagePicked = { uri -> pendingImageUri = uri },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // 侧边栏
        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showSidebar = false }
                )
                Sidebar(
                    conversations = conversations,
                    currentConversationId = currentConversation?.id,
                    onConversationClick = { id -> viewModel.selectConversation(id); showSidebar = false },
                    onNewConversation = { viewModel.createConversation(); showSidebar = false },
                    onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                    onSettingsClick = { showSettings = true; showSidebar = false },
                    onClose = { showSidebar = false }
                )
            }
        }

        // 设置界面
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            SettingsScreen(viewModel = viewModel, onClose = { showSettings = false })
        }
    }
    }
    }
}
