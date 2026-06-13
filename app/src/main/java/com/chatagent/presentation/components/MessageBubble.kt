package com.chatagent.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.chatagent.presentation.ui.theme.Accent

private val UserGreen = Color(0xFF10A37F)
private val AiGray = Color(0xFF1C1C1E)

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
        modifier = modifier
    ) {
        if (isUser) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier.widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(20.dp, 4.dp, 20.dp, 20.dp))
                        .background(UserGreen)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(message.content, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                // 第1行：头像 + 名字
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Accent),
                        contentAlignment = Alignment.Center
                    ) { Text("AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                    Spacer(Modifier.width(8.dp))
                    Text("DeepSeek", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                // 第2行：思考过程（如果有）
                if (!message.thinkingContent.isNullOrEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ThinkingBadge(thinking = message.thinkingContent)
                }

                // 第3行：正文气泡
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp))
                        .background(AiGray)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    MarkdownText(
                        markdown = message.content,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp)
                    )
                }

                // 第4行：复制按钮
                if (message.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    var copied by remember { mutableStateOf(false) }

                    Text(
                        text = if (copied) "已复制 ✓" else "复制",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp).clickable {
                            val clipMgr = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipMgr.setPrimaryClip(android.content.ClipData.newPlainText("AI回复", message.content))
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
}

@Composable
private fun ThinkingBadge(thinking: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.widthIn(max = 280.dp)
            .clip(RoundedCornerShape(14.dp)).background(Color(0xFF2C2C2E))
            .clickable { expanded = !expanded }.padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💡", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("思考过程", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "▲" else "▼", color = Color(0xFF8E8E93), fontSize = 10.sp)
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(thinking, color = Color(0xFFC7C7CC), fontSize = 13.sp, lineHeight = 18.sp, fontStyle = FontStyle.Italic)
            }
        }
    }
}
