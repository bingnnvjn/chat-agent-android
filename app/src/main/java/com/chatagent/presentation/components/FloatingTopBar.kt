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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatagent.data.model.ApiProvider
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
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

    val glassCircle = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop, shape = { CircleShape },
            effects = { vibrancy(); blur(4f.dp.toPx()) },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) }
        )
    } else Modifier

    val glassCapsule = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop, shape = { RoundedCornerShape(25.dp) },
            effects = { vibrancy(); blur(4f.dp.toPx()) },
            highlight = { Highlight.Default },
            onDrawSurface = { drawRect(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) }
        )
    } else Modifier

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = glassCircle.size(50.dp).clip(CircleShape).clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) { Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp) }

        Box(
            modifier = glassCapsule
                .padding(horizontal = 6.dp).height(50.dp)
                .clip(RoundedCornerShape(25.dp)).clickable { showModelMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                title.ifEmpty { currentProvider.defaultModel },
                color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        Box(
            modifier = glassCircle.size(50.dp).clip(CircleShape).clickable { onNewChatClick() },
            contentAlignment = Alignment.Center
        ) { Text("⋯", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp) }
    }

    if (showModelMenu) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showModelMenu = false },
            shape = RoundedCornerShape(16.dp), title = null,
            text = {
                androidx.compose.foundation.layout.Column {
                    currentProvider.models.forEach { model ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { onModelSelect(model); showModelMenu = false }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) { Text(model, fontSize = 15.sp) }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
