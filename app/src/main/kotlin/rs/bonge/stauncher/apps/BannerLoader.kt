package rs.bonge.stauncher.apps

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Renders an app's banner, or its icon when it has no banner, at the size the card needs. */
class BannerLoader(
    private val packageManager: PackageManager,
) {
    // lastUpdateTime makes an updated app load its new banner instead of the cached one.
    private data class Key(
        val packageName: String,
        val lastUpdateTime: Long,
        val width: Int,
        val height: Int,
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
    ): Bitmap? = cache[key(app, width, height)]

    suspend fun load(
        app: InstalledApp,
        width: Int,
        height: Int,
    ): Bitmap? {
        peek(app, width, height)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { render(app, width, height) } ?: return null
        cache.put(key(app, width, height), bitmap)
        return bitmap
    }

    private fun key(
        app: InstalledApp,
        width: Int,
        height: Int,
    ) = Key(app.packageName, app.lastUpdateTime, width, height)

    private fun render(
        app: InstalledApp,
        width: Int,
        height: Int,
    ): Bitmap? {
        val component = ComponentName(app.packageName, app.className)
        val drawable =
            try {
                if (app.hasBanner) {
                    packageManager.getActivityBanner(component)
                } else {
                    packageManager.getActivityIcon(component)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } ?: return null
        return createBitmap(width, height).applyCanvas {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }
    }

    private companion object {
        const val CACHE_BYTES = 8 * 1024 * 1024
    }
}
