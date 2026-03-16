package eu.kanade.core.preference

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import tachiyomi.core.common.preference.CheckboxState

// ─────────────────────────────────────────────────────────────────────────────
// ToggleableState mapping — kept for backward compatibility with the
// TriStateCheckbox in CategoryDialogs.kt.
//
// NOTE: Exclude → Indeterminate is semantically incorrect (Indeterminate means
// "partially selected" in Material 3, not "excluded"). New filter UI should use
// filterIcon() + TriStateFilterChip instead, which shows an explicit ✓ or ✕.
// ─────────────────────────────────────────────────────────────────────────────
fun <T> CheckboxState.TriState<T>.asToggleableState() = when (this) {
    is CheckboxState.TriState.Exclude -> ToggleableState.Indeterminate
    is CheckboxState.TriState.Include -> ToggleableState.On
    is CheckboxState.TriState.None -> ToggleableState.Off
}

// ─────────────────────────────────────────────────────────────────────────────
// filterIcon — maps each TriState to an explicit icon for use in filter chips.
//
// None    → null       (no icon — neutral, unset state)
// Include → Check ✓   (this filter is active — show matching items)
// Exclude → Close ✕   (this filter is active — hide matching items)
//
// Using Close instead of Indeterminate makes the Exclude state immediately
// readable: a ✕ communicates "excluded" far more clearly than a dash.
// ─────────────────────────────────────────────────────────────────────────────
fun <T> CheckboxState.TriState<T>.filterIcon(): ImageVector? = when (this) {
    is CheckboxState.TriState.None -> null
    is CheckboxState.TriState.Include -> Icons.Filled.Check
    is CheckboxState.TriState.Exclude -> Icons.Filled.Close
}

// ─────────────────────────────────────────────────────────────────────────────
// TriStateFilterChip — drop-in composable for filter sheets.
//
// Replaces the TriStateCheckbox + asToggleableState() pattern with a proper
// Material 3 FilterChip that shows:
//   • No icon   when state is None    (chip looks unselected)
//   • ✓ icon   when state is Include  (chip looks selected / filled)
//   • ✕ icon   when state is Exclude  (chip looks selected with close icon)
//
// The cycling order follows CheckboxState.TriState.next():
//   None → Include → Exclude → None → …
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun <T> TriStateFilterChip(
    state: CheckboxState.TriState<T>,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = state.filterIcon()
    FilterChip(
        selected = state !is CheckboxState.TriState.None,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(),
    )
}
