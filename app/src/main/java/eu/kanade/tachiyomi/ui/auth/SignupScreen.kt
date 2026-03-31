package eu.kanade.tachiyomi.ui.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R

class SignupScreen(private val fromStartup: Boolean = false) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val activity = context as? android.app.Activity ?: context
        val screenModel = rememberScreenModel { AuthScreenModel() }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        var fullName by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var confirmPassword by rememberSaveable { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        LaunchedEffect(Unit) {
            screenModel.events.collect { event ->
                when (event) {
                    is AuthScreenModel.Event.LoginSuccess -> navigator.pop() // already logged in
                    is AuthScreenModel.Event.SignUpSuccess -> navigator.pop()
                    is AuthScreenModel.Event.PasswordResetEmailSent -> {} // Handled elsewhere
                    is AuthScreenModel.Event.PasswordResetSuccess -> {} // Handled elsewhere
                    is AuthScreenModel.Event.Dismissed -> navigator.pop()
                }
            }
        }

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
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Background blurs
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.TopStart)
                        .offset(x = (-39).dp, y = (-91).dp)
                        .requiredWidth(width = 156.dp)
                        .requiredHeight(height = 364.dp)
                        .clip(shape = RoundedCornerShape(9999.dp))
                        .blur(radius = 120.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                )
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomEnd)
                        .offset(x = 39.dp, y = 91.dp)
                        .requiredWidth(width = 195.dp)
                        .requiredHeight(height = 455.dp)
                        .clip(shape = RoundedCornerShape(9999.dp))
                        .blur(radius = 150.dp)
                        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 48.dp)
                    ) {
                        Column(modifier = Modifier.padding(bottom = 40.dp)) {
                            Box(
                                modifier = Modifier
                                    .requiredWidth(width = 342.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = buildAnnotatedString {
                                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Black)) { append("SORA ") }
                                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Black)) { append("//\n") }
                                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Black)) { append("CREATE ACCOUNT") }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                    )
                                }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // FULL NAME
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .requiredHeight(height = 56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    BasicTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                        singleLine = true,
                                        enabled = !state.isLoading,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 56.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                        decorationBox = { innerTextField ->
                                            if (fullName.isEmpty()) {
                                                Text(
                                                    text = "Full Name",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    style = TextStyle(fontSize = 16.sp)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.CenterStart)
                                            .padding(start = 16.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Person,
                                            contentDescription = "Person",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // EMAIL
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .requiredHeight(height = 56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    BasicTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                        singleLine = true,
                                        enabled = !state.isLoading,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 56.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                        decorationBox = { innerTextField ->
                                            if (email.isEmpty()) {
                                                Text(
                                                    text = "Email Address",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    style = TextStyle(fontSize = 16.sp)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.CenterStart)
                                            .padding(start = 16.dp)
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

                                // PASSWORD
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .requiredHeight(height = 56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    BasicTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                        singleLine = true,
                                        enabled = !state.isLoading,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 56.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                        decorationBox = { innerTextField ->
                                            if (password.isEmpty()) {
                                                Text(
                                                    text = "Password",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    style = TextStyle(fontSize = 16.sp)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.CenterStart)
                                            .padding(start = 16.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.lock_svg),
                                            contentDescription = "Lock",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                // CONFIRM PASSWORD
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .requiredHeight(height = 56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    BasicTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                                        singleLine = true,
                                        enabled = !state.isLoading,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            keyboardController?.hide()
                                            screenModel.signUp(email, password, confirmPassword, context)
                                        }),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 56.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                        decorationBox = { innerTextField ->
                                            if (confirmPassword.isEmpty()) {
                                                Text(
                                                    text = "Confirm password",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    style = TextStyle(fontSize = 16.sp)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(alignment = Alignment.CenterStart)
                                            .padding(start = 16.dp)
                                            .fillMaxHeight()
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.lock_svg),
                                            contentDescription = "Lock Confirm",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // SIGN UP BUTTON
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .requiredHeight(height = 56.dp)
                                    .clip(shape = RoundedCornerShape(16.dp))
                                    .background(
                                        brush = if (state.isLoading) 
                                            androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant) 
                                        else 
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.inversePrimary
                                                )
                                            )
                                    )
                                    .clickable(enabled = !state.isLoading) {
                                        keyboardController?.hide()
                                        screenModel.signUp(email, password, confirmPassword, context)
                                    }
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "SIGN UP",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.5.em,
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.6.sp
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.arrow_signup),
                                        contentDescription = "Sign Up",
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Divider(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    modifier = Modifier.requiredHeight(height = 1.dp).weight(weight = 0.5f)
                                )
                                Text(
                                    text = "OR CONTINUE WITH",
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    lineHeight = 1.5.em,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Divider(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    modifier = Modifier.requiredHeight(height = 1.dp).weight(weight = 0.5f)
                                )
                            }

                            // GOOGLE BUTTON
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .requiredHeight(height = 56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                    .clickable(enabled = !state.isLoading) {
                                        keyboardController?.hide()
                                        screenModel.signInWithGoogle(activity)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.google_svg),
                                        contentDescription = "Google",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "GOOGLE",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 1.5.em,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                    )
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
                            modifier = Modifier.padding(top = 40.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Medium)) { append("Already have an account? ") }
                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)) { append("Sign In") }
                                    },
                                    modifier = Modifier
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .clickable {
                                            navigator.replace(LoginScreen(fromStartup = fromStartup))
                                        }
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "PRIVACY",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 1.5.em,
                                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                )
                                Text(
                                    text = "TERMS",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 1.5.em,
                                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                )
                                Text(
                                    text = "SUPPORT",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 1.5.em,
                                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

