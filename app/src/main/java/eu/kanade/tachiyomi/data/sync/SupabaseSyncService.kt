package eu.kanade.tachiyomi.data.sync

import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.auth.SupabaseAuthService
import eu.kanade.tachiyomi.data.supabase.CloudCategory
import eu.kanade.tachiyomi.data.supabase.CloudChapter
import eu.kanade.tachiyomi.data.supabase.CloudExtensionRepo
import eu.kanade.tachiyomi.data.supabase.CloudHistory
import eu.kanade.tachiyomi.data.supabase.CloudManga
import eu.kanade.tachiyomi.data.supabase.CloudSensitiveSettings
import eu.kanade.tachiyomi.data.supabase.CloudTrack
import eu.kanade.tachiyomi.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import logcat.LogPriority
import logcat.logcat
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
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
import mihon.domain.extensionrepo.model.ExtensionRepo
import java.util.Date

/**
 * Supabase-backed sync service — replaces [SyncService] (Firestore version).
 *
 * Uses PostgREST bulk upsert instead of Firestore batched writes.
 * No 500-op batch limit; supabase-kt handles auth token injection.
 * Merge logic (last-write-wins) is identical to the original.
 */
class SupabaseSyncService(
    private val handler: DatabaseHandler = Injekt.get(),
    private val authService: SupabaseAuthService = Injekt.get(),
    private val authPrefs: AuthPreferences = Injekt.get(),
    private val syncPrefs: SyncPreferences = Injekt.get(),
    private val preferenceStore: PreferenceStore = Injekt.get(),
) {

    private val client = SupabaseProvider.client

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
                logcat(LogPriority.INFO) { "SupabaseSyncService: sync already in progress, skipping" }
                return SyncResult.Success
            } else {
                logcat(LogPriority.WARN) { "SupabaseSyncService: stale isSyncing flag detected, resetting" }
                syncPrefs.isSyncing().set(false)
            }
        }

        val tokenRefreshed = authService.refreshToken()
        if (!tokenRefreshed) {
            logcat(LogPriority.WARN) { "SupabaseSyncService: token refresh failed, proceeding with existing token" }
        }

        return try {
            syncPrefs.isSyncing().set(true)
            logcat(LogPriority.INFO) { "SupabaseSyncService: starting sync for user $userId" }

            // 1. Read all local data
            val localData = readLocalData()

            // 2. Read cloud data
            val cloudData = readCloudData(userId)

            // 3. Merge: keep newest for each entity
            val merged = mergeData(localData, cloudData)

            // 4. Write merged data back to local DB
            writeLocalData(merged)

            // 5. Upload merged data to cloud (PostgREST bulk upsert)
            uploadToCloud(userId, merged)

            // 6. Update last sync timestamp
            val now = System.currentTimeMillis()
            authPrefs.lastSyncTime().set(now)
            syncPrefs.lastSyncTimestamp().set(now)

            logcat(LogPriority.INFO) { "SupabaseSyncService: sync completed successfully" }
            SyncResult.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseSyncService: sync failed: ${e.message}" }
            val friendlyMessage = when {
                e.message?.contains("JWT expired") == true ->
                    "Sync failed: Session expired. Please sign out and sign back in."
                e.message?.contains("403") == true ||
                e.message?.contains("new row violates") == true ->
                    "Sync failed: Access denied. Please sign out and sign back in, then try again."
                e.message?.contains("Unable to resolve host") == true ->
                    "Sync failed: No internet connection."
                else -> e.message ?: "Unknown sync error"
            }
            SyncResult.Error(friendlyMessage, e)
        } finally {
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

            val extensionRepos = try {
                extension_reposQueries.findAll { baseUrl, name, shortName, website, signingKeyFingerprint ->
                    ExtensionRepo(baseUrl, name, shortName, website, signingKeyFingerprint)
                }.executeAsList()
            } catch (e: Exception) {
                emptyList()
            }

            SyncData(
                manga = allManga,
                chapters = chapters,
                categories = categories,
                tracks = tracks,
                history = history,
                extensionRepos = extensionRepos,
            )
        }
    }

    // ─── Read Cloud Data ────────────────────────────────────────────────────────

    private suspend fun readCloudData(userId: String): SyncData {
        val manga = client.from("user_library")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudManga>()
            .map { it.toDomain() }

        val chapters = client.from("user_chapters")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudChapter>()
            .map { it.toDomain() }

        val categories = client.from("user_categories")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudCategory>()
            .map { it.toDomain() }

        val tracks = client.from("user_tracks")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudTrack>()
            .map { it.toDomain() }

        val history = client.from("user_history")
            .select { filter { eq("user_id", userId) } }
            .decodeList<CloudHistory>()
            .map { it.toDomain() }

        val extensionRepos = try {
            client.from("user_extension_repos")
                .select { filter { eq("user_id", userId) } }
                .decodeList<CloudExtensionRepo>()
                .map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }

        return SyncData(
            manga = manga,
            chapters = chapters,
            categories = categories,
            tracks = tracks,
            history = history,
            extensionRepos = extensionRepos,
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
        val mergedExtensionRepos = mergeLists(
            local.extensionRepos, cloud.extensionRepos,
            keyFn = { it.baseUrl },
            newerFn = { a, _ -> a },
        )

        return SyncData(
            manga = mergedManga,
            chapters = mergedChapters,
            categories = mergedCategories,
            tracks = mergedTracks,
            history = mergedHistory,
            extensionRepos = mergedExtensionRepos,
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

            data.extensionRepos.forEach { repo ->
                runCatching {
                    extension_reposQueries.insert(
                        base_url = repo.baseUrl,
                        name = repo.name,
                        short_name = repo.shortName,
                        website = repo.website,
                        fingerprint = repo.signingKeyFingerprint,
                    )
                }
            }
        }
    }

    // ─── Upload to Cloud ────────────────────────────────────────────────────────

    private suspend fun uploadToCloud(userId: String, data: SyncData) {
        // PostgREST bulk upsert — no Firestore 500-op batch limit
        if (syncPrefs.syncLibrary().get() && data.manga.isNotEmpty()) {
            client.from("user_library").upsert(
                data.manga.map { CloudManga.from(it, userId) },
            )
        }

        if (syncPrefs.syncChapters().get()) {
            val filteredChapters = data.chapters
                .filter { it.read || it.bookmark || it.lastPageRead > 0 }
            if (filteredChapters.isNotEmpty()) {
                client.from("user_chapters").upsert(
                    filteredChapters.map { CloudChapter.from(it, userId) },
                )
            }
        }

        if (syncPrefs.syncCategories().get() && data.categories.isNotEmpty()) {
            client.from("user_categories").upsert(
                data.categories.map { CloudCategory.from(it, userId) },
            )
        }

        if (syncPrefs.syncTracking().get() && data.tracks.isNotEmpty()) {
            client.from("user_tracks").upsert(
                data.tracks.map { CloudTrack.from(it, userId) },
            )
        }

        if (syncPrefs.syncHistory().get()) {
            val filteredHistory = data.history.filter { it.readAt != null }
            if (filteredHistory.isNotEmpty()) {
                client.from("user_history").upsert(
                    filteredHistory.map { CloudHistory.from(it, userId) },
                )
            }
        }

        if (data.extensionRepos.isNotEmpty()) {
            client.from("user_extension_repos").upsert(
                data.extensionRepos.map { CloudExtensionRepo.from(it, userId) },
            )
        }

        // Upload sensitive settings
        uploadSensitiveSettings(userId)
    }

    private suspend fun uploadSensitiveSettings(userId: String) {
        try {
            val allPrefs = preferenceStore.getAll()
            val privateEntries = allPrefs
                .filterKeys { Preference.isPrivate(it) }
                .mapValues { it.value?.toString() ?: "" }

            if (privateEntries.isNotEmpty()) {
                val json = privateEntries.entries.joinToString(",", "{", "}") { (k, v) ->
                    "\"${k.replace("\"", "\\\"")}\"" + ": " + "\"${v.replace("\"", "\\\"")}\""
                }
                val encrypted = SyncDataSerializer.encryptSensitive(json)
                client.from("user_sensitive_settings").upsert(
                    CloudSensitiveSettings(
                        userId = userId,
                        iv = encrypted.iv,
                        ciphertext = encrypted.ciphertext,
                    ),
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SupabaseSyncService: failed to upload sensitive settings: ${e.message}" }
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
