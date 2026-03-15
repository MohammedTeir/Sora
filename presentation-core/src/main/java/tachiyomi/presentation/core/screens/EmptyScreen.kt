package tachiyomi.presentation.core.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.foundation.isSystemInDarkTheme
import dev.icerock.moko.resources.StringResource
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.presentation.core.components.ActionButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

data class EmptyScreenAction(
    val stringRes: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun EmptyScreen(
    stringRes: StringResource,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actions: ImmutableList<EmptyScreenAction>? = null,
) {
    EmptyScreen(
        message = stringResource(stringRes),
        modifier = modifier,
        icon = icon,
        actions = actions,
    )
}

@Composable
fun EmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actions: ImmutableList<EmptyScreenAction>? = null,
) {
    val displayIcon = icon ?: Icons.Outlined.Inbox
    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val glowColor = primaryColor.copy(alpha = if (isDarkTheme) 0.35f else 0.15f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .size(120.dp)
                .drawBehind {
                    val radius = size.maxDimension * 0.65f
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(glowColor, androidx.compose.ui.graphics.Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
        ) {
            Icon(
                imageVector = displayIcon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(64.dp)
            )
        }

        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                shadow = if (isDarkTheme) {
                    androidx.compose.ui.graphics.Shadow(
                        color = glowColor,
                        blurRadius = 16f
                    )
                } else null
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (!actions.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                actions.fastForEach {
                    ActionButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(it.stringRes),
                        icon = it.icon,
                        onClick = it.onClick,
                    )
                }
            }
        }
    }
}
