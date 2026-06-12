package com.chatagent.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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

    // 新消息时滚动到底部
    LaunchedEffect(currentConversation?.messages?.size) {
        currentConversation?.messages?.let { msgs ->
            if (msgs.isNotEmpty()) {
                listState.animateScrollToItem(msgs.size - 1)
            }
        }
    }

    // 流式输出时滚动到 streaming item
    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            currentConversation?.messages?.let { msgs ->
                listState.animateScrollToItem(msgs.size)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                    .fillMaxSize()
                    .padding(top = 76.dp, bottom = 80.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(currentConversation!!.messages) { message ->
                    MessageBubble(message = message)
                }

                if (isStreaming) {
                    val streamingMessage = com.chatagent.data.model.Message(
                        id = "streaming",
                        role = "assistant",
                        content = streamingContent
                    )
                    item {
                        MessageBubble(message = streamingMessage)
                    }
                }
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
                    .padding(bottom = 70.dp)
            )
        }
    }
}
