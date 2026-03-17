package eu.kanade.tachiyomi.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SplashBlue = Color(0xFF2977FF)
private val SplashBackground = Color(0xFF050508)

@Composable
fun SplashContent() {
    // ── Infinite glow breathe ──────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_breathe",
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_scale",
    )

    // ── Flame stroke draw-on  (0 → 2 s) ──────────────────────────────────
    val flameDash = remember { Animatable(1f) } // starts at 1 = fully hidden

    // ── Arc 1 fade-in (delay 2.2 s) ───────────────────────────────────────
    val arc1Alpha = remember { Animatable(0f) }

    // ── Arc 2 fade-in (delay 2.6 s) ───────────────────────────────────────
    val arc2Alpha = remember { Animatable(0f) }

    // ── SORA text (left panel) fade+slide (delay 2.8 s) ───────────────────
    val soraAlpha = remember { Animatable(0f) }
    val soraOffset = remember { Animatable(-40f) }

    // ── Bottom section fade-up (delay 3.1 s) ─────────────────────────────
    val bottomAlpha = remember { Animatable(0f) }
    val bottomOffset = remember { Animatable(30f) }

    // ── Loading bar fill (delay 3.3 s) ────────────────────────────────────
    val loadingProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Flame draw-on over 2 s
        launch {
            flameDash.animateTo(
                targetValue = 0f,
                animationSpec = tween(2000, easing = LinearEasing),
            )
        }
        // Arc 1 – delay 2.2 s
        launch {
            delay(2200)
            arc1Alpha.animateTo(1f, animationSpec = tween(400, easing = EaseOut))
        }
        // Arc 2 – delay 2.6 s
        launch {
            delay(2600)
            arc2Alpha.animateTo(1f, animationSpec = tween(400, easing = EaseOut))
        }
        // SORA text – delay 2.8 s
        launch {
            delay(2800)
            launch { soraAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseOut)) }
            soraOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        // Bottom – delay 3.1 s
        launch {
            delay(3100)
            launch { bottomAlpha.animateTo(1f, animationSpec = tween(500, easing = EaseOut)) }
            bottomOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        // Loading bar – delay 3.3 s
        launch {
            delay(3300)
            loadingProgress.animateTo(1f, animationSpec = tween(1800, easing = EaseInOut))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
    ) {

        // ── Ambient ambient glow (blurred blue circle always visible) ───────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .scale(glowScale)
                .blur(80.dp)
                .clip(RoundedCornerShape(50))
                .background(SplashBlue.copy(alpha = glowAlpha)),
        )

        // ── SORA vertical text  (left side) ────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = soraOffset.value.dp)
                .alpha(soraAlpha.value)
                .padding(start = 24.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_sora_text),
                contentDescription = "SORA",
                modifier = Modifier
                    .width(38.dp)
                    .height(128.dp),
            )
        }

        // ── Center logo with flame & arcs drawn via Canvas ────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 360f
                val scaleY = size.height / 360f

                // ── Outer flame path (stroke draw-on) ──────────────────────
                // Approximate path length for the flame shape
                val flameEstimatedLength = 1600f
                val dashOn = flameEstimatedLength * (1f - flameDash.value)
                val dashOff = flameEstimatedLength

                val flamePaint = Stroke(
                    width = 2.1f * scaleX,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(dashOn.coerceAtLeast(0.01f), dashOff),
                        phase = 0f,
                    ),
                )

                // Draw the flame silhouette as a canvas path
                val flamePath = androidx.compose.ui.graphics.Path().apply {
                    // Translated from SVG path:
                    // M96,236C103,208 82,180 68,152C96,166 110,138 117,96
                    // C131,138 159,124 173,68C187,124 215,138 229,96
                    // C236,138 257,159 285,145C271,180 250,201 257,229
                    // C236,215 215,236 208,264C194,236 166,243 152,271
                    // C145,243 117,236 96,236Z
                    moveTo(96f * scaleX, 236f * scaleY)
                    cubicTo(103f * scaleX, 208f * scaleY, 82f * scaleX, 180f * scaleY, 68f * scaleX, 152f * scaleY)
                    cubicTo(96f * scaleX, 166f * scaleY, 110f * scaleX, 138f * scaleY, 117f * scaleX, 96f * scaleY)
                    cubicTo(131f * scaleX, 138f * scaleY, 159f * scaleX, 124f * scaleY, 173f * scaleX, 68f * scaleY)
                    cubicTo(187f * scaleX, 124f * scaleY, 215f * scaleX, 138f * scaleY, 229f * scaleX, 96f * scaleY)
                    cubicTo(236f * scaleX, 138f * scaleY, 257f * scaleX, 159f * scaleY, 285f * scaleX, 145f * scaleY)
                    cubicTo(271f * scaleX, 180f * scaleY, 250f * scaleX, 201f * scaleY, 257f * scaleX, 229f * scaleY)
                    cubicTo(236f * scaleX, 215f * scaleY, 215f * scaleX, 236f * scaleY, 208f * scaleX, 264f * scaleY)
                    cubicTo(194f * scaleX, 236f * scaleY, 166f * scaleX, 243f * scaleY, 152f * scaleX, 271f * scaleY)
                    cubicTo(145f * scaleX, 243f * scaleY, 117f * scaleX, 236f * scaleY, 96f * scaleX, 236f * scaleY)
                    close()
                }
                drawPath(
                    path = flamePath,
                    color = SplashBlue,
                    style = flamePaint,
                )

                // ── Upper arc  (fade-in at 2.2 s) ─────────────────────────
                // M138,152 C166,133.333 194,133.333 222,152
                val arc1Path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(138f * scaleX, 152f * scaleY)
                    cubicTo(166f * scaleX, 133.333f * scaleY, 194f * scaleX, 133.333f * scaleY, 222f * scaleX, 152f * scaleY)
                }
                drawPath(
                    path = arc1Path,
                    color = SplashBlue.copy(alpha = 0.4f * arc1Alpha.value),
                    style = Stroke(width = 2.1f * scaleX, cap = StrokeCap.Round),
                )

                // ── Lower arc (fade-in at 2.6 s) ──────────────────────────
                // M159,194 C173,184.667 187,184.667 201,194
                val arc2Path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(159f * scaleX, 194f * scaleY)
                    cubicTo(173f * scaleX, 184.667f * scaleY, 187f * scaleX, 184.667f * scaleY, 201f * scaleX, 194f * scaleY)
                }
                drawPath(
                    path = arc2Path,
                    color = SplashBlue.copy(alpha = 0.4f * arc2Alpha.value),
                    style = Stroke(width = 2.1f * scaleX, cap = StrokeCap.Round),
                )
            }
        }

        // ── Bottom section (divider + PURE ESSENCE label + loading bar) ────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    alpha = bottomAlpha.value
                    translationY = bottomOffset.value.dp.toPx()
                }
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Horizontal divider
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            ) {
                // Gradient line: transparent → #2977FF → transparent
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SplashBlue.copy(alpha = 0f),
                            SplashBlue,
                            SplashBlue.copy(alpha = 0f),
                        ),
                    ),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = size.height,
                )
                // White opaque center segment (130–260 out of 390)
                val segStart = size.width * (130f / 390f)
                val segEnd = size.width * (260f / 390f)
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(segStart, size.height / 2f),
                    end = Offset(segEnd, size.height / 2f),
                    strokeWidth = size.height,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PURE ESSENCE SVG label
            Image(
                painter = painterResource(id = R.drawable.splash_pure_essence),
                contentDescription = "PURE ESSENCE",
                modifier = Modifier
                    .width(194.dp)
                    .height(15.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Loading bar  ─ blue gradient, fills left→right
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(loadingProgress.value)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    SplashBlue.copy(alpha = 0.6f),
                                    SplashBlue,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}
