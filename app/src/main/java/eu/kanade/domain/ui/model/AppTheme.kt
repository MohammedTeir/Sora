package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class AppTheme(val titleRes: StringResource?) {
    DEFAULT(MR.strings.label_default),

    // Deprecated - kept for migration compatibility
    MONET(null),
    CUSTOM(null),
    CATPPUCCIN(null),
    GREEN_APPLE(null),
    LAVENDER(null),
    MIDNIGHT_DUSK(null),
    NORD(null),
    STRAWBERRY_DAIQUIRI(null),
    TAKO(null),
    TEALTURQUOISE(null),
    TIDAL_WAVE(null),
    YINYANG(null),
    YOTSUBA(null),
    MONOCHROME(null),
    CLOUDFLARE(null),
    COTTONCANDY(null),
    DOOM(null),
    MATRIX(null),
    MOCHA(null),
    SAPPHIRE(null),
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),
}
