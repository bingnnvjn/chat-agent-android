package com.chatagent.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    // 简单 Markdown 渲染：**粗体**、`行内代码`、行间 ```代码块```
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < markdown.length) {
            when {
                // 代码块 ```...```
                markdown.startsWith("```", i) -> {
                    val end = markdown.indexOf("```", i + 3)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            color = Color(0xFF82AAFF),
                            fontSize = style.fontSize * 0.9f
                        )) {
                            append(markdown.substring(i + 3, end))
                        }
                        i = end + 3
                    } else { append(markdown[i]); i++ }
                }
                // 行内代码 `
                markdown[i] == '`' -> {
                    val end = markdown.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(color = Color(0xFF82AAFF))) {
                            append(markdown.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(markdown[i]); i++ }
                }
                // **粗体**
                markdown.startsWith("**", i) -> {
                    val end = markdown.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(markdown.substring(i + 2, end))
                        }
                        i = end + 2
                    } else { append(markdown[i]); i++ }
                }
                // *斜体*
                markdown[i] == '*' && i + 1 < markdown.length && markdown[i + 1] != '*' -> {
                    val end = markdown.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(markdown.substring(i + 1, end))
                        }
                        i = end + 1
                    } else { append(markdown[i]); i++ }
                }
                else -> { append(markdown[i]); i++ }
            }
        }
    }

    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color
    )
}
