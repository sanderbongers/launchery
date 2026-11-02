package rs.bonge.stauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface

private val RingWidth = 2.dp
private val RingGap = 1.5.dp

/** How a focused [FocusSurface] stands out. */
enum class FocusStyle {
    /** A thin light ring, set off from the surface by a small gap. For tiles, which show artwork. */
    Ring,

    /** The surface turns light and what is on it turns dark. For buttons and list rows. */
    Fill,
}

/**
 * The clickable surface of the home screen and the settings. It grows by [focusedScale] while focused, in the way its
 * [style] says. Text on it takes the content colour, so that it turns dark together with a [FocusStyle.Fill] surface.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    shape: Shape,
    style: FocusStyle,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    focusedScale: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val focusedContainer = if (style == FocusStyle.Fill) scheme.inverseSurface else scheme.surfaceVariant
    val focusedContent = if (style == FocusStyle.Fill) scheme.inverseOnSurface else scheme.onSurface
    // The ring is centred on its inset, so half of its width goes on top of the gap to keep the gap clear.
    val focusedBorder =
        if (style == FocusStyle.Ring) {
            Border(BorderStroke(RingWidth, scheme.border), inset = RingGap + RingWidth / 2, shape = shape)
        } else {
            Border.None
        }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = scheme.surfaceVariant,
                contentColor = scheme.onSurface,
                focusedContainerColor = focusedContainer,
                focusedContentColor = focusedContent,
                pressedContainerColor = focusedContainer,
                pressedContentColor = focusedContent,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        border = ClickableSurfaceDefaults.border(focusedBorder = focusedBorder),
        content = content,
    )
}
