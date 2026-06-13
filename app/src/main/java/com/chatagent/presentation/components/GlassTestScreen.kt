package com.chatagent.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.runtimeShaderEffect
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private val tabNames = listOf("液态按钮", "效果调试", "渐进模糊")

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
            Image(painter = bitmapPainter, contentDescription = null, Modifier.fillMaxSize().layerBackdrop(backdrop), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop))
        }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(40.dp))
            Text("Liquid Glass", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))
            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { Text(if (bgUri == null) "📷 选择壁纸" else "🔄 换壁纸", color = textColor, fontSize = 13.sp) }
            Spacer(Modifier.height(8.dp))

            // Tab 导航
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
                    1 -> PlaygroundPage(backdrop, textColor)
                    2 -> BlurPage(backdrop, textColor)
                }
            }

            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.1f)),
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            ) { Text("关闭", color = textColor) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ══════════════════════════
// Tab 0: ButtonsContent
// ══════════════════════════
@Composable
private fun ButtonsPage(backdrop: Backdrop, textColor: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("LiquidButton 五种变体", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        listOf(
            "透明液态按钮" to null to null,
            "半透明表面" to null to textColor.copy(alpha = 0.15f),
            "蓝色着色" to Color(0xFF0088FF) to null,
            "橙色着色" to Color(0xFFFF8D28) to null,
            "绿色着色" to Color(0xFF10A37F) to null,
        ).forEach { ((label, tint), surface) ->
            LiquidBtn(backdrop, onClick = {}, tint = tint, surface = surface, content = { BasicText(label, style = TextStyle(if (tint != null) Color.White else textColor, 15.sp)) })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LiquidBtn(backdrop: Backdrop, onClick: () -> Unit, tint: Color?, surface: Color?, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val hl = remember(scope) { InteractiveHighlight(scope) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hPx = with(density) { 48.dp.toPx() }

    Row(
        Modifier.fillMaxWidth().height(48.dp)
            .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                effects = { vibrancy(); blur(2f.dp.toPx()); lens(12f.dp.toPx(), 24f.dp.toPx()) },
                layerBlock = {
                    val p = hl.progress; val off = hl.offset
                    val s = lerp(1f, 1f + 4f / hPx, p)
                    translationX = hPx * tanh(0.05f * off.x / hPx)
                    translationY = hPx * tanh(0.05f * off.y / hPx)
                    val drag = 4f / hPx; val angle = atan2(off.y, off.x)
                    scaleX = s + drag * abs(cos(angle) * off.x / hPx)
                    scaleY = s + drag * abs(sin(angle) * off.y / hPx)
                },
                onDrawSurface = {
                    if (tint != null) { drawRect(tint, blendMode = BlendMode.Hue); drawRect(tint.copy(alpha = 0.75f)) }
                    if (surface != null) drawRect(surface)
                }
            ).clip(RoundedCornerShape(999.dp))
            .then(hl.modifier).then(hl.gestureModifier)
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() }
    )
}

// ══════════════════════════
// Tab 1: GlassPlayground
// ══════════════════════════
@Composable
private fun PlaygroundPage(backdrop: Backdrop, textColor: Color) {
    val scope = rememberCoroutineScope()
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val zoomAnim = remember { Animatable(1f) }
    val rotAnim = remember { Animatable(0f) }
    var cornerFrac by remember { mutableFloatStateOf(0.5f) }
    var blurDp by remember { mutableFloatStateOf(0f) }
    var refrH by remember { mutableFloatStateOf(0.2f) }
    var refrA by remember { mutableFloatStateOf(0.2f) }
    var chromAb by remember { mutableFloatStateOf(0f) }

    val reset = {
        scope.launch { launch { offsetAnim.animateTo(Offset.Zero) }; launch { zoomAnim.animateTo(1f) }; launch { rotAnim.animateTo(0f) } }
        cornerFrac = 0.5f; blurDp = 0f; refrH = 0.2f; refrA = 0.2f; chromAb = 0f
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Text("效果调试器", color = textColor.copy(alpha = 0.5f), fontSize = 13.sp)
        Text("双指缩放/旋转/拖拽", color = textColor.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))

        Box(Modifier.size(200.dp).drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(100.dp * cornerFrac) },
            effects = {
                val md = size.minDimension; vibrancy(); blur(blurDp.dp.toPx())
                lens(refrH * md * 0.5f, refrA * md, depthEffect = true, chromaticAberration = chromAb > 0f)
            },
            highlight = { Highlight.Plain },
            layerBlock = { translationX = offsetAnim.value.x; translationY = offsetAnim.value.y; scaleX = zoomAnim.value; scaleY = zoomAnim.value; rotationZ = rotAnim.value }
        ).pointerInput(scope) {
            fun Offset.rotateBy(a: Float): Offset { val r = a * (PI / 180); return Offset((x * cos(r) - y * sin(r)).toFloat(), (x * sin(r) + y * cos(r)).toFloat()) }
            detectTransformGestures { _, pan, gz, gr ->
                scope.launch { offsetAnim.snapTo(offsetAnim.value + pan.rotateBy(rotAnim.value) * zoomAnim.value); zoomAnim.snapTo(zoomAnim.value * gz); rotAnim.snapTo(rotAnim.value + gr) }
            }
        }, contentAlignment = Alignment.Center) {
            BasicText("双指交互", style = TextStyle(textColor.copy(alpha = 0.6f), 14.sp))
        }

        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SliderRow("圆角", cornerFrac, { cornerFrac = it }, 0f..1f, backdrop, textColor)
            SliderRow("模糊", blurDp, { blurDp = it }, 0f..32f, backdrop, textColor)
            SliderRow("折射高度", refrH, { refrH = it }, 0f..1f, backdrop, textColor)
            SliderRow("折射量", refrA, { refrA = it }, 0f..1f, backdrop, textColor)
            SliderRow("色散", chromAb, { chromAb = it }, 0f..1f, backdrop, textColor)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(40.dp)
            .drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
                effects = { vibrancy(); blur(2f.dp.toPx()); lens(8f.dp.toPx(), 14f.dp.toPx()) },
                onDrawSurface = { drawRect(Color(0xFFFF8D28), blendMode = BlendMode.Hue); drawRect(Color(0xFFFF8D28).copy(alpha = 0.75f)) }
            ).clip(RoundedCornerShape(999.dp)).clickable { reset() },
            contentAlignment = Alignment.Center
        ) { Text("重置", color = Color.White, fontSize = 14.sp) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, backdrop: Backdrop, textColor: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = textColor, fontSize = 13.sp)
            Text(String.format("%.2f", value), color = textColor.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        val progress = (value - range.start) / (range.endInclusive - range.start)
        Box(Modifier.fillMaxWidth().height(22.dp).drawBackdrop(backdrop = backdrop, shape = { RoundedCornerShape(999.dp) },
            effects = { vibrancy(); blur(2f.dp.toPx()); lens(4f.dp.toPx(), 8f.dp.toPx()) }, onDrawSurface = {}
        ).clip(RoundedCornerShape(999.dp))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxSize().clip(RoundedCornerShape(999.dp)).background(Color(0xFF0088FF).copy(alpha = 0.3f)))
        }
    }
}

// ══════════════════════════
// Tab 2: ProgressiveBlur
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
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(160.dp).drawPlainBackdrop(backdrop = backdrop, shape = { RectangleShape },
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
