package rs.bonge.stauncher.home

import rs.bonge.stauncher.apps.InstalledApp

/**
 * [apps] are the visible apps in the order to draw; while an app is being moved this is the preview order. [loaded]
 * tells an empty row from one that has not been read yet.
 */
data class HomeUiState(
    val apps: List<InstalledApp> = emptyList(),
    val loaded: Boolean = false,
    val mode: HomeMode = HomeMode.Browsing,
)

sealed interface HomeMode {
    data object Browsing : HomeMode

    /** The actions panel is open for [app]. */
    data class Actions(
        val app: InstalledApp,
    ) : HomeMode

    /** [app] is picked up and being moved along the row. */
    data class Moving(
        val app: InstalledApp,
    ) : HomeMode
}
