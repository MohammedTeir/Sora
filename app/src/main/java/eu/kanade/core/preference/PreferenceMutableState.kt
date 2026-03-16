package eu.kanade.core.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.common.preference.Preference

class PreferenceMutableState<T>(
    private val preference: Preference<T>,
    scope: CoroutineScope,
) : MutableState<T> {

    private val state = mutableStateOf(preference.get())

    // Bug #15 fix: store the Job so it can be cancelled on disposal.
    // Previously launchIn() returned a Job that was silently discarded,
    // making it impossible to stop the upstream Flow subscription when
    // the owning composable left the tree — causing ghost emissions that
    // updated a now-dead MutableState and could trigger
    // "Composition locals are not accessible from CoroutineScope" crashes
    // on back navigation.
    private val job: Job = preference.changes()
        .onEach { state.value = it }
        .launchIn(scope)

    /**
     * Cancel the upstream Flow subscription.
     *
     * Call this from a [DisposableEffect] onDispose block, or use the
     * [Preference.collectAsState] extension below which handles it automatically.
     */
    fun dispose() = job.cancel()

    override var value: T
        get() = state.value
        set(value) {
            preference.set(value)
        }

    override fun component1(): T {
        return state.value
    }

    override fun component2(): (T) -> Unit {
        return preference::set
    }
}

/**
 * Create a [PreferenceMutableState] backed by a [CoroutineScope] tied to the
 * current composition lifetime.
 *
 * Bug #2 fix: previously call sites passed `viewModelScope` or `screenModelScope`
 * which outlive the composable. On configuration change the old scope kept the
 * Flow subscription alive, producing duplicate emissions.
 *
 * This extension uses [rememberCoroutineScope] so the scope — and therefore the
 * Flow subscription — is automatically cancelled when the composable leaves the
 * tree. [DisposableEffect] calls [PreferenceMutableState.dispose] as a belt-and-
 * suspenders guarantee so the Job is cancelled even if the scope itself lingers.
 *
 * Usage:
 * ```kotlin
 * // Before (leaks scope, no cancellation)
 * val pref = remember { myPref.asState(viewModelScope) }
 *
 * // After (scope and Job both cancelled on composition exit)
 * val pref by myPref.collectAsState()
 * ```
 */
@Composable
fun <T> Preference<T>.collectAsState(): State<T> {
    val scope = rememberCoroutineScope()
    val state = remember(this) { asState(scope) }
    DisposableEffect(this) {
        onDispose { state.dispose() }
    }
    return state
}

fun <T> Preference<T>.asState(scope: CoroutineScope) = PreferenceMutableState(this, scope)
