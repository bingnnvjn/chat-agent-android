package com.chatagent.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import androidx.compose.foundation.layout.offset as layoutOffset
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════
// GlassTestScreen — 完整文档示例
// ═══════════════════════════════════════════
@Composable
fun GlassTestScreen(onClose: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var bgUri by remember { mutableStateOf<Uri?>(null) }

    // 照片选择器
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        bgUri = uri
    }

    // 加载 Bitmap
    val bitmap = remember(bgUri) {
        bgUri?.let { uri ->
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
    }

    val defaultBg = Color(0xFF1A1A2E)
    val sectionBg = Color(0x22000000)

    // LayerBackdrop — 捕获照片/背景 + 内容
    val backdrop = rememberLayerBackdrop {
        if (bitmap != null) {
            drawImage(bitmap.asImageBitmap())
        } else {
            drawRect(defaultBg)
        }
        drawContent()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        // 捕获层
        Box(Modifier.fillMaxSize().then(
            Modifier.drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(0) }, effects = {})
        ))

        // 内容
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // ─── 标题 ───
            Text("Liquid Glass 文档示例", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("kyant.gitbook.io/backdrop", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            // ─── 背景选择 ───
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) { Text(if (bgUri == null) "📷 选择照片作背景" else "🔄 更换照片", color = Color.White, fontSize = 13.sp) }
                if (bgUri != null) {
                    Text("✓ 已加载", color = Color(0xFF34C759), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("提示：选一张颜色丰富的照片，玻璃折射效果更明显", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp)

            Spacer(Modifier.height(20.dp))

            // ════════════════════════════════════════
            // Demo 1: Glass Bottom Bar
            // ════════════════════════════════════════
            DemoTitle("1. Glass Bottom Bar", "vibrancy → blur → lens + tint")
            Text("文档教程：玻璃底栏，含着色图标按钮",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth().background(sectionBg, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 普通玻璃按钮
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .drawBackdrop(backdrop = backdrop, shape = { CircleShape },
                                effects = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("🏠", fontSize = 20.sp) }

                    // 着色玻璃按钮 (使用 BlendMode.Hue)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).fillMaxHeight()
                            .drawBackdrop(backdrop = backdrop, shape = { CircleShape },
                                effects = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
                                onDrawSurface = {
                                    val tint = Color(0xFF0088FF)
                                    drawRect(tint, blendMode = BlendMode.Hue)
                                    drawRect(tint.copy(alpha = 0.75f))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("★", fontSize = 20.sp, color = Color.White) }
                }
            }

            // ════════════════════════════════════════
            // Demo 2: Interactive Glass Bottom Bar
            // ════════════════════════════════════════
            DemoTitle("2. Interactive (按压缩放)", "vibrancy → blur → lens + layerBlock")
            Text("按住按钮 → 放大 → 松手回弹，backdrop 不应缩放",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth().background(sectionBg, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 交互按钮 (用 Animatable)
                    val animScope = rememberCoroutineScope()
                    val progressAnim = remember { androidx.compose.animation.core.Animatable(0f) }
                    val maxScale = { width: Float -> (width + 16.dp.toPx()) / width }

                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .drawBackdrop(backdrop = backdrop, shape = { CircleShape },
                                effects = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
                                layerBlock = {
                                    val p = progressAnim.value
                                    val s = lerp(1f, maxScale(size.width), p)
                                    scaleX = s; scaleY = s
                                },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                            )
                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                            .pointerInput(animScope) {
                                val spec = spring(0.5f, 300f, 0.001f)
                                awaitEachGesture {
                                    awaitFirstDown()
                                    animScope.launch { progressAnim.animateTo(1f, spec) }
                                    waitForUpOrCancellation()
                                    animScope.launch { progressAnim.animateTo(0f, spec) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text("按住我", color = Color.White, fontSize = 13.sp) }

                    // 连续胶囊按钮
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(28.dp) },
                                effects = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
                                onDrawSurface = {
                                    val tint = Color(0xFF0088FF)
                                    drawRect(tint, blendMode = BlendMode.Hue)
                                    drawRect(tint.copy(alpha = 0.75f))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("胶囊", color = Color.White, fontSize = 13.sp) }
                }
            }

            // ════════════════════════════════════════
            // Demo 3: Glass Bottom Sheet
            // ════════════════════════════════════════
            DemoTitle("3. Glass Bottom Sheet", "exportedBackdrop + 双层玻璃")
            Text("Sheet 使用 exportedBackdrop，内层按钮复用 sheet 背景",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth().background(sectionBg, RoundedCornerShape(12.dp)).padding(8.dp)) {
                val sheetBackdrop = rememberLayerBackdrop()
                Column(
                    Modifier.fillMaxWidth()
                        .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(36.dp) },
                            effects = { vibrancy(); blur(4f.dp.toPx()); lens(24f.dp.toPx(), 48f.dp.toPx(), true) },
                            exportedBackdrop = sheetBackdrop,
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.4f)) }
                        ).padding(12.dp)
                ) {
                    Text("Sheet 内容", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("下方按钮使用 exportedBackdrop", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    // 内层玻璃按钮（复用 sheetBackdrop）
                    Box(
                        Modifier.fillMaxWidth().height(44.dp)
                            .drawBackdrop(backdrop = sheetBackdrop, shape = { CircleShape },
                                shadow = null,
                                effects = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.5f)) }
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("内层玻璃按钮", color = Color.White, fontSize = 13.sp) }
                }
            }

            // ════════════════════════════════════════
            // Demo 4: Glass Slider (可拖拽)
            // ════════════════════════════════════════
            DemoTitle("4. Glass Slider (可拖拽)", "rememberCombinedBackdrop + 拖拽")
            Text("拖动手柄，背景 + 轨道同时折射 — chromaticAberration",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth().background(sectionBg, RoundedCornerShape(12.dp)).padding(16.dp)) {
                GlassSlider(backdrop = backdrop)
            }

            // ════════════════════════════════════════
            // Demo 5: Progressive Blur
            // ════════════════════════════════════════
            DemoTitle("5. Progressive Blur", "Alpha-masked RuntimeShader")
            Text("从底部到顶部渐变模糊，需 Android 13+",
                color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            Box(Modifier.fillMaxWidth().height(80.dp).background(sectionBg, RoundedCornerShape(12.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    // 用渐变色模拟 progressive blur 效果
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("需 AGSL RuntimeShader — 当前版本暂不支持完整实现",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))) {
                Text("关闭", color = Color.White)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ═══════════════════════════════════════════
// Glass Slider — 可拖拽，带 rememberCombinedBackdrop
// ═══════════════════════════════════════════
@Composable
private fun GlassSlider(backdrop: Backdrop) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxOffset = 260f // 最大拖拽位移 px（根据实际布局可调）

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackBackdrop = rememberLayerBackdrop()

            // 轨道
            Box(
                Modifier.fillMaxWidth().height(6.dp)
                    .layerBackdrop(trackBackdrop)
                    .background(Color(0xFF0088FF), CircleShape)
                    .align(Alignment.CenterStart)
            )

            // 滑块位置
            val sliderPos = (maxWidth / 2f - 28.dp + (offsetX).dp.coerceIn(-maxWidth / 2 + 28.dp, maxWidth / 2 - 28.dp))
            val thumbX = sliderPos

            // 合并背景 + 轨道
            val combined = rememberCombinedBackdrop(backdrop, trackBackdrop)

            // 滑块（可拖拽）
            Box(
                Modifier.offset(x = thumbX).size(52.dp, 30.dp)
                    .drawBackdrop(backdrop = combined, shape = { CircleShape },
                        effects = { lens(12f.dp.toPx(), 16f.dp.toPx(), chromaticAberration = true) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.3f)) }
                    )
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount / 3f)
                                .coerceIn(-maxWidth.value / 2 + 28f, maxWidth.value / 2 - 28f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // 滑块内部的指示线
                Box(Modifier.size(20.dp, 3.dp).background(Color.White.copy(alpha = 0.7f), CircleShape))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("← 左右拖动 →", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
    }
}

// ═══════════════════════════════════════════
// 辅助函数
// ═══════════════════════════════════════════
@Composable
private fun DemoTitle(num: String, subtitle: String) {
    Spacer(Modifier.height(12.dp))
    Text("$num", color = Color(0xFF34D399), fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
}

