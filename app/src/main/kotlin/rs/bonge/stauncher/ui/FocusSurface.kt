package rs.bonge.stauncher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface

private val RingWidth = 2.dp
private val RingGap = 3.5.dp

// The ring of the stock launcher breathes: it lands at about 80% opacity, fades to about 13% and back, 2.3 s a cycle.
private const val RING_PEAK_ALPHA = 0.8f
private const val RING_TROUGH_ALPHA = 0.13f
private const val RING_FADE_MILLIS = 1150

/** How a focused [FocusSurface] stands out. */
enum class FocusStyle {
    /** A thin light ring, set off from the surface by a gap, that pulses while focused. For tiles, which show artwork. */
    Ring,

    /** The surface turns light and what is on it turns dark. For buttons and list rows. */
    Fill,
}

/**
 * The clickable surface of the home screen and the settings. It grows by [focusedScale] while focused, in the way its
 * [style] says, and casts [focusedGlow] around it. Text on it takes the content colour, so that it turns dark together
 * with a [FocusStyle.Fill] surface. Its colour is [containerColor], or the card colour of the theme.
 */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    shape: Shape,
    style: FocusStyle,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    focusedScale: Float = 1f,
    focusedGlow: Glow = Glow.None,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val container = containerColor.takeOrElse { scheme.surfaceVariant }
    val focusedContainer = if (style == FocusStyle.Fill) scheme.inverseSurface else container
    val focusedContent = if (style == FocusStyle.Fill) scheme.inverseOnSurface else scheme.onSurface
    var focused by remember { mutableStateOf(false) }
    val ring =
        if (style == FocusStyle.Ring && focused) {
            Modifier.pulsingRing(shape, focusedScale, scheme.border)
        } else {
            Modifier
        }
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.onFocusChanged { focused = it.isFocused }.then(ring),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = container,
                contentColor = scheme.onSurface,
                focusedContainerColor = focusedContainer,
                focusedContentColor = focusedContent,
                pressedContainerColor = focusedContainer,
                pressedContentColor = focusedContent,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = focusedGlow),
        content = content,
    )
}

/**
 * The ring around a focused tile, drawn behind it. It follows the growth of the tile to [tileScale] and pulses from its
 * peak, starting again each time focus lands. Both are read only while drawing, so a frame of the pulse redraws the ring
 * and does not recompose the surface.
 */
@Composable
private fun Modifier.pulsingRing(
    shape: Shape,
    tileScale: Float,
    color: Color,
): Modifier {
    val alpha = rememberRingPulse()
    val growth = remember { Animatable(1f) }
    LaunchedEffect(tileScale) { growth.animateTo(tileScale) }
    return drawBehind {
        val scale = growth.value
        // The ring is centred on its inset, so half of its width goes on top of the gap to keep the gap clear.
        val inset = (RingGap + RingWidth / 2).toPx() * scale
        val ringSize = Size(size.width * scale + inset * 2, size.height * scale + inset * 2)
        translate((size.width - ringSize.width) / 2, (size.height - ringSize.height) / 2) {
            drawOutline(
                outline = shape.createOutline(ringSize, layoutDirection, this),
                color = color,
                alpha = alpha.value,
                style = Stroke(RingWidth.toPx() * scale),
            )
        }
    }
}

@Composable
private fun rememberRingPulse(): State<Float> {
    val transition = rememberInfiniteTransition(label = "ring")
    return transition.animateFloat(
        initialValue = RING_PEAK_ALPHA,
        targetValue = RING_TROUGH_ALPHA,
        animationSpec = infiniteRepeatable(tween(RING_FADE_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring alpha",
    )
}
