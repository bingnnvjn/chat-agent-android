package com.chatagent.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.data.model.Message
import com.chatagent.presentation.ui.theme.*

/**
 * 消息气泡 — 支持深色/浅色主题，代码预览，思考过程实时显示
 */
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val userColor = if (isDark) DarkUserBubble else LightUserBubble
    val aiColor = if (isDark) DarkAiBubble else LightAiBubble
    val textOnUser = Color.White
    val textOnAi = if (isDark) Color(0xFFE5E5E5) else Color(0xFF1C1C1E)
    val avatarBg = if (isUser) userColor else Accent

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
            // 头像 + 名字行（用户和 AI 统一格式）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isUser) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) { Text("AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (message.role == "user") "我" else "DeepSeek",
                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
                        fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        "我",
                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF8E8E93),
                        fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) { Text("我", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 思考过程（AI 专属，从一开始就在折叠框内）
            if (!isUser && message.thinkingContent != null) {
                ThinkingBadge(
                    thinking = message.thinkingContent,
                    isDark = isDark
                )
                Spacer(Modifier.height(6.dp))
            }

            // 气泡正文
            val bubbleShape = if (isUser) {
                RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp)
            } else {
                RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
            }
            val bgColor = if (isUser) userColor else aiColor

            if (isUser) {
                // 用户气泡：宽度自适应
                Box(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
                        .clip(bubbleShape)
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        message.content,
                        color = textOnUser,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
            } else {
                // AI 气泡：全宽
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(bubbleShape)
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // 使用 MarkdownText 渲染（包含代码预览）
                    MarkdownText(
                        markdown = message.content,
                        color = textOnAi,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp)
                    )
                }
            }

            // 复制按钮（AI 回复底部）
            if (!isUser && message.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var copied by remember { mutableStateOf(false) }
                Text(
                    text = if (copied) "已复制 ✓" else "复制",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp).clickable {
                        val clip = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clip.setPrimaryClip(android.content.ClipData.newPlainText("AI回复", message.content))
                        copied = true
                    }
                )
                if (copied) {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        copied = false
                    }
                }
            }
        }
    }
}

/** 思考过程预览框 — 默认折叠，实时显示思考内容 */
@Composable
private fun ThinkingBadge(thinking: String, isDark: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val badgeBg = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val textColor = if (isDark) Color(0xFFC7C7CC) else Color(0xFF6B7280)

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(badgeBg)
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💡", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("思考过程", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "▲" else "▼", color = textColor, fontSize = 10.sp)
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(8.dp))
                // 实时思考内容
                Text(
                    thinking.ifEmpty { "思考中..." },
                    color = textColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
