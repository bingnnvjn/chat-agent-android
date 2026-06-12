package com.chatagent.presentation.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.presentation.components.ChatInput
import com.chatagent.presentation.components.MessageBubble
import com.chatagent.presentation.components.WelcomeScreen
import com.chatagent.presentation.viewmodel.ChatViewModel
import com.kyant.backdrop.Backdrop

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val currentConversation by viewModel.currentConversation.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val enableThinking by viewModel.enableThinking.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 新消息或流式内容变化时自动滚动到底部
    val scrollTarget = remember {
        derivedStateOf {
            val msgs = currentConversation?.messages
            if (msgs != null && msgs.isNotEmpty()) {
                if (isStreaming) msgs.size  // 流式：滚动到消息列表末尾（streaming item 在之后）
                else msgs.size - 1
            } else null
        }
    }

    LaunchedEffect(currentConversation?.messages?.size) {
        if (currentConversation?.messages?.isNotEmpty() == true) {
            listState.animateScrollToItem(currentConversation!!.messages.size - 1)
        }
    }

    // 流式输出时持续滚动
    LaunchedEffect(isStreaming, streamingContent) {
        if (isStreaming && currentConversation?.messages?.isNotEmpty() == true) {
            val target = currentConversation!!.messages.size  // streaming item index
            if (target > 0) {
                listState.animateScrollToItem(target)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (currentConversation == null || currentConversation!!.messages.isEmpty()) {
            WelcomeScreen(
                onSuggestionClick = { suggestion ->
                    inputText = ""
                    viewModel.sendMessage(suggestion)
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 输入框
        ChatInput(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            },
            onAttach = { /* TODO: 接入文件/图片选择 */ }
            enableThinking = enableThinking,
            onToggleThinking = { viewModel.toggleThinking() }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
