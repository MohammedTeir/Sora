package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Colors
private val SoraBlue = Color(0xFF8AACFF)
private val SoraBlueDeep = Color(0xFF146DF5)
private val OnboardBg = Color(0xFF000000)

internal class WelcomeStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        WelcomeStepContent()
    }
}

@Composable
internal fun WelcomeStepContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = OnboardBg),
    ) {
        // ----- Background glow orbs -----
        Box(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = (-39).dp, y = (-88.3).dp)
                .requiredWidth(width = 195.dp)
                .requiredHeight(height = 442.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 120.dp)
                .background(color = SoraBlue.copy(alpha = 0.1f)),
        )
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .offset(x = 19.5.dp, y = 44.14.dp)
                .requiredWidth(width = 156.dp)
                .requiredHeight(height = 353.dp)
                .clip(shape = RoundedCornerShape(9999.dp))
                .blur(radius = 100.dp)
                .background(color = SoraBlueDeep.copy(alpha = 0.1f)),
        )

        // ----- Manga sticker decorations -----
        // Top-left sticker (Luffy style)
        StickerBox(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = (-6).dp, y = 72.dp)
                .requiredSize(size = 175.dp)
                .rotate(degrees = 12f),
            color = Color(0xFFFF6B35),
            icon = Icons.Filled.MenuBook,
        )

        // Right center sticker (Solo Leveling style)
        StickerBox(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = 20.dp, y = (-30).dp)
                .requiredSize(size = 160.dp)
                .rotate(degrees = 6f),
            color = Color(0xFF9B59B6),
            icon = Icons.Filled.Person,
        )

        // Bottom right sticker (Naruto style)
        StickerBox(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .offset(x = 5.dp, y = (-120).dp)
                .requiredSize(size = 200.dp)
                .rotate(degrees = -12f),
            color = Color(0xFFFF9500),
            icon = Icons.Filled.AutoStories,
        )

        // ----- Center text block -----
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(alignment = Alignment.Center)
                .offset(y = (-30).dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) { append("Welcome to ") }
                    withStyle(
                        style = SpanStyle(
                            color = SoraBlue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) { append("Sora") }
                },
                modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your ultimate manga universe",
                color = Color(0xFFABABAB),
                textAlign = TextAlign.Center,
                lineHeight = 1.63.em,
                style = TextStyle(fontSize = 16.sp),
                modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically),
            )
        }
    }
}

@Composable
private fun StickerBox(
    modifier: Modifier = Modifier,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
            .clip(shape = RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.08f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(72.dp),
        )
    }
}
