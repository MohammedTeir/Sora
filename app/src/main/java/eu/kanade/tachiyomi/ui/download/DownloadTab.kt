package eu.kanade.tachiyomi.ui.download

import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object DownloadTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            return TabOptions(
                index = 6u,
                title = stringResource(MR.strings.label_downloads),
                icon = painterResource(R.drawable.ic_nav_downloads),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        // Scroll to top or handle reselect if needed
    }

    @Composable
    override fun Content() {
        DownloadQueueScreen.Content()
    }
}
