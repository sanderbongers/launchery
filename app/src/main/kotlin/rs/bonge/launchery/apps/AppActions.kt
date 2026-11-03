package rs.bonge.launchery.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

private const val SYSTEM_PACKAGE = "android"

/** Every action returns false when the system cannot carry it out, so the caller can say so. */
class AppActions(
    private val context: Context,
) {
    /** Fails when the activity can no longer be started, for example because the app was removed or disabled. */
    fun launch(app: InstalledApp): Boolean {
        val category = if (app.leanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER
        val intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setComponent(ComponentName(app.packageName, app.className))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(intent)
    }

    fun showAppInfo(app: InstalledApp): Boolean =
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(app)))

    /** The system asks for confirmation itself. */
    fun uninstall(app: InstalledApp): Boolean = start(Intent(Intent.ACTION_DELETE, packageUri(app)))

    fun openSystemSettings(): Boolean = start(Intent(Settings.ACTION_SETTINGS))

    /** The package that handles [openSystemSettings], or null when the system names no single handler. */
    fun systemSettingsPackage(): String? =
        context.packageManager
            .resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
            ?.takeIf { it != SYSTEM_PACKAGE } // "android" is the chooser, which stands for several handlers

    private fun packageUri(app: InstalledApp) = Uri.fromParts("package", app.packageName, null)

    private fun start(intent: Intent): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
}
