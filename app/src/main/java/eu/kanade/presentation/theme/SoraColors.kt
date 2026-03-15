package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color

// ─── Brand ────────────────────────────────────────────────────────────────────
/** Primary brand blue. Single source of truth — import this everywhere. */
val SoraBlue = Color(0xFF2977FF)

/** Lighter variant used in dark-theme primary roles (e.g. text on dark bg). */
val SoraBlueDark = Color(0xFF82B1FF)

// ─── Semantic surface alpha helpers ───────────────────────────────────────────
/** Alpha applied to reader/overlay bars on dark surfaces. */
const val ReaderBarAlphaDark = 0.90f

/** Alpha applied to reader/overlay bars on light surfaces. */
const val ReaderBarAlphaLight = 0.95f

// ─── Scrim / overlay ─────────────────────────────────────────────────────────
/** Semi-transparent black used for image gradient scrims — light-mode safe. */
val MangaCoverScrim = Color(0xAA000000)

// ─── Status semantics (mapped to M3 tertiary in the scheme) ───────────────────
/**
 * Downloaded / success green. Do NOT hardcode — use
 * [MaterialTheme.colorScheme.tertiary] in Compose; this constant is only for
 * places that need a raw Color before a theme is available.
 */
val SoraGreen = Color(0xFF47A84A)
