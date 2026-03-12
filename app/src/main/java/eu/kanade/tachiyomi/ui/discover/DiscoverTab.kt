package eu.kanade.tachiyomi.ui.discover

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object DiscoverTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 7u,
            title = "Discover",
            icon = rememberVectorPainter(Icons.Outlined.Explore),
        )

    override suspend fun onReselect(navigator: Navigator) {}

    @Composable
    override fun Content() {
        DiscoverScreen()
    }
}
