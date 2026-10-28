package rs.bonge.stauncher.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class AppActions(
    private val context: Context,
) {
    /** Returns false when the activity can no longer be started, for example because the app was removed or disabled. */
    fun launch(app: InstalledApp): Boolean {
        val category = if (app.leanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER
        val intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setComponent(ComponentName(app.packageName, app.className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
