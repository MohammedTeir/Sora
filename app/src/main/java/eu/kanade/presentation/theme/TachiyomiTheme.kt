package eu.kanade.presentation.theme

import android.os.Build
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme

/**
 * CompositionLocal holding a mutable dark-mode flag.
 * Default (fallback) value is a light-mode state.
 */
val LocalDarkTheme = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

@Composable
fun TachiyomiTheme(
    isDark: Boolean = LocalDarkTheme.current.value,
    // Issue #1: Material You / Dynamic Color.
    // When true and running on Android 12+ (API 31), the colour scheme is
    // derived from the user's wallpaper via the dynamic color APIs.
    // Defaults to false so the existing Sora Blue palette is unchanged for
    // all existing users. Wired to UiPreferences.useDynamicColor() in
    // ViewExtensions.kt which is the single root theme provider.
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(isDark, useDynamicColor) {
        when {
            // Dynamic color requires Android 12+ (API 31)
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDark ->
                dynamicDarkColorScheme(context)
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                dynamicLightColorScheme(context)
            // Fall back to Sora's fixed AMOLED / light palette
            else ->
                TachiyomiColorScheme.getColorScheme(
                    isDark = isDark,
                    isAmoled = isDark,
                    overrideDarkSurfaceContainers = isDark,
                )
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    content: @Composable () -> Unit,
) = TachiyomiTheme(content = content)
