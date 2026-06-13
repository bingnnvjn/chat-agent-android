package com.chatagent.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import com.chatagent.presentation.components.LiquidBottomTab
import com.chatagent.presentation.components.LiquidBottomTabs
import com.chatagent.presentation.components.LiquidSlider
import com.chatagent.presentation.components.LiquidToggle
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import androidx.compose.ui.graphics.asImageBitmap
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.chatagent.presentation.components.LiquidBottomTab
import com.chatagent.presentation.components.LiquidBottomTabs
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sign

@Composable
fun GlassTestScreen(onClose: () -> Unit = {}) {
    val ctx = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xFF0D0D0D) else Color(0xFFF2F2F7)
    val textColor = if (isDark) Color.White else Color.Black
    val scrollState = rememberScrollState()

    var bgUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        bgUri = uri
    }

    val bitmap = remember(bgUri) {
        bgUri?.let { uri ->
            ctx.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
        }
    }
    val defaultBg = if (isDark) Color(0xFF1A1A2E) else Color(0xFFD0D0DC)

    val backdrop = rememberLayerBackdrop {
        if (bitmap != null) drawImage(bitmap.asImageBitmap())
        else drawRect(defaultBg)
        drawContent()
    }

    Box(Modifier.fillMaxSize().background(containerBg)) {
        Box(Modifier.fillMaxSize().then(
            Modifier.drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(0) }, effects = {})
        ))

        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("Liquid Glass Demo", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("GitHub 仓库示例", color = textColor.copy(alpha = 0.4f), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            // ─── 背景选择 ───
            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f))
            ) { Text(if (bgUri == null) "📷 选择背景照片" else "🔄 更换照片", color = textColor, fontSize = 13.sp) }
            Spacer(Modifier.height(20.dp))

            // ═══ Demo 1: LiquidButton ═══
            SectionTitle("Demo 1: LiquidButton", textColor)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val scope = rememberCoroutineScope()
                val h1 = remember(scope) { InteractiveHighlight(scope) }
                val h2 = remember(scope) { InteractiveHighlight(scope) }
                val h3 = remember(scope) { InteractiveHighlight(scope) }
                listOf(h1, h2, h3).forEach { h ->
                    LiquidButtonDemo(backdrop, h)
                }
            }

            // ═══ Demo 2: LiquidToggle ═══
            SectionTitle("Demo 2: LiquidToggle", textColor)
            var toggleOn by remember { mutableStateOf(false) }
            LiquidToggle(
                selected = { toggleOn },
                onSelect = { toggleOn = it },
                backdrop = backdrop,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            // ═══ Demo 3: LiquidSlider ═══
            SectionTitle("Demo 3: LiquidSlider", textColor)
            var sliderVal by remember { mutableStateOf(0.5f) }
            LiquidSlider(
                value = { sliderVal },
                onValueChange = { sliderVal = it },
                valueRange = 0f..1f,
                visibilityThreshold = 0.01f,
                backdrop = backdrop,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // ═══ Demo 4: LiquidBottomTabs ═══
            SectionTitle("Demo 4: LiquidBottomTabs", textColor)
            var tabIdx by remember { mutableIntStateOf(0) }
            val iconPainter = rememberVectorPainter(FlightIcon)
            val iconCF = ColorFilter.tint(textColor)
            LiquidBottomTabs(
                selectedTabIndex = { tabIdx },
                onTabSelected = { tabIdx = it },
                backdrop = backdrop,
                tabsCount = 4,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp)
            ) {
                repeat(4) { index ->
                    LiquidBottomTab({ tabIdx = index }) {
                        Box(Modifier.size(24.dp).paint(iconPainter, colorFilter = iconCF))
                        BasicText("Tab ${index + 1}", style = TextStyle(textColor, 11.sp))
                    }
                }
            }

            // ═══ Demo 5: 交互式玻璃胶囊 ═══
            SectionTitle("Demo 5: 交互式玻璃胶囊", textColor)
            Text("视觉: 教程 Demo1  |  交互: LiquidButton 液态变形",
                color = textColor.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            InteractiveGlassCapsule(backdrop)

            // ═══ Demo 6: 自适应亮度玻璃 ═══
            SectionTitle("Demo 6: 自适应亮度玻璃", textColor)
            AdaptiveLuminanceGlass(backdrop)

            Spacer(Modifier.height(24.dp))
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.15f))) {
                Text("关闭", color = textColor)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ════════════════════════════════════════
// Demo 1: LiquidCircleButton
// ════════════════════════════════════════
@Composable
private fun LiquidButtonDemo(backdrop: Backdrop, h: InteractiveHighlight) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val sizePx = with(density) { 44.dp.toPx() }
    Box(
        Modifier.size(44.dp)
            .drawBackdrop(backdrop = backdrop, shape = { com.kyant.shapes.Capsule() },
                effects = { vibrancy(); blur(2f.dp.toPx()); lens(12f.dp.toPx(), 24f.dp.toPx()) },
                layerBlock = {
                    val p = h.progress; val s = lerp(1f, 1f + 4f / sizePx, p)
                    val off = h.offset
                    translationX = sizePx * tanh(0.05f * off.x / sizePx)
                    translationY = sizePx * tanh(0.05f * off.y / sizePx)
                    val drag = 4f / sizePx; val angle = atan2(off.y, off.x)
                    scaleX = s + drag * abs(cos(angle) * off.x / sizePx)
                    scaleY = s + drag * abs(sin(angle) * off.y / sizePx)
                },
                onDrawSurface = {}
            ).clip(com.kyant.shapes.Capsule())
            .then(h.drawModifier).then(h.gestureModifier),
        contentAlignment = Alignment.Center
    ) { Text("✈", fontSize = 18.sp) }
}

// ════════════════════════════════════════
// Demo 5: 交互式玻璃胶囊
// ════════════════════════════════════════
@Composable
private fun InteractiveGlassCapsule(backdrop: Backdrop) {
    val scope = rememberCoroutineScope()
    val h = remember(scope) { InteractiveHighlight(scope) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val heightPx = with(density) { 48.dp.toPx() }

    Box(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(48.dp)
            .drawBackdrop(backdrop = backdrop, shape = { Capsule() },
                effects = { vibrancy(); blur(4f.dp.toPx()); lens(16f.dp.toPx(), 32f.dp.toPx()) },
                highlight = { Highlight.Default },
                shadow = { Shadow.Default },
                layerBlock = {
                    val p = h.progress; val s = lerp(1f, 1f + 4f / heightPx, p)
                    val off = h.offset
                    translationX = heightPx * tanh(0.05f * off.x / heightPx)
                    translationY = heightPx * tanh(0.05f * off.y / heightPx)
                    val drag = 4f / heightPx; val angle = atan2(off.y, off.x)
                    scaleX = s + drag * abs(cos(angle) * off.x / heightPx)
                    scaleY = s + drag * abs(sin(angle) * off.y / heightPx)
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.4f)) }
            ).clip(Capsule()).then(h.drawModifier).then(h.gestureModifier),
        contentAlignment = Alignment.Center
    ) { Text("按住→液态变形  松开→回弹", color = Color.White, fontSize = 14.sp) }
}

// ════════════════════════════════════════
// Demo 6: 自适应亮度玻璃
// ════════════════════════════════════════
@Composable
private fun AdaptiveLuminanceGlass(backdrop: Backdrop) {
    val isDark = isSystemInDarkTheme()
    val layer = rememberGraphicsLayer()
    val luminanceAnim = remember(isDark) { Animatable(if (isDark) 0f else 1f) }
    val contentColorAnim = remember(isDark) { Animatable(if (isDark) Color.White else Color.Black) }

    LaunchedEffect(layer) {
        val buffer = IntArray(25)
        while (isActive) {
            kotlinx.coroutines.delay(500)
            val img = layer.toImageBitmap()
            val thumb = img.scale(5, 5)
            thumb.readPixels(buffer)
            val avg = buffer.sumOf { argb ->
                val r = (argb shr 16 and 0xFF) / 255f
                val g = (argb shr 8 and 0xFF) / 255f
                val b = (argb and 0xFF) / 255f
                0.2126 * r + 0.7152 * g + 0.0722 * b
            } / buffer.size
            launch { 
                contentColorAnim.animateTo(if (avg > 0.5f) Color.Black else Color.White, tween(1000))
                luminanceAnim.animateTo(avg.toFloat(), tween(1000))
            }
        }
    }

    Box(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(80.dp)
            .drawBackdrop(backdrop = backdrop, shape = { Capsule() },
                effects = {
                    val l = (luminanceAnim.value * 2f - 1f).let { sign(it) * it * it }
                    colorControls(
                        brightness = if (l > 0f) lerp(0.1f, 0.5f, l) else lerp(0.1f, -0.2f, -l),
                        contrast = if (l > 0f) lerp(1f, 0f, l) else 1f,
                        saturation = 1.5f
                    )
                    blur(if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l) else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l))
                    lens(24f.dp.toPx(), size.minDimension / 2f, depthEffect = true)
                },
                highlight = { Highlight.Plain },
                onDrawBackdrop = { drawBackdrop ->
                    drawBackdrop()
                    layer.record { drawBackdrop() }
                }
            ).clip(Capsule()),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            "luminance: ${(luminanceAnim.value * 100f).fastRoundToInt() / 100.0}",
            style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
            color = { contentColorAnim.value }
        )
    }
}

@Composable
private fun SectionTitle(title: String, textColor: Color) {
    Spacer(Modifier.height(16.dp))
    Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

// Ensure all imports needed for the demos are available
