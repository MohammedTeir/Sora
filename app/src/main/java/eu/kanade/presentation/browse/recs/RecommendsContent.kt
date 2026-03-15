package eu.kanade.presentation.browse.recs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.ui.browse.recs.RecommendationItemResult
import eu.kanade.tachiyomi.ui.browse.recs.sources.RecommendationPagingSource
import kotlinx.collections.immutable.ImmutableMap
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun RecommendsContent(
    items: ImmutableMap<RecommendationPagingSource, RecommendationItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items.forEach { (source, recResult) ->
            item(key = "${source::class.simpleName}-${source.name}-${source.category.resourceId}") {
                GlobalSearchResultItem(
                    title = source.name,
                    subtitle = stringResource(source.category),
                    onClick = {},
                ) {
                    when (recResult) {
                        RecommendationItemResult.Loading -> {
                            GlobalSearchLoadingResultItem()
                        }
                        is RecommendationItemResult.Success -> {
                            GlobalSearchCardRow(
                                titles = recResult.result,
                                getManga = getManga,
                                onClick = onClickItem,
                                onLongClick = onLongClickItem,
                            )
                        }
                        is RecommendationItemResult.Error -> {
                            GlobalSearchErrorResultItem(
                                message = with(LocalContext.current) {
                                    recResult.throwable.formattedMessage
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
