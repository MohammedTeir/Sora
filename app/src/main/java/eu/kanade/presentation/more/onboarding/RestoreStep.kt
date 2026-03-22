package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R

internal class RestoreStep(
    private val onRestoreBackup: () -> Unit,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        RestoreStepContent(onRestoreBackup = onRestoreBackup)
    }
}

@Composable
internal fun RestoreStepContent(
    onRestoreBackup: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = OnboardBlack),
    ) {
        // Background glow orbs
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = (-39).dp, y = 88.dp)
                .requiredWidth(width = 156.dp)
                .requiredHeight(height = 354.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 120.dp)
                .background(color = OnboardBlue.copy(alpha = 0.05f)),
        )
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .offset(x = 39.dp, y = (-176).dp)
                .requiredWidth(width = 195.dp)
                .requiredHeight(height = 442.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 120.dp)
                .background(color = OnboardBlue.copy(alpha = 0.05f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // ---- Mascot / checkmark icon ----
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(OnboardBlue.copy(alpha = 0.1f))
                    .border(2.dp, OnboardBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 40.dp)
                        .background(OnboardBlue.copy(alpha = 0.1f)),
                )
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = OnboardBlue,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ---- Title ----
            Text(
                text = "All Set!",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 1.11.em,
                style = TextStyle(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.9).sp,
                ),
                modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Subtitle ----
            Text(
                text = "Sora is ready to use. Start fresh or restore\nyour library from a previous backup.",
                color = OnboardGray,
                textAlign = TextAlign.Center,
                lineHeight = 1.63.em,
                style = TextStyle(fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ---- Start Fresh Card ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(OnboardCard)
                    .padding(all = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Icon button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(OnboardBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_onboard_fresh),
                        contentDescription = null,
                        tint = OnboardBlue,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Fresh",
                        color = Color.White,
                        lineHeight = 1.4.em,
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Initialize a clean library",
                        color = OnboardGray,
                        lineHeight = 1.43.em,
                        style = TextStyle(fontSize = 14.sp),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = OnboardGray,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Restore Backup Card ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF191919).copy(alpha = 0.5f))
                    .clickable { onRestoreBackup() }
                    .padding(all = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Icon button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF45455B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_onboard_restore),
                        contentDescription = null,
                        tint = Color(0xFFE2E0FC),
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Restore Backup",
                        color = Color.White,
                        lineHeight = 1.4.em,
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "Import your previous data",
                        color = OnboardGray,
                        lineHeight = 1.43.em,
                        style = TextStyle(fontSize = 14.sp),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = OnboardGray,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Terms of service note
            Text(
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(color = OnboardGray, fontSize = 12.sp),
                    ) { append("By continuing, you agree to our ") }
                    withStyle(
                        style = SpanStyle(
                            color = OnboardBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    ) { append("Terms of Service") }
                    withStyle(
                        style = SpanStyle(color = OnboardGray, fontSize = 12.sp),
                    ) { append(".") }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
