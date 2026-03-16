package eu.kanade.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import tachiyomi.domain.source.service.SourceManager

// ─────────────────────────────────────────────────────────────────────────────
// LocalSourceManager — app-scoped CompositionLocal for SourceManager.
//
// Bug #11 fix: the old ifSourcesLoaded() called Injekt.get<SourceManager>()
// inside remember{} on every call site. Voyager pushes and pops screens
// by entering and exiting composition, so every navigation event triggered
// a fresh DI lookup even though SourceManager is a singleton.
//
// Using staticCompositionLocalOf means the value is resolved once at the
// root of the composition tree (in MainActivity) and then read directly
// from the slot table on every subsequent call — zero DI overhead.
//
// Provide this in MainActivity's setComposeContent block:
//
//     CompositionLocalProvider(LocalSourceManager provides Injekt.get()) {
//         /* Navigator / theme / content */
//     }
// ─────────────────────────────────────────────────────────────────────────────
val LocalSourceManager = staticCompositionLocalOf<SourceManager> {
    error(
        "LocalSourceManager not provided. " +
            "Wrap your root composable with " +
            "CompositionLocalProvider(LocalSourceManager provides Injekt.get()) { … }",
    )
}

/**
 * Returns true once [SourceManager.isInitialized] emits true.
 *
 * All 6 call sites across BrowseSourceScreen, GlobalSearchScreen,
 * MangaScreen, MigrateSourceSearchScreen, SourcePreferencesScreen and
 * ReaderActivity stay unchanged — they all receive a plain [Boolean].
 */
@Composable
fun ifSourcesLoaded(): Boolean =
    LocalSourceManager.current.isInitialized.collectAsState().value
