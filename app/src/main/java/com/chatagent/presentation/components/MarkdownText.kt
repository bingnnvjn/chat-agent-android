package com.chatagent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Markdown 文本渲染
 * 支持：**粗体**、`行内代码`、```代码块(带预览框+复制)```
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val codeBg = Color(0xFF1E1E2E)
    val codeTextColor = Color(0xFFCDD6F4)

    // 按 ```代码块``` 分割文本
    val segments = remember(markdown) {
        val list = mutableListOf<Segment>()
        var i = 0
        while (i < markdown.length) {
            val codeStart = markdown.indexOf("```", i)
            if (codeStart == -1) {
                // 剩余纯文本
                list.add(Segment.Text(markdown.substring(i)))
                break
            }
            // 代码块前的文本
            if (codeStart > i) {
                list.add(Segment.Text(markdown.substring(i, codeStart)))
            }
            val codeEnd = markdown.indexOf("```", codeStart + 3)
            if (codeEnd == -1) {
                list.add(Segment.Text(markdown.substring(codeStart)))
                break
            }
            // 提取代码语言和内容
            val raw = markdown.substring(codeStart + 3, codeEnd).trimStart()
            val langEnd = raw.indexOf('\n')
            val (lang, code) = if (langEnd == -1) {
                "" to raw
            } else {
                raw.substring(0, langEnd).trim() to raw.substring(langEnd + 1)
            }
            list.add(Segment.Code(lang, code.trimEnd()))
            i = codeEnd + 3
        }
        list
    }

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        // 打字机动画需要的显示字符数（流式输出时逐渐增加）
        var showLen by remember { mutableStateOf(markdown.length) }

        // 监听 markdown 变化，逐渐增加 showLen（打字机效果）
        // 注意：这个效果在流式输出时每个 token 更新都会触发
        androidx.compose.runtime.LaunchedEffect(markdown) {
            if (showLen < markdown.length) {
                // 新内容到达时逐个增加显示字符
                showLen = markdown.length
            }
        }

        for (seg in segments) {
            when (seg) {
                is Segment.Text -> {
                    val text = if (seg.text.length > showLen) seg.text.substring(0, showLen) else seg.text
                    if (text.isNotBlank()) {
                        InlineMarkdown(text = text, color = color, style = style)
                    }
                }
                is Segment.Code -> {
                    // 代码预览框
                    CodePreviewBox(
                        code = seg.code,
                        language = seg.lang,
                        codeBg = codeBg,
                        codeTextColor = codeTextColor
                    )
                }
            }
        }
    }
}

/** 代码预览框 — 可滚动 + 复制按钮 */
@Composable
private fun CodePreviewBox(
    code: String,
    language: String,
    codeBg: Color,
    codeTextColor: Color
) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    var copied by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(codeBg)
    ) {
        // 代码内容（可水平和垂直滚动）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = code,
                color = codeTextColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
        }

        // 右上角：语言标签 + 复制按钮
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            if (language.isNotBlank()) {
                Text(
                    text = language,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (copied) "已复制" else "复制",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clickable {
                        val clip = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clip.setPrimaryClip(android.content.ClipData.newPlainText("代码", code))
                        copied = true
                    }
            )
        }
    }
    if (copied) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
}

/** 行内 Markdown：粗体、斜体、行内代码 */
@Composable
private fun InlineMarkdown(
    text: String,
    color: Color,
    style: TextStyle
) {
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // 行内代码 `
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            color = Color(0xFF82AAFF),
                            background = Color(0x33000000)
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                // **粗体**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(text[i]); i++ }
                }
                // *斜体*
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(text[i]); i++ }
                }
                else -> { append(text[i]); i++ }
            }
        }
    }

    Text(
        text = annotated,
        style = style,
        color = color
    )
}

private sealed class Segment {
    data class Text(val text: String) : Segment()
    data class Code(val lang: String, val code: String) : Segment()
}
