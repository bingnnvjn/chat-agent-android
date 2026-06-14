package com.chatagent.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.chatagent.data.model.ApiProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 顶部液态玻璃导航栏
 * 按钮效果直接移植自 LiquidButton 示例组件
 */
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
    val capsuleSize = 44.dp

    Box(
        modifier = modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左：液态玻璃圆形按钮
        if (backdrop != null) {
            TopLiquidCircleButton(backdrop, capsuleSize, Modifier.align(Alignment.CenterStart), onMenuClick) {
                Text("‹", fontSize = 20.sp, color = Color.White)
            }
        } else {
            Box(Modifier.size(capsuleSize).align(Alignment.CenterStart).clip(CircleShape)
                .clickable { onMenuClick() }, contentAlignment = Alignment.Center
            ) { Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp) }
        }

        // 中：会话标题胶囊 — 最大 5 字 + "..."
        val displayTitle = title.let { if (it.length > 5) it.take(5) + "…" else it }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val capsuleSizePx = with(density) { capsuleSize.toPx() }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = capsuleSize, max = capsuleSize * 4)
                .height(capsuleSize)
                .let { m ->
                    if (backdrop != null) {
                        // 添加顶部渐变模糊
                        val alphaMaskShader = """
uniform shader content;
uniform float2 size;
half4 main(float2 coord) {
    float blurAlpha = smoothstep(0.0, size.y * 0.5, size.y - coord.y);
    return content.eval(coord) * blurAlpha;
}"""
                        m.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                runtimeShaderEffect("TopFade", alphaMaskShader, "content")
                            },
                            onDrawSurface = {}
                        )
                    } else m
                }
                .clip(Capsule())
                .clickable(onClick = { showModelMenu = true }),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayTitle.ifEmpty { currentProvider.defaultModel },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 右：液态玻璃圆形按钮
        if (backdrop != null) {
            TopLiquidCircleButton(backdrop, capsuleSize, Modifier.align(Alignment.CenterEnd), onNewChatClick) {
                Text("⋯", fontSize = 20.sp, color = Color.White)
            }
        } else {
            Box(Modifier.size(capsuleSize).align(Alignment.CenterEnd).clip(CircleShape)
                .clickable { onNewChatClick() }, contentAlignment = Alignment.Center
            ) { Text("⋯", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp) }
        }
    }

    if (showModelMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showModelMenu = false },
            shape = RoundedCornerShape(16.dp),
            title = null,
            text = {
                androidx.compose.foundation.layout.Column {
                    currentProvider.models.forEach { model ->
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
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

/** 顶部圆形液态按钮 — 从 LiquidButton 示例直接移植 */
@Composable
private fun TopLiquidCircleButton(
    backdrop: Backdrop,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(scope) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { size.toPx() }

    Box(
        modifier = modifier.size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = { vibrancy(); blur(2f.dp.toPx()); lens(12f.dp.toPx(), 24f.dp.toPx()) },
                layerBlock = {
                    val p = highlight.progress
                    val s = lerp(1f, 1f + 4f / sizePx, p)
                    val off = highlight.offset
                    translationX = sizePx * tanh(0.05f * off.x / sizePx)
                    translationY = sizePx * tanh(0.05f * off.y / sizePx)
                    val drag = 4f / sizePx
                    val angle = atan2(off.y, off.x)
                    scaleX = s + drag * abs(cos(angle) * off.x / sizePx)
                    scaleY = s + drag * abs(sin(angle) * off.y / sizePx)
                },
                onDrawSurface = {}
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier),
        contentAlignment = Alignment.Center
    ) { content() }
}
