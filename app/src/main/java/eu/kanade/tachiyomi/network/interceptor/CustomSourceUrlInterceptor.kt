package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.network.MirrorCandidateRegistry
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Intercepts outgoing HTTP requests and replaces the host/scheme/port
 * when the user has configured a custom mirror URL for a source.
 *
 * Custom URLs are stored per original host via [SourcePreferences.customUrlOverride].
 * This lets users work around HTTP 403 / geo-blocked domains by pointing
 * a source at an alternative mirror without touching the extension APK.
 *
 * ## Automatic 403 recovery
 *
 * If no custom URL is configured and the server responds with HTTP 403, the interceptor
 * consults [MirrorCandidateRegistry] for mirror URLs declared by the source via
 * [eu.kanade.tachiyomi.source.online.HttpSource.mirrorCandidates]. Each candidate is probed
 * in order (application interceptors may call `proceed()` multiple times per OkHttp spec).
 * The first candidate that returns a successful (2xx) response is:
 *  1. Saved to [SourcePreferences.customUrlOverride] so every subsequent request uses it
 *     immediately without probing again.
 *  2. Returned to the caller transparently — the user never sees the 403.
 *
 * If no candidate succeeds the original 403 response is returned unchanged.
 */
class CustomSourceUrlInterceptor(
    private val preferences: SourcePreferences,
    private val mirrorCandidateRegistry: MirrorCandidateRegistry,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalHost = request.url.host

        val customUrl = preferences.customUrlOverride(originalHost).get().trim()

        // ── Case 1: custom mirror already configured → rewrite URL and proceed. ──────────
        if (customUrl.isNotBlank()) {
            val custom = customUrl.toHttpUrlOrNull()
            if (custom != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(custom.scheme)
                    .host(custom.host)
                    .port(custom.port)
                    .build()
                return chain.proceed(request.newBuilder().url(newUrl).build())
            }
        }

        // ── Case 2: no custom mirror — proceed normally. ─────────────────────────────────
        val response = chain.proceed(request)
        if (response.code != 403) return response

        // ── Case 3: HTTP 403 — try mirror candidates (OkHttp application interceptors   ──
        //           may call chain.proceed() more than once).                             ──
        val candidates = mirrorCandidateRegistry.candidatesFor(originalHost)
        if (candidates.isEmpty()) return response

        for (candidateUrl in candidates) {
            val candidate = candidateUrl.toHttpUrlOrNull() ?: continue

            val probeUrl = request.url.newBuilder()
                .scheme(candidate.scheme)
                .host(candidate.host)
                .port(candidate.port)
                .build()

            val probeResponse = try {
                chain.proceed(request.newBuilder().url(probeUrl).build())
            } catch (_: Exception) {
                continue
            }

            if (probeResponse.isSuccessful) {
                // Persist the working mirror so future requests skip probing.
                preferences.customUrlOverride(originalHost).set(candidateUrl.trim())
                response.close()
                return probeResponse
            }

            probeResponse.close()
        }

        // No candidate worked — surface the original 403 to the caller.
        return response
    }
}
