package com.chatagent.presentation.components
import androidx.compose.foundation.shape.RoundedCornerShape

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.kyant.shapes.Capsule
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.chatagent.presentation.components.LiquidButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh

private val tabNames = listOf("液态按钮", "自适应亮度", "渐进模糊", "完全体")

@Composable
fun GlassTestScreen(onClose: () -> Unit = {}) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0D0D0D) else Color(0xFFF2F2F7)
    val textColor = if (isDark) Color.White else Color.Black
    var selectedTab by remember { mutableIntStateOf(0) }
    var bgUri by remember { mutableStateOf<Uri?>(null) }
    val ctx = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> bgUri = uri }
    val bitmapPainter = remember(bgUri) {
        bgUri?.let { uri -> try { ctx.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input)?.asImageBitmap()?.let { BitmapPainter(it) } } } catch (_: Exception) { null } }
    }

    val backdrop = rememberLayerBackdrop { drawRect(bgColor); drawContent() }

    Box(Modifier.fillMaxSize().background(bgColor)) {
        if (bitmapPainter != null) {
            Image(painter = bitmapPainter, null, Modifier.fillMaxSize().layerBackdrop(backdrop), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop))
        }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(40.dp))
            Text("Liquid Glass", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))
            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f)), modifier = Modifier.padding(horizontal = 16.dp)
            ) { Text(if (bgUri == null) "📷 选择壁纸" else "🔄 换壁纸", color = textColor, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))

            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabNames.forEachIndexed { i, name ->
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (selectedTab == i) textColor.copy(alpha = 0.2f) else Color.Transparent).clickable { selectedTab = i }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text(name, color = textColor, fontSize = 13.sp, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> ButtonsPage(backdrop, textColor)
                    1 -> AdaptivePage(backdrop, textColor)
                    2 -> BlurPage(backdrop, textColor)
                    3 -> FusionPage(backdrop, textColor)
                }
            }

            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f)), modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)) { Text("关闭", color = textColor) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ══════════════════════════
// Tab 0: Buttons — 直接用 demoAPP 的 LiquidButton
// ══════════════════════════
@Composable
private fun ButtonsPage(backdrop: Backdrop, textColor: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("demoAPP 原版 LiquidButton", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))

        // 直接复用 demoAPP 的 LiquidButton
        LiquidButton({}, backdrop) { BasicText("透明按钮", style = TextStyle(textColor, 15.sp)) }
        LiquidButton({}, backdrop, surfaceColor = textColor.copy(alpha = 0.15f)) { BasicText("半透明表面", style = TextStyle(textColor, 15.sp)) }
        LiquidButton({}, backdrop, tint = Color(0xFF0088FF)) { BasicText("蓝色着色", style = TextStyle(Color.White, 15.sp)) }
        LiquidButton({}, backdrop, tint = Color(0xFFFF8D28)) { BasicText("橙色着色", style = TextStyle(Color.White, 15.sp)) }
        LiquidButton({}, backdrop, tint = Color(0xFF10A37F)) { BasicText("绿色着色", style = TextStyle(Color.White, 15.sp)) }

        Spacer(Modifier.height(16.dp))
    }
}

// ══════════════════════════
// Tab 1: 自适应亮度 — 可拖拽/缩放/旋转
// ══════════════════════════
@Composable
private fun AdaptivePage(backdrop: Backdrop, textColor: Color) {
    AdaptiveLuminanceGlassContent(
        backdrop = backdrop,
        modifier = Modifier.size(200.dp)
    )
}

// ══════════════════════════
// Tab 3: 完全体
// ══════════════════════════
// Tab 3: 完全体 — 自适应亮度 × 液态按钮
// ══════════════════════════
@Composable
private fun FusionPage(backdrop: Backdrop, textColor: Color) {
    val isLight = !isSystemInDarkTheme()
    val layer = rememberGraphicsLayer()
    val luminanceAnim = remember(isLight) { Animatable(if (isLight) 1f else 0f) }

    LaunchedEffect(layer) {
        val buffer = IntArray(25)
        while (isActive) {
            try {
                delay(500)
                val img = layer.toImageBitmap()
                val thumb = img.scale(5, 5)
                thumb.readPixels(buffer)
                val avg = buffer.sumOf { argb ->
                    val r = (argb shr 16 and 0xFF) / 255f
                    val g = (argb shr 8 and 0xFF) / 255f
                    val b = (argb and 0xFF) / 255f
                    0.2126 * r + 0.7152 * g + 0.0722 * b
                } / buffer.size
                launch { luminanceAnim.animateTo(avg.toFloat(), tween(1000)) }
            } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Text("完全体 — 自适应 × 液态按钮", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
        Text("按住→晕染高光+液态变形", color = textColor.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))

        val lum = luminanceAnim.value
        val fusionScope = rememberCoroutineScope()
        val fusionHl = remember(fusionScope) { InteractiveHighlight(fusionScope) }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(48.dp)
                .drawBackdrop(backdrop = backdrop, shape = { Capsule() },
                    effects = {
                        val l = (lum * 2f - 1f).let { sign(it) * it * it }
                        colorControls(
                            brightness = if (l > 0f) lerp(0.1f, 0.5f, l) else lerp(0.1f, -0.2f, -l),
                            contrast = if (l > 0f) lerp(1f, 0f, l) else 1f,
                            saturation = 1.5f
                        )
                        vibrancy()
                        blur(if (l > 0f) lerp(2f.dp.toPx(), 4f.dp.toPx(), l) else lerp(2f.dp.toPx(), 6f.dp.toPx(), -l))
                        lens(
                            if (l > 0f) lerp(12f.dp.toPx(), 20f.dp.toPx(), l) else lerp(12f.dp.toPx(), 24f.dp.toPx(), -l),
                            if (l > 0f) lerp(24f.dp.toPx(), 32f.dp.toPx(), l) else lerp(24f.dp.toPx(), 40f.dp.toPx(), -l),
                            depthEffect = true
                        )
                    },
                    layerBlock = {
                        val w = size.width.toFloat(); val h = size.height.toFloat()
                        val p = fusionHl.progress; val off = fusionHl.offset
                        val s = lerp(1f, 1f + 4f.dp.toPx() / h, p)
                        val maxOff = minOf(w, h)
                        translationX = maxOff * tanh(0.05f * off.x / maxOff)
                        translationY = maxOff * tanh(0.05f * off.y / maxOff)
                        val drag = 4f.dp.toPx() / h; val angle = atan2(off.y, off.x)
                        scaleX = s + drag * abs(cos(angle) * off.x / maxOf(w, h)) * (w / h).coerceAtMost(1f)
                        scaleY = s + drag * abs(sin(angle) * off.y / maxOf(w, h)) * (h / w).coerceAtMost(1f)
                    },
                    onDrawSurface = {}
                )
                .then(fusionHl.modifier).then(fusionHl.gestureModifier)
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicText(
                "完全体  |  lum: ${(lum * 100f).fastRoundToInt() / 100.0}",
                style = TextStyle(textColor, 14.sp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("壁纸越暗→模糊越强、亮度越高、透镜折射越大",
            color = textColor.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))

        // 第2个: 自适应亮度面板（供背景采样用）
        Spacer(Modifier.height(16.dp))
        Text("背景采样区域（需在壁纸上采样）", color = textColor.copy(alpha = 0.3f), fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(140.dp)
                .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(16.dp) },
                    effects = {
                        val l = (lum * 2f - 1f).let { sign(it) * it * it }
                        colorControls(brightness = if (l > 0f) 0.3f else -0.1f, contrast = if (l > 0f) 0.5f else 1f, saturation = 1.5f)
                        blur(if (l > 0f) 12f.dp.toPx() else 4f.dp.toPx())
                        lens(16f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawBackdrop = { d -> d(); layer.record { d() } }
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicText("lum: ${(lum * 100f).fastRoundToInt() / 100.0}",
                style = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center, color = textColor))
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ══════════════════════════
// Tab 2: ProgressiveBlur — 保持
// ══════════════════════════
@Composable
private fun BlurPage(backdrop: Backdrop, textColor: Color) {
    val isDark = isSystemInDarkTheme()
    val tintColor = if (isDark) Color(0xFF808080) else Color.White

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("渐进模糊 (Alpha-masked)", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
        Text("AGSL RuntimeShader · Android 13+", color = textColor.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(160.dp).drawPlainBackdrop(backdrop = backdrop, shape = { androidx.compose.ui.graphics.RectangleShape },
            effects = {
                blur(4f.dp.toPx())
                runtimeShaderEffect("AlphaMask", """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;
half4 main(float2 coord) {
    float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
    float tintAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}""", "content") {
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("tint", tintColor)
                    setFloatUniform("tintIntensity", 0.8f)
                }
            }
        ), contentAlignment = Alignment.Center) {
            BasicText("alpha-masked progressive blur", style = TextStyle(textColor.copy(alpha = 0.6f), 14.sp, textAlign = TextAlign.Center))
        }
        Text("从上到下渐变模糊，底部叠加 tint", color = textColor.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(24.dp))
    }
}
