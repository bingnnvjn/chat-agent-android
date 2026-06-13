package com.chatagent.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.chatagent.data.model.ApiProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

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
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.fillMaxWidth().height(64.dp).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左按钮 — Liquid Glass + 交互
        if (backdrop != null) {
            LiquidGlassCircle(
                backdrop = backdrop,
                size = 44.dp,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onMenuClick
            ) { Text("‹", fontSize = 20.sp) }
        } else {
            Box(Modifier.size(44.dp).align(Alignment.CenterStart).clip(CircleShape)
                .clickable { onMenuClick() }, contentAlignment = Alignment.Center
            ) { Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp) }
        }

        // 中间 — 会话名
        Text(
            title.ifEmpty { currentProvider.defaultModel },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { showModelMenu = true }
        )

        // 右按钮
        if (backdrop != null) {
            LiquidGlassCircle(
                backdrop = backdrop,
                size = 44.dp,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onNewChatClick
            ) { Text("⋯", fontSize = 20.sp) }
        } else {
            Box(Modifier.size(44.dp).align(Alignment.CenterEnd).clip(CircleShape)
                .clickable { onNewChatClick() }, contentAlignment = Alignment.Center
            ) { Text("⋯", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp) }
        }
    }

    if (showModelMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showModelMenu = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            title = null,
            text = {
                androidx.compose.foundation.layout.Column {
                    currentProvider.models.forEach { model ->
                        Box(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .clickable { onModelSelect(model); showModelMenu = false }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) { Text(model, fontSize = 15.sp) }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

/** 液态玻璃圆形按钮 — 含按压变形效果 */
@Composable
private fun LiquidGlassCircle(
    backdrop: Backdrop,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(scope) }

    Box(
        modifier = modifier.size(size)
            .drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(4f.dp.toPx()); lens(12f.dp.toPx(), 24f.dp.toPx()) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.4f) },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                layerBlock = {
                    val p = highlight.progress
                    val s = lerp(1f, 1f + 4.dp.toPx() / size.height, p)
                    val maxOff = size.minDimension
                    val off = highlight.offset
                    translationX = maxOff * tanh(0.05f * off.x / maxOff)
                    translationY = maxOff * tanh(0.05f * off.y / maxOff)
                    val maxDrag = 4.dp.toPx() / size.height
                    val angle = atan2(off.y, off.x)
                    val w = size.width; val h = size.height
                    scaleX = s + maxDrag * abs(cos(angle) * off.x / size.maxDimension) * (w / h).fastCoerceAtMost(1f)
                    scaleY = s + maxDrag * abs(sin(angle) * off.y / size.maxDimension) * (h / w).fastCoerceAtMost(1f)
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            )
            .clip(CircleShape)
            .then(highlight.drawModifier)
            .then(highlight.gestureModifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
