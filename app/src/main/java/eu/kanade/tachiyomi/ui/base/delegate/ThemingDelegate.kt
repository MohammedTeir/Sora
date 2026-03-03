package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import eu.kanade.tachiyomi.R

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(): List<Int> {
            return listOf(
                R.style.Theme_Tachiyomi,
                R.style.ThemeOverlay_Tachiyomi_Amoled,
            )
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        ThemingDelegate.getThemeResIds().forEach(activity::setTheme)
    }
}
