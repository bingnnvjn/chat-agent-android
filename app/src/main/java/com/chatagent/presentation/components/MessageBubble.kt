package com.chatagent.presentation.components
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.data.model.Message
import com.chatagent.presentation.ui.theme.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

@Composable
fun MessageBubble(
    message: Message,
    modelName: String = "AI",
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val tintColor = if (isUser) Color(0xFF10A37F) else Color(0xFF0088FF)
    val textOnTinted = Color.White
    val avatarBg = if (isUser) tintColor else Color(0xFF0088FF)
    var lineCount by remember { mutableIntStateOf(1) }
    val contentText = message.content

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
            // 头像 + 名字行
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
                    Text(modelName, color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                } else {
                    Text("我", color = Color(0xFF8E8E93), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) { Text("我", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 思考过程
            if (!isUser && message.thinkingContent != null) {
                ThinkingBadge(thinking = message.thinkingContent, isDark = isDark)
                Spacer(Modifier.height(6.dp))
            }

            // 行数检测（通过不可见的 BasicText，支持所有文本类型）
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                modifier = Modifier
                    .let { m ->
                        if (isUser) m.fillMaxWidth().wrapContentWidth(Alignment.End)
                        else m.fillMaxWidth()
                    }
            ) {
                // 隐藏的行数检测文本
                androidx.compose.foundation.text.BasicText(
                    text = contentText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    onTextLayout = { result -> lineCount = result.lineCount },
                    modifier = Modifier.alpha(0f).fillMaxWidth()
                )

                // 液态玻璃气泡
                val bubbleShape = if (lineCount <= 1) Capsule() else RoundedRectangle(20.dp)

                Box(
                    modifier = Modifier
                        .let { m ->
                            if (backdrop != null) m.drawBackdrop(
                                backdrop = backdrop,
                                shape = { bubbleShape },
                                effects = { vibrancy(); blur(2f.dp.toPx()); lens(12f.dp.toPx(), 24f.dp.toPx()) },
                                onDrawSurface = {
                                    if (tintColor.isSpecified) {
                                        drawRect(tintColor, blendMode = BlendMode.Hue)
                                        drawRect(tintColor.copy(alpha = 0.75f))
                                    }
                                }
                            ) else m.background(
                                if (isUser) tintColor else if (isDark) Color(0xFF1C1C1E) else Color(0xFFE8E8E8),
                                shape = if (lineCount <= 1) CircleShape else RoundedCornerShape(20.dp)
                            )
                        }
                        .clip(bubbleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (isUser) {
                        Text(
                            contentText,
                            color = textOnTinted,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            overflow = TextOverflow.Clip
                        )
                    } else {
                        MarkdownText(
                            markdown = contentText,
                            color = textOnTinted,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp)
                        )
                    }
                }
            }

            // 复制按钮
            if (!isUser && contentText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var copied by remember { mutableStateOf(false) }
                Text(
                    text = if (copied) "已复制 \u2713" else "复制",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp).clickable {
                        val clip = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clip.setPrimaryClip(android.content.ClipData.newPlainText("AI回复", contentText))
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
            Text("\uD83D\uDCA1", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("思考过程", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "\u25B2" else "\u25BC", color = textColor, fontSize = 10.sp)
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(8.dp))
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
