package com.chatagent.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.data.model.ApiProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

@Composable
fun FloatingTopBar(
    backdrop: Backdrop? = null,
    title: String = "Chat Agent",
    currentProvider: ApiProvider,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onModelSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }
    val tintColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
    val shadowColor = Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier.fillMaxWidth().height(76.dp).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左侧 ☰ 按钮
        GlassCircle(
            backdrop = backdrop,
            size = 44.dp,
            tintColor = tintColor,
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = onMenuClick
        ) { Text("‹", fontSize = 20.sp) }

        // 中间胶囊（模型名称）
        Box(
            modifier = Modifier.height(44.dp).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { RoundedCornerShape(22.dp) },
                    effects = { blur(8f.dp.toPx()) },
                    highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                    shadow = { Shadow(radius = 8.dp, color = shadowColor) },
                    onDrawSurface = {
                        drawRect(tintColor)
                    }
                ) else Modifier
            ).clip(RoundedCornerShape(22.dp)).clickable { showModelMenu = true }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                title.ifEmpty { currentProvider.defaultModel },
                color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        // 右侧 ⋯ 按钮
        GlassCircle(
            backdrop = backdrop,
            size = 44.dp,
            tintColor = tintColor,
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onNewChatClick
        ) { Text("⋯", fontSize = 20.sp) }
    }

    if (showModelMenu) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { showModelMenu = false },
            shape = RoundedCornerShape(16.dp), title = null,
            text = {
                androidx.compose.foundation.layout.Column {
                    currentProvider.models.forEach { model ->
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable { onModelSelect(model); showModelMenu = false }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) { Text(model, fontSize = 15.sp) }
                    }
                }
            }, confirmButton = {}
        )
    }
}

/** 玻璃圆形按钮封装 */
@Composable
private fun GlassCircle(
    backdrop: Backdrop?,
    size: androidx.compose.ui.unit.Dp,
    tintColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size).then(
            if (backdrop != null) Modifier.drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { blur(8f.dp.toPx()) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                shadow = { Shadow(radius = 8.dp, color = tintColor.copy(alpha = 0.6f)) },
                onDrawSurface = {
                    drawRect(tintColor)
                }
            ) else Modifier
        ).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
