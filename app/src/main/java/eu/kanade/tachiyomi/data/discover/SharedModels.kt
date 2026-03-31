package eu.kanade.tachiyomi.data.discover

data class SharedList(
    val id: String,
    val title: String,
    val creatorName: String,
    val creatorId: String,
    val manga: List<Map<String, Any>>,
    val timestamp: Long,
    val importCount: Long,
) {
    fun getMangaItems(): List<SharedMangaItem> {
        return manga.map { map ->
            SharedMangaItem(
                title = map["title"] as? String ?: "",
                sourceId = map["sourceId"] as? Long ?: 0L,
                coverUrl = map["coverUrl"] as? String ?: "",
                sourceUrl = map["sourceUrl"] as? String ?: "",
            )
        }
    }
}

data class SharedMangaItem(
    val title: String,
    val sourceId: Long,
    val coverUrl: String,
    val sourceUrl: String,
)
