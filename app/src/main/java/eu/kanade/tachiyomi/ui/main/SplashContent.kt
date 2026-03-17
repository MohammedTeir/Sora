package eu.kanade.tachiyomi.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.tachiyomi.R

// ─── Brand tokens ─────────────────────────────────────────────────────────────
private val SoraBlue        = Color(0xFF2977FF)
private val SplashBg        = Color(0xFF050508)
private val White40         = Color.White.copy(alpha = 0.40f)
private val SoraBlueGlow    = Color(0xFF2977FF).copy(alpha = 0.18f)
private val SoraBlueDim     = Color(0xFF2977FF).copy(alpha = 0.0f)

/**
 * Replacement for the old SplashContent.
 *
 * Animation timeline (all delays in ms):
 *   0      – background ambient glow fades in
 *   0      – flame outer path draws on  (2 000 ms duration)
 *   2 200  – inner arc 1 draws on       (600 ms)
 *   2 600  – inner arc 2 draws on       (600 ms)
 *   2 800  – vertical "SORA" text slides in from left
 *   3 100  – divider + "PURE ESSENCE" label fades up
 *   3 300  – loading bar fills left → right
 */
@Composable
fun SplashContent() {

    // ── Animation floats ────────────────────────────────────────────────────
    val flameProg   = remember { Animatable(0f) }   // 0→1 outer flame draw-on
    val arc1Prog    = remember { Animatable(0f) }   // inner arc 1
    val arc2Prog    = remember { Animatable(0f) }   // inner arc 2
    val sideAlpha   = remember { Animatable(0f) }   // SORA side text
    val sideOffsetX = remember { Animatable(-40f) } // slide from left
    val bottomAlpha = remember { Animatable(0f) }   // divider + label
    val bottomOffY  = remember { Animatable(16f) }  // fade-up offset
    val loadProg    = remember { Animatable(0f) }   // loading bar
    val glowAlpha   = remember { Animatable(0f) }   // ambient glow

    // Breathing glow for the logo (runs after draw-on completes)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val breatheGlow by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue  = 0.32f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    LaunchedEffect(Unit) {
        // Ambient glow fades in quickly
        launch { glowAlpha.animateTo(1f, tween(800, easing = EaseOut)) }

        // Outer flame draw-on
        launch { flameProg.animateTo(1f, tween(2000, easing = LinearEasing)) }

        // Inner arcs sequentially
        delay(2200)
        launch { arc1Prog.animateTo(1f, tween(600, easing = EaseOut)) }
        delay(400)
        launch { arc2Prog.animateTo(1f, tween(600, easing = EaseOut)) }

        // Side text
        delay(2800)
        launch { sideAlpha.animateTo(1f, tween(600, easing = EaseOut)) }
        launch { sideOffsetX.animateTo(0f, tween(600, easing = EaseOut)) }

        // Bottom section
        delay(3100)
        launch { bottomAlpha.animateTo(1f, tween(700, easing = EaseOut)) }
        launch { bottomOffY.animateTo(0f, tween(700, easing = EaseOut)) }

        // Loading bar
        delay(3300)
        loadProg.animateTo(1f, tween(2400, easing = LinearEasing))
    }

    // ── Root container ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBg),
    ) {

        // ── 1. Ambient background glow (blurred rect from Container.svg) ───
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .alpha(glowAlpha.value)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(SoraBlue.copy(alpha = 0.14f), Color.Transparent),
                    ),
                    shape = RoundedCornerShape(9999.dp),
                ),
        )

        // ── 2. Left vertical "SORA" text (Container__1_.svg) ───────────────
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.splash_sora_text),
            contentDescription = "SORA vertical label",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(38.dp, 128.dp)
                .offset(x = sideOffsetX.value.dp)
                .alpha(sideAlpha.value),
            contentScale = ContentScale.Fit,
        )

        // ── 3. Center logo: animated flame paths drawn via Canvas ──────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Drop-shadow glow layer (breathing)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .blur(40.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SoraBlue.copy(alpha = breatheGlow),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(9999.dp),
                    ),
            )

            // Animated SVG paths via Canvas
            Canvas(modifier = Modifier.size(240.dp)) {
                val scaleX = size.width  / 360f
                val scaleY = size.height / 360f

                // Outer flame path
                drawAnimatedPath(
                    pathData = FLAME_PATH,
                    progress = flameProg.value,
                    color    = SoraBlue,
                    strokeW  = 2.1f * scaleX.coerceAtLeast(1f),
                    scaleX   = scaleX,
                    scaleY   = scaleY,
                )

                // Inner arc 1  (M138 152 C166... M222 152)
                drawAnimatedPath(
                    pathData = ARC1_PATH,
                    progress = arc1Prog.value,
                    color    = SoraBlue.copy(alpha = 0.4f),
                    strokeW  = 2.1f * scaleX.coerceAtLeast(1f),
                    scaleX   = scaleX,
                    scaleY   = scaleY,
                )

                // Inner arc 2  (M159 194 ...)
                drawAnimatedPath(
                    pathData = ARC2_PATH,
                    progress = arc2Prog.value,
                    color    = SoraBlue.copy(alpha = 0.4f),
                    strokeW  = 2.1f * scaleX.coerceAtLeast(1f),
                    scaleX   = scaleX,
                    scaleY   = scaleY,
                )
            }
        }

        // ── 4. Bottom section: divider + label + loading bar ───────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .alpha(bottomAlpha.value)
                .offset(y = bottomOffY.value.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            // Horizontal divider (Container__1_ top + gradient)
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.splash_divider),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                contentScale = ContentScale.FillWidth,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // "PURE ESSENCE" label (Container__3_.svg)
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.splash_pure_essence),
                contentDescription = "PURE ESSENCE",
                modifier = Modifier
                    .width(200.dp)
                    .height(15.dp),
                contentScale = ContentScale.Fit,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading bar — blue gradient, fills left to right
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(White40.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = loadProg.value)
                        .height(3.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(SoraBlue.copy(alpha = 0.6f), SoraBlue),
                            ),
                        ),
                )
            }
        }
    }
}

// ─── Path draw-on helper ───────────────────────────────────────────────────────

/**
 * Draws [pathData] up to [progress] fraction using PathMeasure.
 * Scales the path by [scaleX]/[scaleY] to fit the Canvas size.
 */
private fun DrawScope.drawAnimatedPath(
    pathData: String,
    progress: Float,
    color: Color,
    strokeW: Float,
    scaleX: Float,
    scaleY: Float,
) {
    if (progress <= 0f) return
    val androidPath = android.graphics.Path().apply { parseSvgPath(pathData) }
    val composePath = androidPath.asComposePath()

    val measure   = PathMeasure()
    measure.setPath(composePath, false)
    val totalLen  = measure.length
    val stopDist  = totalLen * progress

    val segment = Path()
    measure.getSegment(0f, stopDist, segment, true)

    translate(left = 0f, top = 0f) {
        drawPath(
            path   = scalePath(segment, scaleX, scaleY),
            color  = color,
            style  = Stroke(
                width     = strokeW,
                cap       = StrokeCap.Round,
                join      = StrokeJoin.Round,
            ),
        )
    }
}

/** Returns a new Path scaled by sx/sy. */
private fun scalePath(src: Path, sx: Float, sy: Float): Path {
    val matrix = android.graphics.Matrix().apply { setScale(sx, sy) }
    val ap = android.graphics.Path()
    src.asAndroidPath().transform(matrix, ap)
    return ap.asComposePath()
}

/** Minimal SVG path parser for M/C/L/V commands (sufficient for these paths). */
private fun android.graphics.Path.parseSvgPath(d: String) {
    val tokens = d.trim().split(Regex("(?=[MmCcLlVvZz])|,|\\s+")).filter { it.isNotBlank() }
    var i = 0
    var cx = 0f; var cy = 0f
    while (i < tokens.size) {
        when (val cmd = tokens[i++]) {
            "M" -> { cx = tokens[i++].toFloat(); cy = tokens[i++].toFloat(); moveTo(cx, cy) }
            "C" -> {
                val x1 = tokens[i++].toFloat(); val y1 = tokens[i++].toFloat()
                val x2 = tokens[i++].toFloat(); val y2 = tokens[i++].toFloat()
                val x  = tokens[i++].toFloat(); val y  = tokens[i++].toFloat()
                cubicTo(x1, y1, x2, y2, x, y); cx = x; cy = y
            }
            "L" -> { cx = tokens[i++].toFloat(); cy = tokens[i++].toFloat(); lineTo(cx, cy) }
            "V" -> { cy = tokens[i++].toFloat(); lineTo(cx, cy) }
            "Z", "z" -> close()
            else -> { /* skip unknown */ }
        }
    }
}

// ─── Exact SVG path data from the assets ──────────────────────────────────────

private const val FLAME_PATH =
    "M96 236 C103 208 82 180 68 152 C96 166 110 138 117 96 C131 138 159 124 173 68 " +
    "C187 124 215 138 229 96 C236 138 257 159 285 145 C271 180 250 201 257 229 " +
    "C236 215 215 236 208 264 C194 236 166 243 152 271 C145 243 117 236 96 236 Z"

private const val ARC1_PATH =
    "M138 152 C166 133.333 194 133.333 222 152"

private const val ARC2_PATH =
    "M159 194 C173 184.667 187 184.667 201 194"

/*
 * ─── Drawable resource references ────────────────────────────────────────────
 *
 * Add the following to your res/drawable directory (copy the SVG files in):
 *
 *   ic_splash_sora_text.xml  ← "Container (1).svg"   (38×128 vertical SORA)
 *   ic_splash_divider.xml    ← "Horizontal Divider.svg" (390×1 gradient line)
 *   ic_splash_label.xml      ← "Container (3).svg"   (390×15 PURE ESSENCE text)
 *
 * Android Studio will auto-convert .svg → VectorDrawable when you drag them
 * into res/drawable. Alternatively run:
 *
 *   ./gradlew generateDebugAssets
 *
 * and reference them as R.drawable.splash_sora_text etc.
 *
 * NOTE: The outer flame + inner arcs from Container.svg are drawn directly via
 * Canvas using the raw path coordinates above — no drawable needed for those.
 * ─────────────────────────────────────────────────────────────────────────────
 */
