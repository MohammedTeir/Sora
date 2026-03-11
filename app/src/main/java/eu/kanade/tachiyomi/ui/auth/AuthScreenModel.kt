package eu.kanade.tachiyomi.ui.auth

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.auth.AuthPreferences
import eu.kanade.tachiyomi.data.auth.FirebaseAuthService
import eu.kanade.tachiyomi.data.sync.SyncService
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
    private val authService: FirebaseAuthService = Injekt.get(),
    private val syncService: SyncService = Injekt.get(),
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
                    logcat(LogPriority.WARN, e) { "AuthScreenModel: login failed" }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Login failed",
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
                    logcat(LogPriority.WARN, e) { "AuthScreenModel: sign up failed" }
                    mutableState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Sign up failed",
                        )
                    }
                }
        }
    }

    fun clearError() {
        mutableState.update { it.copy(errorMessage = null) }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun persistAuthState(userId: String, email: String) {
        authPrefs.isLoggedIn().set(true)
        authPrefs.userId().set(userId)
        authPrefs.userEmail().set(email)
        authPrefs.userDisplayName().set(authService.getUserDisplayName() ?: email)
    }

    private fun triggerPostLoginSync(context: android.content.Context) {
        // Schedule one-time sync now, and periodic sync going forward
        SyncWorker.enqueueSingleSync(context)
        SyncWorker.enqueuePeriodicSync(context)
        mutableState.update { it.copy(isLoading = false) }
    }
}
