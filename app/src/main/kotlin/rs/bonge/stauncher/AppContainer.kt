package rs.bonge.stauncher

import android.app.Application
import android.content.pm.LauncherApps
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import rs.bonge.stauncher.apps.AppActions
import rs.bonge.stauncher.apps.AppRepository
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.PackageChanges
import rs.bonge.stauncher.layout.Arranged
import rs.bonge.stauncher.layout.Layout
import rs.bonge.stauncher.layout.LayoutRepository
import rs.bonge.stauncher.layout.arrangeApps
import rs.bonge.stauncher.layout.createLayoutStore
import rs.bonge.stauncher.settings.SettingsRepository
import rs.bonge.stauncher.settings.UserSettings
import rs.bonge.stauncher.storage.createJsonStore
import rs.bonge.stauncher.time.Clock

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

    // The settings cog and the settings screen lead to the system Settings, so a new layout starts without its tile.
    val layoutRepository =
        LayoutRepository(
            createLayoutStore(
                file = application.dataStoreFile("layout.json"),
                scope = storeScope,
                defaultLayout = { Layout(hidden = setOfNotNull(appActions.systemSettingsPackage())) },
            ),
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
