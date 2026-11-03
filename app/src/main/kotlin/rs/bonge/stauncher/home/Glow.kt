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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val GLOW_FADE_MILLIS = 500
private val GlowRadius = 250.dp

// The glow of the stock launcher, measured on a Chromecast: a halo that is strongest at the focused tile and, from
// there, falls off about e-fold per 63 dp. Each pair is a distance as a part of the radius and the opacity there.
private val GlowProfile =
    arrayOf(0f to 0.5f, 0.19f to 0.24f, 0.34f to 0.13f, 0.5f to 0.07f, 0.75f to 0.03f, 1f to 0f)

private const val GLOW_MASK_SIZE = 256

// The glow is the same soft disc for every app, so it is worked out once, in white, and each frame only scales it and
// tints it with the colour of the app. A gradient shader redrawn on every frame of the ring's pulse was too heavy for a
// Chromecast.
private val GlowMask: ImageBitmap by lazy {
    val half = GLOW_MASK_SIZE / 2f
    val pixels =
        IntArray(GLOW_MASK_SIZE * GLOW_MASK_SIZE) { index ->
            val distance = hypot((index % GLOW_MASK_SIZE) + 0.5f - half, (index / GLOW_MASK_SIZE) + 0.5f - half) / half
            val alpha = (glowOpacityAt(distance) * 255f).roundToInt()
            (alpha shl 24) or 0xFFFFFF
        }
    createBitmap(GLOW_MASK_SIZE, GLOW_MASK_SIZE)
        .apply { setPixels(pixels, 0, GLOW_MASK_SIZE, 0, 0, GLOW_MASK_SIZE, GLOW_MASK_SIZE) }
        .asImageBitmap()
}

private fun glowOpacityAt(distance: Float): Float {
    val next = GlowProfile.indexOfFirst { (stop, _) -> stop >= distance }
    if (next < 0) return 0f
    if (next == 0) return GlowProfile.first().second
    val (from, fromOpacity) = GlowProfile[next - 1]
    val (to, toOpacity) = GlowProfile[next]
    return fromOpacity + (toOpacity - fromOpacity) * (distance - from) / (to - from)
}

/**
 * The soft light of the focused app's colour behind the row: its [center] on the screen and its colour, which fades from
 * one app to the next. Both are read only while drawing, so a change redraws the glow and does not recompose.
 */
@Stable
class GlowState {
    internal val color = Animatable(Color.Transparent, Color.VectorConverter(ColorSpaces.Srgb))

    internal var center by mutableStateOf(Offset.Zero)

    /** Fades to the glow of the opaque colour [target], or away when it is null, keeping its colour while it fades. */
    internal suspend fun fadeTo(target: Color?) {
        val to = target ?: color.value.copy(alpha = 0f)
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
            val diameter = (radius * 2).roundToInt()
            drawImage(
                GlowMask,
                dstOffset = IntOffset((glow.center.x - radius).roundToInt(), (glow.center.y - radius).roundToInt()),
                dstSize = IntSize(diameter, diameter),
                colorFilter = ColorFilter.tint(color, BlendMode.SrcIn),
            )
        }
    }
