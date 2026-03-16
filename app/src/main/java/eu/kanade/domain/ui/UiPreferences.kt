package eu.kanade.domain.ui

import eu.kanade.domain.ui.model.TabletUiMode
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class UiPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun relativeTime() = preferenceStore.getBoolean("relative_time_v2", true)

    fun dateFormat() = preferenceStore.getString("app_date_format", "")

    fun tabletUiMode() = preferenceStore.getEnum("tablet_ui_mode", TabletUiMode.AUTOMATIC)

    fun imagesInDescription() = preferenceStore.getBoolean("pref_render_images_description", true)

    /**
     * Issue #1: Material You / Dynamic Color support.
     *
     * When enabled on Android 12+ (API 31), the app colour scheme is derived
     * from the user's wallpaper via [androidx.compose.material3.dynamicDarkColorScheme]
     * and [androidx.compose.material3.dynamicLightColorScheme].
     *
     * Defaults to false so the existing Sora Blue palette is preserved for all
     * existing users. They can opt in via Settings → Appearance.
     */
    fun useDynamicColor() = preferenceStore.getBoolean("use_dynamic_color", false)

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            else -> DateTimeFormatter.ofPattern(format, Locale.getDefault())
        }
    }
}
