package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val slideDistance = rememberSlideDistance()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            WelcomeStep(),
            StorageStep(),
            PermissionStep(),
            RestoreStep(onRestoreBackup = onRestoreBackup),
        )
    }

    BackHandler(enabled = currentStep != 0) {
        currentStep--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardBlack)
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Top navigation bar ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (currentStep in 1 until steps.lastIndex) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Progress dots (steps 1 and 2 only — storage & permissions)
                if (currentStep in 1..2) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(3) { index ->
                            val isActive = index == currentStep - 1
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .then(if (isActive) Modifier.requiredWidth(24.dp) else Modifier.requiredWidth(8.dp))
                                    .clip(CircleShape)
                                    .background(if (isActive) OnboardBlue else Color(0xFF3F3F46)),
                            )
                        }
                    }
                }
            }

            // ---- Step content area ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        materialSharedAxisX(
                            forward = targetState > initialState,
                            slideDistance = slideDistance,
                        )
                    },
                    label = "stepContent",
                ) { step ->
                    steps[step].Content()
                }
            }

            // ---- Bottom action area ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, OnboardBlack.copy(alpha = 0.95f), OnboardBlack),
                        ),
                    )
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (currentStep) {
                    0 -> {
                        // Welcome
                        OnboardGradientButton(text = "Next", onClick = { currentStep++ })
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 4 dots, first wide+blue, rest small+dark
                            Box(
                                modifier = Modifier
                                    .height(8.dp).requiredWidth(24.dp)
                                    .clip(CircleShape).background(OnboardBlue),
                            )
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp).clip(CircleShape)
                                        .background(Color(0xFF3F3F46)),
                                )
                            }
                        }
                    }

                    1 -> {
                        // Storage
                        OnboardGradientButton(
                            text = "Continue",
                            onClick = { currentStep++ },
                            enabled = steps[currentStep].isComplete,
                            showArrow = true,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "STORAGE PERMISSIONS WILL BE REQUESTED NEXT",
                            color = OnboardGray.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.1.sp,
                            ),
                        )
                    }

                    2 -> {
                        // Permissions
                        OnboardGradientButton(
                            text = "Continue",
                            onClick = { currentStep++ },
                            showArrow = true,
                        )
                    }

                    3 -> {
                        // Final/Restore
                        OnboardGradientButton(
                            text = "Go to Home",
                            onClick = onComplete,
                            showArrow = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardGradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showArrow: Boolean = false,
) {
    val bgBrush = if (enabled) {
        Brush.linearGradient(listOf(OnboardBlue, OnboardBlueDeep))
    } else {
        Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF333333)))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(56.dp)
            .clip(CircleShape)
            .background(brush = bgBrush)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.Black else Color.Gray,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.45.sp,
            ),
        )
        if (showArrow && enabled) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_onboard_arrow),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
