package com.chatagent.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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

private val SendGreen = Color(0xFF10A37F)

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
    val surfaceTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
    val activeTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { selectedImageUri = it; onImagePicked(it) }
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // + 按钮
        if (backdrop != null) {
            LiquidGlassCircleSmall(
                backdrop = backdrop, size = 34.dp,
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onDrawSurface = { drawRect(surfaceTint) }
            ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Light) }
        } else {
            Box(Modifier.size(34.dp).clip(CircleShape).clickable { }.background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) { Text("+", color = placeholderColor, fontSize = 20.sp) }
        }

        Spacer(Modifier.width(8.dp))

        // 🧠 按钮
        if (backdrop != null) {
            LiquidGlassCircleSmall(
                backdrop = backdrop, size = 34.dp,
                onClick = onToggleThinking,
                onDrawSurface = { if (enableThinking) drawRect(activeTint) else drawRect(surfaceTint) }
            ) { Text("🧠", fontSize = 16.sp) }
        } else {
            Box(Modifier.size(34.dp).clip(CircleShape).clickable { }.background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) { Text("🧠", fontSize = 16.sp) }
        }

        Spacer(Modifier.width(8.dp))

        // 输入胶囊
        Box(
            modifier = Modifier.weight(1f).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                    effects = { vibrancy(); blur(4f.dp.toPx()); lens(10f.dp.toPx(), 18f.dp.toPx()) },
                    highlight = { Highlight(width = 0.5.dp, alpha = 0.4f) },
                    shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                    onDrawSurface = { drawRect(surfaceTint) }
                ) else Modifier
            ).clip(RoundedCornerShape(999.dp)).height(34.dp)
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
                            else m.clip(RoundedCornerShape(14.dp)).background(placeholderColor.copy(alpha = 0.2f))
                        }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("↑", color = Color.White, fontSize = 16.sp) }
            }
        }
    }
}

/** 小型液态玻璃圆钮 */
@Composable
private fun LiquidGlassCircleSmall(
    backdrop: Backdrop,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onDrawSurface: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(scope) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { size.toPx() }

    Box(
        modifier = Modifier.size(size)
            .drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(4f.dp.toPx()); lens(8f.dp.toPx(), 14f.dp.toPx()) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.4f) },
                shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.04f)) },
                layerBlock = {
                    val p = highlight.progress
                    val s = lerp(1f, 1f + 3f / sizePx, p)
                    val maxOff = sizePx
                    val off = highlight.offset
                    translationX = maxOff * tanh(0.05f * off.x / maxOff)
                    translationY = maxOff * tanh(0.05f * off.y / maxOff)
                    val maxDrag = 3f / sizePx
                    val angle = atan2(off.y, off.x)
                    scaleX = s + maxDrag * abs(cos(angle) * off.x / sizePx) * 1f
                    scaleY = s + maxDrag * abs(sin(angle) * off.y / sizePx) * 1f
                },
                onDrawSurface = onDrawSurface
            )
            .clip(CircleShape)
            .then(highlight.drawModifier)
            .then(highlight.gestureModifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
