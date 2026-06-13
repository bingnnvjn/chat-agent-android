package com.chatagent.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.tanh
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private val SendGreen = Color(0xFF10A37F)

/**
 * 底部液态玻璃输入栏
 * 圆形按钮使用 LiquidButton 示例效果模式
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

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { selectedImageUri = it; onImagePicked(it) }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // + 液态玻璃按钮
        if (backdrop != null) {
            LiquidCircleButton(backdrop, 34.dp, onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Light, color = Color.White) }
        } else {
            Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) { Text("+", color = placeholderColor, fontSize = 20.sp) }
        }

        Spacer(Modifier.width(8.dp))

        // 🧠 液态玻璃按钮（带激活状态反馈）
        if (backdrop != null) {
            LiquidCircleButton(
                backdrop = backdrop, size = 34.dp,
                onClick = onToggleThinking,
                surfaceTint = if (enableThinking) Color(0xFF10A37F).copy(alpha = 0.3f) else Color.Transparent
            ) {
                Text("🧠", fontSize = 16.sp)
            }
        } else {
            Box(Modifier.size(34.dp).clip(CircleShape)
                .background(if (enableThinking) Color(0xFF10A37F).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) { Text("🧠", fontSize = 16.sp) }
        }

        Spacer(Modifier.width(8.dp))

        // 输入胶囊（液态玻璃 + 交互变形 + 自适应亮度）
        val capsuleScope = rememberCoroutineScope()
        val capsuleHighlight = remember(capsuleScope) { InteractiveHighlight(capsuleScope) }
        val isDark = isSystemInDarkTheme()
        val lum = if (isDark) 0.2f else 0.7f
        val capsulePx = with(density) { 34.dp.toPx() }

        Box(
            modifier = Modifier.weight(1f).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                    effects = {
                        val l = lum
                        colorControls(
                            brightness = if (l > 0.5f) 0.05f else 0.15f,
                            contrast = if (l > 0.5f) 1f else 0.9f
                        )
                        vibrancy()
                        blur(if (l > 0.5f) 2f.dp.toPx() else 4f.dp.toPx())
                        lens(10f.dp.toPx(), 18f.dp.toPx())
                    },
                    layerBlock = {
                        val p = capsuleHighlight.progress
                        val off = capsuleHighlight.offset
                        val s = lerp(1f, 1f + 2f / capsulePx, p)
                        translationX = capsulePx * tanh(0.03f * off.x / capsulePx)
                        translationY = capsulePx * tanh(0.03f * off.y / capsulePx)
                        scaleX = s; scaleY = s
                    },
                    onDrawSurface = {}
                ) else Modifier
            ).clip(RoundedCornerShape(999.dp)).height(34.dp)
                .then(capsuleHighlight.modifier)
                .pointerInput(capsuleScope) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        capsuleHighlight.onPress(down.position)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change != null) {
                                capsuleHighlight.onMove(change.position)
                            }
                        } while (event.changes.any { it.pressed })
                        capsuleHighlight.onRelease()
                    }
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp)) {
                BasicTextField(
                    value = value, onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                    cursorBrush = SolidColor(SendGreen),
                    decorationBox = { inner ->
                        Box(Modifier.padding(vertical = 10.dp)) {
                            if (value.isEmpty() && selectedImageUri == null) {
                                Text("iMessage 信息", color = placeholderColor, fontSize = 14.sp)
                            }
                            inner()
                        }
                    }
                )
                val hasSend = value.isNotBlank() || selectedImageUri != null
                Box(
                    modifier = Modifier.height(28.dp)
                        .let { m ->
                            if (hasSend) m.clip(RoundedCornerShape(14.dp)).background(SendGreen).clickable { onSend() }
                            else m.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f))
                        }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("↑", color = Color.White, fontSize = 16.sp) }
            }
        }
    }
}

/** 底部液态玻璃圆形按钮 — 从 LiquidButton 示例移植 */
@Composable
private fun LiquidCircleButton(
    backdrop: Backdrop,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    surfaceTint: Color = Color.Transparent,
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
                effects = { vibrancy(); blur(2f.dp.toPx()); lens(8f.dp.toPx(), 14f.dp.toPx()) },
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
                onDrawSurface = {
                    if (surfaceTint.alpha > 0f) drawRect(surfaceTint)
                }
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
