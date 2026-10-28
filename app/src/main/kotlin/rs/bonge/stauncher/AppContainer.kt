package rs.bonge.stauncher

import android.app.Application
import android.content.pm.LauncherApps
import rs.bonge.stauncher.apps.AppActions
import rs.bonge.stauncher.apps.AppRepository
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.PackageChanges

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
    val bannerLoader = BannerLoader(application.packageManager)
    val appActions = AppActions(application)
}
