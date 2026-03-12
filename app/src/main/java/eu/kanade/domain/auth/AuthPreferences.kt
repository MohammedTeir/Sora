package eu.kanade.domain.auth

import tachiyomi.core.common.preference.PreferenceStore

class AuthPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun isLoggedIn() = preferenceStore.getBoolean("auth_is_logged_in", false)

    fun userId() = preferenceStore.getString("auth_user_id", "")

    fun userEmail() = preferenceStore.getString("auth_user_email", "")

    fun userDisplayName() = preferenceStore.getString("auth_user_display_name", "")

    fun lastSyncTime() = preferenceStore.getLong("auth_last_sync_time", 0L)

    fun autoSync() = preferenceStore.getBoolean("auth_auto_sync_enabled", true)
}
