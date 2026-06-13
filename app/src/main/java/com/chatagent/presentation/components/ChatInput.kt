package com.chatagent.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

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
    val activeTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textColorVariant = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { selectedImageUri = it; onImagePicked(it) } }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // + 按钮（图片选择）
        GlassCircle(
            backdrop = backdrop,
            size = 34.dp,
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            content = { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Light) }
        )

        Spacer(Modifier.width(8.dp))

        // 🧠 按钮
        Box(
            modifier = Modifier.size(34.dp).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { CircleShape },
                    effects = { blur(8f.dp.toPx()) },
                    highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                    shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                    onDrawSurface = {
                        if (enableThinking) drawRect(activeTint)
                        else drawRect(surfaceTint)
                    }
                ) else Modifier
            ).clip(CircleShape).clickable { onToggleThinking() },
            contentAlignment = Alignment.Center
        ) {
            Text("🧠", fontSize = 16.sp, modifier = Modifier.graphicsLayer {
                scaleX = if (enableThinking) 1.15f else 1f
                scaleY = if (enableThinking) 1.15f else 1f
            })
        }

        Spacer(Modifier.width(8.dp))

        // 输入胶囊
        Box(
            modifier = Modifier.weight(1f).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                    effects = { blur(8f.dp.toPx()) },
                    highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
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
                        .animateContentSize()
                        .let { m ->
                            if (hasSend) m.clip(RoundedCornerShape(14.dp)).background(SendGreen).clickable { onSend() }
                            else m.clip(RoundedCornerShape(14.dp)).background(textColorVariant)
                        }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("↑", color = Color.White, fontSize = 16.sp) }
            }
        }
    }
}

/** 底栏玻璃圆钮 */
@Composable
private fun GlassCircle(
    backdrop: Backdrop?,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(size).then(
            if (backdrop != null) Modifier.drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { blur(8f.dp.toPx()) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                onDrawSurface = {
                    drawRect(surfaceTint)
                }
            ) else Modifier
        ).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
