package com.chatagent.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.launch

/**
 * Liquid Glass 效果测试界面
 *
 * 严格按照官方文档 (https://kyant.gitbook.io/backdrop) 实现：
 * 效果顺序：color filter ⇒ blur ⇒ lens
 */
@Composable
fun GlassTestScreen(
    onClose: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val bgColor = Color(0xFF1A0533)

    // 1. CanvasBackdrop: 自定义渐变背景 + 装饰图形
    val canvasBackdrop = rememberCanvasBackdrop {
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color(0xFF1A0533), Color(0xFF6B2FA0), Color(0xFF9B4DCA))
            )
        )
        // 装饰光晕
        drawCircle(Color.White.copy(alpha = 0.12f), 80f, center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.3f))
        drawCircle(Color(0xFF34C759).copy(alpha = 0.08f), 60f, center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.6f))
        drawCircle(Color(0xFF007AFF).copy(alpha = 0.06f), 50f, center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.8f))
    }

    // 2. LayerBackdrop: 捕获实际内容（包含背景绘制）
    val layerBackdrop = rememberLayerBackdrop {
        drawRect(bgColor)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // 背景捕获层
        Box(
            modifier = Modifier.fillMaxSize().then(
                Modifier.drawBackdrop(
                    backdrop = canvasBackdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    effects = { /* 直接绘制canvas内容 */ }
                )
            )
        )

        // 可滚动内容
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(60.dp))
                Text("Liquid Glass 测试", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("按官方文档效果顺序: colorFilter → blur → lens",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))

                // ═══ 1. blur 不同半径 ═══
                SectionTitle("blur (模糊)")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(2f to "2dp", 6f to "6dp", 12f to "12dp", 24f to "24dp").forEach { (r, label) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { blur(r) })
                            Spacer(Modifier.height(4.dp))
                            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }

                // ═══ 2. 效果链: opacity + blur ═══
                SectionTitle("opacity → blur (透明度+模糊)")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1f to "100%", 0.6f to "60%", 0.3f to "30%", 0.1f to "10%").forEach { (a, label) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { opacity(a); blur(6f) })
                            Spacer(Modifier.height(4.dp))
                            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }

                // ═══ 3. vibrancy + blur ═══
                SectionTitle("vibrancy → blur (增强饱和度)")
                GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { vibrancy(); blur(6f) }, modifier = Modifier.size(100.dp))
                Spacer(Modifier.height(4.dp))
                Text("饱和度×1.5，颜色更鲜艳", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                // ═══ 4. colorControls + blur ═══
                SectionTitle("colorControls → blur (色彩控制)")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Triple("鲜艳", 1.8f, 0f),
                        Triple("明亮", 1.2f, 0.1f),
                        Triple("冷色", 1f, -0.05f)
                    ).forEach { (label, sat, bri) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            GlassBlock(backdrop = layerBackdrop, shape = RoundedCornerShape(16.dp), effects = { colorControls(saturation = sat, brightness = bri); blur(6f) })
                            Spacer(Modifier.height(4.dp))
                            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }

                // ═══ 5. lens 效果 ═══
                SectionTitle("lens (折射) — Android 13+ 需 RuntimeShader")
                Text("使用 RoundedCornerShape + refraction, shape 必须是 CornerBasedShape",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "折射 8px" to LensParams(8f, 16f, false, false),
                        "折射 16px" to LensParams(16f, 32f, false, false),
                        "带景深" to LensParams(16f, 32f, true, false),
                        "色散色彩" to LensParams(16f, 32f, false, true)
                    ).forEach { (label, params) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            GlassBlock(
                                backdrop = layerBackdrop,
                                shape = RoundedCornerShape(16.dp),
                                effects = { blur(4f); lens(params.rh, params.ra, params.de, params.ca) }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                    }
                }

                // ═══ 7. 交互式玻璃按钮 (Interactive Glass Bottom Bar 教程) ═══
                SectionTitle("交互式玻璃按钮 (按压缩放)")
                Text("使用 pointerInput + layerBlock 防止背景缩放",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                Spacer(Modifier.height(12.dp))
                InteractiveGlassButton(backdrop = layerBackdrop)

                // ═══ 8. 完整效果链：colorControls → blur → lens ═══
                SectionTitle("完整效果链: vibrancy → blur → lens")
                Box(
                    modifier = Modifier.size(140.dp, 80.dp)
                        .drawBackdrop(
                            backdrop = layerBackdrop,
                            shape = { RoundedCornerShape(40.dp) },
                            effects = { vibrancy(); blur(6f); lens(12f, 20f) },
                            highlight = { Highlight(width = 0.5.dp, alpha = 0.6f) },
                            shadow = { Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.1f)) },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
                        )
                        .clip(RoundedCornerShape(40.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("✨", fontSize = 28.sp) }
                Spacer(Modifier.height(8.dp))
                Text("玻璃胶囊", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

                // ═══ 9. 模拟顶栏 ═══
                SectionTitle("模拟顶栏 (实际 44dp)")
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlassButton44(backdrop = layerBackdrop, modifier = Modifier.align(Alignment.CenterStart)) { Text("‹", fontSize = 20.sp) }
                    Box(
                        modifier = Modifier.height(44.dp)
                            .drawBackdrop(
                                backdrop = layerBackdrop, shape = { RoundedCornerShape(22.dp) },
                                effects = { vibrancy(); blur(8f); lens(8f, 12f) },
                                highlight = { Highlight(width = 0.5.dp, alpha = 0.6f) },
                                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.08f)) },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                            ).clip(RoundedCornerShape(22.dp)).padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("DeepSeek", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                    GlassButton44(backdrop = layerBackdrop, modifier = Modifier.align(Alignment.CenterEnd)) { Text("⋯", fontSize = 20.sp) }
                }

                // ═══ 10. 模拟底栏 ═══
                SectionTitle("模拟底栏 (实际 34dp)")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GlassButton34(backdrop = layerBackdrop) { Text("+", fontSize = 18.sp) }
                    Spacer(Modifier.width(8.dp))
                    GlassButton34(backdrop = layerBackdrop) { Text("🧠", fontSize = 14.sp) }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(34.dp)
                            .drawBackdrop(
                                backdrop = layerBackdrop, shape = { RoundedCornerShape(17.dp) },
                                effects = { vibrancy(); blur(8f); lens(6f, 10f) },
                                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
                            ).clip(RoundedCornerShape(17.dp)).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) { Text("iMessage 信息", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) }
                }

                // ═══ lens 说明 ═══
                SectionTitle("关于 lens 效果")
                Text("• lens 使用 AGSL RuntimeShader (Android 13+)\n" +
                     "• shape 必须是 CornerBasedShape (RoundedCornerShape)\n" +
                     "• 效果顺序: colorFilter → blur → lens\n" +
                     "• 部分 Android 15 设备可能因 GPU 驱动不兼容崩溃\n" +
                     "• refractionHeight: 折射区域高度 (px)\n" +
                     "• refractionAmount: 折射偏移量 (px)\n" +
                     "• depthEffect: 是否启用景深感\n" +
                     "• chromaticAberration: 是否启用色散",
                    color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 18.sp)

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) { Text("关闭", color = Color.White) }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ═══ 玻璃效果方块 ═══
@Composable
private fun GlassBlock(
    backdrop: Backdrop,
    shape: RoundedCornerShape,
    effects: com.kyant.backdrop.BackdropEffectScope.() -> Unit,
    modifier: Modifier = Modifier.size(80.dp)
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop, shape = { shape },
                effects = effects,
                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.08f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            )
            .clip(shape)
    )
}

// ═══ 交互式玻璃按钮 ═══
@Composable
private fun InteractiveGlassButton(backdrop: Backdrop) {
    val scope = rememberCoroutineScope()
    val progress = remember { mutableFloatStateOf(0f) }
    val scale by animateFloatAsState(
        targetValue = 1f + progress.floatValue * 0.15f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Box(
        modifier = Modifier.size(80.dp)
            .drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(8f); lens(8f, 14f) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.6f) },
                shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.1f)) },
                layerBlock = {
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* click action */ }
            .pointerInput(scope) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { progress.floatValue = 1f }
                    waitForUpOrCancellation()
                    scope.launch { progress.floatValue = 0f }
                }
            },
        contentAlignment = Alignment.Center
    ) { Text("👆", fontSize = 24.sp) }
}

// ═══ 44dp 玻璃圆钮 ═══
@Composable
private fun GlassButton44(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(44.dp)
            .drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(8f); lens(6f, 10f) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                shadow = { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.08f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            ).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ═══ 34dp 玻璃圆钮 ═══
@Composable
private fun GlassButton34(
    backdrop: Backdrop,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(34.dp)
            .drawBackdrop(
                backdrop = backdrop, shape = { CircleShape },
                effects = { vibrancy(); blur(8f); lens(5f, 8f) },
                highlight = { Highlight(width = 0.5.dp, alpha = 0.5f) },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.06f)) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
            ).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

private data class LensParams(
    val rh: Float,
    val ra: Float,
    val de: Boolean,
    val ca: Boolean
)
