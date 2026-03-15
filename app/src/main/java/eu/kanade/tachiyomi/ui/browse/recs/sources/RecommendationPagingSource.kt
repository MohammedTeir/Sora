package eu.kanade.tachiyomi.ui.browse.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * General class for recommendation sources.
 */
abstract class RecommendationPagingSource(
    protected val manga: Manga,
) {
    // Display name
    abstract val name: String

    // Localized category name
    open val category: StringResource = MR.strings.recommendations_similar

    /**
     * Recommendation sources that display results from a source extension,
     * can override this property to associate results with a specific source.
     */
    open val associatedSourceId: Long? = null

    abstract suspend fun requestNextPage(currentPage: Int): MangasPage

    companion object {
        fun createSources(manga: Manga): List<RecommendationPagingSource> {
            return buildList {
                add(AniListPagingSource(manga))
                add(MangaUpdatesCommunityPagingSource(manga))
                add(MangaUpdatesSimilarPagingSource(manga))
                add(MyAnimeListPagingSource(manga))
            }.sortedWith(compareBy({ it.name }, { it.category.resourceId }))
        }
    }
}

/**
 * General class for recommendation sources backed by trackers.
 */
abstract class TrackerRecommendationPagingSource(
    protected val endpoint: String,
    manga: Manga,
) : RecommendationPagingSource(manga) {
    private val getTracks: GetTracks by injectLazy()

    protected val trackerManager: TrackerManager by injectLazy()
    protected val client by lazy { Injekt.get<NetworkHelper>().client }
    protected val json by injectLazy<Json>()

    /**
     * Tracker id associated with the recommendation source.
     */
    abstract val associatedTrackerId: Long?

    abstract suspend fun getRecsBySearch(search: String): List<SManga>
    abstract suspend fun getRecsById(id: String): List<SManga>

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        val tracks = getTracks.await(manga.id)

        val recs = try {
            val id = tracks.find { it.trackerId == associatedTrackerId }?.remoteId
            val results = if (id != null) {
                getRecsById(id.toString())
            } else {
                getRecsBySearch(manga.title)
            }
            logcat { name + " > Results: " + results.size }

            results.ifEmpty { throw NoResultsException() }
        } catch (e: Exception) {
            if (e !is NoResultsException) {
                logcat(LogPriority.ERROR, e) { name }
            }
            throw e
        }

        return MangasPage(recs, false)
    }
}
