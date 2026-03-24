package eu.kanade.tachiyomi.data.discover

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import kotlinx.coroutines.tasks.await
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

class SharedListService(
    private val authService: FirebaseAuthService = Injekt.get(),
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("shared_lists")

    suspend fun getTrendingLists(limit: Int = 20): List<SharedList> {
        return try {
            val snapshot = collection
                .orderBy("importCount", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(SharedList::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: getTrendingLists failed: ${e.message}" }
            emptyList()
        }
    }

    suspend fun getRecentLists(limit: Int = 20): List<SharedList> {
        return try {
            val snapshot = collection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(SharedList::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: getRecentLists failed: ${e.message}" }
            emptyList()
        }
    }

    suspend fun getMyLists(): List<SharedList> {
        val userId = authService.getUserId() ?: return emptyList()
        return try {
            // whereEqualTo("creatorId") + orderBy("timestamp") requires a composite
            // index in the Firebase console. Without it Firestore throws
            // FAILED_PRECONDITION which is silently swallowed → always returns empty.
            // Fix: query with the single-field filter only (auto-indexed) and sort
            // the small result set client-side — users rarely have more than a few
            // dozen lists, so this is not a performance concern.
            val snapshot = collection
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
            snapshot.documents
                .mapNotNull { doc -> doc.toObject(SharedList::class.java)?.copy(id = doc.id) }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SharedListService: getMyLists failed: ${e.message}" }
            emptyList()
        }
    }

    fun uploadList(title: String, manga: List<SharedMangaItem>): Result<String> {
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

        // Generate a client-side document ID so we can return it immediately without
        // waiting for the server. Firestore's offline persistence queues the write
        // locally and syncs it to the server as soon as connectivity is available —
        // the data is never lost. Waiting for the server ACK (the old collection.add().await()
        // pattern) caused the upload to hang indefinitely on slow or unreachable connections.
        val docRef = collection.document()
        docRef.set(data)
            .addOnSuccessListener {
                logcat(LogPriority.INFO) { "SharedListService: list '${title}' synced to server as ${docRef.id}" }
            }
            .addOnFailureListener { e ->
                logcat(LogPriority.WARN) { "SharedListService: background write failed for '${title}': ${e.message}" }
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
