package eu.kanade.tachiyomi.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import kotlinx.coroutines.tasks.await
import logcat.LogPriority
import logcat.logcat
import tachiyomi.data.DatabaseHandler
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

    /**
     * Called after login: downloads cloud data, merges with local, then uploads merged result.
     */
    suspend fun syncOnLogin(): SyncResult = runSync()

    /**
     * Called on app start if the user is logged in.
     */
    suspend fun syncOnStartup(): SyncResult {
        if (!syncPrefs.syncOnStartup().get()) return SyncResult.Success
        return runSync()
    }

    /**
     * Manual sync trigger from the Sync Settings screen.
     */
    suspend fun syncNow(): SyncResult = runSync()

    // ─── Core Sync Logic ───────────────────────────────────────────────────────

    private suspend fun runSync(): SyncResult {
        val userId = authService.getUserId()
            ?: return SyncResult.Error("Not logged in")

        if (syncPrefs.isSyncing().get()) {
            logcat(LogPriority.INFO) { "SyncService: sync already in progress, skipping" }
            return SyncResult.Success
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

            // 5. Upload merged data to cloud
            uploadToCloud(userId, merged)

            // 6. Update last sync timestamp
            val now = System.currentTimeMillis()
            authPrefs.lastSyncTime().set(now)
            syncPrefs.lastSyncTimestamp().set(now)

            logcat(LogPriority.INFO) { "SyncService: sync completed successfully" }
            SyncResult.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "SyncService: sync failed" }
            SyncResult.Error(e.message ?: "Unknown sync error", e)
        } finally {
            syncPrefs.isSyncing().set(false)
        }
    }

    // ─── Read Local Data ────────────────────────────────────────────────────────

    private suspend fun readLocalData(): SyncData {
        return handler.await {
            val manga = mangasQueries.getFavorites(MangaMapper::mapManga).executeAsList()
            val chapters = chaptersQueries.getAllChapters(::mapChapter).executeAsList()
            val categories = categoriesQueries.getCategories(::mapCategory).executeAsList()
                .filter { !it.isSystemCategory }
            val tracks = manga_syncQueries.getTracks(TrackMapper::mapTrack).executeAsList()
            val history = historyQueries.getAllHistory(HistoryMapper::mapHistory).executeAsList()
            SyncData(
                manga = manga,
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

        val mangaDocs = userRef.collection("library").get().await()
        val chapterDocs = userRef.collection("chapters").get().await()
        val categoryDocs = userRef.collection("categories").get().await()
        val trackDocs = userRef.collection("tracking").get().await()
        val historyDocs = userRef.collection("history").get().await()

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

    /**
     * Merges local and cloud data. For each entity:
     * - If only in local → keep local
     * - If only in cloud → keep cloud
     * - If in both → keep the one with the newer `lastModifiedAt` / `updatedAt`
     */
    private fun mergeData(local: SyncData, cloud: SyncData): SyncData {
        val mergedManga = mergeLists(
            local.manga,
            cloud.manga,
            keyFn = { it.id },
            newerFn = { a, b -> if (a.lastModifiedAt >= b.lastModifiedAt) a else b },
        )
        val mergedChapters = mergeLists(
            local.chapters,
            cloud.chapters,
            keyFn = { it.id },
            newerFn = { a, b -> if (a.lastModifiedAt >= b.lastModifiedAt) a else b },
        )
        val mergedCategories = mergeLists(
            local.categories,
            cloud.categories,
            keyFn = { it.id },
            newerFn = { a, _ -> a }, // categories don't have timestamps, keep local
        )
        val mergedTracks = mergeLists(
            local.tracks,
            cloud.tracks,
            keyFn = { it.id },
            newerFn = { a, _ -> a }, // keep local (external tracker state)
        )
        val mergedHistory = mergeLists(
            local.history,
            cloud.history,
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
        val allKeys = localMap.keys + cloudMap.keys
        return allKeys.distinct().mapNotNull { key ->
            val localItem = localMap[key]
            val cloudItem = cloudMap[key]
            when {
                localItem != null && cloudItem != null -> newerFn(localItem, cloudItem)
                localItem != null -> localItem
                cloudItem != null -> cloudItem
                else -> null
            }
        }
    }

    // ─── Write Local Data ───────────────────────────────────────────────────────

    private suspend fun writeLocalData(data: SyncData) {
        handler.await(inTransaction = true) {
            // Update manga fields (read status, flags, notes)
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

            // Upsert history
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
        val batch = firestore.batch()

        // Library (only favorites)
        data.manga.forEach { manga ->
            val docRef = userRef.collection("library").document(manga.id.toString())
            batch.set(docRef, SyncDataSerializer.mangaToMap(manga).filterValues { it != null }, SetOptions.merge())
        }

        // Chapters (only read/bookmarked or with progress)
        data.chapters
            .filter { it.read || it.bookmark || it.lastPageRead > 0 }
            .forEach { chapter ->
                val docRef = userRef.collection("chapters").document(chapter.id.toString())
                batch.set(docRef, SyncDataSerializer.chapterToMap(chapter).filterValues { it != null }, SetOptions.merge())
            }

        // Categories
        data.categories.forEach { category ->
            val docRef = userRef.collection("categories").document(category.id.toString())
            batch.set(docRef, SyncDataSerializer.categoryToMap(category).filterValues { it != null }, SetOptions.merge())
        }

        // Tracking
        data.tracks.forEach { track ->
            val docRef = userRef.collection("tracking").document(track.id.toString())
            batch.set(docRef, SyncDataSerializer.trackToMap(track).filterValues { it != null }, SetOptions.merge())
        }

        // History (entries with actual read dates)
        data.history
            .filter { it.readAt != null }
            .forEach { history ->
                val docRef = userRef.collection("history").document(history.chapterId.toString())
                batch.set(docRef, SyncDataSerializer.historyToMap(history).filterValues { it != null }, SetOptions.merge())
            }

        batch.commit().await()

        // Upload encrypted sensitive settings (tracker tokens) separately
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
        // Collect all private preferences (tracker tokens, passwords) and encrypt them
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
            logcat(LogPriority.WARN, e) { "SyncService: failed to upload sensitive settings" }
        }
    }

    // ─── Helper mappers for DatabaseHandler inline usage ───────────────────────

    private fun mapChapter(
        id: Long,
        mangaId: Long,
        url: String,
        name: String,
        scanlator: String?,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        pagesCount: Long,
        chapterNumber: Double,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        lastModifiedAt: Long,
        version: Long,
        @Suppress("UNUSED_PARAMETER") isSyncing: Long,
    ): Chapter = Chapter(
        id = id,
        mangaId = mangaId,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        pagesCount = pagesCount,
        dateFetch = dateFetch,
        sourceOrder = sourceOrder,
        url = url,
        name = name,
        dateUpload = dateUpload,
        chapterNumber = chapterNumber,
        scanlator = scanlator,
        lastModifiedAt = lastModifiedAt,
        version = version,
    )

    private fun mapCategory(
        id: Long,
        name: String,
        order: Long,
        flags: Long,
        hidden: Boolean,
    ): Category = Category(
        id = id,
        name = name,
        order = order,
        flags = flags,
        hidden = hidden,
    )
}

// ─── Data Container ────────────────────────────────────────────────────────────

data class SyncData(
    val manga: List<Manga>,
    val chapters: List<Chapter>,
    val categories: List<Category>,
    val tracks: List<Track>,
    val history: List<History>,
)
