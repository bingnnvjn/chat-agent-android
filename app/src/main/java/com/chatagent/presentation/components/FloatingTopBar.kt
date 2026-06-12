package com.chatagent.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.data.model.ApiProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

@Composable
fun FloatingTopBar(
    backdrop: Backdrop? = null,
    title: String = "Chat Agent",
    currentProvider: ApiProvider,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onModelSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showModelMenu by remember { mutableStateOf(false) }

    val rowModifier = if (backdrop != null) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(28.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 20f.dp.toPx())
                },
                highlight = { Highlight.Default }
            )
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    }

    Box(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "菜单",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .clickable { showModelMenu = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.ifEmpty { currentProvider.defaultModel },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "▾",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .clickable { onNewChatClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新对话",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showModelMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showModelMenu = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "选择模型",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            },
            text = {
                Column {
                    currentProvider.models.forEach { model ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onModelSelect(model)
                                    showModelMenu = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = model,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    }
}
