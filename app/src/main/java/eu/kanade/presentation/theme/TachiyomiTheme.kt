package eu.kanade.presentation.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme

/**
 * CompositionLocal holding a mutable dark-mode flag.
 * Default (fallback) value is a light-mode state.
 */
val LocalDarkTheme = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

@Composable
fun TachiyomiTheme(
    isDark: Boolean = LocalDarkTheme.current.value,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = remember(isDark) {
            TachiyomiColorScheme.getColorScheme(
                isDark = isDark,
                isAmoled = isDark, // AMOLED only makes sense in dark mode
                overrideDarkSurfaceContainers = isDark,
            )
        },
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    content: @Composable () -> Unit,
) = TachiyomiTheme(content = content)
