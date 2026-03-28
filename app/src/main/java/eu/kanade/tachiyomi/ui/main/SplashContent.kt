package eu.kanade.tachiyomi.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R

@Composable
fun SplashContent(onFinished: () -> Unit) {
    val logoColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    // Animation States
    val flameProgress = remember { Animatable(0f) }
    var showArcs by remember { mutableStateOf(false) }
    var showArc2 by remember { mutableStateOf(false) }
    var showLeftSide by remember { mutableStateOf(false) }
    var showBottom by remember { mutableStateOf(false) }
    val loadingProgress = remember { Animatable(0f) }

    // --- Performance: hoist heavy path objects so they are created once, not per-frame ---
    val flamePath = remember {
        PathParser().parsePathString(
            "M96 236C103 208 82 180 68 152C96 166 110 138 117 96C131 138 159 124 173 68" +
            "C187 124 215 138 229 96C236 138 257 159 285 145C271 180 250 201 257 229" +
            "C236 215 215 236 208 264C194 236 166 243 152 271C145 243 117 236 96 236V236"
        ).toPath()
    }
    val androidFlamePath = remember { flamePath.asAndroidPath() }
    val pathMeasure = remember { android.graphics.PathMeasure(androidFlamePath, false) }
    val totalLength = remember { pathMeasure.length }
    // Reused every frame via reset() — eliminates per-frame allocation
    val drawPath = remember { Path() }

    // Breathing Glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Sequential Animation Logic
        flameProgress.animateTo(1f, tween(2000, easing = FastOutSlowInEasing))

        launch {
            delay(200)
            showArcs = true
        }
        launch {
            delay(600)
            showArc2 = true
        }
        launch {
            delay(800)
            showLeftSide = true
        }
        launch {
            delay(1100)
            showBottom = true
        }
        launch {
            delay(1300)
            loadingProgress.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing))
            delay(500)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Phone Shape Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(bgColor)
        ) {
            // Ambient Glow in background
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp)
                    .blur(50.dp)
                    .background(logoColor.copy(alpha = 0.1f * glowAlpha), RoundedCornerShape(80.dp))
            )

            // Centered Logo Content
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(360.dp),
                contentAlignment = Alignment.Center
            ) {
                // Flame — drawn with a matrix transform so it is always centered + properly scaled
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Advance segment to current animation progress (reuse drawPath, no allocation)
                    drawPath.reset()
                    drawPath.asAndroidPath().reset()
                    pathMeasure.getSegment(
                        0f,
                        totalLength * flameProgress.value,
                        drawPath.asAndroidPath(),
                        true,
                    )

                    // Compute scale + translation that centers the path inside this canvas
                    val bounds = android.graphics.RectF()
                    androidFlamePath.computeBounds(bounds, true)
                    val pathW = bounds.width()
                    val pathH = bounds.height()
                    val targetSize = minOf(size.width, size.height) * 0.72f
                    val scale = targetSize / maxOf(pathW, pathH)
                    val tx = (size.width  - pathW * scale) / 2f - bounds.left * scale
                    val ty = (size.height - pathH * scale) / 2f - bounds.top  * scale

                    val matrix = android.graphics.Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(tx, ty)
                    }
                    drawPath.asAndroidPath().transform(matrix)

                    val strokeWidth = 2.1f * scale

                    // Glow layer
                    drawPath(
                        path = drawPath,
                        color = logoColor.copy(alpha = 0.3f * glowAlpha),
                        style = Stroke(width = strokeWidth * 3.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    // Main stroke
                    drawPath(
                        path = drawPath,
                        color = logoColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // Inner Arcs (from Resources)
                Column(
                    modifier = Modifier.offset(y = (-10).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(visible = showArcs, enter = fadeIn(tween(600))) {
                        Image(
                            painter = painterResource(R.drawable.ic_splash_arc1),
                            contentDescription = null,
                            modifier = Modifier.size(width = 90.dp, height = 25.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    AnimatedVisibility(visible = showArc2, enter = fadeIn(tween(600))) {
                        Image(
                            painter = painterResource(R.drawable.ic_splash_arc2),
                            contentDescription = null,
                            modifier = Modifier.size(width = 50.dp, height = 15.dp)
                        )
                    }
                }
            }

            // Left Side Vertical Asset (from Resources)
            AnimatedVisibility(
                visible = showLeftSide,
                enter = slideInHorizontally(tween(1000, easing = LinearOutSlowInEasing)) { -it } + fadeIn(tween(1000)),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_splash_side),
                    contentDescription = null,
                    modifier = Modifier.size(width = 38.dp, height = 128.dp)
                )
            }

            // Bottom Section (Pure Essence & Divider & Loading Bar)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = showBottom,
                    enter = slideInVertically(tween(800)) { it / 2 } + fadeIn(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_splash_divider),
                            contentDescription = null,
                            modifier = Modifier.width(300.dp).height(1.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_splash_label),
                            contentDescription = null,
                            modifier = Modifier.size(width = 390.dp, height = 15.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // Loading Bar
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(loadingProgress.value)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(logoColor.copy(alpha = 0.5f), logoColor)
                                        ),
                                        RoundedCornerShape(1f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
