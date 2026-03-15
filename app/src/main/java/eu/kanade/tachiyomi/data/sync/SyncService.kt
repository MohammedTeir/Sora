
package eu.kanade.tachiyomi.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import kotlinx.coroutines.tasks.await
import logcat.LogPriority
import logcat.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.data.history.HistoryMapper
import tachiyomi.data.manga.MangaMapper
import tachiyomi.data.track.TrackMapper
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

sealed class SyncResult {
    data object Success : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
}

class SyncService(
    private val handler: DatabaseHandler = Injekt.get(),
    private val authService: FirebaseAuthService = Injekt.get(),
    private val authPrefs: AuthPreferences = Injekt.get(),
    private val syncPrefs: SyncPreferences = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
) {

    private val firestore = FirebaseFirestore.getInstance()

    // Firestore hard limit per batch
    private val BATCH_SIZE = 400

    suspend fun syncOnLogin(): SyncResult = runSync()

    suspend fun syncOnStartup(): SyncResult {
        if (!syncPrefs.syncOnStartup().get()) return SyncResult.Success
        return runSync()
    }

    suspend fun syncNow(): SyncResult = runSync()

    // ─── Core Sync Logic ───────────────────────────────────────────────────────

    private suspend fun runSync(): SyncResult {
        val userId = authService.getUserId()
            ?: return SyncResult.Error("Not logged in")

        // Guard: if isSyncing is stuck true for > 10 minutes, reset it
        if (syncPrefs.isSyncing().get()) {
            val lastSync = authPrefs.lastSyncTime().get()
            val elapsed = System.currentTimeMillis() - lastSync
            if (elapsed < 10 * 60 * 1000) {
                logcat(LogPriority.INFO) { "SyncService: sync already in progress, skipping" }
                return SyncResult.Success
            } else {
                // Stale lock — reset it
                logcat(LogPriority.WARN) { "SyncService: stale isSyncing flag detected, resetting" }
                syncPrefs.isSyncing().set(false)
            }
        }

        val tokenRefreshed = authService.refreshToken()
        if (!tokenRefreshed) {
            logcat(LogPriority.WARN) { "SyncService: token refresh failed, proceeding with existing token" }
        }

        return try {
            syncPrefs.isSyncing().set(true)
            logcat(LogPriority.INFO) { "SyncService: starting sync for user $userId" }

            // 1. Read all local data
            val localData = readLocalData()

            // 2. Read cloud data
            val cloudData = readCloudData(userId)

            // 3. Merge: keep newest for each entity
            val merged = mergeData(localData, cloudData)

            // 4. Write merged data back to local DB
            writeLocalData(merged)

            // 5. Upload merged data to cloud (in safe batches of BATCH_SIZE)
            uploadToCloud(userId, merged)

            // 6. Update last sync timestamp
            val now = System.currentTimeMillis()
            authPrefs.lastSyncTime().set(now)
            syncPrefs.lastSyncTimestamp().set(now)

            logcat(LogPriority.INFO) { "SyncService: sync completed successfully" }
            SyncResult.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SyncService: sync failed: ${e.message}" }
            val friendlyMessage = when {
                e.message?.contains("PERMISSION_DENIED") == true ->
                    "Sync failed: Access denied. Please sign out and sign back in, then try again."
                e.message?.contains("UNAVAILABLE") == true ||
                e.message?.contains("Unable to resolve host") == true ->
                    "Sync failed: No internet connection."
                else -> e.message ?: "Unknown sync error"
            }
            SyncResult.Error(friendlyMessage, e)
        } finally {
            // Always release the lock — even on crash
            syncPrefs.isSyncing().set(false)
        }
    }

    // ─── Read Local Data ────────────────────────────────────────────────────────

    private suspend fun readLocalData(): SyncData {
        val selectedIds = syncPrefs.syncSelectedMangaIds().get()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

        return handler.await {
            val allManga = if (syncPrefs.syncLibrary().get()) {
                mangasQueries.getFavorites(MangaMapper::mapManga).executeAsList()
                    .let { list ->
                        if (selectedIds.isEmpty()) list else list.filter { it.id in selectedIds }
                    }
            } else {
                emptyList()
            }

            val syncedMangaIds = allManga.map { it.id }.toSet()

            val chapters = if (syncPrefs.syncChapters().get()) {
                chaptersQueries.getAllChapters(::mapChapter).executeAsList()
                    .filter { syncedMangaIds.isEmpty() || it.mangaId in syncedMangaIds }
            } else {
                emptyList()
            }

            val categories = if (syncPrefs.syncCategories().get()) {
                categoriesQueries.getCategories(::mapCategory).executeAsList()
                    .filter { !it.isSystemCategory }
            } else {
                emptyList()
            }

            val tracks = if (syncPrefs.syncTracking().get()) {
                manga_syncQueries.getTracks(TrackMapper::mapTrack).executeAsList()
                    .filter { syncedMangaIds.isEmpty() || it.mangaId in syncedMangaIds }
            } else {
                emptyList()
            }

            val history = if (syncPrefs.syncHistory().get()) {
                historyQueries.getAllHistory(HistoryMapper::mapHistory).executeAsList()
            } else {
                emptyList()
            }

            SyncData(
                manga = allManga,
                chapters = chapters,
                categories = categories,
                tracks = tracks,
                history = history,
            )
        }
    }

    // ─── Read Cloud Data ────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private suspend fun readCloudData(userId: String): SyncData {
        val userRef = firestore.collection("users").document(userId)

        val mangaDocs    = userRef.collection("library").get().await()
        val chapterDocs  = userRef.collection("chapters").get().await()
        val categoryDocs = userRef.collection("categories").get().await()
        val trackDocs    = userRef.collection("tracking").get().await()
        val historyDocs  = userRef.collection("history").get().await()

        val manga = mangaDocs.documents.mapNotNull {
            SyncDataSerializer.mapToManga(it.data as? Map<String, Any?> ?: return@mapNotNull null)
        }
        val chapters = chapterDocs.documents.mapNotNull {
            SyncDataSerializer.mapToChapter(it.data as? Map<String, Any?> ?: return@mapNotNull null)
        }
        val categories = categoryDocs.documents.mapNotNull {
            SyncDataSerializer.mapToCategory(it.data as? Map<String, Any?> ?: return@mapNotNull null)
        }
        val tracks = trackDocs.documents.mapNotNull {
            SyncDataSerializer.mapToTrack(it.data as? Map<String, Any?> ?: return@mapNotNull null)
        }
        val history = historyDocs.documents.mapNotNull {
            SyncDataSerializer.mapToHistory(it.data as? Map<String, Any?> ?: return@mapNotNull null)
        }

        return SyncData(
            manga = manga,
            chapters = chapters,
            categories = categories,
            tracks = tracks,
            history = history,
        )
    }

    // ─── Merge Logic ────────────────────────────────────────────────────────────

    private fun mergeData(local: SyncData, cloud: SyncData): SyncData {
        val mergedManga = mergeLists(
            local.manga, cloud.manga,
            keyFn = { it.id },
            newerFn = { a, b -> if (a.lastModifiedAt >= b.lastModifiedAt) a else b },
        )
        val mergedChapters = mergeLists(
            local.chapters, cloud.chapters,
            keyFn = { it.id },
            newerFn = { a, b -> if (a.lastModifiedAt >= b.lastModifiedAt) a else b },
        )
        val mergedCategories = mergeLists(
            local.categories, cloud.categories,
            keyFn = { it.id },
            newerFn = { a, _ -> a },
        )
        val mergedTracks = mergeLists(
            local.tracks, cloud.tracks,
            keyFn = { it.id },
            newerFn = { a, _ -> a },
        )
        val mergedHistory = mergeLists(
            local.history, cloud.history,
            keyFn = { it.chapterId },
            newerFn = { a, b ->
                val aTime = a.readAt?.time ?: 0L
                val bTime = b.readAt?.time ?: 0L
                if (aTime >= bTime) a else b
            },
        )

        return SyncData(
            manga = mergedManga,
            chapters = mergedChapters,
            categories = mergedCategories,
            tracks = mergedTracks,
            history = mergedHistory,
        )
    }

    private fun <T, K> mergeLists(
        local: List<T>,
        cloud: List<T>,
        keyFn: (T) -> K,
        newerFn: (T, T) -> T,
    ): List<T> {
        val localMap = local.associateBy(keyFn)
        val cloudMap = cloud.associateBy(keyFn)
        return (localMap.keys + cloudMap.keys).distinct().mapNotNull { key ->
            val l = localMap[key]
            val c = cloudMap[key]
            when {
                l != null && c != null -> newerFn(l, c)
                l != null -> l
                c != null -> c
                else -> null
            }
        }
    }

    // ─── Write Local Data ───────────────────────────────────────────────────────

    private suspend fun writeLocalData(data: SyncData) {
        handler.await(inTransaction = true) {
            data.manga.forEach { manga ->
                runCatching {
                    val existing = mangasQueries.getMangaById(manga.id).executeAsOneOrNull()
                    if (existing != null) {
                        mangasQueries.update(
                            source = manga.source,
                            url = manga.url,
                            artist = manga.artist,
                            author = manga.author,
                            description = manga.description,
                            genre = manga.genre?.let(StringListColumnAdapter::encode),
                            title = manga.title,
                            status = manga.status,
                            thumbnailUrl = manga.thumbnailUrl,
                            favorite = manga.favorite,
                            lastUpdate = manga.lastUpdate,
                            nextUpdate = null,
                            initialized = null,
                            viewer = manga.viewerFlags,
                            chapterFlags = manga.chapterFlags,
                            coverLastModified = null,
                            dateAdded = manga.dateAdded,
                            updateStrategy = manga.updateStrategy.let(UpdateStrategyColumnAdapter::encode),
                            calculateInterval = null,
                            version = manga.version,
                            isSyncing = 1L,
                            notes = manga.notes,
                            mangaId = manga.id,
                        )
                    } else {
                        mangasQueries.insert(
                            source = manga.source,
                            url = manga.url,
                            artist = manga.artist,
                            author = manga.author,
                            description = manga.description,
                            genre = manga.genre,
                            title = manga.title,
                            status = manga.status,
                            thumbnailUrl = manga.thumbnailUrl,
                            favorite = manga.favorite,
                            lastUpdate = manga.lastUpdate,
                            nextUpdate = 0L,
                            initialized = false,
                            viewerFlags = manga.viewerFlags,
                            chapterFlags = manga.chapterFlags,
                            coverLastModified = 0L,
                            dateAdded = manga.dateAdded,
                            updateStrategy = manga.updateStrategy,
                            calculateInterval = 0,
                            version = manga.version,
                            notes = manga.notes,
                        )
                    }
                }
            }

            data.categories.forEach { category ->
                runCatching {
                    categoriesQueries.upsertById(
                        id = category.id,
                        name = category.name,
                        order = category.order,
                        flags = category.flags,
                        hidden = category.hidden,
                    )
                }
            }

            data.tracks.forEach { track ->
                runCatching {
                    manga_syncQueries.insert(
                        mangaId = track.mangaId,
                        syncId = track.trackerId,
                        remoteId = track.remoteId,
                        libraryId = null,
                        title = track.title,
                        lastChapterRead = track.lastChapterRead,
                        totalChapters = track.totalChapters,
                        status = track.status,
                        score = track.score,
                        remoteUrl = track.remoteUrl,
                        startDate = track.startDate,
                        finishDate = track.finishDate,
                        private = track.private,
                    )
                }
            }

            data.chapters.forEach { chapter ->
                runCatching {
                    chaptersQueries.update(
                        mangaId = chapter.mangaId,
                        url = chapter.url,
                        name = chapter.name,
                        scanlator = chapter.scanlator,
                        read = chapter.read,
                        bookmark = chapter.bookmark,
                        lastPageRead = chapter.lastPageRead,
                        pagesCount = chapter.pagesCount,
                        chapterNumber = chapter.chapterNumber,
                        sourceOrder = chapter.sourceOrder,
                        dateFetch = chapter.dateFetch,
                        dateUpload = chapter.dateUpload,
                        version = chapter.version,
                        isSyncing = 1L,
                        chapterId = chapter.id,
                    )
                }
            }

            data.history.forEach { history ->
                runCatching {
                    historyQueries.upsert(
                        chapterId = history.chapterId,
                        readAt = history.readAt ?: Date(0),
                        time_read = history.readDuration.coerceAtLeast(0),
                    )
                }
            }
        }
    }

    // ─── Upload to Cloud ────────────────────────────────────────────────────────

    private suspend fun uploadToCloud(userId: String, data: SyncData) {
        val userRef = firestore.collection("users").document(userId)

        // Collect all write operations as (docRef, data) pairs
        val operations = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>()

        if (syncPrefs.syncLibrary().get()) {
            data.manga.forEach { manga ->
                operations += userRef.collection("library").document(manga.id.toString()) to
                    SyncDataSerializer.mangaToMap(manga).filterValues { it != null }
            }
        }

        if (syncPrefs.syncChapters().get()) {
            data.chapters
                .filter { it.read || it.bookmark || it.lastPageRead > 0 }
                .forEach { chapter ->
                    operations += userRef.collection("chapters").document(chapter.id.toString()) to
                        SyncDataSerializer.chapterToMap(chapter).filterValues { it != null }
                }
        }

        if (syncPrefs.syncCategories().get()) {
            data.categories.forEach { category ->
                operations += userRef.collection("categories").document(category.id.toString()) to
                    SyncDataSerializer.categoryToMap(category).filterValues { it != null }
            }
        }

        if (syncPrefs.syncTracking().get()) {
            data.tracks.forEach { track ->
                operations += userRef.collection("tracking").document(track.id.toString()) to
                    SyncDataSerializer.trackToMap(track).filterValues { it != null }
            }
        }

        if (syncPrefs.syncHistory().get()) {
            data.history
                .filter { it.readAt != null }
                .forEach { history ->
                    operations += userRef.collection("history").document(history.chapterId.toString()) to
                        SyncDataSerializer.historyToMap(history).filterValues { it != null }
                }
        }

        // Split into chunks of BATCH_SIZE to stay under Firestore's 500-op limit
        operations.chunked(BATCH_SIZE).forEach { chunk ->
            val batch: WriteBatch = firestore.batch()
            chunk.forEach { (docRef, docData) ->
                batch.set(docRef, docData, SetOptions.merge())
            }
            batch.commit().await()
            logcat(LogPriority.INFO) { "SyncService: committed batch of ${chunk.size} ops" }
        }

        // Upload sensitive settings separately
        uploadSensitiveSettings(userRef)

        // Update profile's lastSyncAt
        userRef.set(
            mapOf("lastSyncAt" to System.currentTimeMillis()),
            SetOptions.merge(),
        ).await()
    }

    private suspend fun uploadSensitiveSettings(
        userRef: com.google.firebase.firestore.DocumentReference,
    ) {
        try {
            val allPrefs = preferenceStore.getAll()
            val privateEntries = allPrefs
                .filterKeys { Preference.isPrivate(it) }
                .mapValues { it.value?.toString() ?: "" }

            if (privateEntries.isNotEmpty()) {
                val json = privateEntries.entries.joinToString(",", "{", "}") { (k, v) ->
                    "\"${k.replace("\"", "\\\"")}\": \"${v.replace("\"", "\\\"")}\""
                }
                val encrypted = SyncDataSerializer.encryptSensitive(json)
                userRef.collection("sensitiveSettings").document("encrypted")
                    .set(
                        mapOf(
                            "iv" to encrypted.iv,
                            "ciphertext" to encrypted.ciphertext,
                            "updatedAt" to System.currentTimeMillis(),
                        ),
                    )
                    .await()
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SyncService: failed to upload sensitive settings: ${e.message}" }
        }
    }

    // ─── Helper mappers ─────────────────────────────────────────────────────────

    private fun mapChapter(
        id: Long, mangaId: Long, url: String, name: String, scanlator: String?,
        read: Boolean, bookmark: Boolean, lastPageRead: Long, pagesCount: Long,
        chapterNumber: Double, sourceOrder: Long, dateFetch: Long, dateUpload: Long,
        lastModifiedAt: Long, version: Long,
        @Suppress("UNUSED_PARAMETER") isSyncing: Long,
    ): Chapter = Chapter(
        id = id, mangaId = mangaId, read = read, bookmark = bookmark,
        lastPageRead = lastPageRead, pagesCount = pagesCount, dateFetch = dateFetch,
        sourceOrder = sourceOrder, url = url, name = name, dateUpload = dateUpload,
        chapterNumber = chapterNumber, scanlator = scanlator,
        lastModifiedAt = lastModifiedAt, version = version,
    )

    private fun mapCategory(
        id: Long, name: String, order: Long, flags: Long, hidden: Boolean,
    ): Category = Category(id = id, name = name, order = order, flags = flags, hidden = hidden)
}

// ─── Data Container ────────────────────────────────────────────────────────────

data class SyncData(
    val manga: List<Manga>,
    val chapters: List<Chapter>,
    val categories: List<Category>,
    val tracks: List<Track>,
    val history: List<History>,
)
