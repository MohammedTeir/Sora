package mihon.feature.upcoming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.theme.SoraBlue
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.util.isTabletUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.launch
import mihon.feature.upcoming.components.UpcomingItem
import mihon.feature.upcoming.components.calendar.Calendar
import tachiyomi.core.common.Constants
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun UpcomingScreenContent(
    state: UpcomingScreenModel.State,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickUpcoming: (manga: Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val onClickDay: (LocalDate, Int) -> Unit = { date, offset ->
        state.headerIndexes[date]?.let {
            scope.launch {
                listState.animateScrollToItem(it + offset)
            }
        }
    }
    Scaffold(
        topBar = { UpcomingToolbar() },
        modifier = modifier,
    ) { paddingValues ->
        if (isTabletUi()) {
            UpcomingScreenLargeImpl(
                listState = listState,
                items = state.items,
                events = state.events,
                paddingValues = paddingValues,
                selectedYearMonth = state.selectedYearMonth,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = { onClickDay(it, 0) },
                onClickUpcoming = onClickUpcoming,
            )
        } else {
            UpcomingScreenSmallImpl(
                listState = listState,
                items = state.items,
                events = state.events,
                paddingValues = paddingValues,
                selectedYearMonth = state.selectedYearMonth,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = { onClickDay(it, 1) },
                onClickUpcoming = onClickUpcoming,
            )
        }
    }
}

@Composable
private fun UpcomingToolbar() {
    val navigator = LocalNavigator.currentOrThrow
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = navigator::pop) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.action_bar_up_description),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Book,
                contentDescription = null,
                tint = SoraBlue,
                modifier = Modifier.size(24.dp),
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sora",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { uriHandler.openUri(Constants.URL_HELP_UPCOMING) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = stringResource(MR.strings.upcoming_guide),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun DateHeading(
    date: LocalDate,
    mangaCount: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = relativeDateText(date),
            modifier = Modifier
                .padding(MaterialTheme.padding.small)
                .padding(start = MaterialTheme.padding.small),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        Badge(
            containerColor = SoraBlue,
            contentColor = androidx.compose.ui.graphics.Color.White,
        ) {
            Text("$mangaCount")
        }
    }
}

@Composable
private fun UpcomingScreenSmallImpl(
    listState: LazyListState,
    items: ImmutableList<UpcomingUIModel>,
    events: ImmutableMap<LocalDate, Int>,
    paddingValues: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (manga: Manga) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = paddingValues,
        state = listState,
    ) {
        item(key = "upcoming-calendar") {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        }
        items(
            items = items,
            key = { "upcoming-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is UpcomingUIModel.Header -> "header"
                    is UpcomingUIModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is UpcomingUIModel.Item -> {
                    UpcomingItem(
                        upcoming = item.manga,
                        onClick = { onClickUpcoming(item.manga) },
                    )
                }
                is UpcomingUIModel.Header -> {
                    DateHeading(
                        date = item.date,
                        mangaCount = item.mangaCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingScreenLargeImpl(
    listState: LazyListState,
    items: ImmutableList<UpcomingUIModel>,
    events: ImmutableMap<LocalDate, Int>,
    paddingValues: PaddingValues,
    selectedYearMonth: YearMonth,
    setSelectedYearMonth: (YearMonth) -> Unit,
    onClickDay: (LocalDate) -> Unit,
    onClickUpcoming: (manga: Manga) -> Unit,
) {
    TwoPanelBox(
        modifier = Modifier.padding(paddingValues),
        startContent = {
            Calendar(
                selectedYearMonth = selectedYearMonth,
                events = events,
                setSelectedYearMonth = setSelectedYearMonth,
                onClickDay = onClickDay,
            )
        },
        endContent = {
            FastScrollLazyColumn(state = listState) {
                items(
                    items = items,
                    key = { "upcoming-${it.hashCode()}" },
                    contentType = {
                        when (it) {
                            is UpcomingUIModel.Header -> "header"
                            is UpcomingUIModel.Item -> "item"
                        }
                    },
                ) { item ->
                    when (item) {
                        is UpcomingUIModel.Item -> {
                            UpcomingItem(
                                upcoming = item.manga,
                                onClick = { onClickUpcoming(item.manga) },
                            )
                        }
                        is UpcomingUIModel.Header -> {
                            DateHeading(
                                date = item.date,
                                mangaCount = item.mangaCount,
                            )
                        }
                    }
                }
            }
        },
    )
}
