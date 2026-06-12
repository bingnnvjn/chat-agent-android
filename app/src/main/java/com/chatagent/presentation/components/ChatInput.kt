package com.chatagent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.chatagent.presentation.ui.theme.Accent
import com.chatagent.presentation.ui.theme.White
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

@Composable
fun ChatInput(
    backdrop: Backdrop? = null,
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit = {},
    enableThinking: Boolean = false,
    onToggleThinking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val modifierWithBackdrop = if (backdrop != null) {
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 20f.dp.toPx())
                },
                highlight = { Highlight.Default },
                shadow = { Shadow.Default }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    }

    Box(modifier = modifierWithBackdrop) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧加号
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onAttach() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 输入框
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(Accent),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                text = "输入消息...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 右侧按钮组
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (enableThinking) Accent.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable { onToggleThinking() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💡", fontSize = 18.sp)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.isNotBlank()) Accent
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        .clickable {
                            if (value.isNotBlank()) onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        color = White,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
