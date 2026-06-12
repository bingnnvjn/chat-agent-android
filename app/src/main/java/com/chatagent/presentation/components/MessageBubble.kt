package com.chatagent.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.data.model.Message
import com.chatagent.presentation.ui.theme.Accent
import com.chatagent.presentation.ui.theme.White

private val UserGreen = Color(0xFF10A37F)
private val AiGray = Color(0xFF1C1C1E)

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    if (isUser) {
        // 用户：绿色右对齐气泡
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                    .background(UserGreen)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = message.content, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp)
            }
        }
    } else {
        // AI：深灰色左对齐气泡 + 思考弹窗
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // AI 头像
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    // 思考内容（独立小窗口）
                    if (!message.thinkingContent.isNullOrEmpty()) {
                        ThinkingBadge(thinking = message.thinkingContent)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // 回复气泡
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                            .background(AiGray)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        MarkdownText(
                            markdown = message.content,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp, lineHeight = 22.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingBadge(thinking: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2C2C2E))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "💡", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("思考过程", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "▲" else "▼", color = Color(0xFF8E8E93), fontSize = 10.sp)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(thinking, color = Color(0xFFC7C7CC), fontSize = 13.sp, lineHeight = 18.sp, fontStyle = FontStyle.Italic)
        }
    }
}
