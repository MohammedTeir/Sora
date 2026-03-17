package eu.kanade.tachiyomi.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R

@Composable
fun SplashContent(onFinished: () -> Unit) {
    val logoColor = Color(0xFF2977FF)
    val bgColor = Color(0xFF050508)
    
    // Animation States
    val flameProgress = remember { Animatable(0f) }
    var showArcs by remember { mutableStateOf(false) }
    var showArc2 by remember { mutableStateOf(false) }
    var showLeftSide by remember { mutableStateOf(false) }
    var showBottom by remember { mutableStateOf(false) }
    val loadingProgress = remember { Animatable(0f) }
    
    // Breathing Glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Sequential Animation Logic
        flameProgress.animateTo(1f, tween(2000, easing = FastOutSlowInInterpolator))
        
        launch {
            delay(200) // 2.2s total delay for arcs after flame start
            showArcs = true
        }
        launch {
            delay(600) // 2.6s total
            showArc2 = true
        }
        launch {
            delay(800) // 2.8s total
            showLeftSide = true
        }
        launch {
            delay(1100) // 3.1s total
            showBottom = true
        }
        launch {
            delay(1300) // 3.3s total
            loadingProgress.animateTo(1f, tween(1500, easing = LinearOutSlowInInterpolator))
            delay(500) // Brief pause at full bar
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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
            // Ambient Glow in background (Container.svg blurred rect)
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
                // Centered Container Logo (Flame) - Animated path kept in code for draw-on effect
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val flamePath = PathParser().parsePathString("M96 236C103 208 82 180 68 152C96 166 110 138 117 96C131 138 159 124 173 68C187 124 215 138 229 96C236 138 257 159 285 145C271 180 250 201 257 229C236 215 215 236 208 264C194 236 166 243 152 271C145 243 117 236 96 236V236").toPath()
                    
                    val pathMeasure = android.graphics.PathMeasure(flamePath.asAndroidPath(), false)
                    val length = pathMeasure.length
                    val drawPath = Path()
                    pathMeasure.getSegment(0f, length * flameProgress.value, drawPath.asAndroidPath(), true)
                    
                    // Glow Effect
                    drawPath(
                        path = drawPath,
                        color = logoColor.copy(alpha = 0.3f * glowAlpha),
                        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    
                    // Main Path
                    drawPath(
                        path = drawPath,
                        color = logoColor,
                        style = Stroke(width = 2.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
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
                enter = slideInHorizontally(tween(1000, easing = LinearOutSlowInInterpolator)) { -it } + fadeIn(tween(1000)),
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
                // Divider + Label + Loader
                AnimatedVisibility(
                    visible = showBottom,
                    enter = slideInVertically(tween(800)) { it / 2 } + fadeIn(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Horizontal Divider (from Resources or Compose Brush)
                        Image(
                            painter = painterResource(R.drawable.ic_splash_divider),
                            contentDescription = null,
                            modifier = Modifier.width(300.dp).height(1.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // "PURE ESSENCE" Label (from Resources)
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
