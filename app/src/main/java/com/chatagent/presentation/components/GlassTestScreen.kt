package com.chatagent.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch

private data class BgOption(val name: String, val top: Color, val bottom: Color)

private val bgOptions = listOf(
    BgOption("极光紫", Color(0xFF1A0533), Color(0xFF9B4DCA)),
    BgOption("深海蓝", Color(0xFF0A1628), Color(0xFF1E88E5)),
    BgOption("森林绿", Color(0xFF0D1F12), Color(0xFF43A047)),
    BgOption("暖阳橙", Color(0xFF2D1400), Color(0xFFFF7043)),
    BgOption("黑白灰", Color(0xFF121212), Color(0xFF616161)),
)

@Composable
fun GlassTestScreen(onClose: () -> Unit = {}) {
    val scrollState = rememberScrollState()
    var bgIndex by remember { mutableStateOf(0) }
    val bg = bgOptions[bgIndex]

    // 背景渐变画布
    val canvasBackdrop = rememberCanvasBackdrop {
        drawRect(brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(bg.top, bg.bottom)))
        drawCircle(Color.White.copy(alpha = 0.08f), 80f, center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.25f))
        drawCircle(Color.White.copy(alpha = 0.05f), 50f, center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.6f))
    }

    // LayerBackdrop（含背景绘制）
    val layerBackdrop = rememberLayerBackdrop {
        drawRect(bg.top)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize().background(bg.top)) {
        // 背景层
        Box(Modifier.fillMaxSize().then(
            Modifier.drawBackdrop(backdrop = canvasBackdrop, shape = { RoundedCornerShape(0) }, effects = {})
        ))

        // 内容
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // 标题
            Text("🧪 Liquid Glass", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Backdrop v1.0  —  效果顺序: colorFilter → blur → lens", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            // ─── 背景切换 ───
            Text("切换背景", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bgOptions.forEachIndexed { i, option ->
                    Box(
                        modifier = Modifier.size(44.dp, 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(option.top, option.bottom)))
                            .clickable { bgIndex = i }
                            .then(if (i == bgIndex) Modifier.padding(2.dp) else Modifier),
                        contentAlignment = Alignment.Center
                    ) { if (i == bgIndex) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(20.dp))

            // ════════════════════════════════════════════
            // 1. LayerBackdrop 基础
            // ════════════════════════════════════════════
            GroupTitle("LayerBackdrop — 捕获内容")
            Text("rememberLayerBackdrop { drawRect(bg); drawContent() } + Modifier.layerBackdrop()",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp)
                    .drawBackdrop(backdrop = layerBackdrop, shape = { RoundedCornerShape(12.dp) },
                        effects = { blur(8f) },
                        highlight = { Highlight(width = 0.5.dp, alpha = 0.4f) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
                    ).clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("透过玻璃看到背景渐变 + 光晕", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) }

            // ════════════════════════════════════════════
            // 2. CanvasBackdrop — 自定义绘制
            // ════════════════════════════════════════════
            GroupTitle("CanvasBackdrop — 自定义画布")
            Text("rememberCanvasBackdrop { ... }  不依赖坐标，可独立绘制",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)

            // ════════════════════════════════════════════
            // 3. CombinedBackdrop — 合并多个
            // ════════════════════════════════════════════
            GroupTitle("CombinedBackdrop — 合并背景")
            val combined = rememberCombinedBackdrop(canvasBackdrop, layerBackdrop)
            Box(
                modifier = Modifier.size(100.dp)
                    .drawBackdrop(backdrop = combined, shape = { CircleShape },
                        effects = { blur(6f) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                    ).clip(CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("合并", color = Color.White, fontSize = 14.sp) }

            // ════════════════════════════════════════════
            // 4. emptyBackdrop
            // ════════════════════════════════════════════
            GroupTitle("emptyBackdrop — 空背景")
            val empty = emptyBackdrop()
            Box(
                modifier = Modifier.size(60.dp)
                    .drawBackdrop(backdrop = empty, shape = { CircleShape },
                        effects = { blur(6f) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.3f)) }
                    ).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Text("∅", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp) }

            // ════════════════════════════════════════════
            // 5. blur — 模糊
            // ════════════════════════════════════════════
            GroupTitle("blur(radius) — 模糊")
            Grid4(
                items = listOf(2f to "2px", 6f to "6px", 12f to "12px", 24f to "24px"),
                backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp),
                effect = { blur(it) }
            )

            // ════════════════════════════════════════════
            // 6. opacity → blur  (colorFilter → blur)
            // ════════════════════════════════════════════
            GroupTitle("opacity(α) → blur — 先调色后模糊")
            Grid4(
                items = listOf(1f to "100%", 0.6f to "60%", 0.3f to "30%", 0.1f to "10%"),
                backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp),
                effect = { opacity(it); blur(6f) }
            )

            // ════════════════════════════════════════════
            // 7. vibrancy → blur
            // ════════════════════════════════════════════
            GroupTitle("vibrancy() → blur — 饱和度×1.5")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { blur(6f) }, modifier = Modifier.size(80.dp))
                GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { vibrancy(); blur(6f) }, modifier = Modifier.size(80.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text("← 无", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    Text("vibrancy →", color = Color(0xFF34D399), fontSize = 11.sp)
                }
            }

            // ════════════════════════════════════════════
            // 8. colorControls → blur
            // ════════════════════════════════════════════
            GroupTitle("colorControls → blur — 色彩控制")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Triple("默认", 1f, 0f), Triple("鲜艳", 1.8f, 0f), Triple("明亮", 1.2f, 0.1f)).forEach { (label, sat, bri) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { colorControls(saturation = sat, brightness = bri); blur(6f) })
                        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }

            // ════════════════════════════════════════════
            // 9. lens (需 CornerBasedShape)
            // ════════════════════════════════════════════
            GroupTitle("lens — 折射 (需 CornerBasedShape, Android13+)")
            Text("参数: refractionHeight / refractionAmount / depthEffect / chromaticAberration",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "仅 blur" to LensData(0f, 0f, false, false),
                    "折射" to LensData(8f, 16f, false, false),
                    "+景深" to LensData(8f, 16f, true, false),
                    "+色散" to LensData(8f, 16f, false, true),
                ).forEach { (label, ld) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp),
                            effects = { blur(4f); lens(ld.rh, ld.ra, ld.de, ld.ca) })
                        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }

            // ════════════════════════════════════════════
            // 10. 完整效果链 + 交互
            // ════════════════════════════════════════════
            GroupTitle("完整效果 + 交互 (按压缩放)")
            Text("vibrancy → blur → lens + highlight + shadow + layerBlock(scale)",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            InteractiveGlassDemo(backdrop = layerBackdrop)

            // ════════════════════════════════════════════
            // 11. 模拟顶栏 44dp
            // ════════════════════════════════════════════
            GroupTitle("模拟顶栏 44dp — vibrancy → blur → lens")
            Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(50.dp), contentAlignment = Alignment.Center) {
                GlassCircle44(backdrop = layerBackdrop, modifier = Modifier.align(Alignment.CenterStart)) { Text("‹", fontSize = 20.sp) }
                Box(
                    modifier = Modifier.height(44.dp)
                        .drawBackdrop(backdrop = layerBackdrop, shape = { RoundedCornerShape(22.dp) },
                            effects = { vibrancy(); blur(8f); lens(6f, 10f) },
                            highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                            shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.08f)) },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                        ).clip(RoundedCornerShape(22.dp)).padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) { Text("DeepSeek", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                GlassCircle44(backdrop = layerBackdrop, modifier = Modifier.align(Alignment.CenterEnd)) { Text("⋯", fontSize = 20.sp) }
            }

            // ════════════════════════════════════════════
            // 12. 模拟底栏 34dp
            // ════════════════════════════════════════════
            GroupTitle("模拟底栏 34dp — vibrancy → blur → lens")
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassCircle34(backdrop = layerBackdrop) { Text("+", fontSize = 18.sp) }
                Spacer(Modifier.width(6.dp))
                GlassCircle34(backdrop = layerBackdrop) { Text("🧠", fontSize = 14.sp) }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.weight(1f).height(34.dp)
                        .drawBackdrop(backdrop = layerBackdrop, shape = { RoundedCornerShape(17.dp) },
                            effects = { vibrancy(); blur(8f); lens(5f, 8f) },
                            highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                            shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
                        ).clip(RoundedCornerShape(17.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) { Text("iMessage 信息", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))) {
                Text("关闭", color = Color.White)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ═══ 组件 ═══

@Composable private fun GroupTitle(t: String) {
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(t, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Grid4(
    items: List<Pair<Float, String>>,
    backdrop: Backdrop,
    shape: RoundedCornerShape,
    effect: com.kyant.backdrop.BackdropEffectScope.(Float) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (v, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                GlassBlock(backdrop = backdrop, shape = shape, effects = { effect(v) })
                Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun GlassBlock(backdrop: Backdrop, shape: RoundedCornerShape, effects: com.kyant.backdrop.BackdropEffectScope.() -> Unit, modifier: Modifier = Modifier.size(72.dp)) {
    Box(
        modifier = modifier
            .drawBackdrop(backdrop = backdrop, shape = { shape }, effects = effects,
                highlight = { Highlight(width = 0.5.dp, alpha = 0.4f) },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
            ).clip(shape)
    )
}

@Composable
private fun InteractiveGlassDemo(backdrop: Backdrop) {
    val scope = rememberCoroutineScope()
    val progress = remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(targetValue = 1f + progress.floatValue * 0.12f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "s")

    Box(
        modifier = Modifier.size(80.dp)
            .drawBackdrop(backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(8f); lens(6f, 12f) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.6f) },
                shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.1f)) },
                layerBlock = { scaleX = scale; scaleY = scale },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            ).clip(CircleShape)
            .pointerInput(scope) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { progress.floatValue = 1f }
                    waitForUpOrCancellation()
                    scope.launch { progress.floatValue = 0f }
                }
            },
        contentAlignment = Alignment.Center
    ) { Text("按我", color = Color.White, fontSize = 14.sp) }
}

@Composable
private fun GlassCircle44(backdrop: Backdrop, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.size(44.dp)
            .drawBackdrop(backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(8f); lens(4f, 8f) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.08f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            ).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun GlassCircle34(backdrop: Backdrop, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp)
            .drawBackdrop(backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(8f); lens(3f, 6f) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.4f) },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            ).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

private data class LensData(val rh: Float, val ra: Float, val de: Boolean, val ca: Boolean)
