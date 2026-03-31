package eu.kanade.tachiyomi.data.auth

import eu.kanade.tachiyomi.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import logcat.LogPriority
import logcat.logcat

/**
 * Supabase Auth service — drop-in replacement for [FirebaseAuthService].
 *
 * Key differences from the Firebase version:
 * - `signOut()` is `suspend` (revokes token server-side).
 * - Google Sign-In uses OAuth redirect via Custom Tabs (no Credential Manager).
 * - Password reset confirmation is handled server-side via email link — no client OOB code needed.
 * - Token refresh is mostly automatic, but exposed for pre-sync manual refresh.
 */
class SupabaseAuthService {

    private val auth get() = SupabaseProvider.client.auth

    fun isLoggedIn(): Boolean = auth.currentUserOrNull() != null

    fun getUserId(): String? = auth.currentUserOrNull()?.id

    fun getUserEmail(): String? = auth.currentUserOrNull()?.email

    fun getUserDisplayName(): String? =
        auth.currentUserOrNull()?.userMetadata
            ?.get("full_name")?.toString()?.removeSurrounding("\"")

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val uid = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Sign in succeeded but user is null"))
            logcat(LogPriority.INFO) { "SupabaseAuthService: signed in user $uid" }
            Result.success(uid)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseAuthService: sign in failed: ${e.message}" }
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<Pair<String, Boolean>> {
        return try {
            val user = auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // If currentUserOrNull() is not null, the user is fully logged in (no email verification required).
            // If it is null, but user?.id is present, it means email verification is pending.
            val sessionUser = auth.currentUserOrNull()
            val uid = sessionUser?.id ?: user?.id
                ?: return Result.failure(Exception("Sign up succeeded but user ID is missing"))
            
            val isFullyLoggedIn = sessionUser != null
            logcat(LogPriority.INFO) { "SupabaseAuthService: created user $uid. Logged in: $isFullyLoggedIn" }
            Result.success(Pair(uid, isFullyLoggedIn))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseAuthService: sign up failed: ${e.message}" }
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email)
            logcat(LogPriority.INFO) { "SupabaseAuthService: password reset email sent to $email" }
            Result.success(Unit)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseAuthService: password reset failed: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Supabase handles password-reset confirmation via the email link itself.
     * The user clicks the link → is redirected back to the app with a valid session.
     * A new password can then be set via [updatePassword].
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            auth.updateUser { password = newPassword }
            logcat(LogPriority.INFO) { "SupabaseAuthService: password updated" }
            Result.success(Unit)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseAuthService: update password failed: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Google Sign-In via Supabase OAuth — opens a Custom Tab for the OAuth flow.
     * The redirect URI must be configured in the Supabase dashboard.
     */
    suspend fun signInWithGoogle(): Result<String> {
        return try {
            auth.signInWith(Google)
            val uid = auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Google sign-in succeeded but user is null"))
            logcat(LogPriority.INFO) { "SupabaseAuthService: Google sign-in user $uid" }
            Result.success(uid)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "SupabaseAuthService: Google sign-in failed: ${e.message}" }
            Result.failure(e)
        }
    }

    /**
     * Force-refreshes the Supabase session token.
     * supabase-kt does this automatically, but we call it before sync for safety.
     */
    suspend fun refreshToken(): Boolean {
        return try {
            auth.refreshCurrentSession()
            true
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SupabaseAuthService: token refresh failed: ${e.message}" }
            false
        }
    }

    suspend fun signOut() {
        logcat(LogPriority.INFO) { "SupabaseAuthService: signing out" }
        try {
            auth.signOut()
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "SupabaseAuthService: sign out failed: ${e.message}" }
        }
    }
}
