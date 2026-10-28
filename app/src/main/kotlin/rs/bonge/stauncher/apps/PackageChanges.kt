package rs.bonge.stauncher.apps

import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class PackageChanges(
    private val launcherApps: LauncherApps,
) {
    /**
     * Emits once the callback is registered, so a scan that follows cannot miss a change, and again on every package
     * change. The callback is registered only while the flow is collected.
     */
    fun changes(): Flow<Unit> =
        callbackFlow {
            val callback =
                object : LauncherApps.Callback() {
                    override fun onPackageRemoved(
                        packageName: String,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackageAdded(
                        packageName: String,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackageChanged(
                        packageName: String,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackagesAvailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackagesUnavailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean,
                    ) {
                        trySend(Unit)
                    }
                }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            trySend(Unit)
            awaitClose { launcherApps.unregisterCallback(callback) }
        }.conflate()
}
