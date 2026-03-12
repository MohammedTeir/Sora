package eu.kanade.tachiyomi.ui.download

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
                index = 2u,
                title = stringResource(MR.strings.label_downloads),
                icon = rememberVectorPainter(Icons.Outlined.Download),
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
