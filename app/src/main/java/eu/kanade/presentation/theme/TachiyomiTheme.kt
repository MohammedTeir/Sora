package eu.kanade.presentation.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme

@Composable
fun TachiyomiTheme(
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = remember {
            TachiyomiColorScheme.getColorScheme(
                isDark = true,
                isAmoled = true,
                overrideDarkSurfaceContainers = true,
            )
        },
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    content: @Composable () -> Unit,
) = TachiyomiTheme(content)
