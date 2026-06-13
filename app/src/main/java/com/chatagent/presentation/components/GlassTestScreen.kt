package com.chatagent.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.opacity
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Liquid Glass 效果测试界面
 * 展示所有可用参数：blur、lens、colorFilter、highlight、shadow
 */
@Composable
fun GlassTestScreen(
    onClose: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // LayerBackdrop: 捕获下方内容的真实画面
    val layerBackdrop = rememberLayerBackdrop()

    // CanvasBackdrop: 自定义绘制内容（纯色渐变，不需要layerBackdrop时用）
    val canvasBackdrop = rememberCanvasBackdrop {
        // 画一个渐变背景供玻璃折射
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A0533),
                    Color(0xFF6B2FA0),
                    Color(0xFF9B4DCA)
                )
            )
        )
        // 画一些文字/图形让折射效果更明显
        drawCircle(Color.White.copy(alpha = 0.1f), 60f, center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.3f))
        drawCircle(Color.White.copy(alpha = 0.08f), 40f, center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.6f))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A0533)) {
        // 使用 layerBackdrop 捕获全屏内容
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景捕获层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A0533))
                    .then(
                        // 用 drawPlainBackdrop 绘制canvas内容到捕获层
                        Modifier.drawPlainBackdrop(
                            backdrop = canvasBackdrop,
                            shape = { RoundedCornerShape(0.dp) },
                            effects = { /* 无效果 - direct draw */ }
                        )
                    )
            )

            // 可滚动内容
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(60.dp))

                    Text("Liquid Glass 效果测试",
                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("查看不同效果参数下的玻璃渲染表现",
                        color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    Spacer(Modifier.height(32.dp))

                    // ═══ 测试组 1: blur 不同半径 ═══
                    SectionTitle("blur (模糊半径)")
                    GlassDemoRow(
                        labels = listOf("2dp", "6dp", "12dp", "24dp"),
                        radii = listOf(2f, 6f, 12f, 24f)
                    ) { radius ->
                        GlassDemoItem(
                            backdrop = layerBackdrop,
                            shape = RoundedCornerShape(16.dp),
                            effects = { blur(radius) }
                        )
                    }

                    SectionTitle("blur + 高光 (Highlight)")
                    GlassDemoRow(
                        labels = listOf("Default", "Ambient", "Plain"),
                        highlights = listOf(
                            Highlight.Default,
                            Highlight.Ambient,
                            Highlight.Plain
                        )
                    ) { h ->
                        GlassDemoItem(
                            backdrop = layerBackdrop,
                            shape = RoundedCornerShape(16.dp),
                            effects = { blur(6f) },
                            highlight = { h }
                        )
                    }

                    SectionTitle("blur + 阴影 (Shadow)")
                    GlassDemoRow(
                        labels = listOf("Default", "大阴影", "浅阴影"),
                        shadows = listOf(
                            Shadow.Default,
                            Shadow(radius = 48.dp, color = Color.Black.copy(alpha = 0.3f)),
                            Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.05f))
                        )
                    ) { s ->
                        GlassDemoItem(
                            backdrop = layerBackdrop,
                            shape = RoundedCornerShape(16.dp),
                            effects = { blur(6f) },
                            shadow = { s }
                        )
                    }

                    SectionTitle("opacity (透明度)")
                    GlassDemoRow(
                        labels = listOf("100%", "70%", "40%", "15%"),
                        opacities = listOf(1f, 0.7f, 0.4f, 0.15f)
                    ) { alpha ->
                        GlassDemoItem(
                            backdrop = layerBackdrop,
                            shape = RoundedCornerShape(16.dp),
                            effects = { blur(6f); opacity(alpha) }
                        )
                    }

                    SectionTitle("vibrancy (增强饱和度)")
                    GlassDemoItem(
                        backdrop = layerBackdrop,
                        shape = RoundedCornerShape(16.dp),
                        effects = { blur(6f); vibrancy() },
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("vibrancy 将饱和度提升 1.5 倍",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                    SectionTitle("colorControls (色彩控制)")
                    GlassDemoItem(
                        backdrop = layerBackdrop,
                        shape = RoundedCornerShape(16.dp),
                        effects = { blur(6f); colorControls(saturation = 1.8f, brightness = 0.05f) },
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("saturation=1.8 brightness=0.05",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                    SectionTitle("onDrawSurface (覆盖色)")
                    GlassDemoRow(
                        labels = listOf("无覆盖", "10%白", "30%白", "50%黑")
                    ) { index ->
                        val surfaceColor = when (index) {
                            0 -> Color.Transparent
                            1 -> Color.White.copy(alpha = 0.1f)
                            2 -> Color.White.copy(alpha = 0.3f)
                            else -> Color.Black.copy(alpha = 0.5f)
                        }
                        GlassDemoItem(
                            backdrop = layerBackdrop,
                            shape = RoundedCornerShape(16.dp),
                            effects = { blur(6f) },
                            onDrawSurface = { drawRect(surfaceColor) }
                        )
                    }

                    // ═══ 模拟顶栏按钮测试 ═══
                    SectionTitle("模拟顶栏按钮 (实际尺寸)")
                    Text("44dp 圆形按钮 + 44dp 胶囊",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                    Spacer(Modifier.height(12.dp))

                    // 模拟顶栏布局
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 左按钮
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.CenterStart)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { CircleShape },
                                    effects = { blur(8f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("‹", color = Color.White, fontSize = 20.sp) }

                        // 中胶囊
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { RoundedCornerShape(22.dp) },
                                    effects = { blur(8f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(RoundedCornerShape(22.dp))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("DeepSeek", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }

                        // 右按钮
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.CenterEnd)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { CircleShape },
                                    effects = { blur(8f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("⋯", color = Color.White, fontSize = 20.sp) }
                    }

                    // ═══ 模拟底栏输入框测试 ═══
                    SectionTitle("模拟底栏 (实际尺寸)")
                    Text("34dp +按钮 + 34dp 🧠 + 34dp 输入胶囊",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // + 按钮
                        Box(
                            modifier = Modifier.size(34.dp)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { CircleShape },
                                    effects = { blur(8f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = Color.White, fontSize = 20.sp) }

                        Spacer(Modifier.width(8.dp))

                        // 🧠 按钮
                        Box(
                            modifier = Modifier.size(34.dp)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { CircleShape },
                                    effects = { blur(8f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("🧠", fontSize = 16.sp) }

                        Spacer(Modifier.width(8.dp))

                        // 输入胶囊
                        Box(
                            modifier = Modifier.weight(1f).height(34.dp)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { RoundedCornerShape(17.dp) },
                                    effects = { blur(8f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(RoundedCornerShape(17.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) { Text("iMessage 信息", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) }
                    }

                    Spacer(Modifier.height(24.dp))

                    // lens 效果说明
                    SectionTitle("lens (折射效果)")
                    Text("lens 使用 AGSL RuntimeShader，需要 GPU 支持",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text("参数：refractionHeight, refractionAmount, depthEffect, chromaticAberration",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text("⚠️ 部分 Android 15 设备可能不支持 RuntimeShader",
                        color = Color(0xFFFF6B6B).copy(alpha = 0.7f), fontSize = 12.sp)

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // lens 效果（小尺寸测试）
                        Box(
                            modifier = Modifier.weight(1f).height(80.dp)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { RoundedCornerShape(50.dp) },
                                    effects = { blur(6f) },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                                )
                                .clip(RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("blur only", color = Color.White, fontSize = 13.sp) }

                        Box(
                            modifier = Modifier.weight(1f).height(80.dp)
                                .drawBackdrop(
                                    backdrop = layerBackdrop,
                                    shape = { RoundedCornerShape(50.dp) },
                                    effects = {
                                        blur(4f)
                                        // lens 暂时注释掉，用 stronger blur 代替
                                    },
                                    highlight = { Highlight.Default },
                                    shadow = { Shadow.Default },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                                )
                                .clip(RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text("blur+高亮", color = Color.White, fontSize = 13.sp) }
                    }

                    Spacer(Modifier.height(40.dp))

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("关闭", color = Color.White)
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GlassDemoRow(
    labels: List<String>,
    radii: List<Float>? = null,
    highlights: List<Highlight>? = null,
    shadows: List<Shadow>? = null,
    opacities: List<Float>? = null,
    content: @Composable (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                content(index)
                Spacer(Modifier.height(4.dp))
                Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }
        }
    }
}

/**
 * 单个玻璃效果演示方块
 */
@Composable
private fun GlassDemoItem(
    backdrop: Backdrop,
    shape: RoundedCornerShape,
    effects: com.kyant.backdrop.BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = null,
    shadow: (() -> Shadow?)? = null,
    innerShadow: (() -> com.kyant.backdrop.shadow.InnerShadow?)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    modifier: Modifier = Modifier.size(80.dp)
) {
    // 用 `onDrawSurface` 接收 DrawScope 参数（需要完整类型的 lambda）
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = effects,
                highlight = highlight,
                shadow = shadow,
                innerShadow = innerShadow,
                onDrawSurface = onDrawSurface
            )
            .clip(shape)
            .background(Color.Transparent)
    )
}

