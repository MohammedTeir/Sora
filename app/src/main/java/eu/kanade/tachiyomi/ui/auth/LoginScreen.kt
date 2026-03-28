package eu.kanade.tachiyomi.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R

class LoginScreen(private val fromStartup: Boolean = false) : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val activity = context as? android.app.Activity ?: context
        val screenModel = rememberScreenModel { AuthScreenModel() }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var passwordVisible by rememberSaveable { mutableStateOf(false) }
        val keyboardController = LocalSoftwareKeyboardController.current

        // Handle events
        LaunchedEffect(Unit) {
            screenModel.events.collect { event ->
                when (event) {
                    is AuthScreenModel.Event.LoginSuccess -> navigator.pop()
                    is AuthScreenModel.Event.SignUpSuccess -> navigator.pop()
                    is AuthScreenModel.Event.PasswordResetEmailSent -> {} // Handled in reset screen
                    is AuthScreenModel.Event.PasswordResetSuccess -> {} // Handled in step 3 screen
                    is AuthScreenModel.Event.Dismissed -> navigator.pop()
                }
            }
        }

        // Show error as snackbar
        LaunchedEffect(state.errorMessage) {
            state.errorMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                screenModel.clearError()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                // Background blurred circles
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.TopStart)
                        .offset(x = (-96).dp, y = (-96).dp)
                        .requiredSize(size = 384.dp)
                        .clip(shape = RoundedCornerShape(9999.dp))
                        .blur(radius = 120.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .offset(x = 192.dp, y = 250.dp)
                        .requiredSize(size = 500.dp)
                        .clip(shape = RoundedCornerShape(9999.dp))
                        .blur(radius = 150.dp)
                        .background(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f))
                )

                // Main Content Scrollable Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = 48.dp,
                                bottom = 96.dp
                            )
                    ) {
                        // Header text
                        Column(
                            modifier = Modifier.padding(bottom = 64.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "SORA",
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.em,
                                        style = TextStyle(
                                            fontSize = 80.sp,
                                            fontWeight = FontWeight.Light,
                                            letterSpacing = (-4).sp,
                                            shadow = Shadow(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                offset = Offset(0f, 0f),
                                                blurRadius = 20f
                                            )
                                        ),
                                        modifier = Modifier
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                            .shadow(elevation = 20.dp)
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "EDITORIAL NOIR / ACCESS PORTAL",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.33.em,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 2.4.sp
                                        ),
                                        modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically)
                                    )
                                }
                            }
                        }

                        // Form Container
                        Column(
                            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.Top),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape = RoundedCornerShape(24.dp))
                                    .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .border(
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(all = 32.dp)
                                    .shadow(elevation = 40.dp, shape = RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Identity Input
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "IDENTITY",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 1.5.em,
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    letterSpacing = 1.sp
                                                ),
                                                modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(shape = RoundedCornerShape(12.dp))
                                                    .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .border(
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(16.dp)
                                            ) {
                                                BasicTextField(
                                                    value = email,
                                                    onValueChange = { email = it },
                                                    modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                                                    textStyle = TextStyle(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Email,
                                                        imeAction = ImeAction.Next,
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                                    decorationBox = { innerTextField ->
                                                        if (email.isEmpty()) {
                                                            Text(
                                                                text = "Email Address",
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = TextStyle(
                                                                    fontSize = 16.sp,
                                                                    fontWeight = FontWeight.Normal
                                                                )
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                )
                                            }
                                            Image(
                                                painter = painterResource(id = R.drawable.email_svg),
                                                contentDescription = "Container",
                                                modifier = Modifier
                                                    .align(alignment = Alignment.CenterEnd)
                                                    .padding(end = 16.dp)
                                                    .requiredSize(20.dp)
                                            )
                                        }
                                    }

                                    // Password Input
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "SECURITY KEY",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 1.5.em,
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                            Text(
                                                text = "FORGOT?",
                                                color = MaterialTheme.colorScheme.primary,
                                                lineHeight = 1.5.em,
                                                style = TextStyle(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    letterSpacing = 1.sp
                                                ),
                                                modifier = Modifier.clickable { navigator.push(ResetPasswordStep1Screen()) }
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(shape = RoundedCornerShape(12.dp))
                                                    .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .border(
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(16.dp)
                                            ) {
                                                BasicTextField(
                                                    value = password,
                                                    onValueChange = { password = it },
                                                    modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                    textStyle = TextStyle(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    ),
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.Password,
                                                        imeAction = ImeAction.Done,
                                                    ),
                                                    keyboardActions = KeyboardActions(
                                                        onDone = {
                                                            keyboardController?.hide()
                                                            screenModel.login(email, password, context)
                                                        },
                                                    ),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                                    decorationBox = { innerTextField ->
                                                        if (password.isEmpty()) {
                                                            Text(
                                                                text = "Password",
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = TextStyle(
                                                                    fontSize = 16.sp,
                                                                    fontWeight = FontWeight.Normal
                                                                )
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                )
                                            }
                                            IconButton(
                                                onClick = { passwordVisible = !passwordVisible },
                                                modifier = Modifier.align(alignment = Alignment.CenterEnd).padding(end = 16.dp).size(20.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.lock_svg),
                                                    contentDescription = "Lock",
                                                    modifier = Modifier.requiredSize(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Login Button
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(shape = RoundedCornerShape(24.dp))
                                            .background(color = if (state.isLoading) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary)
                                            .clickable(enabled = !state.isLoading) {
                                                keyboardController?.hide()
                                                screenModel.login(email, password, context)
                                            }
                                            .padding(vertical = 20.dp)
                                            .shadow(
                                                elevation = 30.dp,
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                    ) {
                                        if (state.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text(
                                                text = "SIGN IN",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 1.43.em,
                                                style = TextStyle(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    letterSpacing = 1.4.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            text = "OR CONTINUE WITH",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 1.5.em,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 3.sp
                                            )
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shape = RoundedCornerShape(12.dp))
                                        .border(
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !state.isLoading) {
                                            keyboardController?.hide()
                                            screenModel.signInWithGoogle(activity)
                                        }
                                        .padding(vertical = 16.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.google_svg),
                                        contentDescription = "SVG",
                                        modifier = Modifier.requiredSize(size = 20.dp)
                                    )
                                    Text(
                                        text = "GOOGLE",
                                        color = MaterialTheme.colorScheme.onSurface,
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

                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "NEW TO THE COLLECTIVE? ",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.5.em,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = "CREATE ACCOUNT",
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.5.em,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.clickable {
                                            navigator.replace(SignupScreen(fromStartup = fromStartup))
                                        }
                                    )
                                }
                                
                                if (!fromStartup) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = "CONTINUE AS GUEST",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 1.5.em,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 1.sp
                                            ),
                                            modifier = Modifier.clickable {
                                                navigator.pop()
                                            }
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = "PRIVACY",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = "TERMS",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                    Text(
                                        text = "SUPPORT",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Back button if not from startup
                if (!fromStartup) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .padding(top = 32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}
