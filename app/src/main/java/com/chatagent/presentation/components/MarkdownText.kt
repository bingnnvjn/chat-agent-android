package com.chatagent.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    // 简化版本：直接显示文本，支持基本格式
    val formattedText = markdown
        .replace("**", "") // 移除加粗标记
        .replace("*", "") // 移除斜体标记
        .replace("`", "") // 移除代码标记
        .replace("```", "") // 移除代码块标记

    Text(
        text = formattedText,
        modifier = modifier,
        color = color,
        style = style
    )
}
