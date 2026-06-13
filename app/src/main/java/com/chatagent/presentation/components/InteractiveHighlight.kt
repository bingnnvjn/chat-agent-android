package com.chatagent.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 交互式高光 — 跟踪手指位置+按压进度
 * 从 AndroidLiquidGlass 目录库移植
 */
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {
    private val pressProgressSpec = spring(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgress = Animatable(0f, 0.001f)
    private val positionAnim = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPos = Offset.Zero
    val progress: Float get() = pressProgress.value
    val offset: Offset get() = positionAnim.value - startPos

    /** 绘制高光层 */
    val drawModifier: Modifier = Modifier.drawWithContent {
        val p = progress
        if (p > 0f) {
            drawRect(Color.White.copy(0.08f * p), blendMode = BlendMode.Plus)
            val pos = position(size, positionAnim.value)
            drawCircle(
                color = Color.White.copy(0.15f * p),
                radius = size.minDimension * 1.5f,
                center = androidx.compose.ui.geometry.Offset(
                    pos.x.fastCoerceIn(0f, size.width),
                    pos.y.fastCoerceIn(0f, size.height)
                ),
                blendMode = BlendMode.Plus
            )
        }
        drawContent()
    }

    /** 手势跟踪 */
    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPos = down.position
                animationScope.launch {
                    launch { pressProgress.animateTo(1f, pressProgressSpec) }
                    launch { positionAnim.snapTo(startPos) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgress.animateTo(0f, pressProgressSpec) }
                    launch { positionAnim.animateTo(startPos, positionSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgress.animateTo(0f, pressProgressSpec) }
                    launch { positionAnim.animateTo(startPos, positionSpec) }
                }
            }
        ) { change, _ ->
            animationScope.launch { positionAnim.snapTo(change.position) }
        }
    }
}
