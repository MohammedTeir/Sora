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

    fun signInWithGoogle(context: android.content.Context) {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val credentialManager = androidx.credentials.CredentialManager.create(context)
                val signInWithGoogleOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(
                    context.getString(eu.kanade.tachiyomi.R.string.default_web_client_id),
                ).build()
                val request = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                authService.signInWithGoogle(googleCredential.idToken)
                    .onSuccess { userId ->
                        persistAuthState(userId, authService.getUserEmail() ?: "")
                        triggerPostLoginSync(context)
                        _events.send(Event.LoginSuccess)
                    }
                    .onFailure { e ->
                        mutableState.update { it.copy(isLoading = false, errorMessage = friendlyAuthError(e)) }
                    }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // User dismissed the Google account picker — not an error, just reset loading state.
                mutableState.update { it.copy(isLoading = false) }
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                mutableState.update { it.copy(isLoading = false, errorMessage = "No Google account found on this device.") }
            } catch (e: Exception) {
                logcat(LogPriority.WARN) { "AuthScreenModel: Google sign-in failed: ${e.message}" }
                mutableState.update { it.copy(isLoading = false, errorMessage = friendlyAuthError(e)) }
            }
        }
    }

    private fun friendlyAuthError(e: Throwable): String {
        val msg = e.message ?: return "Authentication failed"
        return when {
            "CONFIGURATION_NOT_FOUND" in msg -> "Sign-in is not configured on the server. Please contact support."
            "SIGN_IN_CANCELLED" in msg || "activity is cancelled" in msg.lowercase() || "GetCredentialCancellationException" in e.javaClass.name -> "Google sign-in was cancelled."
            "EMAIL_NOT_FOUND" in msg || "INVALID_EMAIL" in msg -> "Invalid email address."
            "WEAK_PASSWORD" in msg -> "Password is too weak. Use at least 6 characters."
            "EMAIL_EXISTS" in msg || "email address is already in use" in msg -> "An account with this email already exists."
            "INVALID_PASSWORD" in msg || "wrong-password" in msg -> "Incorrect password."
            "USER_NOT_FOUND" in msg || "no user record" in msg -> "No account found with this email."
            "NETWORK_ERROR" in msg || "Unable to resolve host" in msg -> "Network error. Check your internet connection."
            "TOO_MANY_REQUESTS" in msg -> "Too many attempts. Please try again later."
            else -> e.localizedMessage ?: "Authentication failed"
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
