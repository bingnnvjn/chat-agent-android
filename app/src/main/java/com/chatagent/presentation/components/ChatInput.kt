package com.chatagent.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onAttach: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tintColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 独立圆形加号按钮（玻璃悬浮）
        Box(
            modifier = Modifier.size(48.dp).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { CircleShape },
                    effects = { blur(4f.dp.toPx()) },
                    highlight = { Highlight.Default },
                    onDrawSurface = { drawRect(tintColor) }
                ) else Modifier
            ).clip(CircleShape).clickable { onAttach() },
            contentAlignment = Alignment.Center
        ) { Text("+", color = placeholderColor, fontSize = 26.sp, fontWeight = FontWeight.Light) }

        // 独立长条胶囊输入框（玻璃悬浮）
        Box(
            modifier = Modifier.weight(1f).padding(start = 10.dp).then(
                if (backdrop != null) Modifier.drawBackdrop(
                    backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                    effects = { blur(4f.dp.toPx()) },
                    highlight = { Highlight.Default },
                    onDrawSurface = { drawRect(tintColor) }
                ) else Modifier
            ).clip(RoundedCornerShape(999.dp)).height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp)
            ) {
                BasicTextField(
                    value = value, onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(SendGreen),
                    decorationBox = { inner ->
                        Box(Modifier.padding(vertical = 12.dp)) {
                            if (value.isEmpty()) Text("iMessage 信息", color = placeholderColor, fontSize = 16.sp)
                            inner()
                        }
                    }
                )

                // 发送胶囊（嵌在输入框右侧内部）
                val hasText = value.isNotBlank()
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .let { m ->
                            if (hasText) m.clip(RoundedCornerShape(17.dp)).background(SendGreen).clickable { onSend() }
                            else m.clip(RoundedCornerShape(17.dp)).background(placeholderColor.copy(alpha = 0.2f))
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("↑", color = Color.White, fontSize = 20.sp) }
            }
        }
    }
}
