package com.chatagent.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight

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
    val tintColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
    val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { selectedImageUri = it; onImagePicked(it) } }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // + 按钮（图片选择）
        Box(
            modifier = Modifier.size(34.dp).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { CircleShape },
                    effects = { blur(4f.dp.toPx()) },
                    highlight = { Highlight.Default },
                    onDrawSurface = { drawRect(tintColor) }
                ) else Modifier
            ).clip(CircleShape).clickable {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            contentAlignment = Alignment.Center
        ) { Text("+", color = placeholderColor, fontSize = 20.sp, fontWeight = FontWeight.Light) }

        Spacer(Modifier.width(8.dp))

        // 思考模式开关
        Box(
            modifier = Modifier.size(34.dp).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { CircleShape },
                    effects = { blur(4f.dp.toPx()) },
                    highlight = { Highlight.Default },
                    onDrawSurface = {
                        if (enableThinking) drawRect(accentColor)
                        else drawRect(tintColor)
                    }
                ) else Modifier
            ).clip(CircleShape).clickable { onToggleThinking() },
            contentAlignment = Alignment.Center
        ) { Text("🧠", fontSize = 16.sp) }

        Spacer(Modifier.width(8.dp))

        // 输入胶囊
        Box(
            modifier = Modifier.weight(1f).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                    effects = { blur(4f.dp.toPx()) },
                    highlight = { Highlight.Default },
                    onDrawSurface = { drawRect(tintColor) }
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
