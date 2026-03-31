package eu.kanade.tachiyomi.ui.auth

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.R

data class ResetPasswordStep2Screen(val email: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { AuthScreenModel() }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.events.collect { event ->
                when (event) {
                    is AuthScreenModel.Event.PasswordResetEmailSent -> {
                        Toast.makeText(context, "Reset link resent to ${event.email}", Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        LaunchedEffect(state.errorMessage) {
            state.errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                screenModel.clearError()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            // Background blur shape
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(500.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .blur(120.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Main Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 82.5.dp, bottom = 50.5.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(48.dp, Alignment.Top),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // Header Icons Graphic
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.size(128.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.size(128.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .size(96.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .rotate(-3f)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                                .border(
                                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .shadow(40.dp, RoundedCornerShape(16.dp))
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.icon_email), // Updated to correct SVG
                                                contentDescription = "Email Icon",
                                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.shadow(15.dp)
                                            )
                                        }
                                    }
                                    // Smaller decorated piece
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-11).dp)
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .rotate(12f)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                            .border(
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.icon_open_email), // Fallback or extra graphic
                                            contentDescription = "Small Mail Graphic",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), RoundedCornerShape(9999.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), RoundedCornerShape(9999.dp))
                                    )
                                }
                            }

                            // Text Content
                            Column(
                                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Check Your\nEmail",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.em,
                                        style = TextStyle(
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = (-2.4).sp
                                        )
                                    )
                                    Text(
                                        text = "We've sent a password reset\nlink to your email address.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.56.em,
                                        style = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
}

                                // Interactive Actions Box
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            RoundedCornerShape(32.dp)
                                        )
                                        .padding(32.dp)
                                        .shadow(40.dp, RoundedCornerShape(32.dp))
                                ) {
                                    // Open Email App Button
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.inversePrimary
                                                    )
                                                )
                                            )
                                            .clickable {
                                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                // Create chooser just in case user doesn't have an email app installed/defaulted
                                                context.startActivity(Intent.createChooser(intent, "Open Email App"))
                                            }
                                            .padding(vertical = 20.dp)
                                            .shadow(30.dp, RoundedCornerShape(12.dp))
                                    ) {
                                        Text(
                                            text = "Open Email App",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 1.56.em,
                                            style = TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Image(
                                            painter = painterResource(id = R.drawable.icon_open_email),
                                            contentDescription = "Open Email App",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                                        )
                                    }

                                    // Resend Link Button
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                            .border(
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = !state.isLoading) {
                                                screenModel.sendPasswordResetEmail(email)
                                            }
                                            .padding(vertical = 20.dp)
                                    ) {
                                        Text(
                                            text = if (state.isLoading) "Sending..." else "Resend Link",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 1.56.em,
                                            style = TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }

                                // Return to login Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            navigator.popUntilRoot()
                                        }
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.icon_back_to_login),
                                            contentDescription = "Back",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Text(
                                            text = "RETURN TO LOGIN",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 1.33.em,
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 1.2.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Area
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        )
                    }
                    Text(
                        text = "SORA ARCHITECTURE © 2024",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 1.5.em,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp
                        )
                    )
                }
            }
            
            // App Bar Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "SORA RESET",
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 1.33.em,
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.2).sp
                    )
                )
            }
        }
    }
}
