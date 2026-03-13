package eu.kanade.translation.presentation
 
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import eu.kanade.translation.model.TranslationBlock
import kotlin.math.max
 
@Composable
fun SmartTranslationBlock(
    modifier: Modifier = Modifier,
    block: TranslationBlock,
    scaleFactor: Float,
    fontFamily: FontFamily,
    isRtl: Boolean = false,
) {
    val (padX, padY) = block.textPadding()
    val xPx = max((block.x - padX / 2) * scaleFactor, 0.0f)
    val yPx = max((block.y - padY / 2) * scaleFactor, 0.0f)
    val width = ((block.width + padX) * scaleFactor).pxToDp()
    val height = ((block.height + padY) * scaleFactor).pxToDp()
    val isVertical = block.angle > 85
 
    // Internal padding so text doesn't touch block edges
    val internalPadX = (block.symWidth * 0.3f * scaleFactor).pxToDp()
    val internalPadY = (block.symHeight * 0.2f * scaleFactor).pxToDp()
 
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val textAlign = if (isRtl) TextAlign.Start else TextAlign.Center
 
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = modifier
                .wrapContentSize(Alignment.CenterStart, true)
                .offset(xPx.pxToDp(), yPx.pxToDp())
                .requiredSize(width, height),
        ) {
            val density = LocalDensity.current
            val fontSize = remember { mutableStateOf(16.sp) }
            SubcomposeLayout { constraints ->
                val maxWidthPx = with(density) { width.roundToPx() }
                val maxHeightPx = with(density) { height.roundToPx() }
                val internalPadXPx = with(density) { internalPadX.roundToPx() }
                val internalPadYPx = with(density) { internalPadY.roundToPx() }
                val availableWidthPx = maxWidthPx - (internalPadXPx * 2)
                val availableHeightPx = maxHeightPx - (internalPadYPx * 2)
 
                // Float-precision binary search for optimal font size (0.5sp granularity)
                var low = 1f
                var high = 100f
                var bestSize = low
 
                while (high - low > 0.5f) {
                    val mid = (low + high) / 2f
                    val lineHeight = (mid * 1.15f).sp
                    val textLayoutResult = subcompose(mid) {
                        Text(
                            text = block.translation,
                            fontSize = mid.sp,
                            lineHeight = lineHeight,
                            fontFamily = fontFamily,
                            color = Color.Black,
                            overflow = TextOverflow.Visible,
                            textAlign = textAlign,
                            maxLines = Int.MAX_VALUE,
                            softWrap = true,
                            modifier = Modifier
                                .width(width)
                                .padding(horizontal = internalPadX, vertical = internalPadY)
                                .rotate(if (isVertical) 0f else block.angle)
                                .align(Alignment.Center),
                        )
                    }[0].measure(Constraints(maxWidth = maxWidthPx))
 
                    if (textLayoutResult.height <= maxHeightPx) {
                        bestSize = mid
                        low = mid
                    } else {
                        high = mid
                    }
                }
                fontSize.value = bestSize.sp
 
                val bestLineHeight = (bestSize * 1.15f).sp
 
                // Measure final layout
                val textPlaceable = subcompose(Unit) {
                    Text(
                        text = block.translation,
                        fontSize = fontSize.value,
                        lineHeight = bestLineHeight,
                        fontFamily = fontFamily,
                        color = Color.Black,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = textAlign,
                        maxLines = Int.MAX_VALUE,
                        modifier = Modifier
                            .width(width)
                            .padding(horizontal = internalPadX, vertical = internalPadY)
                            .rotate(if (isVertical) 0f else block.angle)
                            .align(Alignment.Center),
                    )
                }[0].measure(constraints)
 
                layout(textPlaceable.width, textPlaceable.height) {
                    textPlaceable.place(0, 0)
                }
            }
        }
    }
}
