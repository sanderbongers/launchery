package rs.bonge.launchery

import android.app.Application
import android.content.pm.LauncherApps
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rs.bonge.launchery.apps.AppActions
import rs.bonge.launchery.apps.AppRepository
import rs.bonge.launchery.apps.BannerLoader
import rs.bonge.launchery.apps.PackageChanges
import rs.bonge.launchery.layout.Arranged
import rs.bonge.launchery.layout.Layout
import rs.bonge.launchery.layout.LayoutRepository
import rs.bonge.launchery.layout.arrangeApps
import rs.bonge.launchery.settings.SettingsRepository
import rs.bonge.launchery.settings.UserSettings
import rs.bonge.launchery.storage.createJsonStore
import rs.bonge.launchery.time.Clock

/** Builds the app-wide objects once; constructor injection by hand keeps the dependencies visible. */
class AppContainer(
    application: Application,
) {
    val appRepository =
        AppRepository(
            packageManager = application.packageManager,
            selfPackage = application.packageName,
            packageChanges = PackageChanges(application.getSystemService(LauncherApps::class.java)),
        )
    val appActions = AppActions(application)
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The settings cog and the settings sheet lead to the system Settings, so a new layout starts without its tile.
    val layoutRepository =
        LayoutRepository(
            createJsonStore(application.dataStoreFile("layout.json"), storeScope, Layout.serializer()) {
                Layout(hidden = setOfNotNull(appActions.systemSettingsPackage()))
            },
        )
    val settingsRepository =
        SettingsRepository(
            createJsonStore(application.dataStoreFile("settings.json"), storeScope, UserSettings.serializer()) {
                UserSettings()
            },
        )

    /** Emits only once both the installed apps and the saved layout are known, so the order never flashes. */
    val arrangedApps: Flow<Arranged> = combine(appRepository.apps, layoutRepository.layout, ::arrangeApps)
    val bannerLoader = BannerLoader(application.packageManager)
    val clock = Clock(application)
}
