package eu.kanade.tachiyomi.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import logcat.LogPriority
import logcat.logcat

class FirebaseAuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getUserId(): String? = auth.currentUser?.uid

    fun getUserEmail(): String? = auth.currentUser?.email

    fun getUserDisplayName(): String? = auth.currentUser?.displayName ?: auth.currentUser?.email

    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Sign in succeeded but user is null"))
            logcat(LogPriority.INFO) { "FirebaseAuthService: signed in user $uid" }
            Result.success(uid)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "FirebaseAuthService: sign in failed: ${e.message}" }
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Sign up succeeded but user is null"))
            logcat(LogPriority.INFO) { "FirebaseAuthService: created user $uid" }
            Result.success(uid)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { "FirebaseAuthService: sign up failed: ${e.message}" }
            Result.failure(e)
        }
    }

    fun signOut() {
        logcat(LogPriority.INFO) { "FirebaseAuthService: signing out" }
        auth.signOut()
    }
}
