package eu.kanade.tachiyomi.network

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry that maps an HTTP source's original hostname to an ordered list of
 * alternative mirror base URLs. Populated by [eu.kanade.tachiyomi.source.AndroidSourceManager]
 * whenever extensions are loaded or updated, and queried by
 * [eu.kanade.tachiyomi.network.interceptor.CustomSourceUrlInterceptor] to attempt automatic
 * 403-recovery without any user interaction.
 */
class MirrorCandidateRegistry {

    private val map = ConcurrentHashMap<String, List<String>>()

    /**
     * Register [candidates] for [originalHost].
     * Passing an empty list removes any existing entry.
     */
    fun register(originalHost: String, candidates: List<String>) {
        if (candidates.isEmpty()) {
            map.remove(originalHost)
        } else {
            map[originalHost] = candidates
        }
    }

    /** Returns the ordered mirror candidates for [originalHost], or an empty list if none. */
    fun candidatesFor(originalHost: String): List<String> =
        map[originalHost] ?: emptyList()
}
