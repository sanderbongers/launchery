package rs.bonge.stauncher.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

private val LabelAreaHeight = 28.dp
private val LabelTopGap = 6.dp

/**
 * The name of a round button of [buttonWidth], shown below it while the button has focus. It is wider than the button
 * and is centred on it, or, with [endAligned], ends where the button ends. It always takes its space, so that nothing
 * moves when it shows.
 */
@Composable
internal fun ButtonLabel(
    text: String,
    visible: Boolean,
    buttonWidth: Dp,
    modifier: Modifier = Modifier,
    endAligned: Boolean = false,
) {
    Box(
        modifier = modifier.width(buttonWidth).height(LabelAreaHeight).padding(top = LabelTopGap),
        contentAlignment = if (endAligned) Alignment.TopEnd else Alignment.TopCenter,
    ) {
        if (visible) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                modifier =
                    Modifier.wrapContentWidth(
                        align = if (endAligned) Alignment.End else Alignment.CenterHorizontally,
                        unbounded = true,
                    ),
            )
        }
    }
}
