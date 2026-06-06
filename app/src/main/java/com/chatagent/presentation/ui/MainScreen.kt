package com.chatagent.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chatagent.presentation.components.FloatingTopBar
import com.chatagent.presentation.components.Sidebar
import com.chatagent.presentation.viewmodel.ChatUiState
import com.chatagent.presentation.viewmodel.ChatViewModel

@Composable
fun MainScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState(ChatUiState())
    val conversations by viewModel.conversations.collectAsState(emptyList())
    val currentConversation by viewModel.currentConversation.collectAsState(null)
    var showSidebar by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 主聊天界面
        ChatScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(top = 72.dp)
        )

        // 悬浮顶栏
        FloatingTopBar(
            title = currentConversation?.title ?: "Chat Agent",
            currentProvider = uiState.currentProvider,
            onMenuClick = { showSidebar = true },
            onNewChatClick = { viewModel.createConversation() },
            onModelSelect = { model -> viewModel.setModel(model) }
        )

        // 侧边栏
        AnimatedVisibility(
            visible = showSidebar,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 背景遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showSidebar = false }
                )

                // 侧边栏
                Sidebar(
                    conversations = conversations,
                    currentConversationId = currentConversation?.id,
                    onConversationClick = { id ->
                        viewModel.selectConversation(id)
                        showSidebar = false
                    },
                    onNewConversation = {
                        viewModel.createConversation()
                        showSidebar = false
                    },
                    onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                    onSettingsClick = {
                        showSettings = true
                        showSidebar = false
                    },
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
            SettingsScreen(
                viewModel = viewModel,
                onClose = { showSettings = false }
            )
        }
    }
}
