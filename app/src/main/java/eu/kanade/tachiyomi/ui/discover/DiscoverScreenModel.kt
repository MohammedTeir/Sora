package eu.kanade.tachiyomi.ui.discover

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import eu.kanade.tachiyomi.data.discover.SharedList
import eu.kanade.tachiyomi.data.discover.SharedListService
import eu.kanade.tachiyomi.data.discover.SharedMangaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.manga.interactor.GetLibraryManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DiscoverScreenModel(
    private val sharedListService: SharedListService = Injekt.get(),
    private val authService: FirebaseAuthService = Injekt.get(),
    private val getLibraryManga: GetLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val createCategoryWithName: CreateCategoryWithName = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
) : StateScreenModel<DiscoverScreenModel.State>(State()) {

    data class State(
        val trendingLists: List<SharedList> = emptyList(),
        val recentLists: List<SharedList> = emptyList(),
        val myLists: List<SharedList> = emptyList(),
        // isFetchingLists — true only while the Firestore read queries are in-flight.
        // Used to show/hide the LinearProgressIndicator and the initial full-screen spinner.
        val isFetchingLists: Boolean = false,
        // isUploadingList — true only while an upload or import operation is in-flight.
        // Kept separate so a completed upload can immediately trigger a list refresh
        // without the guard in loadLists() blocking it.
        val isUploadingList: Boolean = false,
        val isInitialLoad: Boolean = true,
        val isLoggedIn: Boolean = false,
        val pendingListIds: Set<String> = emptySet(),
        val missingMangaTitles: List<String> = emptyList(),
        val importMessage: String? = null,
        val errorMessage: String? = null,
    ) {
        // Convenience accessor used by the UI to show progress indicators.
        val isLoading: Boolean get() = isFetchingLists || isUploadingList
    }

    // Loading is driven by the lifecycle observer in DiscoverScreen.kt
    // (onResume → loadLists()).  Removing the init-based call prevents
    // two concurrent fetches on first composition.

    fun loadLists() {
        // Guard: only block if a *list-fetch* is already running.
        // Intentionally does NOT check isUploadingList — this allows uploadList()
        // and deleteMyList() to call loadLists() immediately after they finish
        // without the guard silently dropping the refresh.
        if (state.value.isFetchingLists) return

        screenModelScope.launch {
            // Check auth status BEFORE starting any Firestore calls so the FAB
            // (and "My Lists" section) are shown correctly even when the network
            // calls fail. Previously isLoggedIn was only updated inside the try
            // success block, so a Firestore error on cold-start would leave
            // isLoggedIn = false for the rest of the session.
            val loggedIn = authService.isLoggedIn()
            mutableState.update { it.copy(isFetchingLists = true, isLoggedIn = loggedIn) }

            try {
                // All three fetches run in parallel.
                // Trending & Recent are public (no auth required by Firestore rules).
                // My Lists requires login.
                val trendingDeferred = async { sharedListService.getTrendingLists() }
                val recentDeferred   = async { sharedListService.getRecentLists() }
                val myListsDeferred  = async {
                    if (loggedIn) sharedListService.getMyLists() else emptyList()
                }

                val serverTrending = trendingDeferred.await()
                val serverRecent   = recentDeferred.await()
                val serverMyLists  = myListsDeferred.await()

                mutableState.update { currentState ->
                    // IDs the server now knows about
                    val serverIds = (serverTrending + serverRecent + serverMyLists)
                        .map { it.id }.toSet()
                    // Pending IDs not yet confirmed by server
                    val stillPending = currentState.pendingListIds - serverIds
                    // Preserve optimistic entries whose writes haven't synced yet
                    val pendingMyLists = currentState.myLists.filter { it.id in stillPending }
                    val pendingRecentLists = currentState.recentLists.filter { it.id in stillPending }

                    currentState.copy(
                        trendingLists   = serverTrending,
                        recentLists     = pendingRecentLists + serverRecent,
                        myLists         = pendingMyLists + serverMyLists,
                        isFetchingLists = false,
                        isInitialLoad   = false,
                        pendingListIds  = stillPending,
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "DiscoverScreenModel: loadLists failed: ${e.message}" }
                // isLoggedIn is already set above, so the FAB stays visible on error.
                mutableState.update {
                    it.copy(
                        isFetchingLists = false,
                        isInitialLoad   = false,
                        errorMessage    = "Failed to load lists. Pull down to retry.",
                    )
                }
            }
        }
    }

    fun importList(sharedList: SharedList) {
        screenModelScope.launch {
            mutableState.update { it.copy(isUploadingList = true) }
            try {
                // 1. Create a new category named after the list
                createCategoryWithName.await(sharedList.title)

                // 2. Find the newly created category by name
                val allCategories = getCategories.await()
                val newCategory = allCategories.find { it.name == sharedList.title }
                if (newCategory == null) {
                    mutableState.update {
                        it.copy(
                            isUploadingList = false,
                            errorMessage    = "Could not create category '${sharedList.title}'.",
                        )
                    }
                    return@launch
                }

                // 3. Match manga in shared list against the user's library by title
                val libraryManga  = getLibraryManga.await()
                val mangaItems    = sharedList.getMangaItems()
                val missingTitles = mutableListOf<String>()
                var importedCount = 0

                for (item in mangaItems) {
                    val match = libraryManga.find {
                        it.manga.title.equals(item.title, ignoreCase = true)
                    }
                    if (match != null) {
                        val existing = match.categories
                        setMangaCategories.await(
                            match.manga.id,
                            (existing + newCategory.id).distinct(),
                        )
                        importedCount++
                    } else {
                        missingTitles.add(item.title)
                    }
                }

                // 4. Increment import count in Firestore
                if (sharedList.id.isNotEmpty()) {
                    sharedListService.incrementImportCount(sharedList.id)
                }

                val message = if (importedCount > 0) {
                    "Imported $importedCount of ${mangaItems.size} manga to '${sharedList.title}'"
                } else {
                    "No manga from this list were found in your library."
                }

                mutableState.update {
                    it.copy(
                        isUploadingList    = false,
                        importMessage      = message,
                        missingMangaTitles = missingTitles,
                    )
                }
                // Refresh lists so the updated import count is visible.
                loadLists()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "DiscoverScreenModel: importList failed: ${e.message}" }
                mutableState.update {
                    it.copy(
                        isUploadingList = false,
                        errorMessage    = "Import failed: ${e.localizedMessage}",
                    )
                }
            }
        }
    }

    fun uploadList(title: String, selectedManga: List<SharedMangaItem>) {
        if (title.isBlank()) {
            mutableState.update { it.copy(errorMessage = "List title cannot be empty.") }
            return
        }
        if (selectedManga.isEmpty()) {
            mutableState.update { it.copy(errorMessage = "Select at least one manga to share.") }
            return
        }
        screenModelScope.launch {
            mutableState.update { it.copy(isUploadingList = true) }

            // Refactored to if/else so we can reference the returned docId (String)
            // for the optimistic SharedList — `.onSuccess { }` is not a suspend lambda
            // so we cannot call suspend functions (getUserId, etc.) inside it.
            val result = sharedListService.uploadList(title, selectedManga)

            if (result.isSuccess) {
                val docId = result.getOrNull() ?: ""

                // Optimistic update: add the newly created list to both myLists and
                // recentLists immediately so the user sees it without waiting for
                // Firestore propagation. The background loadLists() call below will
                // eventually replace this entry with the real Firestore document.
                val optimisticList = SharedList(
                    id          = docId,
                    title       = title,
                    creatorName = "You",
                    manga       = selectedManga.map {
                        mapOf<String, Any>(
                            "title"     to it.title,
                            "sourceId"  to it.sourceId,
                            "coverUrl"  to it.coverUrl,
                            "sourceUrl" to it.sourceUrl,
                        )
                    },
                    timestamp   = System.currentTimeMillis(),
                )

                mutableState.update { state ->
                    state.copy(
                        isUploadingList = false,
                        importMessage   = "List '$title' shared successfully!",
                        // Prepend so the new list appears first
                        myLists         = listOf(optimisticList) + state.myLists,
                        recentLists     = listOf(optimisticList) + state.recentLists,
                        // Track as pending so loadLists() preserves this entry
                        // until the fire-and-forget write syncs to the server.
                        pendingListIds  = state.pendingListIds + docId,
                    )
                }
                // No immediate loadLists() here — the fire-and-forget Firestore write
                // hasn't synced yet, so a query would return stale data and overwrite
                // the optimistic entry. The lifecycle observer (onResume) will refresh
                // once the write has propagated.
            } else {
                val e = result.exceptionOrNull()
                mutableState.update {
                    it.copy(
                        isUploadingList = false,
                        errorMessage    = e?.localizedMessage ?: "Upload failed",
                    )
                }
            }
        }
    }

    fun deleteMyList(listId: String) {
        screenModelScope.launch {
            // Remove from pending tracking so loadLists() won't preserve it.
            mutableState.update { it.copy(pendingListIds = it.pendingListIds - listId) }
            sharedListService.deleteList(listId)
            loadLists()
        }
    }

    /** Call after the snackbar for importMessage has been shown. */
    fun clearImportMessage() {
        mutableState.update { it.copy(importMessage = null) }
    }

    /** Call after the snackbar for errorMessage has been shown. */
    fun clearErrorMessage() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    /** Legacy helper kept for compatibility. */
    fun clearMessages() {
        mutableState.update { it.copy(importMessage = null, errorMessage = null) }
    }

    fun clearMissingManga() {
        mutableState.update { it.copy(missingMangaTitles = emptyList()) }
    }
}
