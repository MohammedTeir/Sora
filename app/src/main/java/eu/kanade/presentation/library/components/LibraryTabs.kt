package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.components.material.TabText
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)

    // Issue #3: read category accent colors from the existing preference store
    // (format: "categoryId:#RRGGBB"). The color dot is already shown in
    // CategoryListItem — here we extend it to the library tab indicator so
    // every category tab highlights in its own assigned color when selected.
    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    val categoryColors = remember { libraryPreferences.categoryColors().get() }

    Column(modifier = Modifier.zIndex(2f)) {
        PrimaryScrollableTabRow(
            selectedTabIndex = currentPageIndex,
            edgePadding = 0.dp,
            // TODO: use default when width is fixed upstream
            // https://issuetracker.google.com/issues/242879624
            divider = {},
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            categories.forEachIndexed { index, category ->
                // Resolve the accent color stored for this category, falling
                // back to the theme primary if none has been assigned.
                val accentColor = remember(categoryColors, category.id) {
                    val entry = categoryColors.find { it.startsWith("${category.id}:") }
                    val hex = entry?.substringAfter(":")
                    if (hex != null) {
                        try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                Tab(
                    selected = currentPageIndex == index,
                    onClick = { onTabItemClick(index) },
                    text = {
                        TabText(
                            text = category.visualName,
                            badgeCount = getItemCountForCategory(category),
                        )
                    },
                    // Use the category's accent color for the selected tab;
                    // fall back to theme primary for categories without a color.
                    selectedContentColor = accentColor ?: MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
