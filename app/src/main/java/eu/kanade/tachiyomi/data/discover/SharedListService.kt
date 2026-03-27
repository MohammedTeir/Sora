package eu.kanade.tachiyomi.data.discover

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class SharedMangaItem(
    val title: String = "",
    val sourceId: Long = 0L,
    val coverUrl: String = "",
    val sourceUrl: String = "",
)

data class SharedList(
    val id: String = "",
    val title: String = "",
    val creatorName: String = "",
    val creatorId: String = "",
    val manga: List<Map<String, Any>> = emptyList(),
    val timestamp: Long = 0L,
    val importCount: Long = 0L,
) {
    fun getMangaItems(): List<SharedMangaItem> = manga.map { m ->
        SharedMangaItem(
            title = m["title"] as? String ?: "",
            // Firestore's Android SDK deserialises numeric fields as either
            // java.lang.Integer (when the value fits in 32 bits) or java.lang.Long.
            // A direct `as? Long` cast returns null for Integer values, silently
            // producing 0L and breaking source matching during list import.
            // Casting via Number first handles both boxed types safely.
            sourceId = (m["sourceId"] as? Number)?.toLong() ?: 0L,
            coverUrl = m["coverUrl"] as? String ?: "",
            sourceUrl = m["sourceUrl"] as? String ?: "",
        )
    }
}

private fun com.google.firebase.firestore.QuerySnapshot.toSharedLists(): List<SharedList> =
    documents.mapNotNull { doc -> doc.toObject(SharedList::class.java)?.copy(id = doc.id) }

class SharedListService(
    private val authService: FirebaseAuthService = Injekt.get(),
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("shared_lists")

    suspend fun getTrendingLists(limit: Int = 20): List<SharedList> {
        val query = collection
            .orderBy("importCount", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        return try {
            query.get().await().toSharedLists()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: getTrendingLists server failed: ${e.message}, trying cache" }
            try {
                query.get(Source.CACHE).await().toSharedLists()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getRecentLists(limit: Int = 20): List<SharedList> {
        val query = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        return try {
            query.get().await().toSharedLists()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: getRecentLists server failed: ${e.message}, trying cache" }
            try {
                query.get(Source.CACHE).await().toSharedLists()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getMyLists(): List<SharedList> {
        val userId = authService.getUserId() ?: run {
            logcat(LogPriority.WARN) { "SharedListService: getMyLists skipped — not logged in" }
            return emptyList()
        }
        // whereEqualTo("creatorId") + orderBy("timestamp") requires a composite
        // index in the Firebase console. Query with the single-field filter only
        // (auto-indexed) and sort the small result set client-side.
        val query = collection.whereEqualTo("creatorId", userId)
        return try {
            query.get().await().toSharedLists().sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: getMyLists server failed: ${e.message}, trying cache" }
            try {
                query.get(Source.CACHE).await().toSharedLists().sortedByDescending { it.timestamp }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun uploadList(title: String, manga: List<SharedMangaItem>): Result<String> {
        val userId = authService.getUserId()
            ?: return Result.failure(Exception("Must be signed in to share a list"))
        val creatorName = authService.getUserDisplayName() ?: "Anonymous"

        val data = hashMapOf(
            "title" to title,
            "creatorName" to creatorName,
            "creatorId" to userId,
            "manga" to manga.map {
                mapOf(
                    "title" to it.title,
                    "sourceId" to it.sourceId,
                    "coverUrl" to it.coverUrl,
                    "sourceUrl" to it.sourceUrl,
                )
            },
            "timestamp" to System.currentTimeMillis(),
            "importCount" to 0L,
        )

        val docRef = collection.document()
        try {
            // Await the server ACK with a timeout so loadLists() will find the
            // document on subsequent fetches. On good connections this completes
            // in ~1-2 s. On slow/no connection the timeout fires and Firestore
            // offline persistence still queues the write for later sync.
            withTimeout(10_000) { docRef.set(data).await() }
            logcat(LogPriority.INFO) { "SharedListService: list '${title}' synced to server as ${docRef.id}" }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SharedListService: upload await failed for '${title}': ${e.message}. Write queued locally." }
        }

        return Result.success(docRef.id)
    }

    suspend fun incrementImportCount(listId: String) {
        try {
            collection.document(listId)
                .update("importCount", FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SharedListService: incrementImportCount failed: ${e.message}" }
        }
    }

    suspend fun deleteList(listId: String): Boolean {
        return try {
            collection.document(listId).delete().await()
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: deleteList failed: ${e.message}" }
            false
        }
    }
}
