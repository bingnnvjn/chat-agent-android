package com.chatagent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private val Red = Color(0xFFFF3B30)
private val LightGray = Color(0xFFF2F2F7)
private val MutedText = Color(0xFF8E8E93)

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit = {},
    enableThinking: Boolean = false,
    onToggleThinking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧加号圆形按钮
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(LightGray)
                .clickable { onAttach() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = MutedText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 中间输入胶囊
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .background(LightGray)
                .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                .height(46.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
                    cursorBrush = SolidColor(Red),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "iMessage 信息",
                                    color = MutedText,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // 发送按钮
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(
                            if (value.isNotBlank()) Red
                            else MutedText.copy(alpha = 0.2f)
                        )
                        .clickable {
                            if (value.isNotBlank()) onSend()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}
