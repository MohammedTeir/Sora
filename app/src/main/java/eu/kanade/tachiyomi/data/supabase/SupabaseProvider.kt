package eu.kanade.tachiyomi.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Singleton Supabase client — injected via [eu.kanade.tachiyomi.di.AppModule].
 */
object SupabaseProvider {

    private const val SUPABASE_URL = "https://kdnbqekfkymrzpqlhfae.supabase.co"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkbmJxZWtma3ltcnpwcWxoZmFlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ5NDA2MTUsImV4cCI6MjA5MDUxNjYxNX0.EjjNKXiv1S6EMPQ3M_j7fLl2ckuWN-fyaPBmh-BUEEg"

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "app.sora"
                host = "auth-callback"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }
}
