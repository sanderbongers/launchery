package rs.bonge.stauncher.settings

import kotlinx.serialization.Serializable

/** How the home row shows the apps: as round icons, or as the banners that apps ship for a TV launcher. */
@Serializable
enum class AppStyle {
    RoundIcons,
    Banners,
    ;

    fun next(): AppStyle = entries[(ordinal + 1) % entries.size]
}

/** What the user chose in the settings, apart from how the apps are arranged. */
@Serializable
data class UserSettings(
    val appStyle: AppStyle = AppStyle.RoundIcons,
)
