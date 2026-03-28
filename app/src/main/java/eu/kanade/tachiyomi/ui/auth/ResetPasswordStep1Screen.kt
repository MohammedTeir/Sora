package eu.kanade.tachiyomi.ui.auth

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

class ResetPasswordStep1Screen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val screenModel = rememberScreenModel { AuthScreenModel() }
        val state by screenModel.state.collectAsState()

        var email by rememberSaveable { mutableStateOf("") }

        LaunchedEffect(Unit) {
            screenModel.events.collect { event ->
                when (event) {
                    is AuthScreenModel.Event.PasswordResetEmailSent -> {
                        Toast.makeText(context, "Password reset link sent to ${event.email}", Toast.LENGTH_LONG).show()
                        navigator.push(ResetPasswordStep2Screen(email = event.email))
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 120.dp, bottom = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Blurred Background Elements
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 80.dp, y = 0.dp)
                                .fillMaxHeight()
                                .width(320.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .blur(120.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-80).dp, y = 0.dp)
                                .fillMaxHeight()
                                .width(256.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .blur(100.dp)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f))
                        )

                        // Mascot area (scaled down/rotated)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(192.dp)
                                .rotate(2f)
                        ) {
                            Column(
                                modifier = Modifier.rotate(2f)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .size(192.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                                        .shadow(50.dp, RoundedCornerShape(16.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.soramascot),
                                        contentDescription = "Sora Mascot",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(9999.dp))
                                    .blur(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 22.dp, y = 13.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .rotate(8f)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .shadow(15.dp, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "SORA ASSIST",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    lineHeight = 1.33.em,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = (-0.6).sp
                                    ),
                                    modifier = Modifier
                                        .width(75.dp)
                                        .height(16.dp)
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                )
                            }
                        }

                        // Text Area
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 240.dp, bottom = 40.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "FORGOT\nPASSWORD?",
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 1.em,
                                style = TextStyle(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = (-2.4).sp
                                ),
                                modifier = Modifier.wrapContentHeight()
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Enter your email to receive a\npassword reset link.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 1.63.em,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = Modifier.wrapContentHeight()
                            )
                        }

                        // Forms Area
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 446.dp, bottom = 16.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Email Input
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(71.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                                            .padding(start = 48.dp, end = 16.dp, top = 23.dp, bottom = 24.dp)
                                            .shadow(20.dp, RoundedCornerShape(12.dp))
                                    ) {
                                        BasicTextField(
                                            value = email,
                                            onValueChange = { email = it },
                                            textStyle = TextStyle(
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(align = Alignment.CenterVertically),
                                            decorationBox = { innerTextField ->
                                                if (email.isEmpty()) {
                                                    Text(
                                                        text = "your@email.com",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = TextStyle(
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Normal
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset(x = 16.dp, y = 0.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.email_svg),
                                            contentDescription = "Email",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Send Reset Link Button
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable(enabled = !state.isLoading) {
                                            keyboardController?.hide()
                                            screenModel.sendPasswordResetEmail(email)
                                        }
                                        .padding(vertical = 20.dp)
                                        .shadow(30.dp, RoundedCornerShape(12.dp))
                                ) {
                                    Text(
                                        text = if (state.isLoading) "Sending..." else "Send Reset Link",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.56.em,
                                        style = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.wrapContentHeight()
                                    )
                                    if (!state.isLoading) {
                                        Image(
                                            painter = painterResource(id = R.drawable.arrow_send_email),
                                            contentDescription = "Send",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                            modifier = Modifier.size(19.dp, 16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Back to Sign In
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable { navigator.pop() }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.icon_back_to_login),
                                    contentDescription = "Back",
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "BACK TO SIGN IN",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 1.33.em,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.2.sp
                                    ),
                                    modifier = Modifier.wrapContentHeight()
                                )
                            }
                        }
                    }
                }
            }

            // Top App Bar Area
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_back_home),
                            contentDescription = "Back",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { navigator.pop() }
                        )
                        Text(
                            text = "SORA RESET",
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 1.33.em,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-1.2).sp
                            ),
                            modifier = Modifier.wrapContentHeight()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .shadow(elevation = 40.dp)
            )
        }
    }
}
