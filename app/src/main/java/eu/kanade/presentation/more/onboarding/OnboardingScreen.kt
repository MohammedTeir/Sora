package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import eu.kanade.presentation.theme.SoraBlue

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
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0) {
        currentStep--
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Top Navigation & Progress Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (hidden on step 0 and last step if desired)
                if (currentStep > 0) {
                    IconButton(onClick = { currentStep-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp)) // Placeholder to balance center
                }

                Spacer(modifier = Modifier.weight(1f))

                // Progress Indicator Pills (Steps 1, 2, 3 only)
                if (currentStep in 1..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(2) { index ->
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(if (index == currentStep - 1) 24.dp else 12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index <= currentStep - 1) SoraBlue else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp)) // Placeholder for symmetry
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                ) {
                    steps[it].Content()
                }
            }

            // Bottom Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (currentStep == 0) {
                    // Welcome Step Layout
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoraBlue),
                            shape = CircleShape
                        ) {
                            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        // Welcome Page Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (index == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                } else if (currentStep == 1) {
                    // Storage Step
                    Button(
                        onClick = { currentStep++ },
                        enabled = steps[currentStep].isComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoraBlue,
                            disabledContainerColor = Color(0xFF333333)
                        ),
                        shape = CircleShape
                    ) {
                        Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else if (currentStep == 2) {
                    // Permissions Step
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { currentStep++ },
                            enabled = steps[currentStep].isComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoraBlue,
                            ),
                            shape = CircleShape
                        ) {
                            Text("Grant All", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                            ),
                            shape = CircleShape
                        ) {
                            Text("Maybe Later", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0AEC0))
                        }
                    }
                } else if (currentStep == 3) {
                    // Restore Step
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoraBlue),
                        shape = CircleShape
                    ) {
                        Text("Go to Home", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
