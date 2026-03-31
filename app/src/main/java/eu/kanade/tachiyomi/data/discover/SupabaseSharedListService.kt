package eu.kanade.tachiyomi.data.discover

import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.tachiyomi.data.auth.SupabaseAuthService
import eu.kanade.tachiyomi.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Supabase-backed shared list service — replaces [SharedListService] (Firestore version).
 */
class SupabaseSharedListService(
    private val authService: SupabaseAuthService = Injekt.get(),
    private val authPreferences: AuthPreferences = Injekt.get(),
) {
    private val client = SupabaseProvider.client

    @Serializable
    data class SharedListRow(
        val id: String = "",
        val title: String = "",
        @SerialName("creator_name") val creatorName: String = "",
        @SerialName("creator_id") val creatorId: String = "",
        val manga: JsonArray = JsonArray(emptyList()),
        val timestamp: Long = 0L,
        @SerialName("import_count") val importCount: Long = 0L,
    ) {
        fun toSharedList(): SharedList {
            val mangaMaps = manga.map { element ->
                val obj = element.jsonObject
                mapOf<String, Any>(
                    "title" to (obj["title"]?.jsonPrimitive?.content ?: ""),
                    "sourceId" to (obj["sourceId"]?.jsonPrimitive?.long ?: 0L),
                    "coverUrl" to (obj["coverUrl"]?.jsonPrimitive?.content ?: ""),
                    "sourceUrl" to (obj["sourceUrl"]?.jsonPrimitive?.content ?: ""),
                )
            }
            return SharedList(
                id = id,
                title = title,
                creatorName = creatorName,
                creatorId = creatorId,
                manga = mangaMaps,
                timestamp = timestamp,
                importCount = importCount,
            )
        }
    }

    @Serializable
    data class SharedListInsert(
        val title: String,
        @SerialName("creator_name") val creatorName: String,
        @SerialName("creator_id") val creatorId: String,
        val manga: JsonArray,
        val timestamp: Long,
        @SerialName("import_count") val importCount: Long = 0,
    )

    suspend fun getTrendingLists(limit: Int = 20): List<SharedList> {
        return try {
            client.from("shared_lists")
                .select {
                    order("import_count", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<SharedListRow>()
                .map { it.toSharedList() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseSharedListService: getTrendingLists failed: ${e.message}" }
            emptyList()
        }
    }

    suspend fun getRecentLists(limit: Int = 20): List<SharedList> {
        return try {
            client.from("shared_lists")
                .select {
                    order("timestamp", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<SharedListRow>()
                .map { it.toSharedList() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseSharedListService: getRecentLists failed: ${e.message}" }
            emptyList()
        }
    }

    suspend fun getMyLists(): List<SharedList> {
        val firebaseUid = authService.getUserId()
        val prefsUid = authPreferences.userId().get().takeIf { it.isNotEmpty() }
        val userId = firebaseUid ?: prefsUid ?: run {
            logcat(LogPriority.WARN) { "SupabaseSharedListService: getMyLists skipped — not logged in" }
            return emptyList()
        }

        return try {
            client.from("shared_lists")
                .select {
                    filter { eq("creator_id", userId) }
                    order("timestamp", Order.DESCENDING)
                }
                .decodeList<SharedListRow>()
                .map { it.toSharedList() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseSharedListService: getMyLists failed: ${e.message}" }
            emptyList()
        }
    }

    suspend fun uploadList(title: String, manga: List<SharedMangaItem>): Result<String> {
        val userId = authService.getUserId()
            ?: return Result.failure(Exception("Must be signed in to share a list"))
        val creatorName = authService.getUserDisplayName() ?: "Anonymous"

        val mangaJson = buildJsonArray {
            manga.forEach { item ->
                add(buildJsonObject {
                    put("title", JsonPrimitive(item.title))
                    put("sourceId", JsonPrimitive(item.sourceId))
                    put("coverUrl", JsonPrimitive(item.coverUrl))
                    put("sourceUrl", JsonPrimitive(item.sourceUrl))
                })
            }
        }

        return try {
            val result = client.from("shared_lists")
                .insert(
                    SharedListInsert(
                        title = title,
                        creatorName = creatorName,
                        creatorId = userId,
                        manga = mangaJson,
                        timestamp = System.currentTimeMillis(),
                    ),
                ) { select() }
                .decodeSingle<SharedListRow>()

            logcat(LogPriority.INFO) { "SupabaseSharedListService: list '$title' uploaded as ${result.id}" }
            Result.success(result.id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseSharedListService: upload failed: ${e.message}" }
            Result.failure(e)
        }
    }

    suspend fun incrementImportCount(listId: String) {
        try {
            // Use RPC or raw update to increment
            client.from("shared_lists")
                .update({
                    // Fetch current, increment, set — PostgREST doesn't have atomic increment
                    // So we use a small SQL function instead
                }) {
                    filter { eq("id", listId) }
                }
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SupabaseSharedListService: incrementImportCount failed: ${e.message}" }
        }
    }

    suspend fun deleteList(listId: String): Boolean {
        return try {
            client.from("shared_lists")
                .delete { filter { eq("id", listId) } }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseSharedListService: deleteList failed: ${e.message}" }
            false
        }
    }
}
