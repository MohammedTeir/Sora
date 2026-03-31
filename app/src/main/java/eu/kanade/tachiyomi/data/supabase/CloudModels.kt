package eu.kanade.tachiyomi.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track
import mihon.domain.extensionrepo.model.ExtensionRepo
import java.util.Date

/**
 * PostgREST-compatible DTOs for Supabase cloud sync tables.
 * Each DTO maps 1-to-1 with a PostgreSQL table row.
 */

// ─── Library ────────────────────────────────────────────────────────────────────

@Serializable
data class CloudManga(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val source: Long = 0,
    val url: String,
    val title: String,
    val artist: String? = null,
    val author: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val favorite: Boolean = false,
    @SerialName("last_update") val lastUpdate: Long = 0,
    @SerialName("date_added") val dateAdded: Long = 0,
    @SerialName("viewer_flags") val viewerFlags: Long = 0,
    @SerialName("chapter_flags") val chapterFlags: Long = 0,
    @SerialName("update_strategy") val updateStrategy: String = "ALWAYS_UPDATE",
    @SerialName("last_modified_at") val lastModifiedAt: Long = 0,
    val notes: String = "",
) {
    fun toDomain(): Manga = Manga(
        id = id, source = source, url = url, title = title,
        artist = artist, author = author, description = description,
        genre = genre, status = status, thumbnailUrl = thumbnailUrl,
        favorite = favorite, lastUpdate = lastUpdate, nextUpdate = 0L,
        fetchInterval = 0, dateAdded = dateAdded, viewerFlags = viewerFlags,
        chapterFlags = chapterFlags, coverLastModified = 0L,
        updateStrategy = runCatching {
            eu.kanade.tachiyomi.source.model.UpdateStrategy.valueOf(updateStrategy)
        }.getOrDefault(eu.kanade.tachiyomi.source.model.UpdateStrategy.ALWAYS_UPDATE),
        initialized = false, lastModifiedAt = lastModifiedAt,
        favoriteModifiedAt = null, version = 1L, notes = notes,
    )

    companion object {
        fun from(manga: Manga, userId: String) = CloudManga(
            id = manga.id, userId = userId, source = manga.source,
            url = manga.url, title = manga.title, artist = manga.artist,
            author = manga.author, description = manga.description,
            genre = manga.genre, status = manga.status,
            thumbnailUrl = manga.thumbnailUrl, favorite = manga.favorite,
            lastUpdate = manga.lastUpdate, dateAdded = manga.dateAdded,
            viewerFlags = manga.viewerFlags, chapterFlags = manga.chapterFlags,
            updateStrategy = manga.updateStrategy.name,
            lastModifiedAt = manga.lastModifiedAt, notes = manga.notes,
        )
    }
}

// ─── Chapter ────────────────────────────────────────────────────────────────────

@Serializable
data class CloudChapter(
    val id: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("manga_id") val mangaId: Long,
    val url: String,
    val name: String = "",
    val read: Boolean = false,
    val bookmark: Boolean = false,
    @SerialName("last_page_read") val lastPageRead: Long = 0,
    @SerialName("chapter_number") val chapterNumber: Double = -1.0,
    val scanlator: String? = null,
    @SerialName("date_upload") val dateUpload: Long = 0,
    @SerialName("last_modified_at") val lastModifiedAt: Long = 0,
) {
    fun toDomain(): Chapter = Chapter(
        id = id, mangaId = mangaId, url = url, name = name,
        read = read, bookmark = bookmark, lastPageRead = lastPageRead,
        pagesCount = 0L, dateFetch = 0L, sourceOrder = 0L,
        dateUpload = dateUpload, chapterNumber = chapterNumber,
        scanlator = scanlator, lastModifiedAt = lastModifiedAt, version = 1L,
    )

    companion object {
        fun from(chapter: Chapter, userId: String) = CloudChapter(
            id = chapter.id, userId = userId, mangaId = chapter.mangaId,
            url = chapter.url, name = chapter.name, read = chapter.read,
            bookmark = chapter.bookmark, lastPageRead = chapter.lastPageRead,
            chapterNumber = chapter.chapterNumber, scanlator = chapter.scanlator,
            dateUpload = chapter.dateUpload, lastModifiedAt = chapter.lastModifiedAt,
        )
    }
}

// ─── Category ───────────────────────────────────────────────────────────────────

@Serializable
data class CloudCategory(
    val id: Long,
    @SerialName("user_id") val userId: String,
    val name: String,
    val order: Long = 0,
    val flags: Long = 0,
    val hidden: Boolean = false,
) {
    fun toDomain(): Category = Category(
        id = id, name = name, order = order, flags = flags, hidden = hidden,
    )

    companion object {
        fun from(category: Category, userId: String) = CloudCategory(
            id = category.id, userId = userId, name = category.name,
            order = category.order, flags = category.flags, hidden = category.hidden,
        )
    }
}

// ─── Track ──────────────────────────────────────────────────────────────────────

@Serializable
data class CloudTrack(
    val id: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("manga_id") val mangaId: Long,
    @SerialName("tracker_id") val trackerId: Long,
    @SerialName("remote_id") val remoteId: Long = 0,
    val title: String = "",
    @SerialName("last_chapter_read") val lastChapterRead: Double = 0.0,
    @SerialName("total_chapters") val totalChapters: Long = 0,
    val status: Long = 0,
    val score: Double = 0.0,
    @SerialName("remote_url") val remoteUrl: String = "",
    @SerialName("start_date") val startDate: Long = 0,
    @SerialName("finish_date") val finishDate: Long = 0,
    val private: Boolean = false,
) {
    fun toDomain(): Track = Track(
        id = id, mangaId = mangaId, trackerId = trackerId, remoteId = remoteId,
        libraryId = null, title = title, lastChapterRead = lastChapterRead,
        totalChapters = totalChapters, status = status, score = score,
        remoteUrl = remoteUrl, startDate = startDate, finishDate = finishDate,
        private = private,
    )

    companion object {
        fun from(track: Track, userId: String) = CloudTrack(
            id = track.id, userId = userId, mangaId = track.mangaId,
            trackerId = track.trackerId, remoteId = track.remoteId,
            title = track.title, lastChapterRead = track.lastChapterRead,
            totalChapters = track.totalChapters, status = track.status,
            score = track.score, remoteUrl = track.remoteUrl,
            startDate = track.startDate, finishDate = track.finishDate,
            private = track.private,
        )
    }
}

// ─── History ────────────────────────────────────────────────────────────────────

@Serializable
data class CloudHistory(
    val id: Long,
    @SerialName("user_id") val userId: String,
    @SerialName("chapter_id") val chapterId: Long,
    @SerialName("read_at") val readAt: Long? = null,
    @SerialName("read_duration") val readDuration: Long = 0,
) {
    fun toDomain(): History = History(
        id = id, chapterId = chapterId,
        readAt = readAt?.let { Date(it) },
        readDuration = readDuration,
    )

    companion object {
        fun from(history: History, userId: String) = CloudHistory(
            id = history.id, userId = userId, chapterId = history.chapterId,
            readAt = history.readAt?.time, readDuration = history.readDuration,
        )
    }
}

// ─── Extension Repo ─────────────────────────────────────────────────────────────

@Serializable
data class CloudExtensionRepo(
    @SerialName("base_url") val baseUrl: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("short_name") val shortName: String? = null,
    val website: String,
    @SerialName("signing_key_fingerprint") val signingKeyFingerprint: String,
) {
    fun toDomain(): ExtensionRepo = ExtensionRepo(
        baseUrl = baseUrl, name = name, shortName = shortName,
        website = website, signingKeyFingerprint = signingKeyFingerprint,
    )

    companion object {
        fun from(repo: ExtensionRepo, userId: String) = CloudExtensionRepo(
            baseUrl = repo.baseUrl, userId = userId, name = repo.name,
            shortName = repo.shortName, website = repo.website,
            signingKeyFingerprint = repo.signingKeyFingerprint,
        )
    }
}

// ─── Shared List ────────────────────────────────────────────────────────────────

@Serializable
data class CloudSharedList(
    val id: String = "",
    val title: String = "",
    @SerialName("creator_name") val creatorName: String = "",
    @SerialName("creator_id") val creatorId: String = "",
    val manga: kotlinx.serialization.json.JsonArray = kotlinx.serialization.json.JsonArray(emptyList()),
    val timestamp: Long = 0L,
    @SerialName("import_count") val importCount: Long = 0L,
)

// ─── Encrypted Settings ─────────────────────────────────────────────────────────

@Serializable
data class CloudSensitiveSettings(
    @SerialName("user_id") val userId: String,
    val iv: String,
    val ciphertext: String,
)
