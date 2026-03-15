package eu.kanade.presentation.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SearchBarBg = Color(0xFF1C1C1E)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGrey = Color(0xFF9E9E9E)
private val PrimaryBlue = Color(0xFF2F80ED)

@Composable
fun SearchInputBar(
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) PrimaryBlue else Color.Transparent,
        label = "searchBorderColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(SearchBarBg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(25.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = if (isFocused) PrimaryBlue else TextGrey,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search for manga, authors...",
                        color = TextGrey,
                        fontSize = 14.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(color = TextWhite, fontSize = 14.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(PrimaryBlue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) onSearch(query.trim())
                        },
                    ),
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { query = "" },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Clear search",
                        tint = TextGrey,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = onVoiceSearch,
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Voice search",
                        tint = TextGrey,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
