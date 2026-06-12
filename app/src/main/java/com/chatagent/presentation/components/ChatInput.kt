package com.chatagent.presentation.components

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
    val circleGlass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop, shape = { CircleShape },
            effects = { blur(4f.dp.toPx()) },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) }
        )
    } else Modifier

    val capsuleGlass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
            effects = { blur(4f.dp.toPx()) },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) }
        )
    } else Modifier

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = circleGlass.size(46.dp).clip(CircleShape).clickable { onAttach() },
            contentAlignment = Alignment.Center
        ) { Text("+", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 26.sp, fontWeight = FontWeight.Light) }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = capsuleGlass.weight(1f).clip(RoundedCornerShape(999.dp))
                .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp).height(46.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = value, onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(SendGreen),
                    decorationBox = { inner ->
                        Box(Modifier.padding(vertical = 8.dp)) {
                            if (value.isEmpty()) Text("iMessage 信息", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                            inner()
                        }
                    }
                )
                val sendBg = if (value.isNotBlank()) SendGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier.height(34.dp).clip(RoundedCornerShape(17.dp))
                        .background(sendBg)
                        .then(if (value.isNotBlank()) Modifier.clickable { onSend() } else Modifier)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("↑", color = Color.White, fontSize = 20.sp) }
            }
        }
    }
}
