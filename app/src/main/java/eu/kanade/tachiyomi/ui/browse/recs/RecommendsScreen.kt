package eu.kanade.tachiyomi.ui.browse.recs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.recs.RecommendsContent
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class RecommendsScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val screenModel = rememberScreenModel { RecommendsScreenModel(mangaId) }
        val state by screenModel.state.collectAsState()

        val onClickItem = { manga: Manga ->
            if (manga.source != -1L) {
                navigator.push(MangaScreen(manga.id, true))
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.recommendations_title, state.title.orEmpty()),
                    scrollBehavior = scrollBehavior,
                    navigateUp = navigator::pop,
                )
            },
        ) { paddingValues ->
            if (state.items.isEmpty()) {
                LoadingScreen()
            } else {
                RecommendsContent(
                    items = state.filteredItems,
                    contentPadding = paddingValues,
                    getManga = @Composable { manga: Manga -> screenModel.getManga(manga) },
                    onClickItem = onClickItem,
                    onLongClickItem = onClickItem,
                )
            }
        }
    }
}
