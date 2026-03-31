package eu.kanade.tachiyomi.ui.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.tachiyomi.data.auth.SupabaseAuthService
import eu.kanade.tachiyomi.data.sync.SupabaseSyncService
import eu.kanade.tachiyomi.data.sync.SyncWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AuthScreenModel(
    private val authService: SupabaseAuthService = Injekt.get(),
    private val syncService: SupabaseSyncService = Injekt.get(),
    private val authPrefs: AuthPreferences = Injekt.get(),
) : StateScreenModel<AuthScreenModel.State>(State()) {

    private val _events = Channel<Event>(Channel.UNLIMITED)
    val events: Flow<Event> = _events.receiveAsFlow()

    // ─── State ─────────────────────────────────────────────────────────────────

    data class State(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    sealed interface Event {
        data object LoginSuccess : Event
        data object SignUpSuccess : Event
        data class PasswordResetEmailSent(val email: String) : Event
        data object PasswordResetSuccess : Event
        data object Dismissed : Event
    }

    // ─── Actions ───────────────────────────────────────────────────────────────

    fun login(email: String, password: String, context: android.content.Context) {
        if (email.isBlank() || password.isBlank()) {
            mutableState.update { it.copy(errorMessage = "Email and password cannot be empty") }
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            authService.signIn(email.trim(), password)
                .onSuccess { userId ->
                    persistAuthState(userId, email.trim())
                    triggerPostLoginSync(context)
                    _events.send(Event.LoginSuccess)
                }
                .onFailure { e ->
                    logcat(LogPriority.WARN) { "AuthScreenModel: login failed: ${e.message}" }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyAuthError(e),
                        )
                    }
                }
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String, context: android.content.Context) {
        when {
            email.isBlank() || password.isBlank() -> {
                mutableState.update { it.copy(errorMessage = "All fields are required") }
                return
            }
            password != confirmPassword -> {
                mutableState.update { it.copy(errorMessage = "Passwords do not match") }
                return
            }
            password.length < 6 -> {
                mutableState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
                return
            }
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            authService.signUp(email.trim(), password)
                .onSuccess { userId ->
                    persistAuthState(userId, email.trim())
                    triggerPostLoginSync(context)
                    _events.send(Event.SignUpSuccess)
                }
                .onFailure { e ->
                    logcat(LogPriority.WARN) { "AuthScreenModel: sign up failed: ${e.message}" }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyAuthError(e),
                        )
                    }
                }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            mutableState.update { it.copy(errorMessage = "Email cannot be empty") }
            return
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            authService.sendPasswordResetEmail(email.trim())
                .onSuccess {
                    mutableState.update { it.copy(isLoading = false) }
                    _events.send(Event.PasswordResetEmailSent(email.trim()))
                }
                .onFailure { e ->
                    logcat(LogPriority.WARN) { "AuthScreenModel: reset password failed: ${e.message}" }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyAuthError(e),
                        )
                    }
                }
        }
    }

    fun confirmPasswordReset(newPassword: String, confirmPassword: String) {
        when {
            newPassword.isBlank() || confirmPassword.isBlank() -> {
                mutableState.update { it.copy(errorMessage = "All fields are required") }
                return
            }
            newPassword != confirmPassword -> {
                mutableState.update { it.copy(errorMessage = "Passwords do not match") }
                return
            }
            newPassword.length < 8 -> {
                mutableState.update { it.copy(errorMessage = "Password must be at least 8 characters") }
                return
            }
            !newPassword.any { !it.isLetterOrDigit() } -> {
                mutableState.update { it.copy(errorMessage = "Password must contain a special symbol") }
                return
            }
        }

        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            authService.updatePassword(newPassword)
                .onSuccess {
                    mutableState.update { it.copy(isLoading = false) }
                    _events.send(Event.PasswordResetSuccess)
                }
                .onFailure { e ->
                    logcat(LogPriority.WARN) { "AuthScreenModel: confirm password reset failed: ${e.message}" }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyAuthError(e),
                        )
                    }
                }
        }
    }

    fun signInWithGoogle(context: android.content.Context) {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Supabase handles the OAuth flow via Custom Tabs — no Credential Manager needed.
                authService.signInWithGoogle()
                    .onSuccess { userId ->
                        persistAuthState(userId, authService.getUserEmail() ?: "")
                        triggerPostLoginSync(context)
                        _events.send(Event.LoginSuccess)
                    }
                    .onFailure { e ->
                        mutableState.update { it.copy(isLoading = false, errorMessage = friendlyAuthError(e)) }
                    }
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "AuthScreenModel: Google sign-in failed: ${e.message}" }
                mutableState.update { it.copy(isLoading = false, errorMessage = friendlyAuthError(e)) }
            }
        }
    }

    private fun friendlyAuthError(e: Throwable): String {
        val msg = e.message ?: return "Authentication failed"
        return when {
            "JWT expired" in msg -> "Session expired. Please sign in again."
            "Invalid login credentials" in msg -> "Incorrect email or password."
            "User already registered" in msg -> "An account with this email already exists."
            "WEAK_PASSWORD" in msg || "at least" in msg.lowercase() -> "Password is too weak. Use at least 6 characters."
            "rate limit" in msg.lowercase() || "too many" in msg.lowercase() -> "Too many attempts. Please try again later."
            "Unable to resolve host" in msg || "network" in msg.lowercase() -> "Network error. Check your internet connection."
            "cancelled" in msg.lowercase() -> "Sign-in was cancelled."
            else -> e.localizedMessage ?: "Authentication failed"
        }
    }

    fun clearError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    fun loginAsGuest() {
        authPrefs.isGuest().set(true)
        screenModelScope.launch {
            _events.send(Event.Dismissed)
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun persistAuthState(userId: String, email: String) {
        authPrefs.isLoggedIn().set(true)
        authPrefs.isGuest().set(false)
        authPrefs.userId().set(userId)
        authPrefs.userEmail().set(email)
        authPrefs.userDisplayName().set(authService.getUserDisplayName() ?: "")
    }

    private fun triggerPostLoginSync(context: android.content.Context) {
        // Schedule one-time sync now, and periodic sync going forward
        SyncWorker.enqueueSingleSync(context)
        SyncWorker.enqueuePeriodicSync(context)
        mutableState.update { it.copy(isLoading = false) }
    }
}
