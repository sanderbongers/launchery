package rs.bonge.launchery.home

import rs.bonge.launchery.apps.InstalledApp
import rs.bonge.launchery.settings.AppStyle

/**
 * [apps] are the visible apps in the order to draw; while an app is being moved this is the preview order. [hidden] are
 * the apps the user hid, by name. [loaded] tells an empty row from one that has not been read yet. [homeRequests]
 * counts the times the user went home, which the row answers by focusing its first app; the number matters only when
 * it changes.
 */
data class HomeUiState(
    val apps: List<InstalledApp> = emptyList(),
    val loaded: Boolean = false,
    val mode: HomeMode = HomeMode.Browsing,
    val homeRequests: Int = 0,
    val appStyle: AppStyle = AppStyle.RoundIcons,
    val hidden: List<InstalledApp> = emptyList(),
)

sealed interface HomeMode {
    data object Browsing : HomeMode

    /** The settings sheet is open. */
    data object Settings : HomeMode

    /** The actions screen is open for [app]. */
    data class Actions(
        val app: InstalledApp,
    ) : HomeMode

    /** [app] is picked up and being moved along the row. */
    data class Moving(
        val app: InstalledApp,
    ) : HomeMode
}
