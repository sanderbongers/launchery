package rs.bonge.stauncher.apps

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** What the loader draws for an app. */
enum class ArtKind {
    /** The banner, or the icon for an app that has no banner. */
    Banner,

    /** The icon, even for an app that has a banner, for the places that show an app small. */
    Icon,

    /** The icon cut to a circle. Nothing outside the circle is drawn, so the surface it sits on shows around the icon. */
    RoundIcon,
}

/** Renders an app's banner or icon at the size the screen needs. */
class BannerLoader(
    private val packageManager: PackageManager,
) {
    // lastUpdateTime makes an updated app load its new banner instead of the cached one.
    private data class Key(
        val packageName: String,
        val lastUpdateTime: Long,
        val width: Int,
        val height: Int,
        val kind: ArtKind,
    )

    private val cache =
        object : LruCache<Key, Bitmap>(CACHE_BYTES) {
            override fun sizeOf(
                key: Key,
                value: Bitmap,
            ): Int = value.byteCount
        }

    fun peek(
        app: InstalledApp,
        width: Int,
        height: Int,
        kind: ArtKind,
    ): Bitmap? = cache[key(app, width, height, kind)]

    suspend fun load(
        app: InstalledApp,
        width: Int,
        height: Int,
        kind: ArtKind,
    ): Bitmap? {
        peek(app, width, height, kind)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { render(app, width, height, kind) } ?: return null
        cache.put(key(app, width, height, kind), bitmap)
        return bitmap
    }

    // The glow colour does not depend on the size of the art, so it is read from one small rendering. [NO_GLOW] marks
    // art without a colour, which is worth remembering too.
    private val glowColors = ConcurrentHashMap<Key, Int>()

    /**
     * The most vivid colour of the app's art as an opaque ARGB value, falling back to its muted ones, or null for art
     * with no colour at all. The colour is worked out once per app and kind of art.
     */
    suspend fun glowColor(
        app: InstalledApp,
        kind: ArtKind,
    ): Int? {
        val key = key(app, GLOW_ART_SIZE, GLOW_ART_SIZE, kind)
        val color =
            glowColors[key] ?: run {
                val bitmap = load(app, GLOW_ART_SIZE, GLOW_ART_SIZE, kind) ?: return null
                val found = withContext(Dispatchers.Default) { vividColor(bitmap) } ?: NO_GLOW
                found.also { glowColors[key] = it }
            }
        return color.takeIf { it != NO_GLOW }
    }

    // A white or grey logo has no vivid colour; like the stock launcher, it then gets a pale grey glow from its muted ones.
    private fun vividColor(bitmap: Bitmap): Int? {
        val palette = Palette.from(bitmap).generate()
        val swatch =
            palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch
                ?: palette.lightMutedSwatch ?: palette.mutedSwatch
        return swatch?.rgb
    }

    private fun key(
        app: InstalledApp,
        width: Int,
        height: Int,
        kind: ArtKind,
    ) = Key(app.packageName, app.lastUpdateTime, width, height, kind)

    private fun render(
        app: InstalledApp,
        width: Int,
        height: Int,
        kind: ArtKind,
    ): Bitmap? {
        val component = ComponentName(app.packageName, app.className)
        val drawable =
            try {
                if (app.hasBanner && kind == ArtKind.Banner) {
                    packageManager.getActivityBanner(component)
                } else {
                    packageManager.getActivityIcon(component)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } ?: return null
        return createBitmap(width, height).applyCanvas {
            if (kind == ArtKind.RoundIcon) {
                drawRoundIcon(drawable, width)
            } else {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(this)
            }
        }
    }

    // Like the stock launcher, the icon fills the circle edge to edge. A flat icon has its corners cut off, and where it
    // has no background of its own, the surface behind the circle shows through.
    private fun Canvas.drawRoundIcon(
        drawable: Drawable,
        size: Int,
    ) {
        val circle = Path().apply { addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW) }
        withClip(circle) {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(this)
        }
    }

    private companion object {
        const val CACHE_BYTES = 8 * 1024 * 1024
        const val GLOW_ART_SIZE = 64

        // A swatch is opaque, so 0, which is transparent black, can never be one.
        const val NO_GLOW = 0
    }
}
