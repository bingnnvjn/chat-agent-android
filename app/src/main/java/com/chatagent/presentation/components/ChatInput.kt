package com.chatagent.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.Capsule
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.chatagent.presentation.components.scale
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sign
import kotlin.math.tanh
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private val SendGreen = Color(0xFF10A37F)

/**
 * 底部液态玻璃输入栏 — 1.2x 放大版
 * 左侧 + 按钮 + 输入胶囊（含向上箭头 + 思考模式弹出）
 */
@Composable
fun ChatInput(
    backdrop: Backdrop? = null,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enableThinking: Boolean = false,
    onToggleThinking: () -> Unit = {},
    onImagePicked: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = androidx.compose.ui.platform.LocalDensity.current

    // 1.2x 尺寸常量
    val btnSize = 44.dp
    val capsuleHeight = 44.dp

    // 思考模式弹出
    var showThinkingMenu by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { selectedImageUri = it; onImagePicked(it) }
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // + 液态玻璃按钮 (1.2x)
            if (backdrop != null) {
                BottomCircleButton(backdrop, btnSize, onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Light, color = Color.White) }
            } else {
                Box(Modifier.size(btnSize).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) { Text("+", color = placeholderColor, fontSize = 24.sp) }
            }

            Spacer(Modifier.width(10.dp))

            // 输入胶囊（完全体液态玻璃：自适应亮度 + 交互变形 + 渐变模糊 + 箭头）
            val capsuleScope = rememberCoroutineScope()
            val capsuleHighlight = remember(capsuleScope) { InteractiveHighlight(capsuleScope) }
            val capsulePx = with(density) { capsuleHeight.toPx() }

            // 自适应亮度采样
            val lumLayer = rememberGraphicsLayer()
            val luminanceAnim = remember { Animatable(0.5f) }
            LaunchedEffect(backdrop) {
                val buffer = IntArray(25)
                while (isActive) {
                    try {
                        val img = lumLayer.toImageBitmap()
                        val thumb = img.scale(5, 5)
                        thumb.readPixels(buffer)
                        val avg = buffer.sumOf { argb ->
                            val r = (argb shr 16 and 0xFF) / 255f
                            val g = (argb shr 8 and 0xFF) / 255f
                            val b = (argb and 0xFF) / 255f
                            0.2126 * r + 0.7152 * g + 0.0722 * b
                        } / buffer.size
                        launch { luminanceAnim.animateTo(avg.toFloat(), tween(1000)) }
                    } catch (_: Exception) {}
                }
            }

            val lum = luminanceAnim.value

            // 底部渐变模糊 shader
            val bottomFadeShader = """
uniform shader content;
uniform float2 size;
half4 main(float2 coord) {
    float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
    return content.eval(coord) * blurAlpha;
}"""

            Box(
                modifier = Modifier.weight(1f).height(capsuleHeight)
                    .let { m ->
                        if (backdrop != null) m.drawBackdrop(
                            backdrop = backdrop, shape = { Capsule() },
                            effects = {
                                val l = (lum * 2f - 1f).let { sign(it) * it * it }
                                colorControls(
                                    brightness = if (l > 0f) lerp(0.1f, 0.5f, l) else lerp(0.1f, -0.2f, -l),
                                    contrast = if (l > 0f) lerp(1f, 0f, l) else 1f,
                                    saturation = 1.5f
                                )
                                vibrancy()
                                blur(if (l > 0f) lerp(4f.dp.toPx(), 8f.dp.toPx(), l) else lerp(4f.dp.toPx(), 2f.dp.toPx(), -l))
                                lens(12f.dp.toPx(), 22f.dp.toPx())
                                runtimeShaderEffect("BottomFade", bottomFadeShader, "content") {}
                            },
                            layerBlock = {
                                val p = capsuleHighlight.progress
                                val off = capsuleHighlight.offset
                                val s = lerp(1f, 1f + 3f / capsulePx, p)
                                translationX = capsulePx * tanh(0.03f * off.x / capsulePx)
                                translationY = capsulePx * tanh(0.03f * off.y / capsulePx)
                                scaleX = s; scaleY = s
                            },
                            onDrawBackdrop = { d -> d(); lumLayer.record { d() } }
                        ) else m
                    }
                    .clip(Capsule())
                    .pointerInput(capsuleScope) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            capsuleHighlight.onPress(down.position)
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null) capsuleHighlight.onMove(change.position)
                            } while (event.changes.any { it.pressed })
                            capsuleHighlight.onRelease()
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ← 向上箭头头（点击弹出思考模式）
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showThinkingMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // 只有箭头头（三角形），无竖线
                        Text("▲", fontSize = 14.sp, color = textColor)
                    }

                    // 输入框
                    BasicTextField(
                        value = value, onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = textColor, fontSize = 17.sp),
                        cursorBrush = SolidColor(SendGreen),
                        decorationBox = { inner ->
                            Box(Modifier.padding(vertical = 12.dp)) {
                                if (value.isEmpty() && selectedImageUri == null) {
                                    Text("iMessage 信息", color = placeholderColor, fontSize = 17.sp)
                                }
                                inner()
                            }
                        }
                    )

                    // 发送按钮
                    val hasSend = value.isNotBlank() || selectedImageUri != null
                    Box(
                        modifier = Modifier.height(32.dp)
                            .let { m ->
                                if (hasSend) m.clip(RoundedCornerShape(14.dp)).background(SendGreen).clickable { onSend() }
                                else m.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f))
                            }.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("↑", color = Color.White, fontSize = 19.sp) }
                }
            }
        }

        // 思考模式弹出浮层
        if (showThinkingMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 4.dp, y = (-capsuleHeight - 8.dp))
                    .width(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .let { m ->
                        if (backdrop != null) m.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(14.dp) },
                            effects = { vibrancy(); blur(6f.dp.toPx()); lens(8f.dp.toPx(), 14f.dp.toPx()) },
                            onDrawSurface = { drawRect(Color(0xFF1C1C1E).copy(alpha = 0.85f)) }
                        ) else m.background(Color(0xFF1C1C1E))
                    }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleThinking(); showThinkingMenu = false }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        if (enableThinking) "🧠 思考模式 ✓" else "🧠 思考模式",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // 点击外部关闭
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showThinkingMenu = false }
            )
        }
    }
}

/** 底部液态玻璃圆形按钮 — 1.2x */
@Composable
private fun BottomCircleButton(
    backdrop: Backdrop,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(scope) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { size.toPx() }

    Box(
        modifier = Modifier.size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = { vibrancy(); blur(2f.dp.toPx()); lens(10f.dp.toPx(), 18f.dp.toPx()) },
                layerBlock = {
                    val p = highlight.progress
                    val s = lerp(1f, 1f + 3f / sizePx, p)
                    val off = highlight.offset
                    translationX = sizePx * tanh(0.05f * off.x / sizePx)
                    translationY = sizePx * tanh(0.05f * off.y / sizePx)
                    val drag = 3f / sizePx
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
