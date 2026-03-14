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
        val isLoading: Boolean = false,
        val isInitialLoad: Boolean = true,
        val isLoggedIn: Boolean = false,
        val missingMangaTitles: List<String> = emptyList(),
        // Separate fields for snackbar so clearing one doesn't trigger the other
        val importMessage: String? = null,
        val errorMessage: String? = null,
    )

    init {
        loadLists()
    }

    fun loadLists() {
        // Guard: don't start a second load if one is already running
        if (state.value.isLoading) return

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }

            val loggedIn = authService.isLoggedIn()

            // All three fetches run in parallel.
            // Trending & Recent are public (no auth required by Firestore rules).
            // My Lists requires login.
            val trendingDeferred = async { sharedListService.getTrendingLists() }
            val recentDeferred   = async { sharedListService.getRecentLists() }
            val myListsDeferred  = async {
                if (loggedIn) sharedListService.getMyLists() else emptyList()
            }

            mutableState.update {
                it.copy(
                    trendingLists = trendingDeferred.await(),
                    recentLists   = recentDeferred.await(),
                    myLists       = myListsDeferred.await(),
                    isLoading     = false,
                    isInitialLoad = false,
                    isLoggedIn    = loggedIn,
                )
            }
        }
    }

    fun importList(sharedList: SharedList) {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                // 1. Create a new category named after the list
                createCategoryWithName.await(sharedList.title)

                // 2. Find the newly created category by name
                val allCategories = getCategories.await()
                val newCategory = allCategories.find { it.name == sharedList.title }
                if (newCategory == null) {
                    mutableState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = "Could not create category '${sharedList.title}'.",
                        )
                    }
                    return@launch
                }

                // 3. Match manga in shared list against the user's library by title
                val libraryManga = getLibraryManga.await()
                val mangaItems   = sharedList.getMangaItems()
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
                        isLoading            = false,
                        importMessage        = message,
                        missingMangaTitles   = missingTitles,
                    )
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR) { "DiscoverScreenModel: importList failed: ${e.message}" }
                mutableState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = "Import failed: ${e.localizedMessage}",
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
            mutableState.update { it.copy(isLoading = true) }
            sharedListService.uploadList(title, selectedManga)
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            isLoading     = false,
                            importMessage = "List '$title' shared successfully!",
                        )
                    }
                    loadLists()
                }
                .onFailure { e ->
                    mutableState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = e.localizedMessage ?: "Upload failed",
                        )
                    }
                }
        }
    }

    fun deleteMyList(listId: String) {
        screenModelScope.launch {
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
