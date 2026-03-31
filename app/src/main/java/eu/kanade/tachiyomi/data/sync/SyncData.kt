package eu.kanade.tachiyomi.data.sync

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track
import mihon.domain.extensionrepo.model.ExtensionRepo

data class SyncData(
    val manga: List<Manga>,
    val chapters: List<Chapter>,
    val categories: List<Category>,
    val tracks: List<Track>,
    val history: List<History>,
    val extensionRepos: List<ExtensionRepo>,
)
