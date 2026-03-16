package eu.kanade.tachiyomi.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.view.setComposeContent
import androidx.compose.runtime.CompositionLocalProvider
import eu.kanade.core.util.LocalSourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.injectLazy

abstract class BaseOAuthLoginActivity : BaseActivity() {

    internal val trackerManager: TrackerManager by injectLazy()

    abstract fun handleResult(uri: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            CompositionLocalProvider(LocalSourceManager provides Injekt.get()) {
                LoadingScreen()
            }
        }

        val data = intent.data
        if (data == null) {
            returnToSettings()
        } else {
            handleResult(data)
        }
    }

    internal fun returnToSettings() {
        finish()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
