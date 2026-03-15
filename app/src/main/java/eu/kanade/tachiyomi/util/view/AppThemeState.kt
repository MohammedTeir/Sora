package eu.kanade.tachiyomi.util.view

import androidx.compose.runtime.mutableStateOf

/**
 * Application-wide dark-theme toggle state.
 *
 * All Compose roots (Activity and ComposeView) share this single instance so that
 * toggling dark mode in HomeTab is immediately reflected everywhere in the app.
 */
object AppThemeState {
    val isDark = mutableStateOf(false)
}
