package rs.bonge.stauncher.apps

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class AppRepository(
    private val packageManager: PackageManager,
    private val selfPackage: String,
    packageChanges: PackageChanges,
) {
    private val refreshRequests =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Scans on every collection and after each package change; collecting is what keeps the change callback alive. */
    val apps: Flow<List<InstalledApp>> =
        merge(packageChanges.changes(), refreshRequests)
            .map { scan() }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    fun refresh() {
        refreshRequests.tryEmit(Unit)
    }

    private fun scan(): List<InstalledApp> {
        val candidates =
            query(Intent.CATEGORY_LEANBACK_LAUNCHER, leanback = true) +
                query(Intent.CATEGORY_LAUNCHER, leanback = false)
        return pickLaunchEntries(candidates, selfPackage).mapNotNull(::toInstalledApp)
    }

    private fun query(
        category: String,
        leanback: Boolean,
    ): List<LaunchEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return packageManager
            .queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .map { LaunchEntry(it.activityInfo.packageName, it.activityInfo.name, leanback) }
    }

    // The package can be removed between the query and these lookups.
    private fun toInstalledApp(entry: LaunchEntry): InstalledApp? =
        try {
            val activityInfo =
                packageManager.getActivityInfo(
                    ComponentName(entry.packageName, entry.className),
                    PackageManager.ComponentInfoFlags.of(0),
                )
            val packageInfo = packageManager.getPackageInfo(entry.packageName, PackageManager.PackageInfoFlags.of(0))
            InstalledApp(
                packageName = entry.packageName,
                className = entry.className,
                label = activityInfo.loadLabel(packageManager).toString(),
                leanback = entry.leanback,
                hasBanner = activityInfo.banner != 0 || activityInfo.applicationInfo.banner != 0,
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
}
