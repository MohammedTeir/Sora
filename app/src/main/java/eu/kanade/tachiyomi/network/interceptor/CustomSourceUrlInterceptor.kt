package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.domain.source.service.SourcePreferences
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
 */
class CustomSourceUrlInterceptor(
    private val preferences: SourcePreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalHost = request.url.host

        val customUrl = preferences.customUrlOverride(originalHost).get().trim()
        if (customUrl.isBlank()) return chain.proceed(request)

        val custom = customUrl.toHttpUrlOrNull() ?: return chain.proceed(request)

        val newUrl = request.url.newBuilder()
            .scheme(custom.scheme)
            .host(custom.host)
            .port(custom.port)
            .build()

        val newRequest = request.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
