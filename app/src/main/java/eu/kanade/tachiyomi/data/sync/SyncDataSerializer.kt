package eu.kanade.tachiyomi.data.sync

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import logcat.LogPriority
import logcat.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Serializes domain objects to/from Firestore maps and handles
 * AES-256-GCM encryption for sensitive settings (tracker tokens).
 */
object SyncDataSerializer {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "sora_sync_key"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    // ─── Encryption ────────────────────────────────────────────────────────────

    data class EncryptedPayload(val iv: String, val ciphertext: String)

    fun encryptSensitive(plaintext: String): EncryptedPayload {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        return EncryptedPayload(iv = iv, ciphertext = encrypted)
    }

    fun decryptSensitive(payload: EncryptedPayload): String {
        val key = getOrCreateKey()
        val ivBytes = Base64.decode(payload.iv, Base64.NO_WRAP)
        val ciphertextBytes = Base64.decode(payload.ciphertext, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return String(cipher.doFinal(ciphertextBytes), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        logcat(LogPriority.INFO) { "SyncDataSerializer: generating new AES key in Android Keystore" }
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(keyGenSpec)
        return keyGen.generateKey()
    }

    // ─── Manga ─────────────────────────────────────────────────────────────────

    fun mangaToMap(manga: Manga): Map<String, Any?> = mapOf(
        "id" to manga.id,
        "source" to manga.source,
        "url" to manga.url,
        "title" to manga.title,
        "artist" to manga.artist,
        "author" to manga.author,
        "description" to manga.description,
        "genre" to manga.genre,
        "status" to manga.status,
        "thumbnailUrl" to manga.thumbnailUrl,
        "favorite" to manga.favorite,
        "lastUpdate" to manga.lastUpdate,
        "dateAdded" to manga.dateAdded,
        "viewerFlags" to manga.viewerFlags,
        "chapterFlags" to manga.chapterFlags,
        "updateStrategy" to manga.updateStrategy.name,
        "lastModifiedAt" to manga.lastModifiedAt,
        "notes" to manga.notes,
        "updatedAt" to System.currentTimeMillis(),
    )

    fun mapToManga(map: Map<String, Any?>): Manga? {
        return try {
            Manga(
                id = (map["id"] as? Long) ?: (map["id"] as? Number)?.toLong() ?: return null,
                source = (map["source"] as? Long) ?: (map["source"] as? Number)?.toLong() ?: 0L,
                url = map["url"] as? String ?: return null,
                title = map["title"] as? String ?: return null,
                artist = map["artist"] as? String,
                author = map["author"] as? String,
                description = map["description"] as? String,
                genre = @Suppress("UNCHECKED_CAST") (map["genre"] as? List<String>),
                status = (map["status"] as? Long) ?: (map["status"] as? Number)?.toLong() ?: 0L,
                thumbnailUrl = map["thumbnailUrl"] as? String,
                favorite = map["favorite"] as? Boolean ?: false,
                lastUpdate = (map["lastUpdate"] as? Long) ?: (map["lastUpdate"] as? Number)?.toLong() ?: 0L,
                nextUpdate = 0L,
                fetchInterval = 0,
                dateAdded = (map["dateAdded"] as? Long) ?: (map["dateAdded"] as? Number)?.toLong() ?: 0L,
                viewerFlags = (map["viewerFlags"] as? Long) ?: (map["viewerFlags"] as? Number)?.toLong() ?: 0L,
                chapterFlags = (map["chapterFlags"] as? Long) ?: (map["chapterFlags"] as? Number)?.toLong() ?: 0L,
                coverLastModified = 0L,
                updateStrategy = runCatching {
                    eu.kanade.tachiyomi.source.model.UpdateStrategy.valueOf(map["updateStrategy"] as? String ?: "")
                }.getOrDefault(eu.kanade.tachiyomi.source.model.UpdateStrategy.ALWAYS_UPDATE),
                initialized = false,
                lastModifiedAt = (map["lastModifiedAt"] as? Long) ?: (map["lastModifiedAt"] as? Number)?.toLong() ?: 0L,
                favoriteModifiedAt = null,
                version = 1L,
                notes = map["notes"] as? String ?: "",
            )
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SyncDataSerializer: failed to deserialize manga: ${e.message}" }
            null
        }
    }

    // ─── Chapter ───────────────────────────────────────────────────────────────

    fun chapterToMap(chapter: Chapter): Map<String, Any?> = mapOf(
        "id" to chapter.id,
        "mangaId" to chapter.mangaId,
        "url" to chapter.url,
        "name" to chapter.name,
        "read" to chapter.read,
        "bookmark" to chapter.bookmark,
        "lastPageRead" to chapter.lastPageRead,
        "chapterNumber" to chapter.chapterNumber,
        "scanlator" to chapter.scanlator,
        "dateUpload" to chapter.dateUpload,
        "lastModifiedAt" to chapter.lastModifiedAt,
        "updatedAt" to System.currentTimeMillis(),
    )

    fun mapToChapter(map: Map<String, Any?>): Chapter? {
        return try {
            Chapter(
                id = (map["id"] as? Long) ?: (map["id"] as? Number)?.toLong() ?: return null,
                mangaId = (map["mangaId"] as? Long) ?: (map["mangaId"] as? Number)?.toLong() ?: return null,
                url = map["url"] as? String ?: return null,
                name = map["name"] as? String ?: "",
                read = map["read"] as? Boolean ?: false,
                bookmark = map["bookmark"] as? Boolean ?: false,
                lastPageRead = (map["lastPageRead"] as? Long) ?: (map["lastPageRead"] as? Number)?.toLong() ?: 0L,
                pagesCount = 0L,
                dateFetch = 0L,
                sourceOrder = 0L,
                dateUpload = (map["dateUpload"] as? Long) ?: (map["dateUpload"] as? Number)?.toLong() ?: 0L,
                chapterNumber = (map["chapterNumber"] as? Double) ?: (map["chapterNumber"] as? Number)?.toDouble() ?: -1.0,
                scanlator = map["scanlator"] as? String,
                lastModifiedAt = (map["lastModifiedAt"] as? Long) ?: (map["lastModifiedAt"] as? Number)?.toLong() ?: 0L,
                version = 1L,
            )
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SyncDataSerializer: failed to deserialize chapter: ${e.message}" }
            null
        }
    }

    // ─── History ───────────────────────────────────────────────────────────────

    fun historyToMap(history: History): Map<String, Any?> = mapOf(
        "id" to history.id,
        "chapterId" to history.chapterId,
        "readAt" to history.readAt?.time,
        "readDuration" to history.readDuration,
        "updatedAt" to System.currentTimeMillis(),
    )

    fun mapToHistory(map: Map<String, Any?>): History? {
        return try {
            History(
                id = (map["id"] as? Long) ?: (map["id"] as? Number)?.toLong() ?: return null,
                chapterId = (map["chapterId"] as? Long) ?: (map["chapterId"] as? Number)?.toLong() ?: return null,
                readAt = (map["readAt"] as? Long)?.let { java.util.Date(it) },
                readDuration = (map["readDuration"] as? Long) ?: (map["readDuration"] as? Number)?.toLong() ?: -1L,
            )
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SyncDataSerializer: failed to deserialize history: ${e.message}" }
            null
        }
    }

    // ─── Category ──────────────────────────────────────────────────────────────

    fun categoryToMap(category: Category): Map<String, Any?> = mapOf(
        "id" to category.id,
        "name" to category.name,
        "order" to category.order,
        "flags" to category.flags,
        "hidden" to category.hidden,
        "updatedAt" to System.currentTimeMillis(),
    )

    fun mapToCategory(map: Map<String, Any?>): Category? {
        return try {
            Category(
                id = (map["id"] as? Long) ?: (map["id"] as? Number)?.toLong() ?: return null,
                name = map["name"] as? String ?: return null,
                order = (map["order"] as? Long) ?: (map["order"] as? Number)?.toLong() ?: 0L,
                flags = (map["flags"] as? Long) ?: (map["flags"] as? Number)?.toLong() ?: 0L,
                hidden = map["hidden"] as? Boolean ?: false,
            )
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SyncDataSerializer: failed to deserialize category: ${e.message}" }
            null
        }
    }

    // ─── Track ─────────────────────────────────────────────────────────────────

    fun trackToMap(track: Track): Map<String, Any?> = mapOf(
        "id" to track.id,
        "mangaId" to track.mangaId,
        "trackerId" to track.trackerId,
        "remoteId" to track.remoteId,
        "title" to track.title,
        "lastChapterRead" to track.lastChapterRead,
        "totalChapters" to track.totalChapters,
        "status" to track.status,
        "score" to track.score,
        "remoteUrl" to track.remoteUrl,
        "startDate" to track.startDate,
        "finishDate" to track.finishDate,
        "private" to track.private,
        "updatedAt" to System.currentTimeMillis(),
    )

    fun mapToTrack(map: Map<String, Any?>): Track? {
        return try {
            Track(
                id = (map["id"] as? Long) ?: (map["id"] as? Number)?.toLong() ?: return null,
                mangaId = (map["mangaId"] as? Long) ?: (map["mangaId"] as? Number)?.toLong() ?: return null,
                trackerId = (map["trackerId"] as? Long) ?: (map["trackerId"] as? Number)?.toLong() ?: return null,
                remoteId = (map["remoteId"] as? Long) ?: (map["remoteId"] as? Number)?.toLong() ?: 0L,
                libraryId = null,
                title = map["title"] as? String ?: "",
                lastChapterRead = (map["lastChapterRead"] as? Double) ?: (map["lastChapterRead"] as? Number)?.toDouble() ?: 0.0,
                totalChapters = (map["totalChapters"] as? Long) ?: (map["totalChapters"] as? Number)?.toLong() ?: 0L,
                status = (map["status"] as? Long) ?: (map["status"] as? Number)?.toLong() ?: 0L,
                score = (map["score"] as? Double) ?: (map["score"] as? Number)?.toDouble() ?: 0.0,
                remoteUrl = map["remoteUrl"] as? String ?: "",
                startDate = (map["startDate"] as? Long) ?: (map["startDate"] as? Number)?.toLong() ?: 0L,
                finishDate = (map["finishDate"] as? Long) ?: (map["finishDate"] as? Number)?.toLong() ?: 0L,
                private = map["private"] as? Boolean ?: false,
            )
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SyncDataSerializer: failed to deserialize track: ${e.message}" }
            null
        }
    }
}
