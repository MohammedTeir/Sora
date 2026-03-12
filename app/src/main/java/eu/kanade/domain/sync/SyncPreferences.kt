package eu.kanade.domain.sync

import tachiyomi.core.common.preference.PreferenceStore

class SyncPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun syncOnStartup() = preferenceStore.getBoolean("sync_on_startup", true)

    fun lastSyncTimestamp() = preferenceStore.getLong("sync_last_timestamp", 0L)

    fun isSyncing() = preferenceStore.getBoolean("sync_is_syncing", false)
}
