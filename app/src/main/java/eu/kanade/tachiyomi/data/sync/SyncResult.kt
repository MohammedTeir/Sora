package eu.kanade.tachiyomi.data.sync

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String, val exception: Exception? = null) : SyncResult()
}
