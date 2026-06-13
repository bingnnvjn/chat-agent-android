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
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    // AGSL Shader: smoothstep 径向渐变 — 真正的晕染效果！
    private val shader = if (isRuntimeShaderSupported()) {
        RuntimeShader("""
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}""")
    } else null

    val modifier: Modifier = Modifier.drawWithContent {
        val p = progress
        if (p > 0f) {
            if (shader != null) {
                drawRect(Color.White.copy(0.08f * p), blendMode = BlendMode.Plus)
                shader.apply {
                    val pos = position(size, positionAnim.value)
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("color", Color.White.copy(0.15f * p))
                    setFloatUniform("radius", size.minDimension * 1.5f)
                    setFloatUniform("position", pos.x.fastCoerceIn(0f, size.width), pos.y.fastCoerceIn(0f, size.height))
                }
                drawRect(ShaderBrush(shader.asComposeShader()), blendMode = BlendMode.Plus)
            } else {
                drawRect(Color.White.copy(0.25f * p), blendMode = BlendMode.Plus)
            }
        }
        drawContent()
    }

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

    fun onPress(position: Offset) {
        startPos = position
        animationScope.launch {
            launch { pressProgress.animateTo(1f, pressProgressSpec) }
            launch { positionAnim.snapTo(startPos) }
        }
    }

    fun onMove(position: Offset) {
        animationScope.launch { positionAnim.snapTo(position) }
    }

    fun onRelease() {
        animationScope.launch {
            launch { pressProgress.animateTo(0f, pressProgressSpec) }
            launch { positionAnim.animateTo(startPos, positionSpec) }
        }
    }
}
