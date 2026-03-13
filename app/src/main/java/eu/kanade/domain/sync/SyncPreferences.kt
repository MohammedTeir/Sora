package eu.kanade.domain.sync

import tachiyomi.core.common.preference.PreferenceStore

class SyncPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun syncOnStartup() = preferenceStore.getBoolean("sync_on_startup", true)

    fun lastSyncTimestamp() = preferenceStore.getLong("sync_last_timestamp", 0L)

    fun isSyncing() = preferenceStore.getBoolean("sync_is_syncing", false)

    // ─── Data-category toggles ─────────────────────────────────────────────
    fun syncLibrary()    = preferenceStore.getBoolean("sync_library", true)
    fun syncChapters()   = preferenceStore.getBoolean("sync_chapters", true)
    fun syncHistory()    = preferenceStore.getBoolean("sync_history", true)
    fun syncTracking()   = preferenceStore.getBoolean("sync_tracking", true)
    fun syncCategories() = preferenceStore.getBoolean("sync_categories", true)

    /**
     * IDs of manga the user wants to sync. An empty set means "sync all manga".
     * When non-empty, only the listed manga (and their associated data) are synced.
     */
    fun syncSelectedMangaIds() = preferenceStore.getStringSet("sync_selected_manga_ids", emptySet())
}
