package rs.bonge.stauncher.home

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.dp
import rs.bonge.stauncher.apps.ArtKind
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.settings.AppStyle

// Google TV tints its background only a little with the colour of the focused app: a red banner lifts the red of its
// dark background by about 17 of 255.
private const val GLOW_ALPHA = 0.11f
private const val GLOW_FADE_MILLIS = 400
private val GlowRadius = 440.dp

/** What the loader draws for [app] in [style]; the glow takes its colour from the same art. */
internal fun artKindFor(
    app: InstalledApp,
    style: AppStyle,
): ArtKind =
    when {
        style == AppStyle.RoundIcons -> ArtKind.RoundIcon
        app.hasBanner -> ArtKind.Banner
        else -> ArtKind.Icon
    }

/**
 * The soft light of the focused app's colour behind the row: its [center] on the screen and its colour, which fades from
 * one app to the next. Both are read only while drawing, so a change redraws the glow and does not recompose.
 */
@Stable
class GlowState {
    internal val color = Animatable(Color.Transparent, Color.VectorConverter(ColorSpaces.Srgb))

    internal var center by mutableStateOf(Offset.Zero)

    /** Fades to the glow of [target], or away when it is null, keeping the colour it had while it fades. */
    internal suspend fun fadeTo(target: Color?) {
        val to = target?.copy(alpha = GLOW_ALPHA) ?: color.value.copy(alpha = 0f)
        color.animateTo(to, tween(GLOW_FADE_MILLIS))
    }
}

@Composable
fun rememberGlowState() = remember { GlowState() }

/** Draws the [glow] behind the content, over the background that comes before it in the modifier chain. */
fun Modifier.glow(glow: GlowState): Modifier =
    drawBehind {
        val color = glow.color.value
        if (color.alpha > 0f) {
            val radius = GlowRadius.toPx()
            drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = glow.center, radius = radius))
        }
    }
