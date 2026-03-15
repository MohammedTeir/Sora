package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Sora Color Scheme — Fixed dark AMOLED theme
 *
 * Key colors:
 * Primary #2977FF (Sora Blue)
 * Secondary #2977FF
 * Tertiary #47A84A
 * Background #0A0A0A (AMOLED Black)
 * Surface #0A0A0A
 * Cards #1A1A2E
 */
internal object TachiyomiColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF82B1FF),
        onPrimary = Color(0xFF002D6E),
        primaryContainer = Color(0xFF0042A0),
        onPrimaryContainer = Color(0xFFD6E2FF),
        inversePrimary = Color(0xFF2977FF),
        secondary = Color(0xFF82B1FF), // Unread badge
        onSecondary = Color(0xFF002D6E), // Unread badge text
        secondaryContainer = Color(0xFF0042A0), // Navigation bar selector pill
        onSecondaryContainer = Color(0xFFD6E2FF), // Navigation bar selector icon
        tertiary = Color(0xFF7ADC77), // Downloaded badge
        onTertiary = Color(0xFF003909), // Downloaded badge text
        tertiaryContainer = Color(0xFF005312),
        onTertiaryContainer = Color(0xFF95F990),
        background = Color(0xFF0A0A0A), // AMOLED black
        onBackground = Color(0xFFE8E8ED),
        surface = Color(0xFF0A0A0A), // AMOLED black
        onSurface = Color(0xFFE8E8ED),
        surfaceVariant = Color(0xFF1A1A2E), // Card surfaces
        onSurfaceVariant = Color(0xFFC5C6D0),
        surfaceTint = Color(0xFF82B1FF),
        inverseSurface = Color(0xFFE8E8ED),
        inverseOnSurface = Color(0xFF0A0A0A),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF8F9099),
        outlineVariant = Color(0xFF2A2A3E),
        surfaceContainerLowest = Color(0xFF050508),
        surfaceContainerLow = Color(0xFF0F0F14),
        surfaceContainer = Color(0xFF1A1A2E), // Navigation bar / Cards
        surfaceContainerHigh = Color(0xFF22223A),
        surfaceContainerHighest = Color(0xFF2A2A44),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF2977FF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E2FF),
        onPrimaryContainer = Color(0xFF001945),
        inversePrimary = Color(0xFF82B1FF),
        secondary = Color(0xFF2977FF),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD6E2FF),
        onSecondaryContainer = Color(0xFF001945),
        tertiary = Color(0xFF006E1B),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF95F990),
        onTertiaryContainer = Color(0xFF002203),
        background = Color(0xFFFEFBFF),
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFEFBFF),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFF3EDF7),
        onSurfaceVariant = Color(0xFF44464F),
        surfaceTint = Color(0xFF2977FF),
        inverseSurface = Color(0xFF303034),
        inverseOnSurface = Color(0xFFF2F0F4),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF757780),
        outlineVariant = Color(0xFFC5C6D0),
        surfaceContainerLowest = Color(0xFFF5F1F8),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFFCF7FF),
        surfaceContainerHighest = Color(0xFFFCF7FF),
    )
}
