package com.chatagent.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chatagent.presentation.components.MessageBubble
import com.chatagent.presentation.components.WelcomeScreen
import com.chatagent.presentation.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentConversation by viewModel.currentConversation.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 当消息数量变化时滚动到底部（新消息发送）
    LaunchedEffect(currentConversation?.messages?.size) {
        currentConversation?.messages?.let { msgs ->
            if (msgs.isNotEmpty()) {
                kotlinx.coroutines.delay(50) // 等列表渲染
                listState.animateScrollToItem(msgs.size - 1)
            }
        }
    }

    // 流式输出时持续跟随滚动
    LaunchedEffect(streamingContent) {
        if (isStreaming && streamingContent.isNotEmpty()) {
            val targetIndex = currentConversation?.messages?.size ?: 0
            // 用 scrollToItem 不带动画实现实时跟随
            listState.scrollToItem(targetIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 没有对话或消息为空时显示欢迎页
        if (currentConversation == null || currentConversation!!.messages.isEmpty()) {
            WelcomeScreen(
                onSuggestionClick = { suggestion ->
                    viewModel.sendMessage(suggestion)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 顶部 spacer — 让首条消息可滑到 Top 栏下方
                item { Spacer(Modifier.height(72.dp)) }

                items(currentConversation!!.messages) { message ->
                    MessageBubble(message = message)
                }

                // 流式输出中的 AI 回复（仅在内容非空时显示）
                if (isStreaming && streamingContent.isNotEmpty()) {
                    val streamingMessage = com.chatagent.data.model.Message(
                        id = "streaming",
                        role = "assistant",
                        content = streamingContent
                    )
                    item(key = "streaming") {
                        MessageBubble(message = streamingMessage)
                    }
                }

                // 底部 spacer — 让末条消息可滑到 Under 栏下方
                item { Spacer(Modifier.height(64.dp)) }
            }
        }

        // 错误提示
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            )
        }
    }
}
